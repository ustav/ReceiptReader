package demo;

import com.recognition.software.jdeskew.ImageDeskew;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Point;
import org.opencv.core.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;
import static net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE;
import static net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.highgui.HighGui.toBufferedImage;
import static org.opencv.imgproc.Imgproc.*;

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
//        imshow("cutImage" + rect.toString(), cutImage);
        Mat deskewed = deskewImage(cutImage);
//        imshow("deskewed Image" + rect.toString(), deskewed);

        BufferedImage bufferedCutImage = (BufferedImage) toBufferedImage(deskewed);
        tesseract.setPageSegMode(PSM_SINGLE_BLOCK);
        return tesseract.doOCR(bufferedCutImage);
    }

    private double getSkewAngle(Mat image) {
        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(image);
        ImageDeskew deskew = new ImageDeskew(bufferedImage);
        double angle = deskew.getSkewAngle();
//        System.out.println("Skew Angle: " + angle);
        return angle;
    }

    private Mat deskewImage(Mat image) {
        double angle = getSkewAngle(image);
        double offsetY = abs(tan(angle * PI / 180) * image.cols());

        double negativeAngleOffsetY = 0d;
        double positiveAngleOffsetY = 0d;

        if (angle < 0) {
            negativeAngleOffsetY = offsetY;
        } else {
            positiveAngleOffsetY = offsetY;
        }

        MatOfPoint2f square = new MatOfPoint2f(
                new Point(0, 0),
                new Point(0, image.rows()),
                new Point(image.cols(), image.rows()),
                new Point(image.cols(), 0)
        );

        MatOfPoint2f quad = new MatOfPoint2f(
                new Point(0, negativeAngleOffsetY),
                new Point(0, image.rows() - positiveAngleOffsetY),
                new Point(image.cols(), image.rows() - negativeAngleOffsetY),
                new Point(image.cols(), +positiveAngleOffsetY)
        );

        Mat transformationMatrix = getPerspectiveTransform(quad, square);
        Mat deskewed = Mat.zeros(image.rows(), image.cols(), CV_8UC3);

        warpPerspective(image, deskewed, transformationMatrix, image.size());
        return deskewed;
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
            } else if (word.getText().equalsIgnoreCase("SUBTOTAAL")) {
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

            List<Word> newList = tesseract.getWords(bufferedImage, RIL_TEXTLINE);

            Pattern pattern = Pattern.compile("\\b\\S*\\d+");
            String total = "";
            for (Word word : newList) {
                if (word.getText().toLowerCase().contains("subtotaal")) {
                    Matcher matcher = pattern.matcher(word.getText());
                    if (matcher.find()) {
                        total = matcher.group();
                    }
//                    System.out.println(word.getText());
                } else if (word.getText().toLowerCase().contains("summe")) {
                    Matcher matcher = pattern.matcher(word.getText());
                    if (matcher.find()) {
                        total = matcher.group();
                    }
//                    System.out.println(word.getText());
                }
            }

            int goodsColumnWidth = headerRectangle.x - footerRectangle.x;
            return new TextKeyBlocks(listRect, goodsColumnWidth, total);
        }

        return null;
    }

    private String textToJson(String text, String total) {
        String[] lines = text.split("\\r?\\n");

        Pattern pattern = Pattern.compile("\\b\\S*\\d+$");
        total = total.replace(',', '.');

        boolean firstLine = true;
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"total\": ").append("\"").append(total).append("\",\n");
        builder.append("  \"products\": [\n");
        int noMatchesCount = 0;
        String lastName = "";
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String price = matcher.group();
                String name = line.substring(0, matcher.start() - 1);
                price = price.replace(',', '.');
                price = price.replace(';', '.');
                price = price.replace(':', '.');

//                System.out.println(line + ", price: " + price);
//                System.out.println("name: " + name);

                if (noMatchesCount == 2) {
                    name = lastName;
                }

                lastName = name;
                noMatchesCount = 0;
                if (!firstLine) {
                    builder.append(",\n");
                }

                builder.append("     { ")
                        .append("\"name\": ").append("\"").append(name).append("\", ")
                        .append("\"price\": ").append("\"").append(price).append("\"").append(" }");
            } else {
                if (noMatchesCount == 0) {
                    lastName = line;
                }

//                System.out.println("No matches");
                noMatchesCount++;
            }
            firstLine = false;
        }

        builder.append("\n").append("  ]\n").append("}\n");
        return builder.toString();
    }

    public String extractText(Mat image) {
        ITesseract tesseract = new Tesseract();
        tesseract.setLanguage("nld");
        tesseract.setDatapath("data");
        tesseract.setTessVariable("user_defined_dpi", "270");

        TextKeyBlocks keyBlocks = findKeyBlocks(tesseract, image);

        try {
            if (keyBlocks != null) {
//                System.out.println("=====================  LIST =========================");
                String fullText = recognizeCutBlock(tesseract, image, keyBlocks.listRect);
                return textToJson(fullText, keyBlocks.total);
//                System.out.println("=====================  GOODS =========================");
//                Rect goods = new Rect(keyBlocks.listRect.x, keyBlocks.listRect.y, keyBlocks.goodsColumnWidth, keyBlocks.listRect.height);
//                String goodsText = recognizeCutBlock(tesseract, image, goods);
//                System.out.println(goodsText);
//
//                System.out.println("=====================  PRICES =========================");
//                Rect prices = new Rect(keyBlocks.goodsColumnWidth, keyBlocks.listRect.y, keyBlocks.listRect.width - keyBlocks.goodsColumnWidth, keyBlocks.listRect.height);
//                String pricesText = recognizeCutBlock(tesseract, image, prices);
//                System.out.println(pricesText);

            }
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }
}
