package com.martins.board;

public class ArucoProcessingStateManager {
    private ArucoProcessStateListener arucoProcess;

    public ArucoProcessingStateManager(ArucoProcessStateListener process) {
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
