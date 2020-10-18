/*
 * Copyright (C) 2020 Sorush Omidvar. All rights reserved.
 *
 * This appliction is created under Dr. Mortazavi supervision at Texas A&M University for continuous
 * glucose monitoring project funded by NSF. This project is developed based on the DataLayer
 * Android sample project.
 */

package com.example.android.wearable.datalayer;

import android.app.Activity;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
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

    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";

    private static final String STMI_SENSOR_PATH = "/sensor";
    private static final String STMI_TRANSMISSION_PATH = "/transmission";

    private TextView myTextView;

    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        myTextView= (TextView)findViewById(R.id.textView1);
        myTextView.setText("My Awesome Text");

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);

        mDataItemGeneratorFuture =
                mGeneratorExecutor.scheduleWithFixedDelay(
                        new DataItemGenerator(), 1, 10, TimeUnit.SECONDS);

        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);

        //onlyForTest();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (STMI_SENSOR_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    int readData = 0;
                    for (String key : dataMapItem.getDataMap().keySet()){
                        if (key.equals("time")) continue;;
                        Asset phoneDataAsset = dataMapItem.getDataMap().getAsset(key);
                        new PhoneReceiverAsyncTask().execute(phoneDataAsset);
                        readData++;
                    }
                }else{
                    Log.e(TAG, "Unrecognized path: " + path);
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

    @WorkerThread
    private void sendStartActivityMessage(String node) {
        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this).sendMessage(node, STMI_TRANSMISSION_PATH, new byte[0]);

        try {
            Integer result = Tasks.await(sendMessageTask);
            Log.i(TAG, "Message sent: " + result);
        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }

    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        try {
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
                DataItem dataItem = Tasks.await(dataItemTask);
                Log.i(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }

    public void onMyStart(View view) {
        new StartWearableActivityTask().execute();
    }

    private class PhoneReceiverAsyncTask extends AsyncTask<Asset, Void, Void> implements OnSuccessListener<DataClient.GetFdForAssetResponse> {

        @Override
        protected Void doInBackground(Asset... assets) {
            Asset phoneAsset;
            if (assets.length > 0 && assets[0] != null) {
                phoneAsset = assets[0];
            }else{
                return null;
            }
            Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask =
                    Wearable.getDataClient(getApplicationContext()).getFdForAsset(phoneAsset);
            getFdForAssetResponseTask.addOnSuccessListener(this);
            try {
                DataClient.GetFdForAssetResponse getFdForAssetResponse =
                        Tasks.await(getFdForAssetResponseTask);
                InputStream assetInputStream = getFdForAssetResponse.getInputStream();

                String str = "";
                StringBuffer stringBuffer = new StringBuffer();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetInputStream));
                if (assetInputStream != null) {
                    while ((str = bufferedReader.readLine()) != null) {
                        stringBuffer.append(str + "\n" );
                    }
                }
                bufferedReader.close();
                assetInputStream.close();
                String phoneAssetStr = stringBuffer.toString();
                Log.e(TAG, "Data is: "+phoneAssetStr.substring(0,phoneAssetStr.indexOf('\n')));

                STMISensorDataWritterPhone(phoneAssetStr);
            }catch (Exception e){
                Log.e(TAG, "PhoneReceiverAsyncTask" + e.getMessage());
            }
            return null;
        }

        @Override
        public void onSuccess(DataClient.GetFdForAssetResponse getFdForAssetResponse) {
            Log.e(TAG, "Success received.");
        }
    }
    protected void STMISensorDataWritterPhone(String sensorRawData){

        File root = new File(MainActivity.this.getFilesDir(), "SensorDataFile");
        File gpxfile=null;
        if (!root.exists()) {
            root.mkdir();
        }

        String fileName="";
        try {
            fileName=sensorRawData.substring(0,sensorRawData.indexOf('\n'));
            sensorRawData=sensorRawData.substring(sensorRawData.indexOf('\n')+1);

            gpxfile = new File(root, fileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.write(sensorRawData);
            writer.flush();
            writer.close();
            Log.e(TAG,"STMISensorDataWritter==="+fileName);
         } catch (Exception e) {
            Log.e(TAG,"STMISensorDataWritterPhone"+e.getMessage());
        }
    }
    public void onlyForTest(){
        File root = new File(this.getFilesDir(), "SensorDataFile");
        if (root.exists()) {
            String[] entries = root.list();
            for (String s : entries) {
                File currentFile = new File(root.getPath(), s);
                currentFile.delete();
            }
        }
    }
}