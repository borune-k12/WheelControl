package com.borune.wheelcontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements WheelControl.OnAngleChangeListener{

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((WheelControl)findViewById(R.id.wheel)).setOnAngleChangeListener(this);
    }

    @Override
    public void onAngleChanged(double angle, boolean fromUser) {
        Log.i(TAG,"new angle value: " + angle);
    }
}
