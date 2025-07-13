package com.hdekker.opencv_on_android;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class WindowedFPSCalculatorTest {

        private static final double FPS_DELTA = 0.01; // Tolerance for floating point comparisons

        @Test
        public void givenNoTimeStamps_whenCalculateFPS_thenReturnsZero() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1000.0f); // 1 second window
            Assert.assertEquals(0.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(0.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }

        @Test
        public void givenOneTimeStamp_whenCalculateFPS_thenReturnsZero() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1000.0f);
            calculator.recordFrameTimestamp(System.nanoTime());
            Assert.assertEquals(0.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(0.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }

        @Test
        public void givenTwoTimeStampsExactlyOneSecondApart_whenCalculateFPS_thenReturnsOneFPS() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(2000.0f); // 2 sec window to ensure both fit
            long startTime = 0; // Relative nanoTime
            long oneSecondNanos = TimeUnit.SECONDS.toNanos(1);

            calculator.recordFrameTimestamp(startTime);
            calculator.recordFrameTimestamp(startTime + oneSecondNanos);

            Assert.assertEquals(1.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(1.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }

        @Test
        public void givenThreeTimeStampsSpanningOneSecondForTwoIntervals_whenCalculateFPS_thenReturnsTwoFPS() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(2000.0f);
            long startTime = 0;
            long halfSecondNanos = TimeUnit.MILLISECONDS.toNanos(500);

            calculator.recordFrameTimestamp(startTime);                     // t0
            calculator.recordFrameTimestamp(startTime + halfSecondNanos);   // t1 (0.5s later)
            calculator.recordFrameTimestamp(startTime + 2 * halfSecondNanos); // t2 (1.0s later than t0)

            // (3 frames - 1) / (1 second total duration) = 2 FPS
            Assert.assertEquals(2.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(2.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }


        @Test
        public void givenTimeStampsForSixtyFPSOverOneSecond_whenCalculateFPS_thenReturnsApproxSixtyFPS() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1500.0f); // 1.5s window
            long startTime = 0;
            long frameIntervalNanos = TimeUnit.SECONDS.toNanos(1) / 60; // Approx interval for 60 FPS

            for (int i = 0; i <= 60; i++) { // 61 timestamps to make 60 intervals
                calculator.recordFrameTimestamp(startTime + i * frameIntervalNanos);
            }
            // (61 frames - 1) / (1 second total duration) = 60 FPS
            Assert.assertEquals(60.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(60.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);

        }

        @Test
        public void givenTimeStampsWhereOldOnesFallOutOfWindow_whenCalculateFPS_thenReflectsOnlyWindow() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1000.0f); // 1 second window
            long frameIntervalNanos = TimeUnit.MILLISECONDS.toNanos(100); // 10 FPS (0.1s interval)
            long startTime = 0;

            // Add 10 frames within the first second (0s to 0.9s)
            for (int i = 0; i < 10; i++) {
                calculator.recordFrameTimestamp(startTime + i * frameIntervalNanos);
            }
            // At this point, we have 10 frames over 0.9 seconds
            // FPS = (10-1) / 0.9 = 9 / 0.9 = 10 FPS
            Assert.assertEquals(10.0, calculator.calculateFPS(), FPS_DELTA);


            // Add more frames that push the oldest ones out of the 1-second window
            // Current window: [0ns, 900_000_000ns]
            // Add frame at 1.0s. Window becomes [100_000_000ns, 1_000_000_000ns] (frame 0 removed)
            calculator.recordFrameTimestamp(startTime + 10 * frameIntervalNanos); // Timestamp at 1.0s
            // Now timestamps are [0.1s, 0.2s, ..., 1.0s]. Still 10 frames. Duration 0.9s. FPS = 10.
            Assert.assertEquals(10.0, calculator.calculateFPS(), 0.1); // Allow slightly larger delta for window edge cases


            // Add frame at 1.1s. Window becomes [200_000_000ns, 1_100_000_000ns] (frame 1 removed)
            calculator.recordFrameTimestamp(startTime + 11 * frameIntervalNanos); // Timestamp at 1.1s
            // Timestamps in window: [0.2s, ..., 1.1s]. Still 10 frames. Duration 0.9s. FPS = 10.
            Assert.assertEquals(10.0, calculator.calculateFPS(), 0.1);

            // Add enough frames to completely shift the window
            // Add 5 more frames (total 10 + 5 + 5 = 20 frames added)
            // This will push all the initial 0-0.9s frames out
            for (int i = 12; i < 22; i++) { // from 1.2s up to 2.1s
                calculator.recordFrameTimestamp(startTime + i * frameIntervalNanos);
            }
            // Window should now contain timestamps from 1.2s to 2.1s (10 frames)
            // First timestamp in window is (2.1s - 1.0s window = 1.1s, closest is 1.2s)
            // Last timestamp is 2.1s
            // Expected frames in window: 10 frames (from 1.2s to 2.1s inclusive)
            // Duration: 2.1s - 1.2s = 0.9s
            // FPS: (10-1) / 0.9s = 10 FPS
            Assert.assertEquals(10.0, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(10.0, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }

        @Test
        public void givenIrregularTimeStamps_whenCalculateFPS_thenReturnsCorrectAverage() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1000.0f);
            // Timestamps in nanoseconds
            calculator.recordFrameTimestamp(0);
            calculator.recordFrameTimestamp(100_000_000); // 0.1s (1 frame in 0.1s -> ~10 FPS for this interval)
            calculator.recordFrameTimestamp(600_000_000); // 0.6s (1 frame in 0.5s -> ~2 FPS for this interval)
            calculator.recordFrameTimestamp(800_000_000); // 0.8s (1 frame in 0.2s -> ~5 FPS for this interval)
            // Total 4 frames, 3 intervals. Total duration 0.8s.
            // FPS = (4-1) / 0.8 = 3 / 0.8 = 3.75 FPS
            Assert.assertEquals(3.75, calculator.calculateFPS(), FPS_DELTA);
            Assert.assertEquals(3.75, calculator.calculateFPSAverageInterval(), FPS_DELTA);
        }

        @Test
        public void givenRapidBurtsOfFrames_whenCalculateFPS_thenReflectsWindowAverage() {
            WindowedFPSCalculator calculator = new WindowedFPSCalculator(1000.0f);
            long t = 0;
            // 5 frames very quickly
            calculator.recordFrameTimestamp(t);
            calculator.recordFrameTimestamp(t += 1_000_000); // 1ms
            calculator.recordFrameTimestamp(t += 1_000_000); // 2ms
            calculator.recordFrameTimestamp(t += 1_000_000); // 3ms
            calculator.recordFrameTimestamp(t += 1_000_000); // 4ms
            // 5 frames, duration 4ms. FPS = (5-1) / 0.004 = 4 / 0.004 = 1000 FPS
            Assert.assertEquals(1000.0, calculator.calculateFPS(), FPS_DELTA);

            // Wait for a while, then another burst
            t = 500_000_000; // 0.5 seconds
            calculator.recordFrameTimestamp(t); // 6th frame
            calculator.recordFrameTimestamp(t += 1_000_000); // 7th frame
            calculator.recordFrameTimestamp(t += 1_000_000); // 8th frame
            // Now we have 8 frames. First is at 0ms, last is at 502ms.
            // Duration: 502ms. Frames: 8. Intervals: 7.
            // FPS = 7 / 0.502 = ~13.94
            Assert.assertEquals(7.0 / 0.502, calculator.calculateFPS(), FPS_DELTA);

            // Add frames to push the first burst out
            t = 1_100_000_000; // 1.1 seconds
            calculator.recordFrameTimestamp(t); // Timestamp that pushes out frame at 0ms
            // Window now starts effectively at 100_000_001 ns if the first frame was at 0
            // Or more precisely, timestamps older than (1.1s - 1.0s = 0.1s) are removed.
            // So timestamps from 0ms, 1ms, 2ms, 3ms, 4ms are all out.
            // Remaining: 500ms, 501ms, 502ms, 1100ms
            // Frames: 4. Duration: 1100ms - 500ms = 600ms = 0.6s
            // FPS = (4-1) / 0.6 = 3 / 0.6 = 5.0 FPS
            Assert.assertEquals(5.0, calculator.calculateFPS(), FPS_DELTA);
        }

        @Test(expected = IllegalArgumentException.class)
        public void givenZeroWindowSize_whenCreatingCalculator_thenThrowsException() {
            new WindowedFPSCalculator(0.0f);
        }

        @Test(expected = IllegalArgumentException.class)
        public void givenNegativeWindowSize_whenCreatingCalculator_thenThrowsException() {
            new WindowedFPSCalculator(-1.0f);
        }
}

