package com.hdekker.opencv_on_android;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraUseCaseConfig {

    static String TAG = "CameraUseCaseConfig";
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    ProcessCameraProvider cameraProvider;


    private ExecutorService cameraExecutor;

    public interface OnCameraReadyListener {
        void onCameraReady();
    }

    public CameraUseCaseConfig(Context context){

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        CameraLogger.logAllCameraCapabilities(cameraManager);

    }

    public void startCamera(
            @NonNull Context context, // May not be needed if already have instance context
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ImageAnalysis.Analyzer imageAnalyzer,
            OnCameraReadyListener onCameraReadyListener
    ) {

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(lifecycleOwner, surfaceProvider, imageAnalyzer, cameraProvider);
                onCameraReadyListener.onCameraReady();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;

    private void bindPreviewAndAnalysis(
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull Preview.SurfaceProvider surfaceProvider,
            @NonNull ImageAnalysis.Analyzer imageAnalyzer,
            @NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(surfaceProvider);

        android.util.Size targetResolution = new android.util.Size(720, 720);

        ResolutionStrategy rs = new ResolutionStrategy(
                targetResolution,
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
        );

        ResolutionSelector.Builder builder = new ResolutionSelector.Builder();
        builder.setResolutionStrategy(rs);
        ResolutionSelector res = builder.build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setResolutionSelector(res)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD)) // e.g., 720p
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

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
                    imageAnalysis,
                    videoCapture); // Add your imageAnalysis use case here

            Log.d(TAG, "CameraX Preview and ImageAnalysis bound successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    public void startRecording(Context context) {
        if (videoCapture == null) {
            Log.e(TAG, "VideoCapture use case is not initialized.");
            return;
        }

    // Configure where to save the video
    String name = "CameraX-video-" +
            new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4";

    ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Videos");

    MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions.Builder(
            context.getContentResolver(),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build();

    // Create a listener for recording events
    Consumer<VideoRecordEvent> videoRecordEventListener = event -> {
        if (event instanceof VideoRecordEvent.Start) {
            Log.i(TAG, "Recording started.");
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show();
        } else if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
            if (finalizeEvent.hasError()) {
                Log.e(TAG, "Recording failed: " + finalizeEvent.getError());
            } else {
                Log.i(TAG, "Recording saved to: " + finalizeEvent.getOutputResults().getOutputUri());
                Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show();
            }
            activeRecording = null;
        }
    };

    // Start the recording
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e(TAG, "Error no permissions");
            return;
        }
        activeRecording = videoCapture.getOutput()
            .prepareRecording(context, outputOptions)
                .withAudioEnabled() // Enable audio if needed
                .start(ContextCompat.getMainExecutor(context), videoRecordEventListener);
}

    public void stopRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }


    public void releaseCamera() {
        stopRecording();
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

