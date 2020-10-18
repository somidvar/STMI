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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import java.io.InputStream;

import java.util.concurrent.ExecutionException;

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

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();

                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photoAsset =dataMapItem.getDataMap().getAsset(DataLayerListenerService.IMAGE_KEY);

                    new LoadBitmapAsyncTask().execute(photoAsset);

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for COUNT_PATH");
                } else {
                    Log.d(TAG, "Unrecognized path: " + path);
                }

            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
    }

    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {

            if (params.length > 0) {
                Asset asset = params[0];
                Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask =
                        Wearable.getDataClient(getApplicationContext()).getFdForAsset(asset);

                try {
                    DataClient.GetFdForAssetResponse getFdForAssetResponse =
                            Tasks.await(getFdForAssetResponseTask);

                    InputStream assetInputStream = getFdForAssetResponse.getInputStream();

                    if (assetInputStream != null) {
                        try {
                            String contextSTMI=null;
                            for (int i = 0; i < 10; i++) {
                                char contextSTMIChar=(char) getFdForAssetResponse.getInputStream().read();
                                contextSTMI+=Character.toString(contextSTMIChar);
                            }
                            Log.e(TAG, "-----------------------------------Gholi" + contextSTMI);

                        }catch (Exception e){
                            Log.e(TAG, "real error!");
                        }

                        return BitmapFactory.decodeStream(assetInputStream);

                    } else {
                        Log.w(TAG, "Requested an unknown Asset.");
                        return null;
                    }

                } catch (ExecutionException exception) {
                    Log.e(TAG, "Failed retrieving asset, Task failed: " + exception);
                    return null;

                } catch (InterruptedException exception) {
                    Log.e(TAG, "Failed retrieving asset, interrupt occurred: " + exception);
                    return null;
                }

            } else {
                Log.e(TAG, "Asset must be non-null");
                return null;
            }
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
