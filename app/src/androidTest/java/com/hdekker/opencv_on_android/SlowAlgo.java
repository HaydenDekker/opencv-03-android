package com.hdekker.opencv_on_android;

import org.opencv.core.Mat;

import java.time.Duration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

public class SlowAlgo implements ReactiveImageAlgo{

    Sinks.Many<Mat> sink;
    public SlowAlgo(){

       sink = Sinks.many()
               .multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    }
    @Override
    public Sinks.Many<Mat> getInputSink() {
        return sink;
    }

    @Override
    public Flux<Mat> getOutputFlux() {
        return sink.asFlux()
                .publishOn(Schedulers.boundedElastic())
                .parallel()
                .runOn(Schedulers.parallel())
                .map(mat-> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return mat;
                })
                .sequential();
    }
}
