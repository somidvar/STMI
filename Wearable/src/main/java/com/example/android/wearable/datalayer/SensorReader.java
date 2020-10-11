package com.example.android.wearable.datalayer;

import android.app.usage.UsageEvents;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

public class SensorReader extends FragmentActivity implements SensorEventListener, DataClient.OnDataChangedListener {
    private static final String TAG = "sensorReader";
    String sensorContext="";
    long currentTime;
    private SensorManager sensorManager;
    private File fileAddressDest;

    private int recordCapacity;

    private int accelerometerCounter;
    private int magnetometerCounter;
    private int gyroscopeCounter;
    private int gravityCounter;
    private int heartBeatCounter;

    ArrayList<ArrayList<String>> accelerometerList;
    ArrayList<ArrayList<String>> magnetometerList;
    ArrayList<ArrayList<String>> gyroscopeList;
    ArrayList<ArrayList<String>> gravityList;
    ArrayList<ArrayList<String>> heartBeatList;

    DecimalFormat df;
    ArrayList<String> fileToBeSent;

    SensorReader(SensorManager sensorManager, File fileAddressDest){

        //Log.d(TAG,"constructor");
        this.sensorManager = sensorManager;
        this.fileAddressDest=fileAddressDest;


        /* //List of all sensors
        List<Sensor> mysensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor mine : mysensor)
            Log.d(TAG,mine.getName());
        */

        try {
            recordCapacity = 1000;
            df = new DecimalFormat("###.###");

            fileToBeSent=new ArrayList<>();

            accelerometerList = new ArrayList<>(recordCapacity);
            magnetometerList = new ArrayList<>(recordCapacity);
            gyroscopeList = new ArrayList<>(recordCapacity);
            gravityList = new ArrayList<>(recordCapacity);

            heartBeatList = new ArrayList<>(recordCapacity);

            for (int counterRow = 0; counterRow < recordCapacity; counterRow++) {
                accelerometerList.add(new ArrayList<String>(5));
                magnetometerList.add(new ArrayList<String>(5));
                gyroscopeList.add(new ArrayList<String>(5));
                gravityList.add(new ArrayList<String>(5));
                heartBeatList.add(new ArrayList<String>(3));
                for (int counterField = 0; counterField < 5; counterField++) {
                    accelerometerList.get(counterRow).add("");
                    magnetometerList.get(counterRow).add("");
                    gyroscopeList.get(counterRow).add("");
                    gravityList.get(counterRow).add("");
                }
                for (int counterField = 0; counterField < 3; counterField++) {
                    heartBeatList.get(counterRow).add("");
                }
            }


            accelerometerCounter = 0;
            magnetometerCounter = 0;
            gyroscopeCounter = 0;
            gravityCounter = 0;
            heartBeatCounter = 0;

            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

            if (magneticSensor != null) {
                sensorManager.registerListener(this, magneticSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

            if (gyroscopeSensor != null) {
                sensorManager.registerListener(this, gyroscopeSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

            if (gravitySensor != null) {
                sensorManager.registerListener(this, gravitySensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

            if (heartRateSensor != null) {
                sensorManager.registerListener(this, heartRateSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
        }catch (Exception e) {
            Log.e(TAG, "SensorReader"+e.getMessage());
        }
        onlyForTest();
    }

    public void onSensorChanged(SensorEvent event) {
        //Log.d(TAG,"onSensorChanged");
        /*
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //Log.e(TAG, "magnoX="+df.format(event.values[0])+"\t magnoY="+df.format(event.values[1])+"\t magnoZ="+df.format(event.values[2]));

            for (int fieldCounter = 0; fieldCounter < 3; fieldCounter++) {
                String str = String.format("%.5f", event.values[fieldCounter]);
                gyroscopeList.get(gyroscopeCounter).set(fieldCounter,str);
            }
            gyroscopeList.get(magnetometerCounter).set(3,String.valueOf(hour));
            magnetometerList.get(magnetometerCounter).set(4,String.valueOf(eventTime));

            magnetometerCounter++;
            if(magnetometerCounter==recordCapacity){
                writtingSensorToFile(magnetometerList,"Magnetometer");
                magnetometerCounter=0;
            }

        }
         */
        //long eventTime= (long) (event.timestamp/1e6);
        //int hour=(int) (eventTime/1000/3600);
        //eventTime-=hour*3600*1000;


        long timeNow = System.currentTimeMillis();
        long residualTime=(long) 50*365*24*3600*1000;
        timeNow-=residualTime;//moving the time to Jan 1 2020.
        int dayOfYear=(int) (timeNow/1000/3600/24);
        timeNow-=(long) dayOfYear*24*3600*1000;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.e(TAG, "AccelX="+df.format(event.values[0])+"\t AccelY="+df.format(event.values[1])+"\t AccelZ="+df.format(event.values[2])+"\t time="+eventTime);
            accelerometerCounter=eventRecorder(accelerometerList, "Accelerometer",accelerometerCounter, event.values,dayOfYear, timeNow);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //Log.e(TAG, "magnoX="+df.format(event.values[0])+"\t magnoY="+df.format(event.values[1])+"\t magnoZ="+df.format(event.values[2]));
            magnetometerCounter=eventRecorder(magnetometerList, "Magnometer",magnetometerCounter, event.values,dayOfYear, timeNow);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //Log.e(TAG, "gyroX="+df.format(event.values[0])+"\t gyroY="+df.format(event.values[1])+"\t gyroZ="+df.format(event.values[2]));
            gyroscopeCounter=eventRecorder(gyroscopeList, "Gyroscope",gyroscopeCounter, event.values,dayOfYear, timeNow);
        }
        else if (event.sensor.getType() == Sensor.TYPE_HEART_BEAT) {
            //Log.e(TAG, "gyroX="+df.format(event.values[0])+"\t gyroY="+df.format(event.values[1])+"\t gyroZ="+df.format(event.values[2]));
            heartBeatCounter=eventRecorder(heartBeatList, "HeartRate",heartBeatCounter, event.values,dayOfYear, timeNow);
        }
    }
    protected int eventRecorder(ArrayList<ArrayList<String>> dataList, String parameterName,Integer parameterCounter, float[] eventParameters,int dayOfYear, long eventTime){
        try {
            for (int fieldCounter = 0; fieldCounter < eventParameters.length; fieldCounter++) {
                String str = String.format("%.5f", eventParameters[fieldCounter]);
                dataList.get(parameterCounter).set(fieldCounter, str);
            }
            dataList.get(parameterCounter).set(eventParameters.length, String.valueOf(dayOfYear));
            dataList.get(parameterCounter).set(eventParameters.length + 1, String.valueOf(eventTime));

            parameterCounter++;
            if (parameterCounter == recordCapacity) {
                writtingSensorToFile(dataList, parameterName);
                parameterCounter = 0;
            }
        }catch (Exception e){
            Log.e(TAG,"eventRecorder \t"+e.getMessage());
        }
        return parameterCounter;
    }
    protected void writtingSensorToFile(ArrayList<ArrayList<String>> myList, String ParameterName){
        Log.e(TAG,"writing "+ParameterName+" in writtingSensorToFile");

        File root = new File(this.fileAddressDest, "SensorDataFile");
        File fileName=null;
        int fileAddressIndex = 0;
        try {
            String sensorContext;
            if(myList.get(0).size()==5)
                sensorContext="X \t Y \t Z \t Day of Year \t millisecond\n";
            else
                sensorContext="Var \t Day of Year \t millisecond\n";
            for (int rowCounter=0;rowCounter<myList.size();rowCounter++) {
                for (int columnCounter = 0; columnCounter < myList.get(rowCounter).size(); columnCounter++) {
                    sensorContext+=myList.get(rowCounter).get(columnCounter).toString() + "\t";
                }
                sensorContext+="\n";
            }

            if (!root.exists())
                root.mkdir();

            String[] fileListAr = root.list();
            for (String fileList : fileListAr) {
                if (fileList.contains("RawData") &&fileList.contains(ParameterName) && fileList.contains(".txt")) {
                    int indexTemp1 = fileList.lastIndexOf('-');
                    int indexTemp2 = fileList.indexOf('.');
                    String AddressTemp = fileList.substring(indexTemp1+1, indexTemp2);
                    if (AddressTemp == "" || !isNumber(AddressTemp))
                        continue;
                    if (fileAddressIndex < Integer.parseInt(AddressTemp))
                        fileAddressIndex = Integer.parseInt(AddressTemp);
                }
            }
            fileAddressIndex++;//writting the new file

            fileName = new File(root, "RawData-"+ParameterName+"-"+fileAddressIndex+".txt");
            FileWriter writer = new FileWriter(fileName);
            writer.write(sensorContext);
            writer.flush();
            writer.close();
            fileToBeSent.add(fileName.getPath()+"");
            //Log.e(TAG,"mine="+ParameterName+fileName.getPath());
        } catch (Exception e) {
            Log.e(TAG,"writtingSensorToFile \t"+e.getMessage());
        }
    }

    private boolean isNumber(String fileAddress){
        try{
            Integer.parseInt(fileAddress);
        }catch (Exception e){
            return false;
        }
        return true;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG,"onAccuracyChanged");
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

    }

    public void onlyForTest(){
        File root = new File(this.fileAddressDest, "SensorDataFile");
        if (root.exists()) {
            String[] entries = root.list();
            for (String s : entries) {
                File currentFile = new File(root.getPath(), s);
                currentFile.delete();
            }
        }
    }
}