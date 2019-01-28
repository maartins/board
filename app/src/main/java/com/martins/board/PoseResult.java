package com.martins.board;

import org.opencv.core.Mat;

class PoseResult{
    private Mat rvec;
    private Mat tvec;

    public Mat getRvec() {
        return rvec.clone();
    }

    public void setRvec(Mat rvec) {
        this.rvec = rvec;
    }

    public Mat getTvec() {
        return tvec.clone();
    }

    public void setTvec(Mat tvec) {
        this.tvec = tvec;
    }
}
