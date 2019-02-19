package com.martins.board;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.martins.board.OpenCV.CameraDistortionParams;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BoardObject implements DetecatbleObject, DrawableObject {
    private static final String TAG = "BoardObject";

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;

    private int positionHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;

    private final Shader boardShader = new Shader();

    private Context context;

    private float[] mvpMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] modelMatrix = new float[16];

    private boolean isHidden = false;

    public BoardObject(Context context) {
        this.context = context;

        ObjLoader objLoader = new ObjLoader(context, "info_delis.obj");

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(objLoader.positions.length * 4).order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(objLoader.positions).position(0);

        byteBuffer = ByteBuffer.allocateDirect(objLoader.textureCoordinates.length * 4).order(ByteOrder.nativeOrder());
        colorBuffer = byteBuffer.asFloatBuffer();
        colorBuffer.put(objLoader.textureCoordinates).position(0);

        byteBuffer = ByteBuffer.allocateDirect(objLoader.normals.length * 4).order(ByteOrder.nativeOrder());
        normalBuffer = byteBuffer.asFloatBuffer();
        normalBuffer.put(objLoader.normals).position(0);

        Matrix.setIdentityM(modelMatrix, 0);
        float scale = 0.2f;
        Matrix.scaleM(modelMatrix, 0, modelMatrix, 0, scale, scale, scale);
    }

    @Override
    public void setShaderProgramFiles() throws Exception {
        boardShader.setProgram(R.raw.board_vshader, R.raw.board_fshader, context);
        positionHandle = boardShader.getHandle("aPosition");
        texCoordHandle = boardShader.getHandle("atexCoord");
        mvpMatrixHandle = boardShader.getHandle("uMVPMatrix");
    }

    @Override
    public void recieveViewMatrix(float[] matrix) {
        viewMatrix = matrix;
    }

    @Override
    public void setIsObjectHidden(boolean isHidden) {
        this.isHidden = isHidden;
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
        }
    }

    @Override
    public void draw() {
        if (!isHidden) {
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
            boardShader.useProgram();

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(
                    positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(
                    texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, colorBuffer);

            // Prepare the cube coordinate data.
            /*

            // Prepare the cube color data.


            // Prepare MVP
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, indexBuffer);
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);*/
        }
    }
}
