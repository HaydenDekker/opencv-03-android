package com.hdekker.opencv_on_android;

import android.graphics.PointF;

import com.hdekker.opencv_02_ball_detection.domain.MinEnclosingCircle;
import com.hdekker.opencv_02_ball_detection.domain.PathIntersection;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileNode;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenCVTypeUtils {

    /**
     * Converts an OpenCV MatOfPoint into a List of Android graphics PointF objects.
     *
     * @param matOfPoint The OpenCV MatOfPoint to convert.
     * @return A List of PointF objects. Returns an empty list if the input is null.
     */
    public static List<PointF> convert(MatOfPoint matOfPoint) {
        if (matOfPoint == null) {
            return new ArrayList<>();
        }
        // Convert the MatOfPoint to a list of OpenCV Points
        List<Point> openCVPoints = matOfPoint.toList();

        // Stream the OpenCV Points and map each one to a new Android PointF
        return openCVPoints.stream()
                .map(p -> new PointF((float) p.x, (float) p.y))
                .collect(Collectors.toList());
    }

    public static PointF convert(PathIntersection pi){
        if(pi.intersectionPoints().length==0) {
            return null;
        }

        ProjectileNode latestNodeBefore = pi.res().overLappingAssessment().latestNodeBefore();

        double[] point = pi.findClosestPointsTo(latestNodeBefore.point().x, latestNodeBefore.point().y);

        MinEnclosingCircle moc = new MinEnclosingCircle(
                new Point(point[0], point[1]),
                latestNodeBefore.radius());

        PointF pf = new PointF();
        pf.set(Double.valueOf(moc.point().x).floatValue(),
                Double.valueOf(moc.point().y).floatValue());

        return pf;
    }
}
