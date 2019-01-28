package com.martins.board.OpenCV;

public class ArucoProcessStateManager {
    private ArucoProcessStateListener arucoProcess;

    public ArucoProcessStateManager(ArucoProcessStateListener process) {
        arucoProcess = process;
    }

    public void changeState(ArucoProcessState state) {
        switch (state) {
            case DETECTION:
                arucoProcess.detect();
                changeState(ArucoProcessState.WAIT);
                break;
            case CALIBRATION:
                arucoProcess.calibrate();
                changeState(ArucoProcessState.WAIT);
                break;
            case WAIT:
                break;
        }
    }
}
