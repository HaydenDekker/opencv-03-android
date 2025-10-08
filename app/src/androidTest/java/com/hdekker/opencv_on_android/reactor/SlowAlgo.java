package com.hdekker.opencv_on_android.reactor;

import android.util.Log;

import com.hdekker.opencv_on_android.ReactiveImageAlgo;
import com.hdekker.opencv_on_android.WindowedFPSCalculator;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

public class SlowAlgo<T, K> implements ReactiveImageAlgo<T, K> {

    public static final String SLOW_ALGO = "SlowAlgo";
    int algoSleepTimeMs;
    Sinks.Many<T> sink;

    public int imageCount = 0;

    Function<T, K> mappingFunction;

    /**
     *  The rate of images processed by the algorithm.
     */
    public WindowedFPSCalculator outputFPS = new WindowedFPSCalculator(1000.0f);

    public SlowAlgo(int algoSleepTimeMs,
                    Function<T, K> mappingFunction){

        this.mappingFunction = mappingFunction;
        this.algoSleepTimeMs = algoSleepTimeMs;
        sink = Sinks.many()
               .multicast().onBackpressureBuffer(4,false);
        // keep alive
        sink.asFlux()
                .doFinally(signalType -> Log.i("SlowAlgo", "Stream finished with signal: " + signalType))
                //.doOnNext(m-> Log.i(SLOW_ALGO, "Image received background task."))
                .subscribe(m-> imageCount++, (err) -> Log.e(SLOW_ALGO, "Error in sink"));

    }
    @Override
    public Sinks.Many<T> getInputSink() {
        return sink;
    }

    @Override
    public Flux<K> getOutputFlux() {
        return sink.asFlux()
                .parallel()
                .runOn(Schedulers.newBoundedElastic(4, 4, "ImageProcessor"))
                .map(mappingFunction)
                .map(mat-> {
                    long time = System.currentTimeMillis();
                    int cycles = 0;
                    while(time + 500 > System.currentTimeMillis()){
                        cycles++;
                    }
                    Log.i(SLOW_ALGO, "Computed, " + cycles);
                    return mat;
                })
                .doOnCancel(()-> Log.i(SLOW_ALGO, "task cancelled."))
                .sequential()
                .doOnNext(m -> outputFPS.recordFrameTimestamp(System.nanoTime()))
                .doFinally(signalType -> {
                    // This will be called when the stream terminates (onComplete, onError, or Cancel)
                    Log.i(SLOW_ALGO, "Stream finished with signal: " + signalType);
                });
                //.onErrorResume();
    }



}
