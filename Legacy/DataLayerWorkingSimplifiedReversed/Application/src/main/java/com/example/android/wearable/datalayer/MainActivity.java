/*
 * Copyright (C) 2020 Sorush Omidvar. All rights reserved.
 *
 * This appliction is created under Dr. Mortazavi at Texas A&M University for continuous glucose
 * monitoring project funded by NSF.
 */

package com.example.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity
        implements DataClient.OnDataChangedListener,
                MessageClient.OnMessageReceivedListener,
                CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "phoneMainActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";

    private TextView myTextView;

    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        myTextView= (TextView)findViewById(R.id.textView1);
        myTextView.setText("My Awesome Text");

        STMISensorDataWritter();

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        SensorDataTransfer();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataItemGeneratorFuture =
                mGeneratorExecutor.scheduleWithFixedDelay(
                        new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);

        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "onDataChanged: " + dataEvents);
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();

                if ("/MY_PATH".equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset fuck_you = dataMapItem.getDataMap().getAsset("FUCK_YOU_KEY");
                    new ReadFuckYouAsyncTask().execute(fuck_you);

                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG,messageEvent.getRequestId()+" "+messageEvent.getPath());
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        Log.i(TAG,"onCapabilityChanged"+capabilityInfo);
    }

    public void SensorDataTransfer() {
        File root = new File(MainActivity.this.getFilesDir(), "SensorDataFile");
        File gpxfile=null;

        if (!root.exists()) {
            root.mkdir();
        }
        try {
            gpxfile = new File(root, "RawData");
        } catch (Exception e) {
            Log.e(TAG,"SensorDataTransfer"+e.getMessage());
        }

        Asset asset=toAsset(gpxfile);
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);

        dataItemTask.addOnSuccessListener(
                new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.e(TAG, "Sending image was successful: " + dataItem);
                    }
                });
    }

    @WorkerThread
    private void sendStartActivityMessage(String node) {
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this).sendMessage(node, START_ACTIVITY_PATH, new byte[0]);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.i(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    private static Asset toAsset(File RawData) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        FileInputStream fileInputStream=null;

        try{
            fileInputStream = new FileInputStream(RawData);//Accessing the sensor data file
        }catch(Exception e){
            Log.e(TAG,"toAsset"+e.getMessage());
        }
        try{
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = fileInputStream.read(buf)) != -1;) {
                byteStream.write(buf, 0, readNum);//Reading the file
            }
        }catch (Exception e) {
            Log.e(TAG,"toAsset"+e.getMessage());
        }
        try{
            return Asset.createFromBytes(byteStream.toByteArray());//returning the data as asset
        }
        finally {
            if (null != byteStream) {
                try {
                    byteStream.close();//closing the reader
                } catch (Exception e) {
                    Log.e(TAG,"toAsset"+e.getMessage());
                }
            }
        }
    }

    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                results.add(node.getId());
            }

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    /** Generates a DataItem based on an incrementing count. */
    private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.i(TAG, "Generating DataItem: " + request);
            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItem dataItem = Tasks.await(dataItemTask);
                Log.i(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }
    protected void STMISensorDataWritter(){
        File root = new File(MainActivity.this.getFilesDir(), "SensorDataFile");
        File gpxfile=null;

        if (!root.exists()) {
            root.mkdir();
        }
        try {
            gpxfile = new File(root, "RawData");
            FileWriter writer = new FileWriter(gpxfile);
            writer.write("This is a test file written by Sorush");
            writer.flush();
            writer.close();

        } catch (Exception e) {
            Log.e(TAG,"STMISensorDataWritter"+e.getMessage());
        }
    }
    public void onMyStart(View view) {
        SensorDataTransfer();
    }

    private class ReadFuckYouAsyncTask extends AsyncTask<Asset, Void, Void> {

        @Override
        protected Void doInBackground(Asset... assets) {
            Asset fuck_you_asset;
            if (assets.length > 0) {
                fuck_you_asset = assets[0];
            }else{
                return null;
            }

            Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask =
                    Wearable.getDataClient(getApplicationContext()).getFdForAsset(fuck_you_asset);
            try {
                DataClient.GetFdForAssetResponse getFdForAssetResponse =
                        Tasks.await(getFdForAssetResponseTask);
                InputStream assetInputStream = getFdForAssetResponse.getInputStream();
                if (assetInputStream != null) {
                    String fuck_you_str = null;
                    for (int i = 0; i < 10; i++) {
                        char fuck_you_char = (char) assetInputStream.read();
                        fuck_you_str += Character.toString(fuck_you_char);
                    }
                    Log.e(TAG, fuck_you_str);
                }
            } catch (ExecutionException exception) {
                Log.e(TAG, "Failed retrieving asset, Task failed: " + exception);
            } catch (InterruptedException exception) {
                Log.e(TAG, "Failed retrieving asset, interrupt occurred: " + exception);
            } catch (Exception e){
                Log.e(TAG, "real error!" + e.getMessage());
            }

            return null;
        }
    }
}