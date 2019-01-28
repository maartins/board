package com.martins.board;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraOpenCV implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OpenCVCameraApi";

    private Mat frame;

    private CameraBridgeViewBase openCvCameraView;
    private BaseLoaderCallback loaderCallback;

    private Activity activity;

    private int counter = 0;
    private boolean isCameraCalibration = false;

    public Button.OnClickListener buttonListener;

    CameraOpenCV(final Activity activity, final CameraBridgeViewBase surfaceView) {
        this.activity = activity;

        openCvCameraView = surfaceView;
        openCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);

        loaderCallback = new BaseLoaderCallback(this.activity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i(TAG, "OpenCV loaded successfully");
                        openCvCameraView.enableView();
                    } break;
                    default: {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        buttonListener = new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: calibrate button");
                isCameraCalibration = true;
                counter = 0;
            }
        };
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat color = inputFrame.rgba();
        frame = inputFrame.gray();

        if (isCameraCalibration) {
            //Log.d(TAG, "CALIBRATING CAMERA");

            //String s = processCharucoBoard(frame.getNativeObjAddr(), counter);

            //Log.d(TAG, "onImageAvailable: " + s);

            counter++;
            if (counter > 50) {
                isCameraCalibration = false;
                //s = calibrateCharucoBoard();

                //Log.d(TAG, "onImageAvailable: " + s);
            }
        } else {
            //String s = detectBoard(frame.getNativeObjAddr());

            //Log.d(TAG, "onImageAvailable: " + s);
        }

        return color;
    }

    public void onPause() {
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    public void onResume() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, activity, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    //private native String processCharucoBoard(long mat, int decimator);
    //private native String calibrateCharucoBoard();
    //private native String detectBoard(long mat);
}
