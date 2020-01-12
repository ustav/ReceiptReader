package demo;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static org.opencv.imgcodecs.Imgcodecs.*;

/**
 * A utility class to get rid of annoying boilerplate code while dealing with
 * the OpenCV Java API (which is generated automatically for the most part) and
 * in consequence with lots of Mat's (I/O, conversions to BufferedImages and
 * what not, ...).
 */
public final class OpenCVUtils {

    /**
     * Don't let anyone instantiate this class.
     */
    private OpenCVUtils() {}

    /**
     * Loads an image from a file.
     * This is a wrapper around Highgui.imread() which fails if the file
     * is inside a jar/zip. This function takes care of that case, loads the
     * image in Java and manually creates the Mat...
     *
     * @Param name name of the resource
     * @Param int Flags specifying the color type of a loaded image;
     *            supported: LOAD_COLOR (8-bit, 3-channels),
     *                       LOAD_GRAYSCALE (8-bit, 1-channel),
     * @return Mat of type CV_8UC3 or CV_8UC1 (empty Mat is returned in case of an error)
     */
    public static Mat readImage(String name, int flags) {
        URL url = name.getClass().getResource(name);

        // make sure the file exists
        if (url == null) {
            System.out.println("ResourceNotFound: " + name);
            return new Mat();
        }

        String path = url.getPath();

        // not sure why we (sometimes; while running unpacked from the IDE) end
        // up with the authority-part of the path (a single slash) as prefix,
        // ...anyways: Highgui.imread can't handle it, so that's why.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Mat image = imread(path, flags);

        // ...and if Highgui.imread() has failed, we simply assume that the file
        // is packed in a jar (i.e. Java should be able to read the image)
        if (image.empty()) {
            BufferedImage buf;

            try {
                buf = ImageIO.read(url);
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
                return image;
            }

            int height = buf.getHeight();
            int width = buf.getWidth();
            int rgb, type, channels;

            switch (flags) {
                case IMREAD_GRAYSCALE:
                    type = CvType.CV_8UC1;
                    channels = 1;
                    break;
                case IMREAD_COLOR:
                default:
                    type = CvType.CV_8UC3;
                    channels = 3;
                    break;
            }

            byte[] px = new byte[channels];
            image = new Mat(height, width, type);

            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    rgb = buf.getRGB(x, y);
                    px[0] = (byte)(rgb & 0xFF);
                    if (channels==3) {
                        px[1] = (byte)((rgb >> 8) & 0xFF);
                        px[2] = (byte)((rgb >> 16) & 0xFF);
                    }
                    image.put(y, x, px);
                }
            }
        }

        return image;
    }

    /**
     * Loads an image from a file (8-bit, 3-channels).
     * This is a wrapper around Highgui.imread() which fails if the file
     * is inside a jar/zip. This function takes care of that case, loads the
     * image in Java and manually creates the Mat...
     *
     * @Param name name of the resource
     * @return Mat of type CV_8UC3 (empty Mat is returned in case of an error)
     */
    public static Mat readImage(String name) {
        return readImage(name, IMREAD_COLOR);
    }

    /* ... */
}