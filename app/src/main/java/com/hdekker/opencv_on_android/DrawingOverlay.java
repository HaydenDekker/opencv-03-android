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
}