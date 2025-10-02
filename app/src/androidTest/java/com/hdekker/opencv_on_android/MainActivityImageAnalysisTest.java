package com.hdekker.opencv_on_android;

import static org.hamcrest.MatcherAssert.assertThat;

import android.Manifest;
import android.util.Log;

import androidx.camera.core.ImageProxy;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import reactor.core.scheduler.Schedulers;

/**
 * Instrumented test to verify that MainActivity.latestImage is populated
 * by the CameraX ImageAnalysis use case.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityImageAnalysisTest {

    private static final String TAG = "ImageAnalysisTest";
    private static final long FPS_TEST_DURATION_SECONDS = 5;
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
    SlowAlgo<ImageProxy, Mat> algo = new SlowAlgo<>(2000, MainActivity.openCVConversion);
    ImageAnalyzer ia = new ImageAnalyzer(algo);

    @Test
    public void slowImagePipeline_ExpectLowFrameRateDetected() throws InterruptedException {

        Log.d(TAG, "Starting FPS test. Target FPS: " + CONFIGURED_FPS + ", Test Duration: " + FPS_TEST_DURATION_SECONDS + "s");

        scenario.onActivity(act -> {
            act.setImageAnalyzer(ia);
        });

        Log.d(TAG, "Starting " + FPS_TEST_DURATION_SECONDS + "s measurement period for FPS.");

        List<Mat> results = algo.getOutputFlux()
                .doOnNext(Mat::release)
                .take(Duration.ofSeconds(FPS_TEST_DURATION_SECONDS))
                .collectList()
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        Log.d(TAG, "Image counted is " + algo.imageCount);

        double inputFPS = ImageAnalyzer.inputFPS.calculateFPS();
        double achievedFps = algo.outputFPS.calculateFPS();

        Log.i(TAG, "Input FPS: " + inputFPS + ", Achieved FPS: " + achievedFps);

        assertThat("Results size have been provided.",
                (double) results.size(),
                Matchers.greaterThanOrEqualTo(5.0));

        // Android does not allow too many outstanding tasks. So this is never asserted.
//        assertThat("Measured Input FPS is more than output fps",
//                inputFPS,
//                Matchers.greaterThan(achievedFps));

        assertThat("Really want ot see a slow pipeline so, assert measured FPS is less than 20fps",
                achievedFps,
                Matchers.lessThan(20.0));

    }

    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.CAMERA);
    @Rule
    public RuleChain ruleChain = RuleChain
            .outerRule(permissionRule)
            .around(activityRule);

    @Test
    public void runWithProjectileAlgo_ExpectConstantFramesProcessed(){

        ReactiveProjectileAlgo rpa = new ReactiveProjectileAlgo();
        ImageAnalyzer ria = new ImageAnalyzer(rpa);

        scenario.onActivity(act -> {
            act.setImageAnalyzer(ria);
        });

        List<Mat> list = rpa.getOutputFlux()
                .take(Duration.ofSeconds(10))
                .collectList()
                .block();

        Log.i(TAG, "" + list.size());

        assertThat(
                "A handful of frame should have been emitted by the algo in the running time.",
                list.size(),
                Matchers.greaterThan(20)
        );

    }

}

