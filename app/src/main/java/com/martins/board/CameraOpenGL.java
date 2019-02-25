package com.martins.board;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraOpenGL extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraOpenGL";

    private static ByteBuffer bufferedByteSnapshot;
    private static byte[] arrayByteSnapshot;
    private static Mat matSnapshot;
    private static boolean isBufferedSnapshotReady = false;

    private Context context;

    private FrameReceiver receiver;

    private Camera cam;
    private SurfaceTexture texture;

    private int viewWidth, viewHeight;
    private boolean updateTexture = false;

    private BackgroundObject backgroundObject;
    private CubeObject cube;
    private BoardObject boardObject;

    public CameraOpenGL(Context context) {
        super(context);
        this.context = context;

        init();
    }

    public CameraOpenGL(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    private void init() {
        backgroundObject = new BackgroundObject(context);
        cube = new CubeObject(context);
        boardObject = new BoardObject(context);

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void addFrameReciever(FrameReceiver ip) {
        receiver = ip;
        receiver.addMatrixListener(cube);
        receiver.addMatrixListener(boardObject);
    }

    @Override
    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        updateTexture = true;
        requestRender();
    }

    @Override
    public synchronized void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            backgroundObject.setShaderProgramFiles();
            cube.setShaderProgramFiles();
            boardObject.setShaderProgramFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "SurfaceChanged...");
        GLES20.glViewport(0, 0, width, height);

        viewWidth = width;
        viewHeight = height;

        isBufferedSnapshotReady = false;
        bufferedByteSnapshot = ByteBuffer.allocate(viewWidth * viewHeight * 4);
        arrayByteSnapshot = new byte[bufferedByteSnapshot.capacity()];
        matSnapshot = new Mat(viewHeight, viewWidth, CvType.CV_8UC4);
        isBufferedSnapshotReady = true;

        Log.d(TAG, "Snapshot ready");

        backgroundObject.getTextureHadle().init();
        SurfaceTexture oldSurfaceTexture = texture;
        texture = new SurfaceTexture(backgroundObject.getTextureHadle().getTextureId());
        texture.setOnFrameAvailableListener(this);

        if (oldSurfaceTexture != null) {
            oldSurfaceTexture.release();
        }

        if (cam != null) {
            cam.stopPreview();
            cam.release();
            cam = null;
        }

        cam = Camera.open();
        try {
            cam.setPreviewTexture(texture);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Camera.Parameters params = cam.getParameters();
        List<String> focusModes = params.getSupportedFocusModes();
        List<String> antiBandingModes = params.getSupportedAntibanding();

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        else
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

        if (antiBandingModes.contains(Camera.Parameters.ANTIBANDING_AUTO))
            params.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);

        cam.setParameters(params);
        cam.startPreview();

        backgroundObject.setRatio(getClosestSupportedViewSize(params, viewWidth, viewHeight));
        backgroundObject.onSurfaceChanged(width, height);
        cube.onSurfaceChanged(width, height);
        boardObject.onSurfaceChanged(width, height);

        requestRender();
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (updateTexture) {
            texture.updateTexImage();
            texture.getTransformMatrix(backgroundObject.getTrasnfomMatrix());

            updateTexture = false;

            backgroundObject.draw();
            if (receiver != null && isBufferedSnapshotReady)
                receiver.addFrame(saveTextureAsMat(viewWidth, viewHeight));

            cube.draw();
            boardObject.draw();
        }
    }

    private static Mat saveTextureAsMat(int width, int height) {
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bufferedByteSnapshot);
        ((ByteBuffer) bufferedByteSnapshot.duplicate().clear()).get(arrayByteSnapshot);
        matSnapshot.put(0, 0, arrayByteSnapshot);
        Core.flip(matSnapshot, matSnapshot, Core.ROTATE_90_CLOCKWISE);
        return matSnapshot.clone();
    }


    private float[] getClosestSupportedViewSize(Camera.Parameters params, int viewWidth, int viewHeight){
        float[] size = new float[] {0f, 0f};

        List<Camera.Size> psize = params.getSupportedPreviewSizes();
        if (psize.size() > 0) {
            int i;
            for (i = 0; i < psize.size(); i++) {
                if (psize.get(i).width < viewWidth || psize.get(i).height < viewHeight)
                    break;
            }
            if (i > 0)
                i--;
            params.setPreviewSize(psize.get(i).width, psize.get(i).height);

            size[0] = psize.get(i).width;
            size[1] = psize.get(i).height;
        }

        return size;
    }

    public void onDestroy() {
        updateTexture = false;
        texture.release();

        if (cam != null) {
            cam.stopPreview();
            cam.setPreviewCallback(null);
            cam.release();
        }

        cam = null;
    }
}