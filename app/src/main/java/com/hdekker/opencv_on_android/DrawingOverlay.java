package com.hdekker.opencv_on_android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingOverlay extends View {

    private final Paint paint;
    private final Paint pathPaint; // New paint object for the path

    private final List<PointF> circlesToDraw = new ArrayList<>();
    private final List<PointF> pathPoints = new ArrayList<>(); // New list for path points

    public DrawingOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Set up the paint object for drawing
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE); // To draw hollow circles
        paint.setStrokeWidth(5f);          // Line thickness

        // Set up the new paint object for drawing the path
        pathPaint = new Paint();
        pathPaint.setColor(Color.GREEN); // Use a different color for the path
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(1f); // Make the path line a bit thicker
        pathPaint.setAntiAlias(true);
    }

    // A method to update the circles from your MainActivity or ViewModel
    public void setCircles(List<PointF> points) {
        synchronized (circlesToDraw) {
            circlesToDraw.clear();
            if (points != null) {
                circlesToDraw.addAll(points);
            }
        }
        // Tell the view to redraw itself with the new data
        postInvalidate();
    }

    public void setPath(List<PointF> points) {
        synchronized (pathPoints) {
            pathPoints.clear();
            if (points != null) {
                pathPoints.addAll(points);
            }
        }
        // Tell the view to redraw itself with the new data
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // onDraw will be called when postInvalidate() is called or the view is first drawn.
        synchronized (circlesToDraw) {
            for (PointF point : circlesToDraw) {
                // The canvas coordinates are relative to this view's top-left corner.
                // Example radius
                float circleRadius = 20.0f;
                canvas.drawCircle(point.x, point.y, circleRadius, paint);
            }
        }

        // 2. Draw the path by connecting the points
        synchronized (pathPoints) {
            // We need at least two points to draw a line
            if (pathPoints.size() > 1) {
                for (int i = 0; i < pathPoints.size() - 1; i++) {
                    PointF startPoint = pathPoints.get(i);
                    PointF endPoint = pathPoints.get(i + 1);
                    canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, pathPaint);
                }
            }
        }
    }

    /**
     * Transforms points from the ImageAnalysis coordinate space to the DrawingOverlay's coordinate space.
     */
    public static List<PointF> transformToViewCoordinates(
            List<PointF> imagePoints,
            float imageWidth,  // Add this parameter
            float imageHeight,
            float viewWidth,
            float viewHeight) {
        if (imagePoints == null || imagePoints.isEmpty()) {
            return new ArrayList<>();
        }

        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;

        List<PointF> viewPoints = new ArrayList<>();
        for (PointF imagePoint : imagePoints) {
            float viewX = imagePoint.x * scaleX;
            float viewY = imagePoint.y * scaleY;
            viewPoints.add(new PointF(viewX, viewY));
        }

        return viewPoints;
    }
}