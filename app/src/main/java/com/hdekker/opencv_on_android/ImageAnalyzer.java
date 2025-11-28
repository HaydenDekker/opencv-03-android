package com.hdekker.opencv_on_android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.opencv.core.Mat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public class ImageAnalyzer<T> implements ImageAnalysis.Analyzer {

    private static final String TAG = "ImageAnalyzer";

    ReactiveImageAlgo<ImageProxy, T> algo;

    /**
     *  The rate of images provided to the algorithm.
     */
    static WindowedFPSCalculator inputFPS = new WindowedFPSCalculator(1000.0f);

    int lastNoListenerCount = 0;

    Disposable disposable;
    public ImageAnalyzer(ReactiveImageAlgo<ImageProxy, T> algo){

        this.algo = algo;

        Flux.interval(Duration.ofSeconds(2))
                .subscribe(c->{

                    double inputFPS = ImageAnalyzer.inputFPS.calculateFPS();
                    double achievedFps = 0;

                    if(lastNoListenerCount != noListenerCount){
                        Log.i(TAG, "No listeners connected");
                    }
                    Log.i(TAG, "Input FPS: " + inputFPS + ", Achieved FPS: " + achievedFps);

                });

        disposable = algo.getOutputFlux()
                .subscribe(eventConsumer);

    }

    public Mat latestMatImage = null;
    public AtomicInteger processedFrameCount = new AtomicInteger(0);

    boolean isInitialised = false;

    int noListenerCount = 0;
    Consumer<T> eventConsumer = (t) ->{ noListenerCount++; };

    @SuppressLint("UnsafeOptInUsageError") // For ImageProxy.getImage()
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {

//        Log.d(TAG, "ImageAnalysis: New frame received. Format: " + imageProxy.getFormat() +
//                ", Size: " + imageProxy.getWidth() + "x" + imageProxy.getHeight() +
//                ", Timestamp: " + imageProxy.getImageInfo().getTimestamp());

        long startTime = System.currentTimeMillis();
        inputFPS.recordFrameTimestamp(System.nanoTime());

        algo.getInputSink().emitNext(imageProxy, (err, a)->{
            imageProxy.close();
            Log.e(TAG, "err " + err.toString());
            return false;
        });

        long millis = System.currentTimeMillis() - startTime;
    }

    public void subscribeToEvents(Consumer<T> eventConsumer) {
        this.eventConsumer = eventConsumer;
        disposable.dispose();
        disposable = algo.getOutputFlux()
                .subscribe(eventConsumer);

    }
}