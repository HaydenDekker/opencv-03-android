package com.hdekker.opencv_on_android;

import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.hdekker.opencv_02_ball_detection.config.dev.algo.AlgoResult;
import com.hdekker.opencv_02_ball_detection.config.dev.algo.ProjectileAlgo;
import com.hdekker.opencv_02_ball_detection.domain.Frame;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileAlgoResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class ReactiveProjectileAlgo implements ReactiveImageAlgo<ImageProxy, AlgoResult<ProjectileAlgoResult>> {

    private static final String TAG = "ReactiveProjectileAlgo";
    Sinks.Many<ImageProxy> sink;
    int frameIndex = 0;

    ProjectileAlgo projectileAlgo;

    ReactiveProjectileAlgo(){

        projectileAlgo = new ProjectileAlgo();

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
    public Flux<AlgoResult<ProjectileAlgoResult>> getOutputFlux() {
        return projectileAlgo.resultProducer();
    }

}
