/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
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


public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider,
                DataClient.OnDataChangedListener,
                MessageClient.OnMessageReceivedListener,
                CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        AmbientModeSupport.attach(this);
        STMISensorDataWritter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this).addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "onDataChanged(): " + dataEvents);
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.i(TAG, "onMessageReceived " + event);
        if (event.getPath().equals(DataLayerListenerService.STMI_TRANSMISSION_PATH)){
            String sensorRawDataStr="/data/data/com.example.android.wearable.datalayer/files/SensorDataFile/RawData.txt";
            new SendDataAsyncTask().execute(sensorRawDataStr);
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
                byteStream.write(rawDataContext.getBytes());

                Asset wearableDataAsset = Asset.createFromBytes(byteStream.toByteArray());
                PutDataMapRequest dataMap = PutDataMapRequest.create(DataLayerListenerService.STMI_SENSOR_PATH);
                dataMap.getDataMap().putAsset(DataLayerListenerService.STMI_SENSOR_KEY, wearableDataAsset);
                dataMap.getDataMap().putLong("time", new Date().getTime());
                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent();
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
    protected void STMISensorDataWritter(){
        File root = new File(MainActivity.this.getFilesDir(), "SensorDataFile");
        File gpxfile=null;

        if (!root.exists()) {
            root.mkdir();
        }
        try {
            gpxfile = new File(root, "RawData.txt");
            FileWriter writer = new FileWriter(gpxfile);
            writer.write("123456789\n");
            writer.write("abcdefghij\n");
            writer.write("Sorush is Hero\n");
            writer.flush();
            writer.close();

        } catch (Exception e) {
            Log.e(TAG,"STMISensorDataWritter"+e.getMessage());
        }
    }
}
