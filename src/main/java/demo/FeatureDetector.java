package demo;

import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.xfeatures2d.SURF;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static org.opencv.calib3d.Calib3d.RANSAC;
import static org.opencv.calib3d.Calib3d.findHomography;
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
        rotate(receipt, receipt, ROTATE_90_COUNTERCLOCKWISE);
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
        float ratioThresh = 0.75f;
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

        //-- Localize the object
        List<Point> obj = new ArrayList<>();
        List<Point> scene = new ArrayList<>();
        List<KeyPoint> listOfKeypointsObject = logoKeypoints.toList();
        List<KeyPoint> listOfKeypointsScene = receiptKeypoints.toList();
        for (int i = 0; i < listOfGoodMatches.size(); i++) {
            //-- Get the keypoints from the good matches
            obj.add(listOfKeypointsObject.get(listOfGoodMatches.get(i).queryIdx).pt);
            scene.add(listOfKeypointsScene.get(listOfGoodMatches.get(i).trainIdx).pt);
        }
        MatOfPoint2f objMat = new MatOfPoint2f();
        MatOfPoint2f sceneMat = new MatOfPoint2f();
        objMat.fromList(obj);
        sceneMat.fromList(scene);
        double ransacReprojThreshold = 3.0;
        Mat H = findHomography(objMat, sceneMat, RANSAC, ransacReprojThreshold);

        //-- Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2);
        Mat sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = logo.cols();
        objCornersData[3] = 0;
        objCornersData[4] = logo.cols();
        objCornersData[5] = logo.rows();
        objCornersData[6] = 0;
        objCornersData[7] = logo.rows();
        objCorners.put(0, 0, objCornersData);
        perspectiveTransform(objCorners, sceneCorners, H);
        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(imgMatches, new Point(sceneCornersData[0] + logo.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + logo.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[2] + logo.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + logo.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[4] + logo.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + logo.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[6] + logo.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + logo.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 4);


        float objVecX = objCornersData[2] - objCornersData[0];
        float objVecY = objCornersData[3] - objCornersData[1];

        float sceneVecX = sceneCornersData[2] - sceneCornersData[0];
        float sceneVecY = sceneCornersData[3] - sceneCornersData[1];

        float dot = objVecX * sceneVecX + objVecY * sceneVecY;
        float det = objVecX * sceneVecY - objVecY * sceneVecX;
        double angle = atan2(det, dot);

        System.out.println("angle: " + angle * 180 / PI);

        //-- Show detected matches
        HighGui.imshow("Good Matches", imgMatches);
    }
}
