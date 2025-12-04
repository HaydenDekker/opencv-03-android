package com.hdekker.opencv_on_android.projectile;

import com.hdekker.opencv_02_ball_detection.config.dev.algo.AlgoResult;
import com.hdekker.opencv_02_ball_detection.domain.ProjectileAlgoResult;

import reactor.core.publisher.Flux;

public interface ProjectileEventProducerPort {
    Flux<AlgoResult<ProjectileAlgoResult>> getFlux();
}
