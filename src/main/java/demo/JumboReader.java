package demo;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.resizeWindow;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

public class JumboReader {

    private Mat rotateImageIfNeeded(Mat image, double angle) {
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
        String receiptFilename = "pics/jumbo1.jpg";
        Mat source = imread(receiptFilename, IMREAD_COLOR);
        Mat preparedImage = new ImagePreparator().getPreparedImage(source);

        String logoFilename = "pics/jumbo_logo1.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);
//        imshow("logo", logo);

        FeatureDetectionResult result = new FeatureDetector().matchFeatures(logo, preparedImage);

        if (result.matchFound) {
            Mat rotated = rotateImageIfNeeded(preparedImage, result.angle);
            String text = new TextExtractor().extractText(rotated);
            imshow("rotated", rotated);
            resizeWindow("rotated", 1200, 1200);
        }

        return null;
    }
}
