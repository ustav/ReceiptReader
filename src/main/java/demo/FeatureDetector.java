package demo;

import org.opencv.core.*;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.highgui.HighGui;
import org.opencv.xfeatures2d.SURF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.features2d.Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS;
import static org.opencv.features2d.Features2d.drawMatches;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

public class FeatureDetector {

    public int getRotation() {
        String logoFilename = "pics/jumbo_logo1.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);

        String receiptFilename = "pics/receipt5.jpg";
        Mat receipt = imread(receiptFilename, IMREAD_REDUCED_GRAYSCALE_8);
        imshow("receipt", receipt);

//        Leptonica leptInstance = Leptonica.INSTANCE;
//        Pix pix = leptInstance.pixRead(receiptFilename);
//        int angle = detectOrientation(pix);
//        System.out.println(angle);

        return 0;
    }

    private void computeSkew(Mat scene) {
        Size size = scene.size();

        Mat negative = new Mat();
        bitwise_not(scene, negative);

        Mat binary = new Mat();
        threshold(negative, binary, 0, 255,
                THRESH_BINARY | THRESH_OTSU);

        Mat lines = new Mat();

        HoughLinesP(binary, lines, 1, PI / 180, 100, size.width / 4.f, 20);

        Mat disp_lines = new Mat(size, CV_8UC1, new Scalar(0, 0, 0));
        double angle = 0.;
        int nb_lines = lines.rows();
        for (int i = 0; i < nb_lines / 10; ++i) {
            double[] line = lines.get(i, 0);
            line(disp_lines, new Point(line[0], line[1]), new Point(line[2], line[3]), new Scalar(255, 0, 255), 2);
            angle += atan2(line[3] - line[1], line[2] - line[0]);
        }
        angle /= nb_lines; // mean angle, in radians.

        System.out.println(angle * 180 / PI);

        imshow("lines", scene);
    }

    public void matchFeatures() {
        String logoFilename = "pics/jumbo_logo1.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);
//        imshow("logo", logo);

        String receiptFilename = "pics/receipt6.jpg";
        Mat receipt = imread(receiptFilename, IMREAD_REDUCED_GRAYSCALE_4);
        rotate(receipt, receipt, ROTATE_180);
//        imshow("receipt", receipt);

        SURF surf = SURF.create();

        Mat logoMask = new Mat();
        MatOfKeyPoint logoKeypoints = new MatOfKeyPoint();
        Mat logoDescriptors = new Mat();
        surf.detectAndCompute(logo, logoMask, logoKeypoints, logoDescriptors);

        Mat receiptMask = new Mat();
        MatOfKeyPoint receiptKeypoints = new MatOfKeyPoint();
        Mat receiptDescriptors = new Mat();
        surf.detectAndCompute(receipt, receiptMask, receiptKeypoints, receiptDescriptors);

        // Match descriptors.
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(logoDescriptors, receiptDescriptors, knnMatches, 2);

        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 0.7f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (MatOfDMatch knnMatch : knnMatches) {
            if (knnMatch.rows() > 1) {
                DMatch[] matches = knnMatch.toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        drawMatches(logo, logoKeypoints, receipt, receiptKeypoints, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
        //-- Show detected matches
        HighGui.imshow("Good Matches", imgMatches);
    }
}
