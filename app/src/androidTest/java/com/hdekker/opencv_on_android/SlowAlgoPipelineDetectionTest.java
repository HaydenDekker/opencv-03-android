package com.hdekker.opencv_on_android;


import static org.hamcrest.MatcherAssert.assertThat;

import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hdekker.opencv_on_android.camera.ImageAnalyzerAdapter;
import com.hdekker.opencv_on_android.projectile.ProjectilePipeline;
import com.hdekker.opencv_on_android.reactor.SlowAlgo;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;

import java.time.Duration;
import java.util.List;

import reactor.core.scheduler.Schedulers;

@RunWith(AndroidJUnit4.class)
public class SlowAlgoPipelineDetectionTest {

    public final String TAG = "SlowAlgoPipelineDetectionTest";

    private static final long FPS_TEST_DURATION_SECONDS = 5;
    private static final int CONFIGURED_FPS = 30; // Example: Your target FPS

    private MainActivity activity;

    @Rule
    public final MainActivityPermissionRule activityTestRule = new MainActivityPermissionRule();

    final SlowAlgo<ImageProxy, Mat> algo = new SlowAlgo<>(2000, ImageAnalyzerAdapter.openCVConversion);

    @Before
    public void setUp() {
        ActivityScenario<MainActivity> scenario = activityTestRule.getScenario();
        scenario.onActivity(act -> activity = act);
        try {
            Thread.sleep(1000); // Reduce flakiness, but not ideal.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void slowImagePipeline_ExpectLowFrameRateDetected() {
        Log.d(TAG, "Starting FPS test. " +
                "Target FPS: " + CONFIGURED_FPS + ", " +
                "Test Duration: " + FPS_TEST_DURATION_SECONDS + "s.");

        Log.d(TAG, "Starting " + FPS_TEST_DURATION_SECONDS + "s measurement period for FPS.");

        List<Mat> results = algo.getOutputFlux()
                .doOnNext(Mat::release)
                .take(Duration.ofSeconds(FPS_TEST_DURATION_SECONDS))
                .collectList()
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        Log.d(TAG, "Image counted is " + algo.imageCount);

        double inputFPS = ProjectilePipeline.inputFPS.calculateFPS();
        double achievedFps = algo.outputFPS.calculateFPS();

        Log.i(TAG, "Input FPS: " + inputFPS + ", Achieved FPS: " + achievedFps);

        assert results != null;
        assertThat("Results size have been provided.",
                (double) results.size(),
                Matchers.greaterThanOrEqualTo(5.0));

        // Android does not allow too many outstanding tasks. So this is never asserted.

        assertThat("Really want ot see a slow pipeline so, assert measured FPS is less than 20fps",
                achievedFps,
                Matchers.lessThan(20.0));

    }

}
