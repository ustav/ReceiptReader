package demo;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import static org.opencv.highgui.HighGui.*;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.threshold;

public class Main {

    public static void main(String[] args) {
        System.loadLibrary("opencv_java420");

//        String filename = "pics/jumbo2.jpg";
//        Mat prepared = new ImagePreparator().getPreparedImage(filename);

//        Core.rotate(prepared, prepared, Core.ROTATE_90_CLOCKWISE);

//        imshow("prepared", prepared);
//        resizeWindow("prepared", 1200, 1200);

//        new TextExtractor().extractText(prepared) ;

//        new  FeatureDetector().matchFeatures();

//        new EASTTextDetector().detect();

        String filename;
        if (args != null && args.length > 0) {
            filename = args[0];
        } else {
            filename = "pics/jumbo2.jpg";
        }

        Mat source = imread(filename, IMREAD_COLOR);

        String result = new JumboReader().readReceipt(source);

        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("{}");
        }

        //==============================
        if (windows.size() > 0) {
            waitKey();
            destroyAllWindows();
        }
        System.exit(0);
    }
}
