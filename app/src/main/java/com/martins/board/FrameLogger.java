package com.martins.board;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class FrameLogger {
    private static List<FrameLogger> loggers = new ArrayList<>();

    private Activity mainActivity;
    private ImageView debugView;

    public FrameLogger(ImageView debugView, Activity mainActivity){
        this.mainActivity = mainActivity;
        this.debugView = debugView;
        loggers.add(this);
    }

    private void drawFrame(Bitmap frame) {
        if (mainActivity != null && debugView != null)
            if (debugView.getVisibility() != View.GONE)
                mainActivity.runOnUiThread(() -> debugView.setImageBitmap(frame));
    }

    public static void setDebugFrame(Bitmap frame) {
        for (FrameLogger fl: loggers)
            fl.drawFrame(frame);
    }

    public void setDebugViewWindowState() {
        if (debugView.getVisibility() != View.GONE) {
            debugView.setVisibility(View.GONE);
        } else {
            debugView.setVisibility(View.VISIBLE);
        }
    }
}
