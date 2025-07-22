package com.hdekker.opencv_on_android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.hdekker.opencv_on_android.reactor.SlowAlgo;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;

import java.time.Duration;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Instrumented test to verify that MainActivity.latestImage is populated
 * by the CameraX ImageAnalysis use case.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityImageAnalysisTest {

    private static final String TAG = "ImageAnalysisTest";
    private static final long FPS_TEST_DURATION_SECONDS = 2;
    private static final int CONFIGURED_FPS = 30; // Example: Your target FPS
    private static final int MINIMUM_ACCEPTED_FPS = CONFIGURED_FPS - 1;

    private ActivityScenario<MainActivity> scenario;
    private MainActivity activity;

    @Before
    public void setUp() {
        scenario = activityRule.getScenario();
        scenario.onActivity(act -> {
            activity = act;
        });
        try {
            Thread.sleep(1000); // Reduce flakiness, but not ideal.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // slowAlgo
    SlowAlgo algo = new SlowAlgo(2000);
    ImageAnalyzer ia = new ImageAnalyzer(algo);

    /**
     *  The rate of images processed by the algorithm.
     */
    WindowedFPSCalculator outputFPS = new WindowedFPSCalculator(1000.0f);

    @Test
    public void slowImagePipeline_ExpectInputFPSFasterThanOutputFPS() throws InterruptedException {

        Log.d(TAG, "Starting FPS test. Target FPS: " + CONFIGURED_FPS + ", Test Duration: " + FPS_TEST_DURATION_SECONDS + "s");

        scenario.onActivity(act -> {
            act.setImageAnalyzer(ia);
        });

        Log.d(TAG, "Priming image pipeline.");
        algo.getOutputFlux()
                .timeout(Duration.ofSeconds(20))
                .take(2)
                .blockLast();

        Log.d(TAG, "Image count so far is " + algo.imageCount);
        Log.d(TAG, "Starting " + FPS_TEST_DURATION_SECONDS + "s measurement period for FPS.");

        List<Mat> results = algo.getOutputFlux()
                .doOnNext(m -> {
                    Log.i(TAG, "Image received.");
                    outputFPS.recordFrameTimestamp(System.nanoTime());
                })
                .take(Duration.ofSeconds(5))
                .collectList()
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        Log.d(TAG, "Image count so far is " + algo.imageCount);

        double inputFPS = activity.imageAnalyzer.inputFPS.calculateFPS();
        double achievedFps = outputFPS.calculateFPS();

        Log.i(TAG, "Input FPS: " + inputFPS + ", Achieved FPS: " + achievedFps);

        assertThat("Results size have been provided.",
                (double) results.size(),
                Matchers.greaterThanOrEqualTo(10.0));

        assertThat("Measured Input FPS not equal to output fps",
                inputFPS,
                Matchers.not(Matchers.equalTo(achievedFps)));

        assertThat("Really want ot see a slow pipeline so, assert measured Input FPS not differs by more than 5 fps",
                Math.abs(inputFPS - achievedFps),
                Matchers.greaterThanOrEqualTo(5.0));

    }

    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.CAMERA);
    @Rule
    public RuleChain ruleChain = RuleChain
            .outerRule(permissionRule)
            .around(activityRule);

}

