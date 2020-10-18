package com.example.android.wearable.datalayer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class SensorReader extends FragmentActivity implements SensorEventListener, DataClient.OnDataChangedListener {
    private static final String TAG = "sensorReader";
    String sensorContext="";
    private long timeOffset;
    private boolean timeOffsetGaurd;
    private SensorManager sensorManager;
    private File fileAddressDest;

    public int recordCapacity;
    public int strID;

    private int accelerometerCounter;
    private int magnetometerCounter;
    private int gyroscopeCounter;
    private int gravityCounter;
    private int heartBeatCounter;

    private int accelerometerStrid;
    private int magnetometerStrid;
    private int gyroscopeStrid;
    private int gravityStrid;
    private int readingStrid;
    private int elementNumber=7;

    ArrayList<ArrayList<Float>> accelerometerList;
    ArrayList<ArrayList<Float>> magnetometerList;
    ArrayList<ArrayList<Float>> gyroscopeList;
    ArrayList<ArrayList<Float>> gravityList;
    ArrayList<ArrayList<Float>> heartBeatList;

    ArrayList<String> fileToBeSent;

    SensorReader(SensorManager sensorManager, File fileAddressDest){
        this.sensorManager = sensorManager;
        this.fileAddressDest=fileAddressDest;


        /* //List of all sensors
        List<Sensor> mysensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor mine : mysensor)
            Log.d(TAG,mine.getName());
        */

        try {
            strID=10;
            recordCapacity = (int) 8000/strID;
            fileToBeSent=new ArrayList<>();
            timeOffsetGaurd=false;

            accelerometerList = new ArrayList<>(recordCapacity);
            magnetometerList = new ArrayList<>(recordCapacity);
            gyroscopeList = new ArrayList<>(recordCapacity);
            gravityList = new ArrayList<>(recordCapacity);
            heartBeatList = new ArrayList<>(recordCapacity);

            for (int counterRow = 0; counterRow < recordCapacity; counterRow++) {
                accelerometerList.add(new ArrayList<Float>(elementNumber));
                magnetometerList.add(new ArrayList<Float>(elementNumber));
                gyroscopeList.add(new ArrayList<Float>(elementNumber));
                //gravityList.add(new ArrayList<Float>(elementNumber));
                heartBeatList.add(new ArrayList<Float>(elementNumber-2));
                for (int counterField = 0; counterField <elementNumber; counterField++) {
                    accelerometerList.get(counterRow).add(-10000f);
                    magnetometerList.get(counterRow).add(-10000f);
                    gyroscopeList.get(counterRow).add(-10000f);
                    //gravityList.get(counterRow).add(-10000f);
                }
                for (int counterField = 0; counterField < elementNumber-2; counterField++) {
                    heartBeatList.get(counterRow).add(null);
                }
            }

            accelerometerCounter = 0;
            magnetometerCounter = 0;
            gyroscopeCounter = 0;
            gravityCounter = 0;
            heartBeatCounter = 0;

            readingStrid=10;
            accelerometerStrid=0;
            magnetometerStrid=0;
            gyroscopeStrid=0;
            gravityStrid=0;

            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            Sensor heartBeatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);


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

            if (heartBeatSensor != null) {
                sensorManager.registerListener(this, heartBeatSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

        }catch (Exception e) {
            Log.e(TAG, "SensorReader"+e.getMessage());
        }
        onlyForTest();
    }

    public void onSensorChanged(SensorEvent event) {
        if(!timeOffsetGaurd){//to be more accurate the event timestamp is used. however it is starting with an arbitrary value. So by using the timeOffset, the origine is set to the Jan 1 2020.
            timeOffset=System.currentTimeMillis();
            timeOffset-=(long) 50 * 365 * 24 * 3600 * 1000;//moving it to Jan 1 2020
            timeOffset-=(long) 12*24*3600*1000;//correction of the leap years
            timeOffset+=(long) 19*3600*1000;//correction of the hour
            timeOffset=(long) event.timestamp/1000000-timeOffset;

            timeOffsetGaurd=true;
        }
        long miliSec= (long) (event.timestamp/1000000)-timeOffset;
        int dayOfYear = (int) (miliSec / 1000 / 3600 / 24);
        miliSec -= (long) dayOfYear * 24 * 3600 * 1000;
        int hour = (int) (miliSec / 1000 / 3600);
        miliSec -= (long) hour * 3600 * 1000;
        int minute = (int) (miliSec / 1000 / 60);
        miliSec -= (long) minute * 60 * 1000;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accelerometerStrid % readingStrid == 0)
                //Log.e(TAG, "AccelX="+df.format(event.values[0])+"\t AccelY="+df.format(event.values[1])+"\t AccelZ="+df.format(event.values[2])+"\t time="+ timeNow);
                accelerometerCounter = eventRecorder(accelerometerList, "Accel", accelerometerCounter, event.values, dayOfYear, hour, minute, miliSec);
            accelerometerStrid++;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //Log.e(TAG, "magnoX="+df.format(event.values[0])+"\t magnoY="+df.format(event.values[1])+"\t magnoZ="+df.format(event.values[2]));
            if (magnetometerStrid % readingStrid == 0)
                magnetometerCounter = eventRecorder(magnetometerList, "Magno", magnetometerCounter, event.values, dayOfYear, hour, minute, miliSec);
            magnetometerStrid++;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //Log.e(TAG, "gyroX="+df.format(event.values[0])+"\t gyroY="+df.format(event.values[1])+"\t gyroZ="+df.format(event.values[2]));
            if (gyroscopeStrid % readingStrid == 0)
                gyroscopeCounter = eventRecorder(gyroscopeList, "Gyro", gyroscopeCounter, event.values, dayOfYear, hour, minute, miliSec);
            gyroscopeStrid++;
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_BEAT) {
            float[] heartArray = new float[1];
            heartArray[0] = (int) (event.values[0] * 100);
            //Log.e(TAG, "heart rate="+temporaryValue[0]);
            heartBeatCounter = eventRecorder(heartBeatList, "Heart", heartBeatCounter, heartArray, dayOfYear, hour, minute, miliSec);
        }
    }
    protected int eventRecorder(ArrayList<ArrayList<Float>> dataList, String parameterName,int parameterCounter, float[] eventParameters,int dayOfYear,int hour,int minute, long miliSec){
        try {
            for (int fieldCounter = 0; fieldCounter < eventParameters.length; fieldCounter++) {
                dataList.get(parameterCounter).set(fieldCounter, eventParameters[fieldCounter]);
            }
            dataList.get(parameterCounter).set(eventParameters.length + 0, (float) dayOfYear);
            dataList.get(parameterCounter).set(eventParameters.length + 1, (float) hour);
            dataList.get(parameterCounter).set(eventParameters.length + 2, (float) minute);
            dataList.get(parameterCounter).set(eventParameters.length + 3, (float) miliSec);

            int parameterStart;
            int parameterEnd;

            if (parameterCounter > 0) {
                float current=dataList.get(parameterCounter).get(eventParameters.length + 2);
                float prev=dataList.get(parameterCounter-1).get(eventParameters.length + 2);
                if (current !=prev) {
                    Log.e(TAG,"eventRecorder, I'll go to writting");
                    parameterStart=0;
                    parameterEnd=parameterCounter;
                    writtingSensorToFile(dataList, parameterName, parameterStart,parameterEnd,
                            dataList.get(parameterCounter-1).get(eventParameters.length + 0),
                            dataList.get(parameterCounter-1).get(eventParameters.length + 1),
                            dataList.get(parameterCounter-1).get(eventParameters.length + 2));

                    parameterStart=parameterCounter;
                    parameterEnd=parameterCounter+1;
                    writtingSensorToFile(dataList, parameterName, parameterStart,parameterEnd,
                            dataList.get(parameterCounter).get(eventParameters.length + 0),
                            dataList.get(parameterCounter).get(eventParameters.length + 1),
                            dataList.get(parameterCounter).get(eventParameters.length + 2));

                    parameterCounter = 0;
                    return parameterCounter;
                }
            }
            parameterCounter++;
        }catch (Exception e){
            Log.e(TAG,"eventRecorder \t"+parameterName+"\t"+e.getMessage());
        }
        return parameterCounter;
    }
    protected void writtingSensorToFile(ArrayList<ArrayList<Float>> myList, String parameterName,
                                        int parameterStart,int parameterEnd,float dayOfYear,float hour,float minute){
        Log.i(TAG,"writtingSensorToFile writing "+parameterName);

        File root = new File(this.fileAddressDest, "SensorDataFile");
        File fileName=null;
        try {
            if (!root.exists())
                root.mkdir();

            StringBuffer stringBuffer=new StringBuffer("");
            fileName = new File(root, parameterName+"-"+String.format("%03d",(int) dayOfYear)+"-"
                    +String.format("%02d",(int)hour)+"-"+String.format("%02d",(int)minute)+".txt");
            boolean fileExistance=fileName.exists();
            FileWriter writer = new FileWriter(fileName,true);
            if(!fileExistance) {
                if (myList.get(0).size() == 7)
                    writer.write("X\tY\tZ\tDay\tHour\tMinute\tmili-sec\n");
                else
                    writer.write("Beat\tDay\tHour\tMinute\tmili-sec\n");
            }

            String sensorContext="";
            int elementSize=myList.get(0).size();
            for (int rowCounter=parameterStart;rowCounter<parameterEnd;rowCounter++) {
                if(parameterName=="Heart"){
                    stringBuffer.append((int)(float) myList.get(rowCounter).get(0) + "\t");
                }
                else{
                    for (int columnCounter = 0; columnCounter < 3; columnCounter++) {
                        stringBuffer.append(String.format("%01.5f",(float) myList.get(rowCounter).get(columnCounter)) + "\t");
                    }
                }
                for (int columnCounter = elementSize-4; columnCounter < elementSize; columnCounter++) {
                    int temporaryValue=(int)(float)myList.get(rowCounter).get(columnCounter);
                    stringBuffer.append(temporaryValue);
                    if(columnCounter!=elementSize-1)
                        stringBuffer.append("\t");
                }
                stringBuffer.append("\n");
                sensorContext=stringBuffer.toString();
                if(rowCounter%100==0){
                    writer.write(sensorContext);
                    sensorContext="";
                }
            }
            writer.write(sensorContext);
            writer.flush();
            writer.close();

            fileToBeSent.add(fileName.getPath()+"");
        } catch (Exception e) {
            Log.e(TAG,"writtingSensorToFile \t"+e.getMessage());
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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