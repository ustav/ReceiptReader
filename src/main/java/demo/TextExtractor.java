package demo;

import com.recognition.software.jdeskew.ImageDeskew;
import com.sun.jna.ptr.PointerByReference;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.lept4j.util.LoadLibs;
import net.sourceforge.tess4j.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static net.sourceforge.tess4j.ITessAPI.TRUE;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.toBufferedImage;
import static org.opencv.imgproc.Imgproc.threshold;

public class TextExtractor {

    public void extractText(Mat image) {
//        String receiptFilename = "pics/receipt4.jpg";
//        Mat receipt = imread(receiptFilename, IMREAD_REDUCED_GRAYSCALE_8);
//        imshow("receipt", receipt);

        threshold(image, image, 150, 255, Imgproc.THRESH_BINARY);
//        Core.rotate(prepared, prepared, Core.ROTATE_90_CLOCKWISE);

        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(image);
        try {
            Pix pixImage = LeptUtils.convertImageToPix(bufferedImage);
            int orientation = detectOrientation(pixImage);
            System.out.println("orientation: " + orientation);

            if (orientation != 0) {
                Mat rotated = new Mat();
                switch (orientation) {
                    case 90:
                        Core.rotate(image, rotated, Core.ROTATE_90_CLOCKWISE);
                        break;
                    case 180:
                        Core.rotate(image, rotated, Core.ROTATE_180);
                        break;
                    case 270:
                        Core.rotate(image, rotated, Core.ROTATE_90_COUNTERCLOCKWISE);
                        break;
                }
                bufferedImage = (BufferedImage) toBufferedImage(rotated);
                imshow("rotated", rotated);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageDeskew deskew = new ImageDeskew(bufferedImage);
        System.out.println("Skew Angle: " + deskew.getSkewAngle());

        ITesseract instance = new Tesseract();
        instance.setLanguage("nld");
        instance.setDatapath("data");

        try {
            String result = instance.doOCR(bufferedImage);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }

    public int detectOrientation(Pix image) {
        ITessAPI.TessBaseAPI handle = TessAPI1.TessBaseAPICreate();
        TessAPI1.TessBaseAPIInit3(handle, "data", "osd");
        TessAPI1.TessBaseAPISetImage2(handle, image);

        IntBuffer orientDegB = IntBuffer.allocate(1);
        FloatBuffer orientConfB = FloatBuffer.allocate(1);
        PointerByReference scriptNameB = new PointerByReference();
        FloatBuffer scriptConfB = FloatBuffer.allocate(1);
        int result = TessAPI1.TessBaseAPIDetectOrientationScript(handle, orientDegB, orientConfB, scriptNameB, scriptConfB);
        if (result == TRUE) {
            return orientDegB.get();
        }

        return 0;
    }

}
