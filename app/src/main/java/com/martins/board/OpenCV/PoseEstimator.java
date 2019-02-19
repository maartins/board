package com.martins.board.OpenCV;

import android.graphics.Bitmap;

import com.martins.board.FrameLogger;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

final class PoseEstimator {
    private static final String TAG = "PoseEstimator";
    private static final float MARKER_LENGTH = 0.04f;
    private final Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_100);

    private final DetectorParameters detectorParams = DetectorParameters.create();
    private final List<Mat> markerCorners = new ArrayList<>();
    private final List<Mat> rejectedImgPoints = new ArrayList<>();
    private Mat markerIds = new Mat();

    public final PoseResult run(Mat frame) {
        PoseResult result = null;
        Mat cameraMatrix = CameraDistortionParams.getCameraMatrix();
        Mat distCoeff = CameraDistortionParams.getDistCoeff();

        Aruco.detectMarkers(frame, dictionary, markerCorners, markerIds, detectorParams,
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

            result = new PoseResult();
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

    private void drawDebugFrame(Mat frame){
        Bitmap tbmp = Bitmap.createBitmap(
                (int) frame.size().width, (int) frame.size().height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(frame, tbmp);
        FrameLogger.setDebugFrame(tbmp);
    }
}

/*int retVal = Aruco.estimatePoseBoard(markerCorners, markerIds, GRID_BOARD, cameraMatrix,
        distCoeff, rvecs, tvecs);

if (retVal > 0) {
    //Log.d(TAG, "rvecs: " + rvecs.size() + " depth: " + rvecs.depth() + " dims: " + rvecs.dims());
    //Log.d(TAG, "tvecs: " + tvecs.size() + " depth: " + tvecs.depth() + " dims: " + tvecs.dims());

    Aruco.drawDetectedMarkers(frame, markerCorners, markerIds, new Scalar(120, 120, 120));
    drawDebugFrame(frame);
}*/