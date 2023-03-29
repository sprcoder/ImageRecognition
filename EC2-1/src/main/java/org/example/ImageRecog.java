package org.example;

import java.util.List;

// Import for AWS Rekognition
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Instance;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.Parent;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;

public class ImageRecog {

  // Method to detect label for the image
  public boolean DetectLables(String bucket, String photo) throws Exception {
    boolean result1=false;
    AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

    DetectLabelsRequest request = new DetectLabelsRequest()
        .withImage(new Image().withS3Object(new S3Object().withName(photo).withBucket(bucket)))
        .withMaxLabels(10).withMinConfidence(75F);

    try {
      DetectLabelsResult result = rekognitionClient.detectLabels(request);
      List<Label> labels = result.getLabels();

      System.out.println("Detected labels for " + photo + "\n");
      int count = 0;
      for (Label label : labels) {
        if (label.getName().equals("Car")){
          System.out.println("Label: " + label.getName());
          System.out.println("Confidence: " + label.getConfidence().toString() + "\n");
          if(label.getConfidence() >= 90){
            count++;
            result1=true;
          }
          System.out.println("--------------------");
          System.out.println();
        }
      }
      if(count == 0){
        System.out.println("Image is not a car");
      }
    } catch (AmazonRekognitionException e) {
      e.printStackTrace();
    }
    return result1;
  }
}
