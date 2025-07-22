package com.hdekker.opencv_on_android.reactor;

import android.util.Log;

import com.hdekker.opencv_on_android.ReactiveImageAlgo;

import org.opencv.core.Mat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;

public class SlowAlgo implements ReactiveImageAlgo {

    public static final String SLOW_ALGO = "SlowAlgo";
    int algoSleepTimeMs;
    Sinks.Many<Mat> sink;

    public int imageCount = 0;

    public SlowAlgo(int algoSleepTimeMs){

        this.algoSleepTimeMs = algoSleepTimeMs;
        sink = Sinks.many()
               .multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
        // keep alive
        sink.asFlux()
                .doFinally(signalType -> Log.i("SlowAlgo", "Stream finished with signal: " + signalType))
                .doOnNext(m-> Log.i(SLOW_ALGO, "Image received background task."))
                .subscribe(m->{
                    imageCount++;
                }, (err) -> {
                    Log.e(SLOW_ALGO, "Error in sink");
                });

    }
    @Override
    public Sinks.Many<Mat> getInputSink() {
        return sink;
    }

    @Override
    public Flux<Mat> getOutputFlux() {
        return sink.asFlux()
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .map(mat-> {
                    long time = System.currentTimeMillis();
                    int cycles = 0;
                    while(time + 2000 > System.currentTimeMillis()){
                        cycles++;
                    }
                    Log.i(SLOW_ALGO, "Computed, " + cycles);
                    return mat;
                })
                .doOnCancel(()-> Log.i(SLOW_ALGO, "task cancelled."))
                .sequential()
                .doFinally(signalType -> {
                    // This will be called when the stream terminates (onComplete, onError, or Cancel)
                    Log.i(SLOW_ALGO, "Stream finished with signal: " + signalType);
                });
                //.onErrorResume();
    }
}
