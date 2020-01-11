package demo;

import org.opencv.core.Mat;

public class FeatureDetectionResult {
    public final boolean matchFound;
    public final double angle;

    public FeatureDetectionResult(boolean matchFound, double angle) {
        this.matchFound = matchFound;
        this.angle = angle;
    }
}
