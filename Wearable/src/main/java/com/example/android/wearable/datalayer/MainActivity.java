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
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.Date;

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
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        try {
            String fileName = "";
            String fileAddress= "";

            int assetSizeInt=0;
            for (int counter = 0; counter < sensorReader.fileToBeSent.size(); counter++) {
                String stringTemp=sensorReader.fileToBeSent.get(counter);
                if(stringTemp=="")
                    continue;
                fileAddress +=stringTemp+"\n";
                fileName += stringTemp.substring(stringTemp.lastIndexOf('/') + 1)+"\n";
                sensorReader.fileToBeSent.remove(counter);
                assetSizeInt++;
            }
            if(assetSizeInt>0) {
                String assetSizeStr = String.valueOf(assetSizeInt);
                new SendDataAsyncTask().execute(fileAddress, fileName, assetSizeStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "onDataChanged" + e.getMessage());
        }
    }

    private class SendDataAsyncTask extends AsyncTask<String, Void,Void> implements OnSuccessListener<DataItem>, OnFailureListener {
        private ByteArrayOutputStream readFile(String path, String filename){
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try{
                BufferedReader bufferedReader = new BufferedReader(new FileReader(path),1024);
                byteStream.write((filename + "\n").getBytes());

                String line="";
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

        @Override
        protected Void doInBackground(String... params) {
            try {
                String fileAddress=params[0];
                String fileName=params[1];
                int assetSize=Integer.parseInt(params[2]);
                Asset[] assetArray=new Asset[assetSize];
                PutDataMapRequest dataMap = PutDataMapRequest.create(STMIWatchListenerService.STMI_SENSOR_PATH);
                for(int counter=0;counter<assetSize;counter++){
                    String elementAddress=fileAddress.substring(0,fileAddress.indexOf('\n'));
                    String elementName=fileName.substring(0,fileName.indexOf('\n'));


                    assetArray[counter] = Asset.createFromBytes(readFile(elementAddress, elementName).toByteArray());
                    dataMap.getDataMap().putAsset(elementName,assetArray[counter]);

                    Log.e(TAG,"name="+elementName+"\t address="+elementAddress);

                    fileAddress=fileAddress.substring((fileAddress.indexOf('\n')+1));
                    fileName=fileName.substring((fileName.indexOf('\n')+1));
                }
                dataMap.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent();
                Task<DataItem> putTask = Wearable.getDataClient(getApplicationContext()).putDataItem(request);

                putTask.addOnSuccessListener(this);
                putTask.addOnFailureListener(this);
            } catch (Exception e) {
                Log.e(TAG, "SendDataAsyncTask" + e.getMessage());
            }
            return null;
        }
        @Override
        public void onSuccess(DataItem dataItem) {
            Log.i(TAG,"Success in onSuccess: " + dataItem.getUri().toString());
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            Log.i(TAG,"Failure in onFailure:"+e.getMessage());
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