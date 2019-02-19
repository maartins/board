package com.martins.board;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.martins.board.OpenCV.ArucoProcessState;
import com.martins.board.OpenCV.ArucoProcessing;
import com.martins.board.OpenCV.ArucoProcessStateManager;

public class MainScreen extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    private FrameLogger logger;
    private CameraOpenGL cogl;
    private ArucoProcessing ap = new ArucoProcessing();
    private ArucoProcessStateManager stateManager = new ArucoProcessStateManager(ap);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        removeTitleFromActionbar();

        logger = new FrameLogger(findViewById(R.id.imageView), this);
        cogl = findViewById(R.id.cameraOpenGL);
        cogl.addFrameReciever(ap);

        stateManager.changeState(ArucoProcessState.DETECTION);
    }

    private void removeTitleFromActionbar() {
        android.app.ActionBar ab1 = getActionBar();
        if (ab1 != null) {
            ab1.setDisplayShowTitleEnabled(false);
        } else {
            android.support.v7.app.ActionBar ab2 = getSupportActionBar();
            if (ab2 != null)
                ab2.setDisplayShowTitleEnabled(false);
            else
                Log.d(TAG, "Could not remove Title from Actionbar.");
        }
    }

    public void onNextFrameButtonClicked(View v) {
        if (ap.addCalibrationFrame()) {
            v.setVisibility(View.GONE);
            stateManager.changeState(ArucoProcessState.DETECTION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.calibrate_button:
                findViewById(R.id.next_frame_button).setVisibility(View.VISIBLE);
                stateManager.changeState(ArucoProcessState.CALIBRATION);
                break;
            case R.id.debug_view_button:
                logger.setDebugViewWindowState();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getX() > 500 && e.getY() > 400)
            ap.clearQueue();

        return true;
    }

    @Override
    public void onDestroy() {
        cogl.onDestroy();
        super.onDestroy();
    }
}
