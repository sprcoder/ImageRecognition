package org.example;
import org.example.TextRecog;

//Imports required for S3
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.util.List;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

public class Handler {
  private final S3Client s3Client;
  public static List<String> images = new ArrayList<String>();
  public static Hashtable<String, List<String>> imgText = new Hashtable<>();
  public static int count = 20;

  public Handler() {
    s3Client = DependencyFactory.s3Client();
  }

  public void sendRequest() {
    String bucketName = "njit-cs-643";
    boolean keepGettingMessage = true;
    SqsClient sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();
    try{
      // Checking SQS for messages. Tried to get all messages with 20 attemps
      // Each request from a SQS message waits for 5 seconds to get the message.
      while(keepGettingMessage){
        keepGettingMessage = getMessages(sqsClient, bucketName);
      }
    } catch(Exception e){
      System.out.println(e.getMessage());
    }
    sqsClient.close();
    String fileout = "";
    System.out.println(images.toString()+"\n");
    for(int i = 0; i < images.size(); i++){
      List<String> printer = new ArrayList<String>();
      printer = imgText.get(images.get(i));
      if(printer.size() > 0){
        System.out.print(images.get(i)+": ");
        fileout += images.get(i)+": ";
        for (int j = 0; j < printer.size(); j++){
          System.out.print(printer.get(j)+" | ");
          fileout += printer.get(j)+"|";
        }
        fileout += "\n";
        System.out.println("\n");
      }
    }

    //Writing the results to a text file
    try {
      FileWriter myWriter = new FileWriter("Result.txt");
      myWriter.write(fileout);
      myWriter.close();
      System.out.println("Successfully wrote to the file.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }

    // Closing the connections
    s3Client.close();
    System.out.println();
    System.out.println("Connection closed");
    System.exit(1);
  }

  // Method to get messages from SQS and check for available texts using AWS Rekognition
  public static boolean getMessages(SqsClient sqsClient, String bucket){
    // Requests for 1 message at a time and waits for 5 seconds until the message is available in SQS
    ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl("https://sqs.us-east-1.amazonaws.com/006805423820/A-B.fifo")
                .maxNumberOfMessages(1)
                .waitTimeSeconds(5)
                .build();
    List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
    if (count == 0)
      return false;
    count--;
    boolean isContinue = true;

    // Looks into the messages
    for (Message message : messages){
      String image = message.body();
      // Adds images to a image list 
      images.add(image);

      // Once image name is retrieved from the message, delete the message
      DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl("https://sqs.us-east-1.amazonaws.com/006805423820/A-B.fifo")
                .receiptHandle(message.receiptHandle())
                .build();
      sqsClient.deleteMessage(deleteMessageRequest);
      if(image.equals("-1")){
        isContinue = false;
        images.remove("-1");
        break;
      }
      TextRecog tr = new TextRecog();
      try{
        // In imgText dictionary, for each image key, get the list of text identified by AWS Rekognition
        imgText.put(image, tr.DetectText(bucket, image));
        TimeUnit.SECONDS.sleep(1);
      } catch(Exception e){
        System.out.println(e.getMessage());
      }
    }
    System.out.println(images.toString());
    System.out.println(imgText);
    if (!isContinue)
      return false;
    return true;
  }
}
