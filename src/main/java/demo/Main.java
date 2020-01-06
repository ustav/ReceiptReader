package demo;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;

import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.waitKey;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.loadLibrary("opencv_java420");

        String filename = "pics/receipt4.jpg";
        Mat prepared = new ImagePreparator().getPreparedImage(filename);
        imshow("prepared", prepared);

//        new FeatureDetector().getRotation();
        waitKey();

        //==============================
        HighGui.destroyAllWindows();
        System.exit(0);
    }
}
