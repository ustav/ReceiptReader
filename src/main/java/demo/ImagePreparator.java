package demo;

import org.opencv.core.*;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.Mat.ones;
import static org.opencv.imgproc.Imgproc.*;

public class ImagePreparator {

    private Mat getBinaryImage(Mat originalImage) {
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
//        imshow("gray", gray);


        Mat blurred = new Mat();
        GaussianBlur(gray, blurred, new Size(5, 5), 0);
//        imshow("blurred", blurred);

        Mat canny = new Mat();
        Canny(blurred, canny, 50, 255);
//        imshow("canny", canny);

        Mat kernel = ones(3, 3, CV_8U);
        Mat closing = new Mat();
        morphologyEx(canny, closing, MORPH_DILATE, kernel);
//        imshow("closing", closing);

        return closing;
    }

    private List<MatOfPoint> detectContours(Mat binaryImage) {
        List<MatOfPoint> contours = new ArrayList<>(); // Vector for storing contour
        Mat hierarchy = new Mat();

        findContours(binaryImage, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE); // Find the contours in the image
        return contours;
    }

    private MatOfPoint2f detectPolygon(List<MatOfPoint> contours) {
        if (contours.isEmpty()) {
            return new MatOfPoint2f();
        }

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
        double perimeter = arcLength(largestPolygon, true);
        approxPolyDP(largestPolygon, contoursPolygon, perimeter * 0.04, true);
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

        for (Point point : contourPoints) {
            circle(image, point, 10, new Scalar(0, 255, 0), 5);
        }

        if (contourPoints.length == 4) {
            Rect boundRect = boundingRect(polygon);
            Point center = new Point(boundRect.x + (double) boundRect.width / 2, boundRect.y + (double) boundRect.height / 2);
//            circle(image, center, 20, new Scalar(0, 255, 0), 5);

            Point leftTop = null;
            Point rightTop = null;
            Point leftBotton = null;
            Point rightBottom = null;
            for (Point point : contourPoints) {
                if (point.x < center.x && point.y < center.y) leftTop = point;
                if (point.x < center.x && point.y > center.y) rightTop = point;
                if (point.x > center.x && point.y > center.y) leftBotton = point;
                if (point.x > center.x && point.y < center.y) rightBottom = point;
            }

            if (leftTop == null || rightTop == null || leftBotton == null || rightBottom == null) {
                return image;
            }

            MatOfPoint2f quad = new MatOfPoint2f(
                    leftTop,
                    rightTop,
                    rightBottom,
                    leftBotton
            );

            MatOfPoint2f square = new MatOfPoint2f(
                    new Point(boundRect.x, boundRect.y),
                    new Point(boundRect.x, boundRect.y + boundRect.height),
                    new Point(boundRect.x + boundRect.width, boundRect.y),
                    new Point(boundRect.x + boundRect.width, boundRect.y + boundRect.height)
            );

            Mat transformationMatrix = getPerspectiveTransform(quad, square);
            Mat transformed = Mat.zeros(image.rows(), image.cols(), CV_8UC3);

//            imshow("transformed", transformed);
            warpPerspective(image, transformed, transformationMatrix, image.size());

            drawPolygon(image, contourPoints);
            drawRect(image, boundRect);
            drawRect(transformed, boundRect);

            Mat cropped = new Mat(transformed, boundRect);
//            imshow("cropped", cropped);

            return cropped;
        }

        return image;
    }

    private MatOfPoint2f scalePolygon(MatOfPoint2f polygon, double scale) {
        List<Point> newPoints = new ArrayList<>();
        polygon.toList().forEach(point -> newPoints.add(new Point(point.x * scale, point.y * scale)));
        return new MatOfPoint2f(newPoints.toArray(new Point[newPoints.size()]));
    }

    private Mat binarizeCropped(Mat cropped) {
        Mat result = new Mat();
        cvtColor(cropped, result, COLOR_BGR2GRAY);

        Mat binary = new Mat();
//        adaptiveThreshold(result, result, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 25, 16);

//        Canny(croppedGrey, binary, 50, 255);
        Mat kernel = ones(3, 3, CV_8U);
        Mat closing = new Mat();
//        morphologyEx(binary, binary, MORPH_ELLIPSE, kernel);


//        threshold(result, result, 100, 255, THRESH_BINARY+THRESH_OTSU);
        return result;
    }

    public Mat getPreparedImage(Mat source) {
        Mat binary = getBinaryImage(source);
        List<MatOfPoint> contours = detectContours(binary);
        MatOfPoint2f polygon = detectPolygon(contours);
        double imageScale = source.size().width / binary.size().width;
        MatOfPoint2f scaledPolygon = scalePolygon(polygon, imageScale);
        Mat cropped = cropPolygon(source, scaledPolygon);
        return binarizeCropped(cropped);
    }
}
