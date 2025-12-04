package com.hdekker.opencv_on_android;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public interface ReactiveImageAlgo<T,K> {

    Sinks.Many<T> getInputSink();

    Flux<K> getOutputFlux();

}
