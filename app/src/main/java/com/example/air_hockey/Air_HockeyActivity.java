package com.example.air_hockey;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class Air_HockeyActivity extends AppCompatActivity {
private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    Air_Hockey_Renderer airHockeyRenderer ;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final ActivityManager activityManager= (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000 || Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown") || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86");


        int numLines;
        Intent intent = getIntent();
        numLines = intent.getIntExtra("numLines",7);
        //airHockeyRenderer.setNumLines(numLines);

        airHockeyRenderer = new Air_Hockey_Renderer(this,numLines);


        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        if (supportsEs2) {
// Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);
            // Assign our renderer.
            glSurfaceView.setRenderer(airHockeyRenderer);
            rendererSet = true;
        } else {
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.", Toast.LENGTH_LONG).show();
            return; }
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    // Convert touch coordinates into normalized device
                    // coordinates, keeping in mind that Android's Y
                    // coordinates are inverted.
                    final float normalizedX =
                            (event.getX() / (float) v.getWidth()) * 2 - 1;
                    final float normalizedY =
                            -((event.getY() / (float) v.getHeight()) * 2 - 1);

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                airHockeyRenderer.handleTouchPress(
                                        normalizedX, normalizedY);
                            }
                        });
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                airHockeyRenderer.handleTouchDrag(
                                        normalizedX, normalizedY);
                            }
                        });
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });





        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rendererSet)
            glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet)
            glSurfaceView.onResume();
    }


}