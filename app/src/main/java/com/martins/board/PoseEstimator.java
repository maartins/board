package com.martins.board;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class PoseEstimator implements Callable<PoseResult> {
    private static final String TAG = "PoseEstimator";
    private static final float MARKER_LENGTH = 0.04f;
    private static final Dictionary DICT = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);

    private DetectorParameters detectorParams = DetectorParameters.create();
    private List<Mat> markerCorners = new ArrayList<>();
    private List<Mat> rejectedImgPoints = new ArrayList<>();
    private Mat markerIds = new Mat();
    private Mat frame = new Mat();

    @Override
    public PoseResult call() throws Exception {
        PoseResult result = new PoseResult();
        Mat cameraMatrix = CameraDistortionParams.getCameraMatrix();
        Mat distCoeff = CameraDistortionParams.getDistCoeff();

        Aruco.detectMarkers(frame, DICT, markerCorners, markerIds, detectorParams,
                rejectedImgPoints, cameraMatrix, distCoeff);

        if (markerIds.size().height > 0) {
            Mat rvecs = new Mat();
            Mat tvecs = new Mat();
            Mat rvecs_new = new Mat(1, 3, CvType.CV_64F);
            Mat tvecs_new = new Mat(1, 3, CvType.CV_64F);

            Aruco.estimatePoseSingleMarkers(
                    markerCorners, MARKER_LENGTH, cameraMatrix, distCoeff, rvecs, tvecs);

            rvecs_new.put(0, 0, rvecs.get(0, 0));
            tvecs_new.put(0, 0, tvecs.get(0, 0));

            result.setRvec(rvecs_new.clone());
            result.setTvec(tvecs_new.clone());

            for (int i = 0; i < markerIds.size().height; i++)
                Aruco.drawAxis(frame, cameraMatrix, distCoeff, rvecs_new, tvecs_new, 0.1f);

            drawDebugFrame(frame);

            rvecs_new.release();
            tvecs_new.release();

            rvecs.release();
            tvecs.release();
        }
        frame.release();

        return result;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    private void drawDebugFrame(Mat frame){
        Bitmap tbmp = Bitmap.createBitmap(
                (int) frame.size().width, (int) frame.size().height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(frame, tbmp);
        FrameLogger.setDebugFrame(tbmp);
    }
}
