package com.hdekker.opencv_on_android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.hdekker.opencv_02_ball_detection.config.dev.algo.ProjectileAlgo;
import com.hdekker.opencv_02_ball_detection.domain.ContourROI;
import com.hdekker.opencv_02_ball_detection.domain.serialisers.ContourDeserialiser;
import com.hdekker.opencv_02_ball_detection.domain.serialisers.ContourSerialiser;
import com.hdekker.opencv_on_android.camera.ImageAnalyzerAdapter;
import com.hdekker.opencv_on_android.projectile.ProjectilePipeline;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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

    public IImageAnalyzerAdapter imageAnalyzer;

    private FileLogger fileLogger;

    private TextView fpsValueText;
    private TextView contourMetricText;

    private TextView pathMetricText;

    final ProjectileAlgo projectileAlgo = new ProjectileAlgo();
    final ProjectilePipeline pp = new ProjectilePipeline(projectileAlgo);

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

    void setAppScreenBrightnessBehaviour(){
        // initially keep bright, could later improve based on user experience.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initTextViewObjects();
        setAppScreenBrightnessBehaviour();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (permissionsNotGranted()) {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        ImageAnalyzerAdapter ria = new ImageAnalyzerAdapter(
                previewView,
                this,
                this
        );
        setImageAnalyzer(ria);
        ria.start(pp);

        Flux.interval(Duration.ofSeconds(2))
                .subscribe(c->{
                    double fps = ProjectilePipeline.inputFPS.calculateFPS();
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

        pp.getFlux().subscribe(res->{

            // log
            String algoLog;
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

            List<ContourROI> detectedContours = res.result().frameMatchChangeResult().contourRegionOfInterest();

            Log.i(TAG, "writing points: " + points.size() + " from " + detectedContours.size() + " contours. "
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

            float imageWidth = res.result().frame().frame().width();
            float imageHeight = res.result().frame().frame().height();

            Log.i(TAG, "view: " + viewWidth + "x" + viewHeight + ", image: " + imageWidth + "x" + imageHeight);

            // display
            drawingOverlay.setCircles(DrawingOverlay.transformToViewCoordinates(
                    points,
                    imageWidth,
                    imageHeight,
                    viewWidth,
                    viewHeight));

            if(detectedContours.isEmpty()) return;
            List<PointF> firstPath = OpenCVTypeUtils.convert(detectedContours.get(0).originalContour());
            List<PointF> transformedPath = DrawingOverlay.transformToViewCoordinates(
                    firstPath,
                    imageWidth,
                    imageHeight,
                    viewWidth,
                    viewHeight);
            drawingOverlay.setPath(transformedPath);

        });
    }

    public void setImageAnalyzer(IImageAnalyzerAdapter imageAnalyzer){
        this.imageAnalyzer = imageAnalyzer;
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

    }



}