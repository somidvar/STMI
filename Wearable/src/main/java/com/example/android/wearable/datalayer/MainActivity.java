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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider,DataClient.OnDataChangedListener,
                MessageClient.OnMessageReceivedListener
{

    private static final String TAG = "MainActivity";
    SensorReader sensorReader;
    private boolean willbesend=true;

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


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.e(TAG, "onDataChanged transfer initializing");
        try {
            if(willbesend) {
                willbesend=false;
                String sensorRawDataStr = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/1.txt";
                String sensorRawDataStr4 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/2.txt";

                new SendDataAsyncTask().execute(sensorRawDataStr, "1.txt", sensorRawDataStr4, "2.txt");

//                new SendDataAsyncTask().execute(sensorRawDataStr4, "test (4).txt");
//                String sensorRawDataStr5 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/test (5).txt";
//                new SendDataAsyncTask().execute(sensorRawDataStr5, "test (5).txt");
//                String sensorRawDataStr6 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/test (6).txt";
//                new SendDataAsyncTask().execute(sensorRawDataStr6, "test (6).txt");
//                String sensorRawDataStr7 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/test (7).txt";
//                new SendDataAsyncTask().execute(sensorRawDataStr7, "test (7).txt");
//                String sensorRawDataStr8 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/test (8).txt";
//                new SendDataAsyncTask().execute(sensorRawDataStr8, "test (8).txt");
//                String sensorRawDataStr9 = "/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/test (9).txt";
//                new SendDataAsyncTask().execute(sensorRawDataStr9, "test (9).txt");





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
            }
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
    private class SendDataAsyncTask extends AsyncTask<String, Void,Void> implements OnSuccessListener<DataItem>, OnFailureListener {

        private ByteArrayOutputStream read_file(String path, String filename){
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try{
                Log.e(TAG, "Reading file.!");

                BufferedReader bufferedReader = new BufferedReader(new FileReader(path),1024);
                byteStream.write((filename + "\n").getBytes());

                String line="";
                int count=0;
                while((line = bufferedReader.readLine()) != null){
                    byteStream.write((line+"\n").getBytes());
                }
                bufferedReader.close();
                byteStream.flush();
            } catch (Exception e) {
                Log.e(TAG, "SendDataAsyncTask" + e.getMessage());
            }
            return byteStream;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected Void doInBackground(String... params) {

            try {
                Asset wearableDataAsset = Asset.createFromBytes(read_file(params[0], params[1]).toByteArray());
                Asset wearableDataAsset2 = Asset.createFromBytes(read_file(params[2], params[3]).toByteArray());

                PutDataMapRequest dataMap = PutDataMapRequest.create(STMIWatchListenerService.STMI_SENSOR_PATH);
                dataMap.getDataMap().putAsset(params[1], wearableDataAsset);
                dataMap.getDataMap().putAsset(params[3], wearableDataAsset2);
                dataMap.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent();
                Task<DataItem> putTask = Wearable.getDataClient(getApplicationContext()).putDataItem(request);

                putTask.addOnSuccessListener(this);
                putTask.addOnFailureListener(this);
                /*
                PutDataMapRequest dataMap = PutDataMapRequest.create(STMIWatchListenerService.STMI_SENSOR_PATH);
                dataMap.getDataMap().putAsset(STMIWatchListenerService.STMI_SENSOR_KEY, wearableDataAsset);
                dataMap.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent();
                Log.e(TAG, "5SendDataAsyncTask will be sent!");
                Wearable.getDataClient(getApplicationContext()).putDataItem(request);
                Log.e(TAG, "6SendDataAsyncTask will be sent!");

                 */
            } catch (Exception e) {
                Log.e(TAG, "SendDataAsyncTask" + e.getMessage());
            }
            return null;
        }
        @Override
        public void onSuccess(DataItem dataItem) {
            Log.e(TAG,"Success in onSuccess: " + dataItem.getUri().toString());
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            Log.e(TAG,"Failure in onFailure:"+e.getMessage());
        }
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
