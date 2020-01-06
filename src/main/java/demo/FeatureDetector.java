package demo;

import com.recognition.software.jdeskew.ImageDeskew;
import com.sun.jna.ptr.PointerByReference;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TessAPI1;
import org.opencv.core.*;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.highgui.HighGui;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static net.sourceforge.tess4j.ITessAPI.TRUE;
import static org.opencv.core.Core.NORM_HAMMING;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.CvType.*;
import static org.opencv.features2d.Features2d.drawMatches;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

public class FeatureDetector {

    public int getRotation() {
        String logoFilename = "pics/jumbo_logo1.jpg";
        Mat logo = imread(logoFilename, IMREAD_GRAYSCALE);

        String receiptFilename = "pics/receipt4.jpg";
        Mat receipt = imread(receiptFilename, IMREAD_REDUCED_GRAYSCALE_8);

//        ImageDeskew deskew = new ImageDeskew((BufferedImage) HighGui.toBufferedImage(receipt));
//        System.out.println(deskew.getSkewAngle());

        imshow("receipt", receipt);

//        Leptonica leptInstance = Leptonica.INSTANCE;
//        Pix pix = leptInstance.pixRead(receiptFilename);
//        int angle = detectOrientation(pix);
//        System.out.println(angle);

        return 0;
    }

    public int detectOrientation(Pix srcPix) {
        ITessAPI.TessBaseAPI handle = TessAPI1.TessBaseAPICreate();
        TessAPI1.TessBaseAPIInit3(handle, "tessdata", "osd");
        TessAPI1.TessBaseAPISetImage2(handle, srcPix);
        IntBuffer orientDegB = IntBuffer.allocate(1);
        FloatBuffer orientConfB = FloatBuffer.allocate(1);
        PointerByReference scriptNameB = new PointerByReference();
        FloatBuffer scriptConfB = FloatBuffer.allocate(1);
        int result = TessAPI1
                .TessBaseAPIDetectOrientationScript(handle, orientDegB, orientConfB, scriptNameB,
                        scriptConfB);
        if (result == TRUE) {
            int angle = orientDegB.get();
            return angle;
        }

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

        HoughLinesP(binary, lines, 1, PI  /180, 100, size.width / 4.f, 20);

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

    private void templateDetection(Mat scene, Mat object) {
        Mat result = new Mat();
        int result_cols = scene.cols() - object.cols() + 1;
        int result_rows = scene.rows() - object.rows() + 1;
        result.create(result_rows, result_cols, CvType.CV_32FC1);

        int matchMethod = TM_CCOEFF_NORMED;

        matchTemplate(scene, object, result, matchMethod);

        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        Point matchLoc;

        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        if (matchMethod == TM_SQDIFF || matchMethod == TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }

        Mat img_display = new Mat();
        scene.copyTo(img_display);

        System.out.println(result);

        rectangle(img_display, matchLoc, new Point(matchLoc.x + object.cols(), matchLoc.y + object.rows()),
                new Scalar(0, 255, 0), 2, 8, 0);
        rectangle(result, matchLoc, new Point(matchLoc.x + object.cols(), matchLoc.y + object.rows()),
                new Scalar(255, 0, 0), 2, 8, 0);

        imshow("img_display", img_display);

        result.convertTo(result, CV_8UC1, 255.0);
        imshow("result", result);

//        imshow("logo", object);
//        imshow("receipt", scene);
        HighGui.resizeWindow("result", 1000, 1000);
        HighGui.resizeWindow("img_display", 1000, 1000);
    }

    private void matcher(Mat scene, Mat object) {
        // Initiate SIFT detector
        ORB orb = ORB.create();

        // find the keypoints and descriptors with SIFT
        Mat logoMask = new Mat();
        MatOfKeyPoint logoKeypoints = new MatOfKeyPoint();
        Mat logoDescriptors = new Mat();
        orb.detectAndCompute(object, logoMask, logoKeypoints, logoDescriptors);

        Mat receiptMask = new Mat();
        MatOfKeyPoint receiptKeypoints = new MatOfKeyPoint();
        Mat receiptDescriptors = new Mat();
        orb.detectAndCompute(scene, receiptMask, receiptKeypoints, receiptDescriptors);

//        imshow("logoMask", logoMask);
//        imshow("logoDescriptors", logoDescriptors);
//        imshow("receiptMask", receiptMask);
//        imshow("receiptDescriptors", receiptDescriptors);


        // create BFMatcher object
        BFMatcher matcher = BFMatcher.create(NORM_HAMMING, true);
//
        // Match descriptors.
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(logoDescriptors, receiptDescriptors, matches);

        // Sort them in the order of their distance.
        DMatch[] matchesArray = matches.toArray();
        Arrays.sort(matchesArray, (o1, o2) -> Float.compare(o1.distance, o2.distance));

        if (matchesArray.length > 0) {
            // Draw first 10 matches.
            Mat matchesImage = new Mat();
            drawMatches(object, logoKeypoints, scene, receiptKeypoints, new MatOfDMatch(Arrays.copyOfRange(matchesArray, 0, matchesArray.length > 9 ? 9 : 0)), matchesImage, new Scalar(255, 0, 255));

            imshow("matchesImage", matchesImage);
            HighGui.resizeWindow("matchesImage", 1000, 1000);
        }

    }

}
