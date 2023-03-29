package org.example;
import org.example.ImageRecog;

//Imports required for S3
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.util.List;
import java.util.concurrent.TimeUnit;

//Imports required for sqs
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

public class Handler {
  private final S3Client s3Client;

  public Handler() {
    s3Client = DependencyFactory.s3Client();
  }

  public void sendRequest() {
    String bucketName = "njit-cs-643";
    s3Access(s3Client, bucketName);
    s3Client.close();
    System.out.println();
    System.out.println("Connection closed");
    System.exit(1);
  }

  // S3 bucket initialization and accessing
  public static void s3Access(S3Client s3Client, String bucketName) {
    ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
    Region region = Region.US_EAST_1;
    s3Client = S3Client.builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build();

    listBucketObjects(s3Client, bucketName);
  }

  // Method to list the objects in a s3 bucket
  public static void listBucketObjects(S3Client s3Client, String bucketName) {

    try {
      ListObjectsRequest listObjects = ListObjectsRequest
          .builder()
          .bucket(bucketName)
          .build();

      ImageRecog ir = new ImageRecog();
      SqsClient sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build();

      ListObjectsResponse res = s3Client.listObjects(listObjects);
      List<S3Object> objects = res.contents();
      int iter = 1;
      for (S3Object myValue : objects) {
        System.out.print("\n The name of the key is " + myValue.key());
        System.out.print("\n The object is " + calKb(myValue.size()) + " KBs");
        System.out.println("\n The owner is " + myValue.owner());
        boolean iscar = false;
        try{
          // Calling AWS Rekognition for the image
          iscar = ir.DetectLables(bucketName, myValue.key());
          System.out.println("id"+iter);
          if(iscar){
            TimeUnit.SECONDS.sleep(1);
            System.out.println("Sending "+myValue.key()+"\n");
            
            // SQS message to send image name identified as car
            sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl("https://sqs.us-east-1.amazonaws.com/006805423820/A-B.fifo")
                .messageGroupId("id"+iter)
                .messageBody(myValue.key())
                .build());
            ++iter;
          }
        } catch(Exception ex){
          System.out.println(ex.getMessage());
        }
        //detectImageLabels(rekClient, myValue.);
      }
      // SQS message to close the connection at EC2 B instance
      sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl("https://sqs.us-east-1.amazonaws.com/006805423820/A-B.fifo")
                .messageGroupId("id"+iter)
                .messageBody("-1")
                .build());
      sqsClient.close();
    } catch (S3Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }
  }

  // convert bytes to kbs.
  private static long calKb(Long val) {
    return val / 1024;
  }
}
