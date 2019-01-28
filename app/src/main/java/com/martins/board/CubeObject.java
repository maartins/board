/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.martins.board;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CubeObject implements ViewMatrixListener, DrawableObject{
    private static final int COORDS_PER_VERTEX = 3;
    private static final int VALUES_PER_COLOR = 4;
    private final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
    private final int COLOR_STRIDE = VALUES_PER_COLOR * 4;

    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mColorBuffer;
    private final ByteBuffer mIndexBuffer;

    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private final Shader cubeShader = new Shader();

    private Context context;

    private float[] mvpMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    public CubeObject(Context context) {
        this.context = context;

        final float VERTICES[] = {
            -1f, -1f, -1f,
            1f, -1f, -1f,
            1f, 1f, -1f,
            -1f, 1f, -1f,
            -1f, -1f, 1f,
            1f, -1f, 1f,
            1f, 1f, 1f,
            -1f, 1f, 1f
        };

        final float COLORS[] = {
            0.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,
        };

        final byte INDICES[] = {
            0, 1, 3, 3, 1, 2, // Front face.
            0, 1, 4, 4, 5, 1, // Bottom face.
            1, 2, 5, 5, 6, 2, // Right face.
            2, 3, 6, 6, 7, 3, // Top face.
            3, 7, 4, 4, 3, 0, // Left face.
            4, 5, 7, 7, 6, 5, // Rear face.
        };

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4).order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuffer.asFloatBuffer();
        mVertexBuffer.put(VERTICES).position(0);

        byteBuffer = ByteBuffer.allocateDirect(COLORS.length * 4).order(ByteOrder.nativeOrder());
        mColorBuffer = byteBuffer.asFloatBuffer();
        mColorBuffer.put(COLORS).position(0);

        mIndexBuffer = ByteBuffer.allocateDirect(INDICES.length);
        mIndexBuffer.put(INDICES).position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        float scale = 0.2f;
        Matrix.scaleM(modelMatrix, 0, modelMatrix, 0, scale, scale, scale);
    }

    public void setShaderProgramFiles(int vertexShader, int fragmentShader) throws Exception {
        cubeShader.setProgram(vertexShader, fragmentShader, context);
        positionHandle = cubeShader.getHandle("aPosition");
        colorHandle = cubeShader.getHandle("aColor");
        mvpMatrixHandle = cubeShader.getHandle("uMVPMatrix");
    }

    @Override
    public void onSurfaceChanged(float width, float height) {
        float ratio = width / height;
        if (CameraDistortionParams.hasParameters()) {
            Mat K = CameraDistortionParams.getCameraMatrix();
            float x0 = (float) K.get(0, 2)[0];
            float y0 = (float) K.get(1, 2)[0];

            Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 1f, 1000f);

            projectionMatrix[2] = -(width - x0) / width;
            projectionMatrix[6] = -(height - y0) / height;
            PersonalUtils.displaySquareMatrix(projectionMatrix);
            //Matrix.orthoM(projectionMatrix, 0, 0, width, height, 0, 1f, 1000f);


            float near = 1.0f;
            float far = 1000.0f;
            float alfa = (float) K.get(0, 0)[0];
            float beta = (float) K.get(1, 1)[0];

            float A = near + far;
            float B = near * far;

            float[] pmat = new float[16];
            pmat[0] = near;//alfa;
            pmat[2] = 0;//-x0;
            pmat[5] = near;//beta;
            pmat[6] = 0;//-y0;
            pmat[10] = A;
            pmat[11] = B;
            pmat[14] = -1.0f;

            float right = 0;//-width / 2;
            float left = width;
            float top = 0;//-height / 2;
            float bottom = height;

            right = (near / alfa) * right;
            left = (near / alfa) * left;
            top = (near / beta) * top;
            bottom = (near / beta) * bottom;

            right = right - x0;
            left = left - x0;
            top = top - y0;
            bottom = bottom - y0;

            float x = 2.0f / (right - left);
            float y = 2.0f / (top - bottom);
            float z = -2.0f / (far - near);
            float tx = -(right + left) / (right - left);
            float ty = -(top + bottom) / (top - bottom);
            float tz = -(far + near) / (far - near);

            float[] omat = new float[16];
            omat[0] = x;
            omat[3] = tx;
            omat[5] = y;
            omat[7] = ty;
            omat[10] = z;
            omat[11] = tz;
            omat[15] = 1.0f;

            float rx = 2.0f * near / (right - left);
            float ry = 2.0f * near / (top - bottom);
            float Ax = right + left / (right - left);
            float By = top + bottom / (top - bottom);
            float Cz = -(far + near / (far - near));
            float Dz = -(2.0f * far * near / (far - near));

            float[] emat = new float[16];
            emat[0] = rx;
            emat[2] = Ax;
            emat[5] = ry;
            emat[6] = By;
            emat[10] = Cz;
            emat[11] = Dz;
            emat[14] = -1.0f;

            /*Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
            float[] ptemp = projectionMatrix.clone();
            Matrix.transposeM(projectionMatrix, 0, ptemp, 0);
            PersonalUtils.displaySquareMatrix(projectionMatrix);

            PersonalUtils.displaySquareMatrix(omat);
            PersonalUtils.displaySquareMatrix(pmat);
            //Matrix.multiplyMM(projectionMatrix, 0, pmat, 0, omat, 0);

            PersonalUtils.displaySquareMatrix(emat);*/
            // projectionMatrix = emat;
        }
    }

    @Override
    public void draw() {
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
        cubeShader.useProgram();

        // Prepare the cube coordinate data.
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(
                positionHandle, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);

        // Prepare the cube color data.
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(
                colorHandle, 4, GLES20.GL_FLOAT, false, COLOR_STRIDE, mColorBuffer);

        // Prepare MVP
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, mIndexBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    @Override
    public synchronized void recieveViewMatrix(float[] matrix) {
        viewMatrix = matrix;
    }
}
