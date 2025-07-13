package com.hdekker.opencv_on_android;

import java.util.LinkedList;

public class WindowedFPSCalculator {

        private final LinkedList<Long> frameTimestamps;
        private final long windowSizeNanos;

        public WindowedFPSCalculator(float windowSizeMillis) {
            if (windowSizeMillis <= 0) {
                throw new IllegalArgumentException("Window size must be positive.");
            }
            this.windowSizeNanos = (long) (windowSizeMillis * 1_000_000L);
            this.frameTimestamps = new LinkedList<>();
        }

        /**
         * Records a new frame timestamp.
         * Timestamps should be in nanoseconds (e.g., from System.nanoTime()).
         */
        public void recordFrameTimestamp(long timestampNanos) {
            frameTimestamps.addLast(timestampNanos);
            // Remove timestamps older than the window
            while (!frameTimestamps.isEmpty() && (timestampNanos - frameTimestamps.getFirst() > windowSizeNanos)) {
                frameTimestamps.removeFirst();
            }
        }

        /**
         * Calculates the current FPS based on the frames within the window.
         * @return The calculated FPS, or 0.0 if not enough data.
         */
        public double calculateFPS() {
            if (frameTimestamps.size() < 2) {
                return 0.0; // Not enough frames to calculate FPS
            }
            long firstTimestamp = frameTimestamps.getFirst();
            long lastTimestamp = frameTimestamps.getLast();
            long durationNanos = lastTimestamp - firstTimestamp;

            if (durationNanos <= 0) {
                return 0.0; // Avoid division by zero or negative duration
            }

            // Number of intervals is number of frames - 1
            int frameCountInWindow = frameTimestamps.size();
            return (double) (frameCountInWindow - 1) * 1_000_000_000.0 / durationNanos;
        }

        /**
         * Calculates FPS based on the average time per frame within the current window.
         * This can be more stable if frame intervals are somewhat consistent.
         * @return The calculated FPS, or 0.0 if not enough data.
         */
        public double calculateFPSAverageInterval() {
            if (frameTimestamps.size() < 2) {
                return 0.0;
            }
            long durationNanos = frameTimestamps.getLast() - frameTimestamps.getFirst();
            if (durationNanos <= 0) return 0.0;
            // Number of intervals = number of frames - 1
            return (double) (frameTimestamps.size() - 1) * 1_000_000_000.0 / durationNanos;
        }


        public void reset() {
            frameTimestamps.clear();
        }
}


