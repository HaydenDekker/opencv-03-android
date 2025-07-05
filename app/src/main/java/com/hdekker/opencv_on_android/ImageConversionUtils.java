package com.hdekker.opencv_on_android;

import androidx.camera.core.ImageProxy;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import java.nio.ByteBuffer;

public class ImageConversionUtils {

    public static Mat imageProxyToMat(ImageProxy image) {
        if (image.getFormat() != android.graphics.ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format, YUV_420_888 expected, got " + image.getFormat());
        }

        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer(); // U-plane
        ByteBuffer vBuffer = planes[2].getBuffer(); // V-plane

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining(); // Size of U data
        int vSize = vBuffer.remaining(); // Size of V data

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize);

        // Copy U and V planes, handling interleaved or non-interleaved data
        // This part aims to construct an NV21 format byte array (Y plane, then interleaved VU planes).
        // For YUV_420_888, planes[1] is U and planes[2] is V.
        // A common way to get NV21 from YUV_420_888 is to copy Y, then V, then U with appropriate strides.
        // This example assumes a common scenario where pixel stride is 2 for chroma,
        // and manually interleaves V and U.

        int chromaPixelStride = planes[1].getPixelStride();
        int chromaRowStride = planes[1].getRowStride();

        if (chromaPixelStride == 2) { // UV interleaved (common for NV21/NV12 derived from YUV_420_888 planes)
            int uvIndex = ySize;
            for (int row = 0; row < image.getHeight() / 2; row++) {
                uBuffer.position(row * chromaRowStride);
                vBuffer.position(row * chromaRowStride);
                for (int col = 0; col < image.getWidth() / 2; col++) {
                    nv21[uvIndex++] = vBuffer.get(); // V
                    nv21[uvIndex++] = uBuffer.get(); // U
                }
            }
        } else { // U and V are separate (likely I420 if not interleaved)
            uBuffer.get(nv21, ySize, uSize);
            vBuffer.get(nv21, ySize + uSize, vSize);
        }

        Mat yuvMat = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        yuvMat.put(0, 0, nv21);

        Mat rgbaMat = new Mat();
        // Convert NV21 (YUV) to RGBA
        Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21, 4);

        // Handle rotation if needed (based on imageProxy.getImageInfo().getRotationDegrees())
        // This is a simplified example. You would get the actual rotation value
        // from imageProxy.getImageInfo().getRotationDegrees() and apply it.
        /*
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        switch (rotationDegrees) {
            case 90:
                Core.rotate(rgbaMat, rgbaMat, Core.ROTATE_90_CLOCKWISE);
                break;
            case 180:
                Core.rotate(rgbaMat, rgbaMat, Core.ROTATE_180);
                break;
            case 270:
                Core.rotate(rgbaMat, rgbaMat, Core.ROTATE_90_COUNTERCLOCKWISE);
                break;
        }
        */

        yuvMat.release(); // Release the intermediate YUV Mat

        return rgbaMat;

    }


    // 2. Easy Way to Get Grayscale
    public static Mat toGrayscale(Mat colorMat) {
        Mat grayMat = new Mat();
        if (colorMat.empty()) {
            return grayMat; // Return empty if input is empty
        }
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY); // Or COLOR_RGB2GRAY if your input Mat is RGB
        return grayMat;
    }
}
