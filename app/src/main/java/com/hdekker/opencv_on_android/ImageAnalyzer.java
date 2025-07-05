package com.hdekker.opencv_on_android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.opencv.core.Mat;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ImageAnalyzer";

    public Mat latestMatImage = null;

    @SuppressLint("UnsafeOptInUsageError") // For ImageProxy.getImage()
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

        Log.d(TAG, "ImageAnalysis: New frame received. Format: " + imageProxy.getFormat() +
                ", Size: " + imageProxy.getWidth() + "x" + imageProxy.getHeight() +
                ", Timestamp: " + imageProxy.getImageInfo().getTimestamp());

        Mat bgrMat = null;
        Mat grayMat = null;
        try (imageProxy) {
            bgrMat = ImageConversionUtils.imageProxyToMat(imageProxy);

            if (!bgrMat.empty()) {
                Log.d(TAG, "Successfully converted ImageProxy to BGR Mat: " + bgrMat.size().toString());

                // --- Your processing with bgrMat ---

                // Convert to Grayscale
                grayMat = ImageConversionUtils.toGrayscale(bgrMat);
                if (!grayMat.empty()) {
                    Log.d(TAG, "Successfully converted to Grayscale Mat: " + grayMat.size().toString());
                    // --- Your processing with grayMat ---

                    // This is where you'd update your `latestMatImage` for the test
                    // e.g., this.latestMatImage = grayMat.clone(); // Clone if grayMat will be released
                    // or if bgrMat is released and grayMat
                    // might be a view in some cases.
                    // Safest to clone for storage.
                    if (this.latestMatImage != null) {
                        this.latestMatImage.release();
                    }
                    this.latestMatImage = grayMat.clone(); // Store the grayscale mat
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during ImageProxy to Mat conversion or grayscale: ", e);
        } finally {
            // Release Mats that are locally created if not returned or stored elsewhere
            if (bgrMat != null) {
                bgrMat.release();
            }

        }
    }


    public void releaseLatestImage() {

    }

}