package com.example.jazz.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private TextView tv;
    //the Sensor Manager
    private SensorManager sManager;

    private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private ServerConnectThread serverConnectThread;
    private ManageConnectThread manageConnectThread;

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
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        tv = (TextView) findViewById(R.id.tv);

        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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





        Button button = (Button) findViewById(R.id.button_send);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "bas", Toast.LENGTH_SHORT).show();

            }
        });

        Button button2 = (Button) findViewById(R.id.button_send2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "haut", Toast.LENGTH_SHORT).show();

            }
        });

        Button button3 = (Button) findViewById(R.id.button_send3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "droite", Toast.LENGTH_SHORT).show();

            }
        });

        Button button4 = (Button) findViewById(R.id.button_send4);
        button4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getApplicationContext(), "gauche", Toast.LENGTH_SHORT).show();

            }
        });

    }


    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
       // updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
       // updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        //updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

    //When this Activity isn't visible anymore

    protected void onStop()
    {
        //unregister the sensor listener
        sManager.unregisterListener(this);
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();

        connectThread.cancel();
        connectedThread.cancel();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    public void onSensorChanged(SensorEvent event)
    {
        //if sensor is unreliable, return void
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        //else it will output the Roll, Pitch and Yawn values
        tv.setText("Orientation X  :"+ Float.toString(event.values[2]) +"\n"+
                "Orientation Y  :"+ Float.toString(event.values[1]) );

      //  Log.d("orientation",Float.toString(event.values[2]) +" " +  Float.toString(event.values[1]) + " " + Float.toString(event.values[0]));
    }
}

class ServerConnectThread extends Thread{
    private BluetoothAdapter adapter;
    UUID uuid;
    BluetoothServerSocket serverSocket;
    ManageConnectThread manageConnectThread;

    public ServerConnectThread(BluetoothAdapter mBluetoothAdapter, UUID uuid) {
        this.adapter = mBluetoothAdapter;
        this.uuid = uuid;
        BluetoothServerSocket tmp = null;

        manageConnectThread = new ManageConnectThread();
        try {
            tmp  = adapter.listenUsingRfcommWithServiceRecord("Connect", uuid);
        } catch(IOException e) {
            Log.d("SERVERCONNECT", "Could not get a BluetoothServerSocket:" + e.toString());
        }

        serverSocket = tmp;
    }



    public void run() {
        BluetoothSocket bTSocket = null;

        while(true) {

            try {
                bTSocket = serverSocket.accept();
            } catch (IOException e) {
                Log.d("SERVERCONNECT", "Could not accept an incoming connection.");
                break;
            }
            if (bTSocket != null) {
                Log.d("SmartWatch -> ","connect");

                try {
                    manageConnectThread.sendData(bTSocket, 2);


                } catch (IOException e) {
                    e.printStackTrace();

                }

                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;


            }
        }
    }


    public void closeConnect() {
        try {
            serverSocket.close();
        } catch(IOException e) {
            Log.d("SERVERCONNECT", "Could not close connection:" + e.toString());
        }
    }
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

class ManageConnectThread extends Thread {

    public ManageConnectThread() { }

    public void sendData(BluetoothSocket socket, int data) throws IOException{
        ByteArrayOutputStream output = new ByteArrayOutputStream(4);
        output.write(data);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(output.toByteArray());
    }

    public int receiveData(BluetoothSocket socket) throws IOException{
        byte[] buffer = new byte[4];
        ByteArrayInputStream input = new ByteArrayInputStream(buffer);
        InputStream inputStream = socket.getInputStream();
        inputStream.read(buffer);
        return input.read();
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