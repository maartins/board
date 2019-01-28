package com.martins.board;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

interface FrameReciever {
    void addFrame(Mat image);

    void addMatrixListener(ViewMatrixListener listener);
}
