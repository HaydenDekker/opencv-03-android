package com.hdekker.opencv_on_android;

import com.hdekker.opencv_02_ball_detection.domain.Frame;

public interface FrameConsumerPort {
    void receive(Frame frameFlux);
}
