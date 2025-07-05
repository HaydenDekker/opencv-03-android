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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented test to verify that MainActivity.latestImage is populated
 * by the CameraX ImageAnalysis use case.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityImageAnalysisTest {

    private static final String TAG = "ImageAnalysisTest";
    // Timeout for waiting for an image from ImageAnalysis (in milliseconds)
    private static final long IMAGE_WAIT_TIMEOUT_MS = 10000; // 10 seconds

    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(Manifest.permission.CAMERA);
    @Rule
    public RuleChain ruleChain = RuleChain
            .outerRule(permissionRule)
            .around(activityRule);

    private ActivityScenario<MainActivity> scenario;
    private MainActivity activity;

    @Before
    public void setUp() {
        scenario = activityRule.getScenario();
        // It's good practice to ensure the activity is RESUMED before proceeding,
        // as CameraX often starts in onResume.
        scenario.onActivity(act -> {
            activity = act; // Get a reference to the activity instance
        });
        // Give a brief moment for onResume and CameraX binding to initiate
        // A more robust solution would be an IdlingResource.
        try {
            Thread.sleep(1000); // Reduce flakiness, but not ideal.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void latestImage_shouldBePopulated_byImageAnalysis() throws InterruptedException {
        final CountDownLatch imageReceivedLatch = new CountDownLatch(1);
        AtomicReference<Mat> receivedImage = new AtomicReference<>(null);

        // We need to access the activity instance to check its property.
        // The best way to wait for an asynchronous operation like ImageAnalysis
        // is to either use an IdlingResource, or for simpler cases, poll
        // the condition with a timeout, or modify the SUT to provide a callback for tests.

        // Since MainActivity.latestImage is public, we can poll it.
        // We'll give it some time for CameraX to initialize and process a few frames.

        long startTime = System.currentTimeMillis();
        boolean imageFound = false;

        while (System.currentTimeMillis() - startTime < IMAGE_WAIT_TIMEOUT_MS) {
            final Mat[] currentActivityImage = new Mat[1];
            scenario.onActivity(act -> {
                currentActivityImage[0] = act.imageAnalyzer.latestMatImage;
            });

            if (currentActivityImage[0] != null) {
                Log.d(TAG, String.format("latestImage is not null. Type: " + currentActivityImage[0].type()));
                receivedImage.set(currentActivityImage[0]);
                imageReceivedLatch.countDown(); // Signal that an image was found
                imageFound = true;
                break;
            }
            // Wait a bit before polling again
            Thread.sleep(200); // Poll every 200ms
        }

        // Wait for the latch with a timeout (it might have already counted down)
        if (!imageReceivedLatch.await(1, TimeUnit.MILLISECONDS)) { // Short wait if already counted down
            if (!imageFound) { // Check if we broke the loop because image was found or timeout
                fail("Timeout: MainActivity.latestImage was not populated by ImageAnalysis within "
                        + IMAGE_WAIT_TIMEOUT_MS + "ms.");
            }
        }

        assertNotNull("MainActivity.latestImage should have been populated.", receivedImage.get());

        Mat mat = receivedImage.get();
        // Using AssertJ for more descriptive assertions
        assertFalse("Mat should not be empty", mat.empty());
        assertThat(mat.width(), Matchers.greaterThan(0));
        assertThat(mat.height(), Matchers.greaterThan(0));
        assertThat(mat.channels(), Matchers.greaterThan(0));

        Mat finalImage = receivedImage.get();
        if (finalImage != null) {
            Log.d(TAG, "Successfully asserted latestImage is not null.");
        }
    }

    @After
    public void tearDown() {
        // scenario.close(); // ActivityScenarioRule handles closing the activity.
        // If you were manually managing ImageProxy objects in the test, close them here.
    }
}