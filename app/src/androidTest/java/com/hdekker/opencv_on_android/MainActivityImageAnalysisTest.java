package com.hdekker.opencv_on_android;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import androidx.camera.core.ImageProxy;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

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
        AtomicReference<ImageProxy> receivedImage = new AtomicReference<>(null);

        // We need to access the activity instance to check its property.
        // The best way to wait for an asynchronous operation like ImageAnalysis
        // is to either use an IdlingResource, or for simpler cases, poll
        // the condition with a timeout, or modify the SUT to provide a callback for tests.

        // Since MainActivity.latestImage is public, we can poll it.
        // We'll give it some time for CameraX to initialize and process a few frames.

        long startTime = System.currentTimeMillis();
        boolean imageFound = false;

        while (System.currentTimeMillis() - startTime < IMAGE_WAIT_TIMEOUT_MS) {
            final ImageProxy[] currentActivityImage = new ImageProxy[1];
            scenario.onActivity(act -> {
                // Access the latestImage property from the activity instance
                // Ensure your MainActivity instance is accessible here.
                // If 'activity' from setUp is not consistently set due to timing,
                // re-fetch or pass it.
                currentActivityImage[0] = act.imageAnalyzer.latestImage;
            });

            if (currentActivityImage[0] != null) {
                Log.d(TAG, "latestImage is not null. Format: " + currentActivityImage[0].getFormat());
                // Optionally, you can add more assertions about the image here,
                // e.g., image.getWidth(), image.getHeight(), image.getFormat()
                // Make sure to close the ImageProxy if you are done with it to free resources,
                // though in a test just checking for non-null might be enough if the SUT handles closing.
                // For this test, we assume the MainActivity's analyzer handles closing.
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

        // Important: If your ImageAnalysis use case keeps a reference to the last ImageProxy,
        // and your test also gets a reference, ensure it's handled correctly to avoid
        // "max images exceeded" errors if the analyzer doesn't close them fast enough
        // or if the test holds onto it for too long.
        // For this test, we're just checking if it was populated. The actual ImageProxy
        // lifecycle is managed by your MainActivity's analyzer.
        ImageProxy finalImage = receivedImage.get();
        if (finalImage != null) {
            // In a real scenario, the Analyzer is responsible for closing the ImageProxy.
            // If the test is taking a reference that the analyzer might not know about,
            // or if the test needs to inspect it and the analyzer would close it,
            // coordination is needed. Here, we just check non-null.
            // If the `latestImage` field in MainActivity is replaced by newer frames,
            // then the old one should have been closed by the Analyzer.
            Log.d(TAG, "Successfully asserted latestImage is not null.");
        }
    }

    @After
    public void tearDown() {
        // scenario.close(); // ActivityScenarioRule handles closing the activity.
        // If you were manually managing ImageProxy objects in the test, close them here.
    }
}