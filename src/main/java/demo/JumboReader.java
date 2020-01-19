package demo;

import org.opencv.core.Mat;

import static org.opencv.core.Core.*;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgcodecs.Imgcodecs.imread;

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

    public String readReceipt(Mat input) {
        Mat preparedImage = new ImagePreparator().getPreparedImage(input);
        String logoFilename = "data/jumbo_logo.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);
        if (logo.empty()) {
            return "{ \"error\": \"Logo not found\" }";
        }

        FeatureDetectionResult result = new FeatureDetector().matchFeatures(logo, preparedImage);

        if (result.matchFound) {
            Mat rotated = rotateImageIfNeeded(preparedImage, result.angle);
            return new TextExtractor().extractText(rotated);
        }

        return "{ \"error\": \"No results found\" }";
    }
}
