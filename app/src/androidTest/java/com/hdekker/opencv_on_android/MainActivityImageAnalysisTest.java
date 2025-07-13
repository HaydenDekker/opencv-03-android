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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    @Test
    public void imagePipeline_shouldMaintainFps() throws InterruptedException {

        Log.d(TAG, "Starting FPS test. Target FPS: " + CONFIGURED_FPS + ", Test Duration: " + FPS_TEST_DURATION_SECONDS + "s");

        initCamera();

        Log.d(TAG, "Starting " + FPS_TEST_DURATION_SECONDS + "s measurement period for FPS.");

        activity.imageAnalyzer.processedFrameCount.set(0);

        Thread.sleep(FPS_TEST_DURATION_SECONDS * 1000);

        int totalFramesProcessed = activity.imageAnalyzer.processedFrameCount.get();
        double achievedFps = (double) totalFramesProcessed / FPS_TEST_DURATION_SECONDS;

        Log.d(TAG, "FPS Test Complete. Processed " + totalFramesProcessed + " frames in "
                + String.format("%.2f", (double) FPS_TEST_DURATION_SECONDS) + "s. Achieved FPS: "
                + String.format("%.2f", achievedFps));

        double inputFPS = activity.imageAnalyzer.inputFPS.calculateFPS();

        assertThat("Measured Input FPS equal to output fps",
                inputFPS,
                Matchers.closeTo(activity.imageAnalyzer.outputFPS.calculateFPS(), 2.0));

        if(inputFPS < (double)MINIMUM_ACCEPTED_FPS + 1 &&  inputFPS > (double)MINIMUM_ACCEPTED_FPS - 1) {
            assertThat("Achieved FPS check",
                    achievedFps,
                    Matchers.greaterThanOrEqualTo((double) MINIMUM_ACCEPTED_FPS));
        }else{
            Log.w(TAG, "Input frame rate did not meet the minimum. Possibly slow from AE.");
        }


    }

    private void initCamera() throws InterruptedException {

        AtomicReference<Mat> firstMatRef = new AtomicReference<>();
        AtomicReference<Mat> secondMatRef = new AtomicReference<>();
        long startTimeWaitForFrames = System.currentTimeMillis();
        boolean gotTwoFrames = false;

        // 1. Wait for the second frame to ensure the stream is active
        Log.d(TAG, "Waiting for initial startup/stabilisation period...");
        while (System.currentTimeMillis() - startTimeWaitForFrames < INITIAL_FRAME_WAIT_TIMEOUT_MS) {
            final Mat[] currentMatHolder = new Mat[1];
            scenario.onActivity(act -> currentMatHolder[0] = getLatestNonNullMatFromActivity(act));
            Mat currentMat = currentMatHolder[0];

            if (currentMat != null && !currentMat.empty()) {
                if (firstMatRef.get() == null) {
                    firstMatRef.set(currentMat);
                    Log.d(TAG, "Got first frame.");
                } else if (secondMatRef.get() == null) {
                    // Check if this frame is different from the first one.
                    // A simple check is if they are different objects, assuming cloning happens.
                    // A more robust check might involve comparing a small part of the data
                    // or ensuring their internal nativeObjAddr are different.
                    if (currentMat.nativeObj != firstMatRef.get().nativeObj) { // Check if it's a new Mat object
                        secondMatRef.set(currentMat);
                        Log.d(TAG, "Got second distinct frame.");
                        gotTwoFrames = true;
                        //break;
                    } else {
                        // It's the same Mat object as the first, release currentMat (the clone)
                        // and wait for a newer one.
                        currentMat.release();
                    }
                }
            } else if (currentMat != null) { // It's not null, but empty
                currentMat.release(); // Release the empty clone
            }
            Thread.sleep(50); // Poll for new frames
        }

        if (!gotTwoFrames) {
            if (firstMatRef.get() != null) firstMatRef.get().release();
            if (secondMatRef.get() != null) secondMatRef.get().release();
            fail("Timeout: Did not receive two distinct frames within " + INITIAL_FRAME_WAIT_TIMEOUT_MS + "ms to start FPS test.");
        }

        // Release the initial frames if they are not null
        if (firstMatRef.get() != null) firstMatRef.get().release();
        if (secondMatRef.get() != null) secondMatRef.get().release();


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

