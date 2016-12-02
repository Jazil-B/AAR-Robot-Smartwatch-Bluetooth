package com.example.jazz.control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {


    private TextView mTextView;
    private TextView mClockView;

    private TextView tv;
    //the Sensor Manager
    private SensorManager sManager;

    private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    BluetoothAdapter mBluetoothAdapter;
    private static BluetoothResponseHandler mHandler;

    BluetoothDevice robot;
    private BluetoothSocket socket = null;
    private InputStream receiveStream = null;// Canal de réception
    private OutputStream sendStream = null;// Canal d'émission
    private ConnectThread connectThread;
    static ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        if (mBluetoothAdapter == null)
            Toast.makeText(this, "La montre ne supporte pas le Bluetooth",
                    Toast.LENGTH_SHORT).show();
        else {

            if(!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();

            Toast.makeText(this, "Bluetooth detecté et activé",
                    Toast.LENGTH_SHORT).show();

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {

                    Log.d("Appaired with :","-> "+device.getName());
                    if(device.getName().contains("HC-05")) {

                        robot = device;

                        // serverConnectThread = new ServerConnectThread(mBluetoothAdapter, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                        //serverConnectThread.run();

                        connectThread = new ConnectThread(robot,UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), mBluetoothAdapter);
                        connectThread.run();

                    }
                }


            }
        }

    }

    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    //When this Activity isn't visible anymore

    protected void onStop()
    {
        //unregister the sensor listener
       super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();

        connectThread.cancel();
        connectedThread.cancel();
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private BluetoothAdapter adapter;
        UUID uuid;

        public ConnectThread(BluetoothDevice device, UUID uuid, BluetoothAdapter adapter) {
            this.adapter = adapter;
            this.uuid = uuid;
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            adapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            Log.d("Connexion", "Connected to "+mmDevice.getName());

            MainActivity.connectedThread = new ConnectedThread(mmSocket);
            MainActivity.connectedThread.write(1);
            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                   /* mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();*/
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(int data) {
            try {
                mmOutStream.write(data);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
