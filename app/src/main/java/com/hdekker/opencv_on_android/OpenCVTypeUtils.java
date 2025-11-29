package com.hdekker.opencv_on_android;

import android.graphics.PointF;

import com.hdekker.opencv_02_ball_detection.domain.MinEnclosingCircle;
import com.hdekker.opencv_02_ball_detection.domain.PathIntersection;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileNode;

import org.opencv.core.Point;

public class OpenCVTypeUtils {

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
