package com.example.android.wearable.datalayer;

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
    private int heartBeatCounter;

    ArrayList<ArrayList<String>> accelerometerList;
    ArrayList<ArrayList<String>> magnetometerList;
    ArrayList<ArrayList<String>> heartBeatList;
    DecimalFormat df;

    SensorReader(SensorManager sensorManager, File fileAddressDest){
        //Log.d(TAG,"constructor");
        this.sensorManager = sensorManager;
        this.fileAddressDest=fileAddressDest;

        try {
            recordCapacity = 2;
            df = new DecimalFormat("###.###");

            accelerometerList = new ArrayList<>(recordCapacity);
            heartBeatList = new ArrayList<>(recordCapacity);
            magnetometerList = new ArrayList<>(recordCapacity);

            for (int counterRow = 0; counterRow < recordCapacity; counterRow++) {
                accelerometerList.add(new ArrayList<String>(5));
                magnetometerList.add(new ArrayList<String>(5));
                heartBeatList.add(new ArrayList<String>(3));
                for (int counterField = 0; counterField < 5; counterField++) {
                    accelerometerList.get(counterRow).add("");
                    magnetometerList.get(counterRow).add("");
                }
                for (int counterField = 0; counterField < 3; counterField++) {
                    heartBeatList.get(counterRow).add("");
                }
            }

            List<Sensor> mysensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor mine : mysensor)
                Log.e(TAG,mine.getName());


            accelerometerCounter = 0;
            magnetometerCounter = 0;
            heartBeatCounter = 0;

            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            Sensor heartBeat = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT);

            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
            if (magneticField != null) {
                sensorManager.registerListener(this, magneticField,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }

            if (heartBeat != null) {
                sensorManager.registerListener(this, heartBeat,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
        }catch (Exception e) {
            Log.e(TAG, "SensorReader"+e.getMessage());
        }
    }

    public void onSensorChanged(SensorEvent event) {
        //Log.d(TAG,"onSensorChanged");

        long eventTime= (long) (event.timestamp/1e7);
        int hour=(int) (eventTime/100/3600);
        eventTime-=hour*3600*100;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Log.e(TAG, "AccelX="+df.format(event.values[0])+"\t AccelY="+df.format(event.values[1])+"\t AccelZ="+df.format(event.values[2]));
            /*
            for (int fieldCounter = 0; fieldCounter < 3; fieldCounter++) {
                String str = String.format("%.5f", event.values[fieldCounter]);
                accelerometerList.get(accelerometerCounter).set(fieldCounter,str);
            }
            accelerometerList.get(accelerometerCounter).set(3,String.valueOf(hour));
            accelerometerList.get(accelerometerCounter).set(4,String.valueOf(eventTime));

            accelerometerCounter++;
            if(accelerometerCounter==recordCapacity){
                writtingSensorToFile(accelerometerList,"Accelerometer");
                accelerometerCounter=0;
            }
             */
        }

        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            Log.e(TAG, "magnoX="+df.format(event.values[0])+"\t magnoY="+df.format(event.values[1])+"\t magnoZ="+df.format(event.values[2]));
            /*
            for (int fieldCounter = 0; fieldCounter < 3; fieldCounter++) {
                String str = String.format("%.5f", event.values[fieldCounter]);
                magnetometerList.get(magnetometerCounter).set(fieldCounter,str);
            }
            magnetometerList.get(magnetometerCounter).set(3,String.valueOf(hour));
            magnetometerList.get(magnetometerCounter).set(4,String.valueOf(eventTime));

            magnetometerCounter++;
            if(magnetometerCounter==recordCapacity){
                writtingSensorToFile(magnetometerList,"Magnetometer");
                magnetometerCounter=0;
            }

             */
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_BEAT) {
            Log.e(TAG, "heart="+event.values[0]);
            /*
            heartBeatList.get(heartBeatCounter).set(0,String.valueOf(event.values[0]));
            heartBeatList.get(heartBeatCounter).set(1,String.valueOf(hour));
            heartBeatList.get(heartBeatCounter).set(2,String.valueOf(eventTime));

            heartBeatCounter++;
            if(heartBeatCounter==recordCapacity){
                writtingSensorToFile(heartBeatList,"Heartbeat");
                heartBeatCounter=0;
            }

             */
        }
    }

    protected void writtingSensorToFile(ArrayList<ArrayList<String>> myList, String ParameterName){
        Log.e(TAG,"writing "+ParameterName+" in writtingSensorToFile");

        File root = new File(this.fileAddressDest, "SensorDataFile");
        File fileName=null;
        int fileAddressIndex = 0;
        try {
            String sensorContext;
            if(myList.get(0).size()==5)
                sensorContext="X \t Y \t Z \t Hour \t Centi-Sec\n";
            else
                sensorContext="Var \t Hour \t Centi-Sec\n";
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
        } catch (Exception e) {
            Log.e(TAG,"writtingSensorToFile"+e.getMessage());
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
}