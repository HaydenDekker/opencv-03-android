package com.hdekker.opencv_on_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.camera.view.PreviewView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor; // For running ImageAnalysis on a background thread
    public androidx.camera.core.ImageProxy latestImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView); // Replace with your PreviewView ID
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ... (continued from MainActivity)

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                // Set the resolution for analysis (optional, but recommended)
                // Set the backpressure strategy. STRATEGY_KEEP_ONLY_LATEST is common.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new MyImageAnalyzer(this));

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();

        try {

            Camera camera = cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner
                    cameraSelector,
                    preview,
                    imageAnalysis); // Add your imageAnalysis use case here

            Log.d(TAG, "CameraX Preview and ImageAnalysis bound successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    // ... (continued from MainActivity)

    // --- Inner class for Image Analysis ---
    private static class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private MainActivity mainActivity; // Reference to update latestImage
        private long lastAnalyzedTimestamp = 0L;

        MyImageAnalyzer(MainActivity activity) {
            this.mainActivity = activity;
        }

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

            if (mainActivity.latestImage != null) {
                mainActivity.latestImage.close();
            }
            mainActivity.latestImage = imageProxy;

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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the camera executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // Close the last image if it's still held
        if (latestImage != null) {
            latestImage.close();
            latestImage = null;
        }
    }

    // continued in next step...


}