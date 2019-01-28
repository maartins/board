package com.martins.board;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.security.InvalidParameterException;

class CameraDistortionParams {
    private static final String TAG = "CameraDistortionParams";
    private static Mat cameraMatrix = new Mat();
    private static Mat distCoeff = new Mat();
    private static boolean valid = false;

    public static void readParams() {
        Log.d(TAG, "CHECKING FOR EXISTING CAMERA DISTORTION PARAMS.");
        if (PersonalUtils.fileExists(TAG + ".json")) {
            Log.d(TAG, "EXISTING CAMERA DISTORTION PARAMS FOUND.");

            try {
                JSONObject mainJSONObj = new JSONObject(PersonalUtils.readJSON(TAG));
                JSONArray camMatJSONArray = (JSONArray) mainJSONObj.get("cameraMatrix");
                JSONArray distCoeffJSONArray = (JSONArray) mainJSONObj.get("distCoeff");
                Log.d(TAG, "camMatJSONArray: " + camMatJSONArray.toString());
                Log.d(TAG, "distCoeffJSONArray: " + distCoeffJSONArray.toString());

                cameraMatrix = new Mat(3, 3, CvType.CV_64F);
                distCoeff = new Mat(1, 5, CvType.CV_64F);

                int k = 0;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        cameraMatrix.put(i, j, camMatJSONArray.getDouble(k++));
                    }
                }

                int j = 0;
                for (int i = 0; i < 5; i++) {
                    distCoeff.put(0, i, distCoeffJSONArray.getDouble(j++));
                }

                valid = true;
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "EXISTING CAMERA DISTORTION PARAMS FILE NOT FOUND.");
        }
    }

    public static void saveCameraDistortionParams(Mat cameraMatrix, Mat distCoeff) {
        Log.d(TAG, "SAVE NEW CAMERA DISTORTION PARAMS");
        if (cameraMatrix.size().width != 3 && cameraMatrix.size().height != 3)
            throw new InvalidParameterException("Camera matrix is the wrong size: "
                    + cameraMatrix.size()
                    + " must be (3, 3)");
        if (distCoeff.size().width != 5 && distCoeff.size().height != 1)
            throw new InvalidParameterException("Distortion coeff matrix is the wrong size: "
                    + distCoeff.size()
                    + " must be (5, 1)");

        CameraDistortionParams.cameraMatrix = cameraMatrix;
        CameraDistortionParams.distCoeff = distCoeff;

        JSONObject mainJSONObj = new JSONObject();
        JSONArray camMatJSONArray = new JSONArray();
        JSONArray distCoeffJSONArray = new JSONArray();

        for (int i = 0; i < cameraMatrix.size().width; i++) {
            for (int j = 0; j < cameraMatrix.size().height; j++) {
                // get returns double[] where each value is a channel in Mat
                // e.g. RGB would be tmp [0] [1] [2]
                double[] value = cameraMatrix.get(i, j);
                try {
                    camMatJSONArray.put(value[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d(TAG, "camera matrix: " + camMatJSONArray.toString());

        for (int i = 0; i < distCoeff.size().width; i++) {
            double[] value = distCoeff.get(0, i);
            try {
                distCoeffJSONArray.put(value[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "distortion coeffs: " + distCoeffJSONArray.toString());

        try {
            mainJSONObj.put("cameraMatrix", camMatJSONArray);
            mainJSONObj.put("distCoeff", distCoeffJSONArray);
            Log.d(TAG, "JSON result: " + mainJSONObj.toString());
            PersonalUtils.writeJSON(TAG, mainJSONObj);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public static Mat getDistCoeff() {
        if (valid)
            return distCoeff.clone();
        else
            return new Mat();
    }

    public static Mat getCameraMatrix() {
        if (valid)
            return cameraMatrix.clone();
        else
            return new Mat();
    }

    public static boolean hasParameters(){
        return valid;
    }
}