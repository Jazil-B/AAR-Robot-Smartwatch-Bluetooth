package com.example.jazz.control;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private TextView tv;
    //the Sensor Manager
    private SensorManager sManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);


        tv = (TextView) findViewById(R.id.tv);

        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        Button button = (Button) findViewById(R.id.button_send);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "bas", Toast.LENGTH_SHORT).show();

            }
        });

        Button button2 = (Button) findViewById(R.id.button_send2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "haut", Toast.LENGTH_SHORT).show();

            }
        });

        Button button3 = (Button) findViewById(R.id.button_send3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "droite", Toast.LENGTH_SHORT).show();

            }
        });

        Button button4 = (Button) findViewById(R.id.button_send4);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "gauche", Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
       // updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
       // updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        //updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

    //When this Activity isn't visible anymore

    protected void onStop()
    {
        //unregister the sensor listener
        sManager.unregisterListener(this);
        super.onStop();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    public void onSensorChanged(SensorEvent event)
    {
        //if sensor is unreliable, return void
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        //else it will output the Roll, Pitch and Yawn values
        tv.setText("Orientation X  :"+ Float.toString(event.values[2]) +"\n"+
                "Orientation Y  :"+ Float.toString(event.values[1]) );

        Log.d("orientation",Float.toString(event.values[2]) +" " +  Float.toString(event.values[1]) + " " + Float.toString(event.values[0]));
    }
}
