package com.martins.board.OpenCV;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ArucoToOpenglMatrixCreator {
    private static final int VIEW_MAT_AVG_LIST_SIZE = 3;
    private final Mat OPENGL_CONVERT_MATRIX = Mat.ones(4, 4, CvType.CV_64F);
    private final List<float[]> viewMatrixList = Collections.synchronizedList(new ArrayList<>(VIEW_MAT_AVG_LIST_SIZE));

    private int curViewMatAvgListPos = 0;

    ArucoToOpenglMatrixCreator() {
        viewMatrixList.add(new float[16]);
        viewMatrixList.add(new float[16]);
        viewMatrixList.add(new float[16]);

        for (int i = 1; i < 3; i++)
            for (int j = 0; j < 4; j++)
                OPENGL_CONVERT_MATRIX.put(i, j, -1f);
    }

    public final float[] run(PoseResult result) {
        Mat rvecs_new = result.getRvec();
        Mat tvecs_new = result.getTvec();

        float[] curViewMatrix = createOpenglViewMatrix(rvecs_new, tvecs_new);
        float[] avgViewMatrix = getAverageOpenglViewMatrix();

        /*boolean isGliched = false;
        for (int i = 0; i < curViewMatrix.length; i++)
            if (Math.abs(curViewMatrix[i] - avgViewMatrix[i]) > 2.5f)
                isGliched = true;

        if (!isGliched)*/
        addOpenglViewMatrixToTheList(curViewMatrix);

        avgViewMatrix = getAverageOpenglViewMatrix();

        rvecs_new.release();
        tvecs_new.release();

        return avgViewMatrix;
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
                // transpose and convert to opengl acceptable format
                openglViewMatrix[counter] = (float)viewMatrix.get(j, i)[0];
                counter++;
            }
        }

        rotationMatrix.release();
        return openglViewMatrix;
    }
}
