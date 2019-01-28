package com.martins.board.OpenCV;

import android.graphics.Bitmap;
import android.util.Log;

import com.martins.board.DetecatbleObject;
import com.martins.board.FrameLogger;
import com.martins.board.FrameReciever;

import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ArucoProcessing implements FrameReciever, ArucoProcessStateListener {
    private static final String TAG = "Aruco";
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int VIEW_MAT_AVG_LIST_SIZE = 3;
    private static final float MARKER_LENGTH = 0.04f;
    private static final Dictionary DICT = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
    private static final GridBoard GRID_BOARD = GridBoard.create(5, 7, MARKER_LENGTH, 0.01f, DICT);
    private static final CameraCalibrator CALIBRATOR = new CameraCalibrator();
    private static final int CALIB_COUNT = 5;

    private final Mat OPENGL_CONVERT_MATRIX = Mat.ones(4, 4, CvType.CV_64F);

    private final BlockingQueue<Mat> queue = new LinkedBlockingQueue<>();
    private BlockingQueue<PoseResult> poseResultList = new LinkedBlockingQueue<>();

    private DetecatbleObject detectableObject;
    private List<float[]> viewMatrixList = Collections.synchronizedList(new ArrayList<>(VIEW_MAT_AVG_LIST_SIZE));

    private int curViewMatAvgListPos = 0;
    private int curCalibCount = 0;

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(NUMBER_OF_CORES);
    private PoseEstimator pe = new PoseEstimator();
    private Future<?> estimateResult;
    private Future<?> matrixConversionResult;

    public ArucoProcessing() {
        CameraDistortionParams.readParams();

        viewMatrixList.add(new float[16]);
        viewMatrixList.add(new float[16]);
        viewMatrixList.add(new float[16]);

        for (int i = 1; i < 3; i++)
            for (int j = 0; j < 4; j++)
                OPENGL_CONVERT_MATRIX.put(i, j, -1f);

        exec.scheduleAtFixedRate(() -> {
            if (queue.size() > 20)
                clearQueue();
            Log.d(TAG, "" + poseResultList.size());
        }, 0,1, TimeUnit.SECONDS);
    }

    private void addOpenglViewMatrixToTheList(float[] viewMatrix) {
        if (curViewMatAvgListPos >= VIEW_MAT_AVG_LIST_SIZE)
            curViewMatAvgListPos = 0;

        viewMatrixList.set(curViewMatAvgListPos, viewMatrix);
        curViewMatAvgListPos++;
    }

    private float[] getAverageOpenglViewMatrix() {
        float[] avgViewMatrix = new float[16];

        for (float[] curViewMatrix : viewMatrixList)
            for (int i = 0; i < curViewMatrix.length; i++)
                avgViewMatrix[i] += curViewMatrix[i];

        for (int i = 0; i < avgViewMatrix.length; i++)
            avgViewMatrix[i] /= VIEW_MAT_AVG_LIST_SIZE;

        return avgViewMatrix;
    }

    private void drawDebugFrame(Mat frame){
        Bitmap tbmp = Bitmap.createBitmap(
                (int) frame.size().width, (int) frame.size().height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(frame, tbmp);
        FrameLogger.setDebugFrame(tbmp);
    }

    private float[] createOpenglViewMatrix(Mat rvecs_new, Mat tvecs_new) {
        Mat rotationMatrix = new Mat(3, 3, CvType.CV_64F);
        Calib3d.Rodrigues(rvecs_new, rotationMatrix);

        Mat viewMatrix = Mat.zeros(4, 4, CvType.CV_64F);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                viewMatrix.put(i, j, rotationMatrix.get(i, j)[0]);
            }
                viewMatrix.put(i, 3, tvecs_new.get(0, i)[0] * 4f);
        }
        viewMatrix.put(3, 3, 1f);

        Core.multiply(viewMatrix, OPENGL_CONVERT_MATRIX, viewMatrix);

        float[] openglViewMatrix = new float[16];

        int counter = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                // convert to opengl and transpose
                openglViewMatrix[counter] = (float)viewMatrix.get(j, i)[0];
                counter++;
            }
        }

        rotationMatrix.release();

        return openglViewMatrix;
    }

    private Mat getFrameFromQueue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new Mat(1, 1, CvType.CV_64F);
    }

    public boolean addCalibrationFrame() {
        curCalibCount++;
        if (queue.size() > 2) {
            CALIBRATOR.addFrame(getFrameFromQueue());
            CALIBRATOR.addFrame(getFrameFromQueue());
            clearQueue();
        }

        if (curCalibCount >= CALIB_COUNT) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(CALIBRATOR);
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                curCalibCount = 0;
                detectableObject.setIsObjectHidden(false);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void addFrame(Mat image) {
        try {
            if (image.channels() == 4) {
                Mat frame = new Mat();
                Imgproc.cvtColor(image, frame, Imgproc.COLOR_RGBA2GRAY);
                image.release();
                queue.put(frame);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMatrixListener(DetecatbleObject listener) {
        detectableObject = listener;
    }

    @Override
    public void detect() {
        estimateResult = exec.scheduleAtFixedRate(() -> {
            if (queue.size() > 0 && CameraDistortionParams.hasParameters()) {
                PoseResult result = pe.run(getFrameFromQueue());
                if (result != null)
                    poseResultList.add(result);
            }
        }, 0,5, TimeUnit.MILLISECONDS);

        estimateResult = exec.scheduleAtFixedRate(() -> {
            if (queue.size() > 0 && CameraDistortionParams.hasParameters()) {
                PoseResult result = pe.run(getFrameFromQueue());
                if (result != null)
                    poseResultList.add(result);
            }
        }, 1,5, TimeUnit.MILLISECONDS);

        matrixConversionResult = exec.scheduleAtFixedRate(() -> {
            if (poseResultList.size() > 0) {
                PoseResult result = null;
                try {
                    result = poseResultList.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Mat rvecs_new = result.getRvec();
                Mat tvecs_new = result.getTvec();

                float[] curViewMatrix = createOpenglViewMatrix(rvecs_new, tvecs_new);
                float[] avgViewMatrix = getAverageOpenglViewMatrix();

                boolean isGliched = false;
                for (int i = 0; i < curViewMatrix.length; i++)
                    if (Math.abs(curViewMatrix[i] - avgViewMatrix[i]) > 2.5f)
                        isGliched = true;

                if (!isGliched)
                    addOpenglViewMatrixToTheList(curViewMatrix);

                avgViewMatrix = getAverageOpenglViewMatrix();

                if (detectableObject != null)
                    detectableObject.recieveViewMatrix(avgViewMatrix);

                rvecs_new.release();
                tvecs_new.release();
            }
        }, 0,16, TimeUnit.MILLISECONDS);
    }

    @Override
    public void calibrate(){
        estimateResult.cancel(true);
        matrixConversionResult.cancel(true);
        detectableObject.setIsObjectHidden(true);
    }

    public void clearQueue() {
        //Log.d(TAG, "Image queue cleared");
        queue.clear();
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
