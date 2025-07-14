package com.hdekker.opencv_on_android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.util.Log;

import androidx.annotation.Size;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

/**
 * Instrumented test to verify that MainActivity.latestImage is populated
 * by the CameraX ImageAnalysis use case.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityImageAnalysisTest {

    private static final String TAG = "ImageAnalysisTest";
    // Timeout for waiting for an image from ImageAnalysis (in milliseconds)
    private static final long INITIAL_FRAME_WAIT_TIMEOUT_MS = 10000; // Time to wait for the first couple of frames
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

    private Mat getLatestNonNullMatFromActivity(MainActivity currentActivity) {
        if (currentActivity != null && currentActivity.imageAnalyzer != null && currentActivity.imageAnalyzer.latestMatImage != null) {
            // Important: Clone the Mat to avoid issues with the underlying buffer being reused
            // or modified by the ImageAnalysis thread while you're processing it.
            return currentActivity.imageAnalyzer.latestMatImage.clone();
        }
        return null;
    }

    // slowAlgo
    SlowAlgo algo = new SlowAlgo();
    ImageAnalyzer ia = new ImageAnalyzer(algo);
    // fastAlgo
    /**
     *  The rate of images processed by the algorithm.
     */
    WindowedFPSCalculator outputFPS = new WindowedFPSCalculator(1000.0f);

    @Test
    public void imagePipeline_shouldCaptureFpsAndLatency() throws InterruptedException {

        Log.d(TAG, "Starting FPS test. Target FPS: " + CONFIGURED_FPS + ", Test Duration: " + FPS_TEST_DURATION_SECONDS + "s");
        scenario.onActivity(act -> {
            act.setImageAnalyzer(ia);
        });

        algo.getOutputFlux()
                .timeout(Duration.ofSeconds(20))
                .take(2)
                .blockLast();

        Log.d(TAG, "Starting " + FPS_TEST_DURATION_SECONDS + "s measurement period for FPS.");

        List<Mat> results = algo.getOutputFlux()
                .take(Duration.ofSeconds(10))
                .doOnNext(m -> outputFPS.recordFrameTimestamp(System.nanoTime()))
                .collectList()
                .block();

        double inputFPS = activity.imageAnalyzer.inputFPS.calculateFPS();
        double achievedFps = outputFPS.calculateFPS();

        Log.i(TAG, "Input FPS: " + inputFPS + ", Achieved FPS: " + achievedFps);

        assert results != null;
        assertThat("Results size around 300 frames",
                (double) results.size(),
                Matchers.closeTo(300, 100));


        assertThat("Measured Input FPS equal to output fps",
                inputFPS,
                Matchers.closeTo(achievedFps, 2.0));

        if(inputFPS < (double)MINIMUM_ACCEPTED_FPS + 1 &&  inputFPS > (double)MINIMUM_ACCEPTED_FPS - 1) {
            assertThat("Achieved FPS check",
                    achievedFps,
                    Matchers.greaterThanOrEqualTo((double) MINIMUM_ACCEPTED_FPS));
        }else{
            Log.w(TAG, "Input frame rate did not meet the minimum. Possibly slow from AE.");
        }


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

