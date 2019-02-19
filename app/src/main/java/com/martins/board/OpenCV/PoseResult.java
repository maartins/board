package com.martins.board.OpenCV;

import org.opencv.core.Mat;

class PoseResult{
    private Mat rvec;
    private Mat tvec;

    Mat getRvec() {
        return rvec.clone();
    }

    void setRvec(Mat rvec) {
        this.rvec = rvec;
    }

    Mat getTvec() {
        return tvec.clone();
    }

    void setTvec(Mat tvec) {
        this.tvec = tvec;
    }
}
