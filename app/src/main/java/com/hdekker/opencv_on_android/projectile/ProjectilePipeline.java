package com.hdekker.opencv_on_android.projectile;

import android.util.Log;

import com.hdekker.opencv_02_ball_detection.config.dev.algo.AlgoResult;
import com.hdekker.opencv_02_ball_detection.config.dev.algo.Algorithm;
import com.hdekker.opencv_02_ball_detection.domain.Frame;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileAlgoResult;
import com.hdekker.opencv_on_android.FrameConsumerPort;
import com.hdekker.opencv_on_android.metric.WindowedFPSCalculator;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

// state - config
public class ProjectilePipeline implements ProjectileEventProducerPort, FrameConsumerPort {

    public static final String TAG = "ProjectilePipeline";
    final Algorithm<ProjectileAlgoResult> algo;

    public record Stat(Integer framesProcessed){}

    final AtomicInteger framesProcessed = new AtomicInteger();

    final Sinks.Many<Frame> sink;

    final Flux<AlgoResult<ProjectileAlgoResult>> projectileAlgoResultProducer;

    /**
     *  The rate of images provided to the algorithm.
     */
    public static final WindowedFPSCalculator inputFPS = new WindowedFPSCalculator(1000.0f);
    public final WindowedFPSCalculator achievedFPS = new WindowedFPSCalculator(1000.0f);

    public ProjectilePipeline(Algorithm<ProjectileAlgoResult> algo){
        this.algo = algo;

        sink = Sinks.many().multicast()
                .directBestEffort();

        Flux<Frame> frameFlux = sink.asFlux()
                .doOnNext(ip-> inputFPS.recordFrameTimestamp(System.nanoTime()));

        Flux.interval(Duration.ofSeconds(2))
                .subscribe(c->
                        Log.i(TAG, "Input FPS: " + inputFPS.calculateFPS() +
                        ", Achieved FPS: " + achievedFPS.calculateFPS())
                );

        projectileAlgoResultProducer = algo.process(frameFlux)
                .doOnNext(a-> achievedFPS.recordFrameTimestamp(System.nanoTime()));

    }

    public Stat getStat(){
        return new Stat(framesProcessed.get());
    }

    @Override
    public Flux<AlgoResult<ProjectileAlgoResult>> getFlux() {
        return projectileAlgoResultProducer;
    }

    @Override
    public void receive(Frame frame) {
        framesProcessed.incrementAndGet();
        sink.emitNext(frame,
                (sig, err) -> false);
    }
}
