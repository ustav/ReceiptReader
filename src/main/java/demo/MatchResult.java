package demo;

import org.opencv.core.DMatch;
import org.opencv.core.MatOfKeyPoint;

import java.util.List;

public class MatchResult {
    public final List<DMatch> matches;
    public final MatOfKeyPoint objectKeypoints;
    public final MatOfKeyPoint sceneKeypoints;

    public MatchResult(List<DMatch> matches, MatOfKeyPoint objectKeypoints, MatOfKeyPoint sceneKeypoints) {
        this.matches = matches;
        this.objectKeypoints = objectKeypoints;
        this.sceneKeypoints = sceneKeypoints;
    }
}
