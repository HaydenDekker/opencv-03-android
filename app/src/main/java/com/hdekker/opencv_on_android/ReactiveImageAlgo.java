package com.hdekker.opencv_on_android;

import org.opencv.core.Mat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public interface ReactiveImageAlgo<T,K> {

    public Sinks.Many<T> getInputSink();

    public Flux<K> getOutputFlux();

}
