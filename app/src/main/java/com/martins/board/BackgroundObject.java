package com.martins.board;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.util.List;

public class BackgroundObject implements DrawableObject{
    private final TextureHandle textureHandle = new TextureHandle();
    private final Shader backgroundShader = new Shader();

    private ByteBuffer quadVertices;
    private float[] transformM = new float[16];
    private float[] orientationM = new float[16];
    private float[] ratio = new float[2];

    private int positionHandle;
    private int transformHandle;
    private int orientationHandle;
    private int ratioHandle;

    private Context context;

    public BackgroundObject(Context context){
        this.context = context;

        final byte VERTICES[] = {-1, 1, -1, -1, 1, 1, 1, -1};
        quadVertices = ByteBuffer.allocateDirect(VERTICES.length);
        quadVertices.put(VERTICES).position(0);
    }

    @Override
    public void setShaderProgramFiles() throws Exception {
        backgroundShader.setProgram(R.raw.background_vshader, R.raw.background_fshader, context);
        positionHandle = backgroundShader.getHandle("aPosition");
        transformHandle = backgroundShader.getHandle("uTransformM");
        orientationHandle = backgroundShader.getHandle("uOrientationM");
        ratioHandle = backgroundShader.getHandle("ratios");
    }

    @Override
    public void draw(){
        backgroundShader.useProgram();

        GLES20.glUniformMatrix4fv(transformHandle, 1, false, transformM, 0);
        GLES20.glUniformMatrix4fv(orientationHandle, 1, false, orientationM, 0);
        GLES20.glUniform2fv(ratioHandle, 1, ratio, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureHandle.getTextureId());

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_BYTE, false, 0, quadVertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onSurfaceChanged(float viewWidth, float viewHeight) {
        float[] tempRatio = ratio.clone();

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Matrix.setRotateM(orientationM, 0, 90.0f, 0f, 0f, 1f);
            ratio[1] = tempRatio[0] / viewHeight;
            ratio[0] = tempRatio[1] / viewWidth;
        } else {
            Matrix.setRotateM(orientationM, 0, 0.0f, 0f, 0f, 1f);
            ratio[1] /= viewHeight;
            ratio[0] /= viewWidth;
        }
    }

    public float[] getTrasnfomMatrix() {
        return transformM;
    }

    public TextureHandle getTextureHadle() {
        return textureHandle;
    }

    public void setRatio(float[] size) {
        ratio = size;
    }
}
