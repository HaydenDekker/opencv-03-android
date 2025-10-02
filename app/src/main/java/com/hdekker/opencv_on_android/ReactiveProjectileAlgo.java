package com.hdekker.opencv_on_android;

import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.hdekker.opencv_02_ball_detection.config.dev.algo.ProjectileAlgo;
import com.hdekker.opencv_02_ball_detection.detect.FrameChangeDetector;
import com.hdekker.opencv_02_ball_detection.domain.Frame;

import org.opencv.core.Mat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class ReactiveProjectileAlgo implements ReactiveImageAlgo<ImageProxy, Mat> {

    private static final String TAG = "ReactiveProjectileAlgo";
    Sinks.Many<ImageProxy> sink;
    int frameIndex = 0;

    ProjectileAlgo projectileAlgo;

    ReactiveProjectileAlgo(){

        sink = Sinks.many().multicast()
                .directAllOrNothing();


        sink.asFlux()
                .map(ip->
                    MainActivity.openCVConversion.apply(ip))
                .subscribe(mat-> {
                    Log.i(TAG,"" + mat.width() + " " + mat.height());
                    projectileAlgo.step(new Frame(frameIndex++, mat));
                });

    }

    @Override
    public Sinks.Many<ImageProxy> getInputSink() {
        return sink;
    }

    @Override
    public Flux<Mat> getOutputFlux() {
        return projectileAlgo.resultProducer()
                .map(ar->ar.result().frame().frame());
    }

    @Override
    public void init(ImageProxy imageProxy) {

        Mat initialFrame = MainActivity.openCVConversion.apply(imageProxy);
        Frame frame = new Frame(1, initialFrame);
        projectileAlgo = new ProjectileAlgo(new FrameChangeDetector(), frame);

    }

}
