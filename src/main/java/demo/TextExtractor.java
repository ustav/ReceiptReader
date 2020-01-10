package demo;

import com.recognition.software.jdeskew.ImageDeskew;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

import static org.opencv.highgui.HighGui.toBufferedImage;

public class TextExtractor {

    public String extractText(Mat image) {
        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(image);
        ImageDeskew deskew = new ImageDeskew(bufferedImage);
        System.out.println("Skew Angle: " + deskew.getSkewAngle());

        ITesseract instance = new Tesseract();
        instance.setLanguage("nld");
        instance.setDatapath("data");

        try {
            String result = instance.doOCR(bufferedImage);
            System.out.println(result);
            return result;
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }
}
