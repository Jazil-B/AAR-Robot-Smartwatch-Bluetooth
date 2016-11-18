package com.example.jazz.control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {

    private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null)
            Toast.makeText(this, "Le téléphone ne supporte pas le Bluetooth",
                    Toast.LENGTH_SHORT).show();
        else {
            mBluetoothAdapter.enable();
//            if (!mBluetoothAdapter.isEnabled()) {
//                Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
//            }

            Toast.makeText(this, "Bluetooth detecté et activé",
                    Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    Log.d("Appaired Devices", device.getName() + "\n" + device.getAddress());
                }
                Toast.makeText(this, "Paired with "+pairedDevices.size()+" devices",
                        Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(this, "No devices paired, starting discovery",
                        Toast.LENGTH_SHORT).show();

                mBluetoothAdapter.startDiscovery();

                // Create a BroadcastReceiver for ACTION_FOUND
                final BroadcastReceiver mReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        // When discovery finds a device
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            // Get the BluetoothDevice object from the Intent
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            // Add the name and address to an array adapter to show in a ListView
                            Log.d("Found Devices", device.getName() + "\n" + device.getAddress());
                        }
                    }
                };
                // Register the BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_ENABLE_BLUETOOTH)
            return;
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth detecté et activé",
                    Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    Log.d("Appaired Devices", device.getName() + "\n" + device.getAddress());
                }
                Toast.makeText(this, "Paired with "+pairedDevices.size()+" devices",
                        Toast.LENGTH_SHORT).show();

            }
            else {
                Toast.makeText(this, "No devices paired, starting discovery",
                        Toast.LENGTH_SHORT).show();

                mBluetoothAdapter.startDiscovery();

                // Create a BroadcastReceiver for ACTION_FOUND
                final BroadcastReceiver mReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        // When discovery finds a device
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            // Get the BluetoothDevice object from the Intent
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            // Add the name and address to an array adapter to show in a ListView
                            Log.d("Found Devices", device.getName() + "\n" + device.getAddress());
                        }
                    }
                };
                // Register the BroadcastReceiver
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
            }

        } else {
            Toast.makeText(this, "Bluetooth désactivé",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
