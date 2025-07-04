package com.hdekker.opencv_on_android;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraUseCaseConfig {

    static String TAG = "CameraUseCaseConfig";
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    ProcessCameraProvider cameraProvider;


    private ExecutorService cameraExecutor;

    public CameraUseCaseConfig(Context context){
        //previewView = findViewById(R.id.previewView); // Replace with your PreviewView ID
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

    }

    public void startCamera(
            @NonNull Context context, // May not be needed if already have instance context
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ImageAnalysis.Analyzer imageAnalyzer
    ) {

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(lifecycleOwner, surfaceProvider, imageAnalyzer, cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindPreviewAndAnalysis(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ImageAnalysis.Analyzer imageAnalyzer,
            @NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(surfaceProvider);

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                // Set the resolution for analysis (optional, but recommended)
                // Set the backpressure strategy. STRATEGY_KEEP_ONLY_LATEST is common.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();

        ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

        imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();

        try {

            Camera camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, // LifecycleOwner
                    cameraSelector,
                    preview,
                    imageAnalysis); // Add your imageAnalysis use case here

            Log.d(TAG, "CameraX Preview and ImageAnalysis bound successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }


    public void releaseCamera() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll(); // Ensure all use cases are unbound
        }
        Log.d(TAG, "Camera resources released.");
    }

}
