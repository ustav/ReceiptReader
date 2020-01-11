package demo;

import com.recognition.software.jdeskew.ImageDeskew;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD;
import static org.opencv.highgui.HighGui.imshow;
import static org.opencv.highgui.HighGui.toBufferedImage;
import static org.opencv.imgproc.Imgproc.rectangle;

public class TextExtractor {

    private void drawRectangle(Mat image, Rectangle rectangle) {
        Scalar color = new Scalar(0, 0, 0);
        Rect rect = new Rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        rectangle(image, rect, color, 5);
    }

    private void drawRectangle(Mat image, Rect rectangle) {
        Scalar color = new Scalar(0, 0, 0);
        rectangle(image, rectangle, color, 5);
    }

    private String recognizeBlock(ITesseract tesseract, Mat image, Rect rect) throws TesseractException {
        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(image);
        drawRectangle(image, rect);
        return tesseract.doOCR(bufferedImage, new Rectangle(rect.x, rect.y, rect.width, rect.height));
    }

    private String recognizeCutBlock(ITesseract tesseract, Mat image, Rect rect) throws TesseractException {
        Mat cutImage = new Mat(image, rect);
        imshow("cutImage" + rect.toString(), cutImage);
        BufferedImage bufferedCutImage = (BufferedImage) toBufferedImage(cutImage);
        return tesseract.doOCR(bufferedCutImage);
    }

    private TextKeyBlocks findKeyBlocks(ITesseract tesseract, Mat image) {
        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(image);
        List<Word> wordList = tesseract.getWords(bufferedImage, RIL_WORD);

        Rectangle headerRectangle = null;
        Rectangle footerRectangle = null;
        for (Word word : wordList) {
            Rectangle rectangle = word.getBoundingBox();
            if (word.getText().equalsIgnoreCase("BEDRAG")) {
                headerRectangle = rectangle;
            }

            if (word.getText().equalsIgnoreCase("SUBTOTAAL")) {
                footerRectangle = rectangle;
            }
        }

        if (headerRectangle != null && footerRectangle != null) {
            Rect listRect = new Rect(
                    0,
                    headerRectangle.y + 2 * headerRectangle.height,
                    image.cols(),
                    footerRectangle.y - headerRectangle.y - headerRectangle.height - 2 * footerRectangle.height
            );

            int goodsColumnWidth = headerRectangle.x - footerRectangle.x;
            return new TextKeyBlocks(listRect, goodsColumnWidth);
        }

        return null;
    }

    public String extractText(Mat image) {
//        ImageDeskew deskew = new ImageDeskew(bufferedImage);
//        System.out.println("Skew Angle: " + deskew.getSkewAngle());

        ITesseract tesseract = new Tesseract();
        tesseract.setLanguage("nld");
        tesseract.setDatapath("data");

        TextKeyBlocks keyBlocks = findKeyBlocks(tesseract, image);

        try {
            if (keyBlocks != null) {
                System.out.println("=====================  LIST =========================");
                String fullText = recognizeCutBlock(tesseract, image, keyBlocks.listRect);
                System.out.println(fullText);

                System.out.println("=====================  GOODS =========================");
                Rect goods = new Rect(keyBlocks.listRect.x, keyBlocks.listRect.y, keyBlocks.goodsColumnWidth, keyBlocks.listRect.height);
                String goodsText = recognizeCutBlock(tesseract, image, goods);
                System.out.println(goodsText);

                System.out.println("=====================  PRICES =========================");
                Rect prices = new Rect(keyBlocks.goodsColumnWidth, keyBlocks.listRect.y, keyBlocks.listRect.width - keyBlocks.goodsColumnWidth, keyBlocks.listRect.height);
                String pricesText = recognizeCutBlock(tesseract, image, prices);
                System.out.println(pricesText);
            }
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }
}
