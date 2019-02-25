package com.martins.board;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.martins.board.OpenCV.CameraDistortionParams;

import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

public class BoardObject implements DetecatbleObject, DrawableObject {
    private static final String TAG = "BoardObject";

    private final IntBuffer indexBuffer;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer textureBuffer;
    private final FloatBuffer normalBuffer;

    private int vertexBufferId = 0;
    private int indexBufferId = 0;

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
    private boolean isValid = true;

    public BoardObject(Context context) {
        this.context = context;

        Obj board = null;
        try {
            board = ObjUtils.convertToRenderable(
                    ObjReader.read(context.getAssets().open("info_delis.obj")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (board != null) {
            indexBuffer = ObjData.getFaceVertexIndices(board);
            vertexBuffer = ObjData.getVertices(board);
            normalBuffer = ObjData.getNormals(board);
            textureBuffer = ObjData.getTexCoords(board, 2);
        } else {
            indexBuffer = null;
            vertexBuffer = null;
            normalBuffer = null;
            textureBuffer = null;
            isValid = false;
        }

        Matrix.setIdentityM(modelMatrix, 0);
        float scale = 0.2f;
        Matrix.scaleM(modelMatrix, 0, modelMatrix, 0, scale, scale, scale);

        setupVertexBuffer();
        setupIndexBuffer();
    }

    private void setupVertexBuffer() {
        IntBuffer buffer = IntBuffer.allocate(1);
        GLES20.glGenBuffers(1, buffer);
        vertexBufferId = buffer.get(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
    }

    private void setupIndexBuffer() {
        IntBuffer buffer = IntBuffer.allocate(1);
        GLES20.glGenBuffers(1, buffer);
        indexBufferId = buffer.get(0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GLES20.GL_STATIC_DRAW);
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
        if (!isHidden && isValid) {
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
            boardShader.useProgram();

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(
                    positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(
                    texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
            GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES, indexBuffer.capacity() * 4, GLES20.GL_INT, 0);

            //GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity() * 4, GLES20.GL_INT, indexBuffer);
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
    }
}
