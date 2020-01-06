package demo;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import static org.opencv.highgui.HighGui.*;
import static org.opencv.imgproc.Imgproc.threshold;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary("opencv_java420");

//        String filename = "pics/receipt5.jpg";
//        Mat prepared = new ImagePreparator().getPreparedImage(filename);

//        Core.rotate(prepared, prepared, Core.ROTATE_90_CLOCKWISE);

//        imshow("prepared", prepared);
//        resizeWindow("prepared", 1200, 1200);

//        new TextExtractor().extractText(prepared);

        new  FeatureDetector().matchFeatures();

        //==============================
        if (windows.size() > 0) {
            waitKey();
            destroyAllWindows();
        }
        System.exit(0);
    }
}
