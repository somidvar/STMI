package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stmiMobileBluetoothActivation();
    }

    //Checking the bluetooth adaptor and enabling it if it is disabled
    public void stmiMobileBluetoothActivation(){

        TextView statusTextViewVar = (TextView)findViewById(R.id.statusTextViewMobile);
        Intent btEnablingIntent;
        BluetoothAdapter myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(myBluetoothAdapter==null)
            statusTextViewVar.setText("No Bluetooth available");
        else{
            if(!myBluetoothAdapter.isEnabled()){
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);
            }
            else
                statusTextViewVar.setText("Bluetooth is enabled");
        }
    }
    //Overriding the onActivityResult. After enabling the bluetooth a toast pops up to inform the user.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TextView statusTextViewVar = (TextView)findViewById(R.id.statusTextViewMobile);
        if(requestCode==1){
            if(resultCode==RESULT_OK) {
                Toast.makeText(getApplicationContext(),"Request Accepted",Toast.LENGTH_LONG).show();
                statusTextViewVar.setText("Bluetooth is enabled");
            }
            if(resultCode==RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Request Denied",Toast.LENGTH_LONG).show();
                statusTextViewVar.setText("Bluetooth is disabled");
            }
        }
    }
}
