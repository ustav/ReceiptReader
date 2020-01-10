package demo;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.HighGui;
import org.opencv.xfeatures2d.SURF;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static org.opencv.calib3d.Calib3d.RANSAC;
import static org.opencv.core.Core.perspectiveTransform;
import static org.opencv.features2d.Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS;
import static org.opencv.highgui.HighGui.resizeWindow;
import static org.opencv.imgproc.Imgproc.line;

public class FeatureDetector {

    private List<DMatch> filterMatches(List<MatOfDMatch> knnMatches) {
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
        return listOfGoodMatches;
    }

    public MatchResult findMatches(Mat object, Mat scene) {
        SURF surf = SURF.create();

        Mat objectMask = new Mat();
        MatOfKeyPoint objectKeypoints = new MatOfKeyPoint();
        Mat objectDescriptors = new Mat();
        surf.detectAndCompute(object, objectMask, objectKeypoints, objectDescriptors);

        Mat sceneMask = new Mat();
        MatOfKeyPoint sceneKeypoints = new MatOfKeyPoint();
        Mat sceneDescriptors = new Mat();
        surf.detectAndCompute(scene, sceneMask, sceneKeypoints, sceneDescriptors);

        // Match descriptors.
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(objectDescriptors, sceneDescriptors, knnMatches, 2);

        List<DMatch> listOfGoodMatches = filterMatches(knnMatches);
        return new MatchResult(listOfGoodMatches, objectKeypoints, sceneKeypoints);
    }

    private void drawMatches(Mat object, Mat scene, Mat destination, MatchResult matchResult) {
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(matchResult.matches);
        //-- Draw matches
        Features2d.drawMatches(object, matchResult.objectKeypoints, scene, matchResult.sceneKeypoints, goodMatches, destination, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);
    }

    private Mat findHomography(MatchResult matchResult ) {
        //-- Localize the object
        List<Point> objectPoints = new ArrayList<>();
        List<Point> scenePoints = new ArrayList<>();
        List<KeyPoint> objectKeypoints = matchResult.objectKeypoints.toList();
        List<KeyPoint> sceneKeypoints = matchResult.sceneKeypoints.toList();
        for (DMatch goodMatch : matchResult.matches) {
            //-- Get the keypoints from the good matches
            objectPoints.add(objectKeypoints.get(goodMatch.queryIdx).pt);
            scenePoints.add(sceneKeypoints.get(goodMatch.trainIdx).pt);
        }
        MatOfPoint2f objMat = new MatOfPoint2f();
        MatOfPoint2f sceneMat = new MatOfPoint2f();
        objMat.fromList(objectPoints);
        sceneMat.fromList(scenePoints);
        double ransacReprojThreshold = 3.0;
        return Calib3d.findHomography(objMat, sceneMat, RANSAC, ransacReprojThreshold);
    }

    private Point getVectorFromCornersData(float[] cornersData) {
        double vectorX = cornersData[2] - cornersData[0];
        double vectorY = cornersData[3] - cornersData[1];
        return new Point(vectorX, vectorY);
    }

    private double calculateAngleBetweenVectors(Point vectorA, Point vectorB) {
        double dot = vectorA.x * vectorB.x + vectorA.y * vectorB.y;
        double det = vectorA.x * vectorB.y - vectorA.y * vectorB.x;
        return atan2(det, dot) * 180 / PI;
    }

    private void drawObjectOutline(Mat destination, float[] sceneCornersData, int sceneWidth) {
        Scalar lineColor = new Scalar(0, 255, 0);
        int lineThickness = 4;

        //-- Draw lines between the corners (the mapped object in the scene )
        line(destination, new Point(sceneCornersData[0] + sceneWidth, sceneCornersData[1]), new Point(sceneCornersData[2] + sceneWidth, sceneCornersData[3]), lineColor, lineThickness);
        line(destination, new Point(sceneCornersData[2] + sceneWidth, sceneCornersData[3]), new Point(sceneCornersData[4] + sceneWidth, sceneCornersData[5]), lineColor, lineThickness);
        line(destination, new Point(sceneCornersData[4] + sceneWidth, sceneCornersData[5]), new Point(sceneCornersData[6] + sceneWidth, sceneCornersData[7]), lineColor, lineThickness);
        line(destination, new Point(sceneCornersData[6] + sceneWidth, sceneCornersData[7]), new Point(sceneCornersData[0] + sceneWidth, sceneCornersData[1]), lineColor, lineThickness);
    }

    private Mat getObjectCorners(Mat object) {
        //-- Get the corners from the the object to be "detected"
        Mat objectCorners = new Mat(4, 1, CvType.CV_32FC2);
        float[] objectCornersData = new float[(int) (objectCorners.total() * objectCorners.channels())];
        objectCorners.get(0, 0, objectCornersData);
        objectCornersData[0] = 0;
        objectCornersData[1] = 0;
        objectCornersData[2] = object.cols();
        objectCornersData[3] = 0;
        objectCornersData[4] = object.cols();
        objectCornersData[5] = object.rows();
        objectCornersData[6] = 0;
        objectCornersData[7] = object.rows();
        objectCorners.put(0, 0, objectCornersData);
        return objectCorners;
    }

    private Mat getSceneCorners(Mat objectCorners, Mat homography) {
        Mat sceneCorners = new Mat();
        perspectiveTransform(objectCorners, sceneCorners, homography);
        return sceneCorners;
    }

    public FeatureDetectionResult matchFeatures(Mat object, Mat scene) {
        MatchResult matchResult = findMatches(object, scene);
        Mat homography = findHomography(matchResult);

        Mat objectCorners = getObjectCorners(object);
        float[] objectCornersData = new float[(int) (objectCorners.total() * objectCorners.channels())];
        objectCorners.get(0, 0, objectCornersData);

        Mat sceneCorners = getSceneCorners(objectCorners, homography);
        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);

        double angle = calculateAngleBetweenVectors(getVectorFromCornersData(objectCornersData), getVectorFromCornersData(sceneCornersData));
        System.out.println("angle: " + angle);

        Mat imageWithMatches = new Mat();
        drawMatches(object, scene, imageWithMatches,  matchResult);
        drawObjectOutline(imageWithMatches, sceneCornersData, object.cols());


        //-- Show detected matches
//        HighGui.imshow("Good Matches", imageWithMatches);
//        resizeWindow("Good Matches", 1000, 1000);

        return new FeatureDetectionResult(!matchResult.matches.isEmpty(), angle);
    }
}
