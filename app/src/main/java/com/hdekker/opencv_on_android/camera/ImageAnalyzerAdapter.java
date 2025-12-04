package com.hdekker.opencv_on_android.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.hdekker.opencv_02_ball_detection.domain.Frame;
import com.hdekker.opencv_on_android.FrameConsumerPort;
import com.hdekker.opencv_on_android.IImageAnalyzerAdapter;
import com.hdekker.opencv_on_android.ImageConversionUtils;

import org.opencv.core.Mat;

import java.util.function.Function;

/**
 * To receive images from androids cameraX stream, convert to a frame and send
 * into the application frame consumer port.
 */
public class ImageAnalyzerAdapter implements IImageAnalyzerAdapter, ImageAnalysis.Analyzer {

    private static final String TAG = "ImageAnalyzer";

    CameraUseCaseConfig cameraUseCaseConfig;
    public Integer processedFrameCount = 0;

    FrameConsumerPort frameConsumerPort;

    final PreviewView previewView;

    final Context context;

    final LifecycleOwner lifecycleOwner;

    public ImageAnalyzerAdapter(
            PreviewView previewView,
            Context context,
            LifecycleOwner lifecycleOwner

    ){
        this.previewView = previewView;
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
               Mat mat = openCVConversion.apply(imageProxy);
               imageProxy.close();
               Frame frame = new Frame(processedFrameCount++, mat);
               frameConsumerPort.receive(frame);
    }

    public static final Function<ImageProxy, Mat> openCVConversion = (imageProxy) -> {
        Mat bgrMat = null;
        try (imageProxy) {
            bgrMat = ImageConversionUtils.imageProxyToMat(imageProxy);
        } catch (Exception e) {
            Log.e(TAG, "Error during ImageProxy to Mat conversion: ", e);
        }
        return bgrMat;
    };

    @Override
    public void start(FrameConsumerPort frameConsumerPort) {

        this.frameConsumerPort = frameConsumerPort;
        if (cameraUseCaseConfig != null) {
            cameraUseCaseConfig.releaseCamera();
        }
        cameraUseCaseConfig = new CameraUseCaseConfig(context);
        cameraUseCaseConfig.startCamera(context,
                lifecycleOwner,
                previewView.getSurfaceProvider(), this,
                ()-> cameraUseCaseConfig.startRecording(context));

    }
}