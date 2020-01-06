package demo;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_REDUCED_COLOR_4;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgproc.Imgproc.*;

public class ImagePreparator {

    private Mat getSourceImage(String filename) {
        Mat source = imread(filename, IMREAD_REDUCED_COLOR_4);
        imshow("source", source);
        return source;
    }

    private Mat getBinaryImage(Mat originalImage) {

        //        Mat blurred = new Mat();
//        Imgproc.boxFilter(source, blurred, -1, new Size(10d, 10d));
//        imshow("blurred", blurred);


        //        Mat hls = new Mat();
//        Imgproc.cvtColor(source, hls, COLOR_BGR2HLS);
//
//        Scalar lowerWhite = new Scalar(0,0,120);
//        Scalar upperWhite = new Scalar(172,255,255);
//
//        Mat mask = new Mat();
//        inRange(hls, lowerWhite, upperWhite, mask);
//
//        Mat result = new Mat();
//        Core.bitwise_and(source, source, result, mask);
//
//        imshow("hls", hls);
//        imshow("mask", mask);
//        imshow("result", result);

        Size originalSize = originalImage.size();

        double aspectRatio = originalSize.width / originalSize.height;

        Size scaledSize;
        int minDimension = 300;

        if (aspectRatio < 1) {
            scaledSize = new Size(minDimension, minDimension / aspectRatio);
        } else {
            scaledSize = new Size(minDimension * aspectRatio, minDimension);
        }

        Mat resized = new Mat();
        resize(originalImage, resized, scaledSize);

        Mat gray = new Mat();
        cvtColor(resized, gray, COLOR_BGR2GRAY);
        imshow("gray", gray);

        Mat blurred = new Mat();
        GaussianBlur(gray, blurred, new Size(7, 7), 3, 3);
        imshow("blurred", blurred);

        // The #BORDER_REPLICATE | #BORDER_ISOLATED
        // #THRESH_BINARY or #THRESH_BINARY_INV,
//        adaptiveThreshold(gray, threshold, 255, BORDER_REPLICATE, THRESH_BINARY_INV, 7, 7);

        Mat threshold = new Mat();
        threshold(blurred, threshold, 80, 255, Imgproc.THRESH_BINARY);
        imshow("thr", threshold);
        return threshold;
    }

    private List<MatOfPoint> detectContours(Mat binaryImage) {
        List<MatOfPoint> contours = new ArrayList<>(); // Vector for storing contour
        Mat hierarchy = new Mat();

        findContours(binaryImage, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE); // Find the contours in the image
        return contours;
    }

    private MatOfPoint2f detectPolygon(List<MatOfPoint> contours) {
        int largestContourIndex = 0;
        double largestArea = 0;
        for (int i = 0; i < contours.size(); i++) {
            double a = contourArea(contours.get(i), false);  //  Find the area of contour
            if (a > largestArea) {
                largestArea = a;
                largestContourIndex = i;                //Store the index of largest contour
            }
        }

        MatOfPoint2f largestPolygon = new MatOfPoint2f();
        contours.get(largestContourIndex).convertTo(largestPolygon, CvType.CV_32FC2);

        MatOfPoint2f contoursPolygon = new MatOfPoint2f();
        approxPolyDP(largestPolygon, contoursPolygon, 100, true);
        return contoursPolygon;
    }

    private void drawPolygon(Mat image, Point[] contourPoints) {
        Point P1 = contourPoints[0];
        Point P2 = contourPoints[1];
        Point P3 = contourPoints[2];
        Point P4 = contourPoints[3];

        Scalar redColor = new Scalar(0, 0, 255);

        line(image, P1, P2, redColor, 1, LINE_AA, 0);
        line(image, P2, P3, redColor, 1, LINE_AA, 0);
        line(image, P3, P4, redColor, 1, LINE_AA, 0);
        line(image, P4, P1, redColor, 1, LINE_AA, 0);
    }

    private void drawRect(Mat image, Rect rect) {
        Scalar greenColor = new Scalar(0, 255, 0);
        rectangle(image, rect, greenColor, 1, 8, 0);
    }

    private Mat cropPolygon(Mat image, MatOfPoint2f polygon) {
        Point[] contourPoints = polygon.toArray();

        if (contourPoints.length == 4) {
            MatOfPoint2f quad = new MatOfPoint2f(
                    contourPoints[0],
                    contourPoints[1],
                    contourPoints[3],
                    contourPoints[2]
            );

            Rect boundRect = boundingRect(polygon);
            MatOfPoint2f square = new MatOfPoint2f(
                    new Point(boundRect.x, boundRect.y),
                    new Point(boundRect.x, boundRect.y + boundRect.height),
                    new Point(boundRect.x + boundRect.width, boundRect.y),
                    new Point(boundRect.x + boundRect.width, boundRect.y + boundRect.height)
            );

            Mat transformationMatrix = getPerspectiveTransform(quad, square);
            Mat transformed = Mat.zeros(image.rows(), image.cols(), CV_8UC3);
            warpPerspective(image, transformed, transformationMatrix, image.size());

            drawPolygon(image, contourPoints);
            drawRect(image, boundRect);
            drawRect(transformed, boundRect);

            Mat cropped = new Mat(transformed, boundRect);
            Mat croppedGrey = new Mat();
            cvtColor(cropped, croppedGrey, COLOR_BGR2GRAY);
//            Core.rotate(croppedGrey, croppedGrey, Core.ROTATE_90_CLOCKWISE);

            return croppedGrey;
        }

        return image;
    }

    private MatOfPoint2f scalePolygon(MatOfPoint2f polygon, double scale) {
        List<Point> newPoints = new ArrayList<>();
        polygon.toList().forEach(point -> newPoints.add(new Point(point.x * scale, point.y*scale)));
        return new MatOfPoint2f((Point[]) newPoints.toArray(new Point[newPoints.size()]));
    }

    public Mat getPreparedImage(String filename) {
        Mat source = getSourceImage(filename);
        Mat binary = getBinaryImage(source);
        List<MatOfPoint> contours = detectContours(binary);
        MatOfPoint2f polygon = detectPolygon(contours);
        double imageScale = source.size().width / binary.size().width;
        MatOfPoint2f scaledPolygon = scalePolygon(polygon, imageScale);
        return cropPolygon(source, scaledPolygon);
    }
}
