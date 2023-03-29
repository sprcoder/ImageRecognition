# AWS_Simple_ImageRecognition

## EC2 Instance Setup

1. Created 2 EC2 instances in AWS, EC2-A & EC2-B
2. Each Instace would have the following configuration while creating.

- OS -> Amazon Linux 2 AMI 64 bit(x86)
<img width="730" alt="image" src="https://user-images.githubusercontent.com/120745648/223907363-399bc62d-1b2d-48d4-899c-d551c8eadb0c.png">

- Intance Type -> Selected the t2.micro that has free tier eligibility
<img width="751" alt="image" src="https://user-images.githubusercontent.com/120745648/223907527-41aff11a-e379-4cd5-bfb4-7b759e8253bb.png">

- Key Pair -> Created a new pair for using with my computer as MyMacConnect
<img width="772" alt="image" src="https://user-images.githubusercontent.com/120745648/223907726-7d845714-bb63-47dd-a58c-4fa579001207.png">

- Network Settings -> Created a setting with default values for ssh, http, https

- Used the same setting for the 2 EC2 instances

- Configure Storage -> Used the default free tier storage that was available for both the instances
<img width="742" alt="image" src="https://user-images.githubusercontent.com/120745648/223908317-ceaf21c8-da88-4706-a1e3-2103b6cefbae.png">

- Launch Instance -> With the below configurations, launched the EC2 instance
<img width="355" alt="image" src="https://user-images.githubusercontent.com/120745648/223908430-f6e5b396-a5ef-4134-9cd5-4ec9ead9c37b.png">

## Running the Application

1. Requires SQS setup before starting the application
  - Create a FIFO Queue for the instances to communicate
<img width="1109" alt="image" src="https://user-images.githubusercontent.com/120745648/223909044-0b1773c8-7039-4d44-b6a2-9c17fa4c6511.png">

2. EC2-A Instance 
  - Connect to the EC2-A Instance using SSH
  - In the instance run `aws configure`
    - This would ask for the access key, id and session details that are available in the Learners Lab, AWS Details.
    <img width="293" alt="image" src="https://user-images.githubusercontent.com/120745648/223922262-ef35d5b5-7d0d-4cdc-8da4-5aee9374ff89.png">

  - Once AWS is configured install java, I have used `jdk-11.0.16.1_linux-x64_bin.rpm`
  - After java installation, install maven, I have used `apache-maven-3.9.0`. Provided maven bin path to `$PATH` variable in linux to finalize installation.
  - Create a maven project with the following code
  ```sh
     mvn archetype:generate \
    -DarchetypeGroupId=software.amazon.awssdk \
    -DarchetypeArtifactId=archetype-app-quickstart \
    -DarchetypeVersion=2.18.16
  ```
  - Use the following configuration while creating the maven project
  <img width="243" alt="image" src="https://user-images.githubusercontent.com/120745648/223910850-f9dea197-ffc6-44ba-a669-7305cde24d7a.png">
  - Below is the structure of the generated project

  ```
  ├── src
  │   ├── main
  │   │   ├── java
  │   │   │   └── package
  │   │   │       ├── App.java
  │   │   │       ├── DependencyFactory.java
  │   │   │       └── Handler.java
  │   │   └── resources
  │   │       └── simplelogger.properties
  │   └── test
  │       └── java
  │           └── package
  │               └── HandlerTest.java
  ```

  - `App.java`: main entry of the application
  - `DependencyFactory.java`: creates the SDK client
  - `Handler.java`: you can invoke the api calls using the SDK client here.

3. `Handler.java` holds the main code for my application, which are the following
  - S3 bucket initialization and accessing the `S3Object`. Used the bucket provided in the assignment `njit-cs-643`.
  - Passed the image value from `S3Object` to AWS Rekognition method that would identify the label of the image.
  - Image index that is identified as car with confidence more than 90 is sent as SQS message using the `sqsClient` & `sendMessage` to the queueurl of `A-B.fifo`.
4. `ImageRecog.java` was created to hold the AWS Rekognition method.
  - This method creates a `rekognitionClient` and use the `DetectLabelsRequest` object to identify the labels in the image.
5. Run the program by cleaning the mvn package and executing the package:
  ```sh
  mvn clean package
  mvn exec:java -Dexec.mainClass="org.example.App"
  ```
  - Please refer to `output_a.txt` in `ec2_a_imgrecog.zip/imgRecog/` for the results of execution.

6. EC2-B Instance 
  - For the initial setup of the instance follow the step 2 
  - `Handler.java` holds the main code for EC2-B instance application, which are the following
    - Checks for SQS messages from the FIFO queue `A-B.fifo` using `ReceiveMessageRequest` & `sqsClient`. Tries to get all messages with 20 attemps(As a exit condition, if no message is received from SQS)
    - Each request from a SQS message waits for 5 seconds to get the message.
    - Once message is received, index from the body of the message is retrieved. And the message is deleted using `DeleteMessageRequest`.
    - Passing the index of the image to AWS Rekognition method with the bucketname would retieve a list of texts that is found in the image.
    - The image information along with the text identified from the image is written into a file.
  - `TextRecog.java` was created to hold the AWS Rekognition method.
    - This method would get the image name, bucket name and using the `rekognitionClient` & `DetectTextRequest` identifies the text present in the image.
  - Follow the step 5 to run the application
    - Please refer to `output_b.txt` in `ec2_b_imagerecog_textrecog.zip/imageRecog/` for the results of execution.

