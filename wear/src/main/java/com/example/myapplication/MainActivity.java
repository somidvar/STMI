package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private float heartBeat = -1;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private int Sorush=0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        stmiWearBluetoothActivation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor heartBeat = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);
        if (heartBeat != null) {
            sensorManager.registerListener(this, heartBeat,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {
//in your OnCreate() method

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_BEAT) {
            heartBeat=event.values[0];
        }
        //updateOrientationAngles();




    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    //Checking the bluetooth adaptor and enabling it if it is disabled
    public void stmiWearBluetoothActivation(){

        TextView statusTextViewVar = (TextView)findViewById(R.id.statusTextViewWear);
        Intent btEnablingIntent;
        BluetoothAdapter myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter==null)
            statusTextViewVar.setText("No Bluetooth available");
        else{
            if(!myBluetoothAdapter.isEnabled()){
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);
            }
            else
                statusTextViewVar.setText("Bluetooth is enabled");
        }
    }
    //Overriding the onActivityResult. After enabling the bluetooth a toast pops up to inform the user.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TextView statusTextViewVar = (TextView)findViewById(R.id.statusTextViewWear);
        if(requestCode==1){
            if(resultCode==RESULT_OK) {
                Toast.makeText(getApplicationContext(),"Request Accepted",Toast.LENGTH_LONG).show();
                statusTextViewVar.setText("Bluetooth is enabled");
            }
            if(resultCode==RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Request Denied",Toast.LENGTH_LONG).show();
                statusTextViewVar.setText("Bluetooth is disabled");
            }
        }
    }
}