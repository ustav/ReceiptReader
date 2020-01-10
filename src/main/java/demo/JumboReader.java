package demo;

import org.opencv.core.Mat;

import static org.opencv.core.Core.*;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.imgcodecs.Imgcodecs.*;

public class JumboReader {

    private Mat rotateIfNeeded(Mat image, double angle) {
        Mat rotated = image.clone();
        if (angle > 45 && angle <= 135) {
            rotate(image, rotated, ROTATE_90_COUNTERCLOCKWISE);
        } else if ((angle > 135 && angle <= 225) || ((angle < -135 && angle >= -225))) {
            rotate(image, rotated, ROTATE_180);
        } else if (angle < -45 && angle >= -135) {
            rotate(image, rotated, ROTATE_90_CLOCKWISE);
        }

        return rotated;
    }

    public String readReceipt() {
        String filename = "pics/jumbo2.jpg";
        Mat prepared = new ImagePreparator().getPreparedImage(filename);
        rotate(prepared, prepared, ROTATE_180);

        String logoFilename = "pics/jumbo_logo1.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);
//        imshow("logo", logo);

        String receiptFilename = "pics/jumbo2.jpg";
        Mat receipt = imread(receiptFilename, IMREAD_REDUCED_GRAYSCALE_4);
//        imshow("receipt", receipt);


        FeatureDetectionResult result = new FeatureDetector().matchFeatures(logo, prepared);

        if (result.matchFound) {
            Mat rotated = rotateIfNeeded(prepared, result.angle);
            imshow("rotated", rotated);
            return new TextExtractor().extractText(rotated);
        }

        return null;
    }
}
