package com.hdekker.opencv_on_android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "ImageAnalyzer";

    public ImageProxy latestImage = null;

    @SuppressLint("UnsafeOptInUsageError") // For ImageProxy.getImage()
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Optional: Throttle analysis to reduce processing load
        // long currentTimestamp = System.currentTimeMillis();
        // if (currentTimestamp - lastAnalyzedTimestamp < TimeUnit.SECONDS.toMillis(1)) { // Analyze approx once per second
        //     imageProxy.close(); // IMPORTANT: Always close the ImageProxy
        //     return;
        // }
        // lastAnalyzedTimestamp = currentTimestamp;

        if (latestImage != null) {
            latestImage.close();
        }
        latestImage = imageProxy;

        Log.d(TAG, "ImageAnalysis: New frame received. Format: " + imageProxy.getFormat() +
                ", Size: " + imageProxy.getWidth() + "x" + imageProxy.getHeight() +
                ", Timestamp: " + imageProxy.getImageInfo().getTimestamp());

        // --- Your Image Processing Logic ---
        // Example: Convert to Bitmap (make sure to handle different image formats)
        // Image image = imageProxy.getImage();
        // if (image != null) {
        //     // Convert YUV_420_888 to Bitmap (common format)
        //     // This requires a conversion function.
        //     // Bitmap bitmap = yuvToRgbConverter.toBitmap(image);
        //     // Do something with the bitmap
        // }

    }

    // Method to update latestImage, called from your Analyzer
    // This is one way; the analyzer could also hold a direct reference to this.latestImage
    public void setLatestImage(ImageProxy imageProxy) {
        if (this.latestImage != null) {
            this.latestImage.close(); // Close the previous one
        }
        this.latestImage = imageProxy;
    }

    public void releaseLatestImage() {
        if (latestImage != null) {
            latestImage.close();
            latestImage = null;
        }
    }

}