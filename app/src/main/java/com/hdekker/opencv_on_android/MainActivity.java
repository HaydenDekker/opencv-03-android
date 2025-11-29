package com.hdekker.opencv_on_android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.androidrecord.AndroidRecordModule;
import com.hdekker.opencv_02_ball_detection.config.dev.algo.AlgoResult;
import com.hdekker.opencv_02_ball_detection.domain.MinEnclosingCircle;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileAlgoResult;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileNode;
import com.hdekker.opencv_02_ball_detection.domain.serialisers.ContourDeserialiser;
import com.hdekker.opencv_02_ball_detection.domain.serialisers.ContourSerialiser;
import com.hdekker.opencv_02_ball_detection.domain.serialisers.ProjectileAlgoResultMapper;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    static {

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully from local package.");
        } else {
            Log.e(TAG, "OpenCV load failed from local package. Check library paths or initAsync.");
        }

    }
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private PreviewView previewView;

    private DrawingOverlay drawingOverlay;

    CameraUseCaseConfig cameraUseCaseConfig;
    public ImageAnalyzer imageAnalyzer;

    private FileLogger fileLogger;

    private TextView fpsValueText;
    private TextView contourMetricText;

    private TextView pathMetricText;

    private void initTextViewObjects(){

        previewView = findViewById(R.id.previewView);
        drawingOverlay = findViewById(R.id.drawing_overlay);

        View fpsMetricLayout = findViewById(R.id.fps_metric);
        fpsValueText = fpsMetricLayout.findViewById(R.id.metric_value);
        ImageView fpsIcon = fpsMetricLayout.findViewById(R.id.metric_icon);
        fpsIcon.setImageResource(R.drawable.baseline_shutter_speed_24);

        View contourMetricTextView = findViewById(R.id.contour_metric);
        contourMetricText = contourMetricTextView.findViewById(R.id.metric_value);

        View pathMetricTextView = findViewById(R.id.path_metric);
        pathMetricText = pathMetricTextView.findViewById(R.id.metric_value);

    }

    final private AtomicInteger pointsMaxWaterMark = new AtomicInteger(0);
    final private AtomicInteger pathsMaxWaterMark = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initTextViewObjects();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (permissionsNotGranted()) {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        ReactiveProjectileAlgo rpa = new ReactiveProjectileAlgo();
        ImageAnalyzer<AlgoResult<ProjectileAlgoResult>> ria = new ImageAnalyzer<>(rpa);
        setImageAnalyzer(ria);

        Flux.interval(Duration.ofSeconds(2))
                .subscribe(c->{
                    double fps = ImageAnalyzer.inputFPS.calculateFPS();
                    Log.i(TAG, "Input FPS: " + fps);
                    runOnUiThread(() -> {
                        fpsValueText.setText(String.valueOf(Double.valueOf(fps).intValue()));
                        contourMetricText.setText(String.valueOf(pointsMaxWaterMark.get()));
                        pointsMaxWaterMark.set(0);
                        pathMetricText.setText(String.valueOf(pathsMaxWaterMark.get()));
                        pathsMaxWaterMark.set(0);
                    });
                });

        fileLogger = new FileLogger(getApplicationContext());

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new AndroidRecordModule());
        SimpleModule sm = new SimpleModule();
        sm.addSerializer(MatOfPoint.class, new ContourSerialiser());
        sm.addDeserializer(MatOfPoint.class, new ContourDeserialiser());
        om.registerModule(sm);

        ria.subscribeToEvents(res->{

            // log
            String algoLog = null;
            try {
                algoLog = om.writeValueAsString(res.result());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            fileLogger.append(algoLog);

            // metrics
            List<PointF> points = res.result().pathIntersectionStream()
                    .map(OpenCVTypeUtils::convert)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Log.i(TAG, "writing points: " + points.size() + " from " + res.result().frameMatchChangeResult().contourRegionOfInterest().size() + " contours. "
              + " deflections: " + res.result().deflections().size() +
                    ". path intersections: " + res.result().intersections().size());

            int contours = res.result().frameMatchChangeResult().contourRegionOfInterest().size();
            pointsMaxWaterMark.getAndUpdate(n-> Math.max(n,contours));

            int path = res.result().pathAnalysis().paths().size();
            pathsMaxWaterMark.getAndUpdate(n-> Math.max(n,path));

            // This is a simplified example. A full implementation requires matching aspect ratios
            // between the image and the view. CameraX's CoordinateTransform can be complex.
            // Let's assume a simple scaling for now.
            float viewWidth = drawingOverlay.getWidth();
            float viewHeight = drawingOverlay.getHeight();

            // display
            drawingOverlay.setCircles(DrawingOverlay.transformToViewCoordinates(points, viewWidth, viewHeight));

        });
    }

    public void setImageAnalyzer(ImageAnalyzer<?> imageAnalyzer){
        this.imageAnalyzer = imageAnalyzer;
        if (cameraUseCaseConfig != null) {
            cameraUseCaseConfig.releaseCamera();
        }
        cameraUseCaseConfig = new CameraUseCaseConfig(this);
        cameraUseCaseConfig.startCamera(this, this, previewView.getSurfaceProvider(), imageAnalyzer,
                ()->{
                    cameraUseCaseConfig.startRecording(this);
                });

    }

    private boolean permissionsNotGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (permissionsNotGranted()) {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraUseCaseConfig != null) {
            cameraUseCaseConfig.releaseCamera();
        }

    }

    public static Function<ImageProxy, Mat> openCVConversion = (imageProxy) -> {
        Mat bgrMat = null;
        try (imageProxy) {
            bgrMat = ImageConversionUtils.imageProxyToMat(imageProxy);
            imageProxy.close();
        } catch (Exception e) {
            Log.e(TAG, "Error during ImageProxy to Mat conversion: ", e);
        }
        return bgrMat;
    };


}