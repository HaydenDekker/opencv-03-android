package com.hdekker.opencv_on_android.projectile;

import android.util.Log;

import java.time.Duration;

import reactor.core.publisher.Flux;

/**
 *  why? - to log pipeline telemetry for use by developers
 *  in understanding how this application is performing at runtime.
 *
  */

public class ProjectilePipelineMonitor {

    public static final String TAG = "ProjectilePipelineMonitor";

    public ProjectilePipelineMonitor(ProjectilePipeline projectilePipeline){

        Flux.interval(Duration.ofSeconds(2))
                .subscribe(c-> {

                    ProjectilePipeline.Stat stat = projectilePipeline.getStat();

                    Log.i(TAG, "Input FPS: " + stat.framesReceivedFPS() +
                            ", Achieved FPS: " + stat.framesProcessedFPS());
                    Log.i(TAG, "Pipeline received " + stat.framesReceived() + " frames.");
                    Log.i(TAG, "Pipeline consumed " + stat.framesProcessed() + " frames.");
                });

    }

}
