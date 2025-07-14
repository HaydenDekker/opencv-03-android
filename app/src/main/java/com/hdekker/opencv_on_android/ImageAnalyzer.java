package com.hdekker.opencv_on_android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.opencv.core.Mat;

import java.util.concurrent.atomic.AtomicInteger;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ImageAnalyzer";

    ReactiveImageAlgo algo;

    public ImageAnalyzer(ReactiveImageAlgo algo){
        this.algo = algo;
    }

    public Mat latestMatImage = null;
    public AtomicInteger processedFrameCount = new AtomicInteger(0);

    /**
     *  The rate of images provided to the algorithm.
     */
    WindowedFPSCalculator inputFPS = new WindowedFPSCalculator(1000.0f);

    @SuppressLint("UnsafeOptInUsageError") // For ImageProxy.getImage()
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        Log.d(TAG, "ImageAnalysis: New frame received. Format: " + imageProxy.getFormat() +
                ", Size: " + imageProxy.getWidth() + "x" + imageProxy.getHeight() +
                ", Timestamp: " + imageProxy.getImageInfo().getTimestamp());

        long startTime = System.currentTimeMillis();
        inputFPS.recordFrameTimestamp(System.nanoTime());

        Mat bgrMat = null;
        try (imageProxy) {
            bgrMat = ImageConversionUtils.imageProxyToMat(imageProxy);
            algo.getInputSink().tryEmitNext(bgrMat);
        } catch (Exception e) {
            Log.e(TAG, "Error during ImageProxy to Mat conversion: ", e);
        }

        long millis = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Conversion took " + millis + " millis.");
    }

}