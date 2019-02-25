package com.martins.board.OpenCV;

import com.martins.board.DetecatbleObject;
import com.martins.board.FrameReceiver;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ArucoProcessing implements FrameReceiver, ArucoProcessStateListener {
    private static final String TAG = "Aruco";

    private static final int CALIB_COUNT = 5;
    private final CameraCalibrator calibrator = new CameraCalibrator();
    private int curCalibCount = 0;

    private final BlockingQueue<Mat> frameQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<PoseResult> poseResultQueue = new LinkedBlockingQueue<>();

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(NUMBER_OF_CORES);

    private final PoseEstimator poseEstimator = new PoseEstimator();
    private final ArucoToOpenglMatrixConverter matrixConverter = new ArucoToOpenglMatrixConverter();
    private List<Future<?>> estimateResultList = new ArrayList<>();
    private Future<?> matrixConversionResult;

    private List<DetecatbleObject> detectableObjects = new ArrayList<>();

    private ArucoProcessStateManager apsm = new ArucoProcessStateManager(this);

    public ArucoProcessing() {
        CameraDistortionParams.readParams();

        for (int i = 0; i < NUMBER_OF_CORES - 1; i++)
            estimateResultList.add(null);

        exec.scheduleAtFixedRate(() -> {
            if (frameQueue.size() > 20)
                clearQueue();
            //Log.d(TAG, "" + poseResultQueue.size());
        }, 0,1, TimeUnit.SECONDS);
    }

    private Mat getFrameFromQueue() {
        try {
            return frameQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PoseResult getPoseResultFromQueue() {
        try {
            return poseResultQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addCalibrationFrame() {
        curCalibCount++;
        if (frameQueue.size() > 2) {
            calibrator.addFrame(getFrameFromQueue());
            calibrator.addFrame(getFrameFromQueue());
            clearQueue();
        }

        if (curCalibCount >= CALIB_COUNT) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(calibrator);
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                curCalibCount = 0;

                for (DetecatbleObject o: detectableObjects)
                    o.setIsObjectHidden(false);

                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void addFrame(Mat image) {
        if (image.channels() == 4) {
            exec.schedule(() -> {
                try {
                    Mat frame = new Mat();
                    Imgproc.cvtColor(image, frame, Imgproc.COLOR_RGBA2GRAY);
                    frameQueue.put(frame.clone());
                    image.release();
                    frame.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 0, TimeUnit.MICROSECONDS);
        }
    }

    @Override
    public void addMatrixListener(DetecatbleObject listener) {
        detectableObjects.add(listener);
    }

    @Override
    public void detect() {
        for (int i = 0; i < estimateResultList.size(); i++) {
            estimateResultList.set(i,
                exec.scheduleAtFixedRate(() -> {
                    if (frameQueue.size() > 0 && CameraDistortionParams.hasParameters()) {
                        PoseResult result = poseEstimator.run(getFrameFromQueue());
                        if (result != null)
                            poseResultQueue.add(result);
                    }
                }, i,1, TimeUnit.MILLISECONDS)
            );
        }

        matrixConversionResult = exec.scheduleAtFixedRate(() -> {
            if (poseResultQueue.size() > 0) {
                PoseResult result = getPoseResultFromQueue();
                if (result != null) {
                    float[] viewMatrix = matrixConverter.run(result);
                    for (DetecatbleObject o : detectableObjects)
                        o.recieveViewMatrix(viewMatrix);
                }
            }
        }, 0,4, TimeUnit.MILLISECONDS);
    }

    @Override
    public void calibrate(){
        for (Future<?> threadResult: estimateResultList)
            threadResult.cancel(true);
        matrixConversionResult.cancel(true);
        for (DetecatbleObject o: detectableObjects)
            o.setIsObjectHidden(true);
    }

    public void clearQueue() {
        //Log.d(TAG, "Image frameQueue cleared");
        frameQueue.clear();
    }

    public void changeState(ArucoProcessState state) {
        apsm.changeState(state);
    }
}
