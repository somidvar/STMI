/*
 * Copyright (C) 2020 Sorush Omidvar. All rights reserved.
 *
 * This appliction is created under Dr. Mortazavi at Texas A&M University for continuous glucose
 * monitoring project funded by NSF. This project is developed based on the DataLayer Android sample
 * project.
 */

package com.example.android.wearable.datalayer;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider,DataClient.OnDataChangedListener,
                MessageClient.OnMessageReceivedListener
{

    private static final String TAG = "MainActivity";
    SensorReader sensorReader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getDataClient(this).addListener(this);

        AmbientModeSupport.attach(this);
        //STMISensorDataWritter();
        Context myContext = getApplicationContext();
        SensorManager sensorManager = (SensorManager) getSystemService(myContext.SENSOR_SERVICE);
        sensorReader = new SensorReader(sensorManager,MainActivity.this.getFilesDir());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.e(TAG, "onMessageReceived" + event);
        if (event.getPath().equals(STMIWatchListenerService.STMI_TRANSMISSION_PATH)){


        }
    }

    private class SendDataAsyncTask extends AsyncTask<String, Void,Void> {

        @Override
        protected Void doInBackground(String... params) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(params[0]));
                String EOFGaurd = bufferedReader.readLine();
                String rawDataContext = "";
                while (EOFGaurd != null) {
                    rawDataContext += EOFGaurd + "\n";
                    EOFGaurd = bufferedReader.readLine();
                }
                String fileName=params[1]+"\n";
                byteStream.write(fileName.getBytes());
                //String fileCapacity=String.valueOf(sensorReader.recordCapacity)+"\n";
                //byteStream.write(fileCapacity.getBytes());

                byteStream.write(rawDataContext.getBytes());

                Asset wearableDataAsset = Asset.createFromBytes(byteStream.toByteArray());
                PutDataMapRequest dataMap = PutDataMapRequest.create(STMIWatchListenerService.STMI_SENSOR_PATH);
                dataMap.getDataMap().putAsset(STMIWatchListenerService.STMI_SENSOR_KEY, wearableDataAsset);
                dataMap.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest request = dataMap.asPutDataRequest();
                //request.setUrgent();
                Wearable.getDataClient(getApplicationContext()).putDataItem(request);
                Log.i(TAG, "SendDataAsyncTask is sent!");
                byteStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "SendDataAsyncTask" + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.e(TAG, "onDataChanged transfer initializing");
        try {
            String sensorRawDataStr = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/7.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr, "7.txt");

            /*
            String sensorRawDataStr1 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/2.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr1, "2.txt");

            String sensorRawDataStr2 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/3.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr2, "3.txt");


            String sensorRawDataStr3 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/4.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr3, "4.txt");

            String sensorRawDataStr4 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/5.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr4, "5.txt");

            String sensorRawDataStr5 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/6.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr5, "6.txt");

             */
        }catch (Exception e) {
            Log.e(TAG, "onDataChanged" + e.getMessage());
        }
        /*
        for (int counter=0;counter<sensorReader.fileToBeSent.size();counter++) {

            String fileName=sensorReader.fileToBeSent.get(counter);
            if(fileName=="")
                continue;
            fileName=fileName.substring(fileName.lastIndexOf('/')+1);
            new SendDataAsyncTask().execute(sensorReader.fileToBeSent.get(counter),fileName);
            Log.e(TAG,"onDataChanged sending file:"+sensorReader.fileToBeSent.get(counter));
            sensorReader.fileToBeSent.set(counter,"");
        }

         */
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }

}
