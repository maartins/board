package com.martins.board.OpenCV;

import android.util.Log;

import com.martins.board.OpenCV.CameraDistortionParams;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.CharucoBoard;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CameraCalibrator implements Runnable{
    private static final String TAG = "CameraCalibrator";

    private final Dictionary DICT = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
    private final CharucoBoard CHARUCO_BOARD = CharucoBoard.create(5, 4, 0.025f, 0.0125f, DICT);

    private List<Mat> detectedMarkerCorners = new ArrayList<>();
    private Mat detectedMarkerIds = new Mat();

    private Mat calibCharucoCorners = new Mat();
    private Mat calibCharucoIds = new Mat();

    private List<Mat> validCharucoCorners = new ArrayList<>();
    private List<Mat> validCharucoIds = new ArrayList<>();

    private Mat cameraMatrix = new Mat();
    private Mat distCoeff = new Mat();

    private BlockingQueue<Mat> calibFrames = new LinkedBlockingQueue<>();

    @Override
    public void run(){
        Log.d(TAG, "Camera calibration initiated");

        if (calibFrames.size() > 0) {
            Size frameSize = new Size();

            for (Mat frame : calibFrames) {
                Log.d(TAG, "Next frame:");

                frameSize = frame.size();
                Log.d(TAG, "Frame size:" + frameSize);

                Aruco.detectMarkers(frame, DICT, detectedMarkerCorners, detectedMarkerIds);
                Log.d(TAG, "IDs detected: " + detectedMarkerIds.rows());

                if (detectedMarkerIds.rows() > 0) {
                    Aruco.interpolateCornersCharuco(detectedMarkerCorners, detectedMarkerIds, frame,
                            CHARUCO_BOARD, calibCharucoCorners, calibCharucoIds);

                    Log.d(TAG, "Charuco IDs detected: " + calibCharucoIds.rows());
                    if ((!calibCharucoIds.empty() && !calibCharucoCorners.empty())) {
                        if (calibCharucoIds.rows() > 3 && calibCharucoCorners.total() > 0) {
                            validCharucoCorners.add(calibCharucoCorners);
                            validCharucoIds.add(calibCharucoIds);
                            Log.d(TAG, "Valid charuco ID added");
                        }
                    }
                }
            }

            Log.d(TAG, "valid Charuco Corners size: " + validCharucoCorners.size());
            Log.d(TAG, "valid Charuco Ids size: " + validCharucoIds.size());

            if (validCharucoCorners.size() > 0 || validCharucoIds.size() > 0) {
                Aruco.calibrateCameraCharuco(validCharucoCorners, validCharucoIds, CHARUCO_BOARD,
                        frameSize, cameraMatrix, distCoeff);

                Log.d(TAG, "cameraMatrix size: " + cameraMatrix.size());
                Log.d(TAG, "distCoeff size: " + distCoeff.size());

                CameraDistortionParams.saveCameraDistortionParams(cameraMatrix, distCoeff);
            } else {
                Log.d(TAG, "FAILED TO CALIBRATE, not enough charuco IDs");
            }
        } else {
            throw new InternalError("0 frames provided for calibration");
        }
    }

    public void addFrame(Mat frame) {
        calibFrames.add(frame);
    }
}
