package com.hdekker.opencv_on_android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingOverlay extends View {

    private final Paint paint;
    private final List<PointF> circlesToDraw = new ArrayList<>();
    private float circleRadius = 20.0f; // Example radius

    public DrawingOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Set up the paint object for drawing
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE); // To draw hollow circles
        paint.setStrokeWidth(5f);          // Line thickness
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // onDraw will be called when postInvalidate() is called or the view is first drawn.
        synchronized (circlesToDraw) {
            for (PointF point : circlesToDraw) {
                // The canvas coordinates are relative to this view's top-left corner.
                canvas.drawCircle(point.x, point.y, circleRadius, paint);
            }
        }
    }

    /**
     * Transforms points from the ImageAnalysis coordinate space to the DrawingOverlay's coordinate space.
     */
    public static List<PointF> transformToViewCoordinates(List<PointF> imagePoints, float viewWidth, float viewHeight) {
        if (imagePoints == null || imagePoints.isEmpty()) {
            return new ArrayList<>();
        }

        // Get the dimensions of the image that the analyzer is processing.
        // You must get this from your ImageAnalyzer or the use case itself.
        // For example, if you set a target resolution of 1280x720:
        float imageWidth = 1280; // Example, replace with actual
        float imageHeight = 720; // Example, replace with actual

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