package com.hdekker.opencv_on_android.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;

public class CameraLogger {

    public static final String TAG = "CameraLogger";
    public static void logAllCameraCapabilities(CameraManager cameraManager) {

        if (cameraManager == null) {
            Log.e(TAG, "CameraManager not available.");
            return;
        }

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                Log.d(TAG, "--- Camera ID: " + cameraId + " ---");
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // Get camera facing direction
                Integer cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                String facingString = "Unknown";
                if (cameraFacing != null) {
                    facingString = switch (cameraFacing) {
                        case CameraCharacteristics.LENS_FACING_FRONT -> "Front";
                        case CameraCharacteristics.LENS_FACING_BACK -> "Back";
                        case CameraCharacteristics.LENS_FACING_EXTERNAL -> "External";
                        default -> facingString;
                    };
                }
                Log.d(TAG, "  Facing: " + facingString);

                // Get Hardware Level
                Integer hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                String hardwareLevelString = getString(hardwareLevel);
                Log.d(TAG, "  Hardware Level: " + hardwareLevelString);

                // 1. Log supported output formats
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                );

                if (map == null) {
                    Log.w(TAG, "  StreamConfigurationMap is null for camera ID: " + cameraId);
                    continue;
                }

                int[] outputFormats = map.getOutputFormats();
                if (outputFormats != null && outputFormats.length > 0) {
                    Log.d(TAG, "  Supported Output Formats:");
                    for (int format : outputFormats) {
                        Log.d(TAG, "    - " + formatToString(format));
                    }
                } else {
                    Log.d(TAG, "  No supported output formats found.");
                }

                // 2. Log resolutions and max frame rates for specific formats
                // We're interested in YUV_420_888 for ImageAnalysis
                Log.d(TAG, "  Resolutions & Frame Rates for ImageFormat.YUV_420_888 (for ImageAnalysis):");
                Size[] yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                if (yuvSizes != null && yuvSizes.length > 0) {
                    for (Size size : yuvSizes) {
                        long minFrameDurationNs = map.getOutputMinFrameDuration(ImageFormat.YUV_420_888, size);
                        String maxFps;
                        if (minFrameDurationNs > 0) {
                            maxFps = String.valueOf((int) (1_000_000_000.0 / minFrameDurationNs));
                        } else {
                            maxFps = "N/A (unknown duration)";
                        }
                        Log.d(TAG, "    - Size: " + size.getWidth() + "x" + size.getHeight() + ", Max FPS: " + maxFps);
                    }
                } else {
                    Log.d(TAG, "    No YUV_420_888 sizes found.");
                }

                // You can also check for other common formats:
                Log.d(TAG, "  Resolutions & Frame Rates for ImageFormat.JPEG (for ImageCapture):");
                Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                if (jpegSizes != null && jpegSizes.length > 0) {
                    for (Size size : jpegSizes) {
                        long minFrameDurationNs = map.getOutputMinFrameDuration(ImageFormat.JPEG, size);
                        String maxFps;
                        if (minFrameDurationNs > 0) {
                            maxFps = String.valueOf((int) (1_000_000_000.0 / minFrameDurationNs));
                        } else {
                            maxFps = "N/A (unknown duration)";
                        }
                        Log.d(TAG, "    - Size: " + size.getWidth() + "x" + size.getHeight() + ", Max FPS: " + maxFps);
                    }
                } else {
                    Log.d(TAG, "    No JPEG sizes found.");
                }

                // 3. Log available AE (Auto-Exposure) target FPS ranges
                Range<Integer>[] availableFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if (availableFpsRanges != null && availableFpsRanges.length > 0) {
                    Log.d(TAG, "  Available AE Target FPS Ranges:");
                    for (Range<Integer> range : availableFpsRanges) {
                        Log.d(TAG, "    - [" + range.getLower() + ", " + range.getUpper() + "] FPS");
                    }
                } else {
                    Log.d(TAG, "  No AE Target FPS Ranges found.");
                }

                Log.d(TAG, "---------------------------------");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera characteristics: " + e.getMessage());
        }
    }

    @NonNull
    private static String getString(Integer hardwareLevel) {
        String hardwareLevelString = "UNKNOWN";
        if (hardwareLevel != null) {
            hardwareLevelString = switch (hardwareLevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ->
                        "LIMITED";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
                        "EXTERNAL";
                default -> hardwareLevelString;
            };
        }
        return hardwareLevelString;
    }

    private static String formatToString(int format) {
        return switch (format) {
            case ImageFormat.YUV_420_888 -> "YUV_420_888";
            case ImageFormat.JPEG -> "JPEG";
            case ImageFormat.RAW_SENSOR -> "RAW_SENSOR";
            case ImageFormat.DEPTH16 -> "DEPTH16";
            case ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD";
            case ImageFormat.NV21 -> "NV21";
            case ImageFormat.YUY2 -> "YUY2";
            case ImageFormat.YV12 -> "YV12";
            default -> "Format_" + format;
        };
    }

}
