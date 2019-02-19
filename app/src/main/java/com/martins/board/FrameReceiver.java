package com.martins.board;

import org.opencv.core.Mat;

public interface FrameReceiver {
    void addFrame(Mat image);

    void addMatrixListener(DetecatbleObject listener);
}
