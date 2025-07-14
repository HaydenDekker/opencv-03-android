package com.hdekker.opencv_on_android;

import org.opencv.core.Mat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public interface ReactiveImageAlgo {

    public Sinks.Many<Mat> getInputSink();

    public Flux<Mat> getOutputFlux();

}
