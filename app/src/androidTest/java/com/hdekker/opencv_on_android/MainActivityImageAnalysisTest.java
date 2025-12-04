package com.hdekker.opencv_on_android;

import static org.hamcrest.MatcherAssert.*;

import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;

import reactor.core.publisher.Flux;

/**
 * Instrumented test to verify that MainActivity.latestImage is populated
 * by the CameraX ImageAnalysis use case.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityImageAnalysisTest {

    private static final String TAG = "ImageAnalysisTest";

    private MainActivity activity;

    @Rule
    public final MainActivityPermissionRule activityTestRule = new MainActivityPermissionRule();

    @Before
    public void setUp() {
        ActivityScenario<MainActivity> scenario = activityTestRule.getScenario();
        scenario.onActivity(act -> activity = act);

    }

    /***
     *  A smoke test to ensure the prod configuration
     *  processes images when the activity is started.
     *
     */
    @Test
    public void run_ExpectConstantFramesProcessedByProjectileAlgo(){

        Integer initialPipelineStat = activity.pp.getStat().framesProcessed();
        Log.i(TAG, "Pipeline has initially consumed " + initialPipelineStat + " images.");
        Flux.interval(Duration.ofMillis(100))
                .filter(l-> activity.pp.getStat().framesProcessed() > initialPipelineStat)
                .timeout(Duration.ofSeconds(10))
                .blockFirst();
        Log.i(TAG, "Pipeline consumed " + activity.pp.getStat().framesProcessed() + " frames.");

        Flux.interval(Duration.ofSeconds(1))
                .take(Duration.ofSeconds(60))
                .blockLast();

        // input frame rate equal to output frame rate, i.e not backing up the pipeline.
        Integer framesReceived = activity.pp.getStat().framesReceived();
        Integer framesProcessed = activity.pp.getStat().framesProcessed();

        assertThat("Frames in should be close to out.",
                (double) framesReceived,
                Matchers.closeTo(framesProcessed, 10));


    }

}

