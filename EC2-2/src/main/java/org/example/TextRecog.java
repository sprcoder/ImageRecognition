package org.example;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.TextDetection;
import java.util.List;
import java.util.ArrayList;


public class TextRecog {
  // Method that returns a list of text detected in a car image
  public List<String> DetectText(String bucket, String photo) throws Exception {
    boolean result1=false;
    List<String> detectedText = new ArrayList<String>();
    AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

    DetectTextRequest request = new DetectTextRequest()
              .withImage(new Image()
              .withS3Object(new S3Object()
              .withName(photo)
              .withBucket(bucket)));
    try {
        DetectTextResult result = rekognitionClient.detectText(request);
        List<TextDetection> textDetections = result.getTextDetections();

        System.out.println("Detected lines and words for " + photo);
        for (TextDetection text: textDetections) {
          detectedText.add(text.getDetectedText());
          System.out.println("Detected: " + text.getDetectedText());
          System.out.println("Confidence: " + text.getConfidence().toString());
          System.out.println("Id : " + text.getId());
          System.out.println("Parent Id: " + text.getParentId());
          System.out.println("Type: " + text.getType());
          System.out.println();
        }
    } catch(AmazonRekognitionException e) {
        e.printStackTrace();
    }
    return detectedText;
  }
}
