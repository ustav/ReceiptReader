package demo;

import org.opencv.core.Mat;

import static org.opencv.highgui.HighGui.*;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_COLOR;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class Main {

    public static void main(String[] args) {
        System.loadLibrary("opencv_java420");
        System.loadLibrary("lept");

        String filename;
        if (args != null && args.length > 0) {
            filename = args[0];
        } else {
            System.out.println("{ \"error\": \"File path is not provided.\" }");
            return;
        }

        Mat source = imread(filename, IMREAD_COLOR);

        if (source.empty()) {
            System.out.println("{ \"error\": \"Can not read file.\" }");
            return;
        }

        String result = new JumboReader().readReceipt(source);

        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("{ \"error\": \"No results found\" }");
        }

        if (windows.size() > 0) {
            waitKey();
            destroyAllWindows();
        }

        System.exit(0);
    }
}
