package com.example.jazz.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
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

    private float XOrientation1, XOrientation2, YOrientation1, YOrientation2;
    private boolean onCreate = false;
    private static final int DELTA = 20;
    private static boolean StopSending = false;

    private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
    BluetoothAdapter mBluetoothAdapter;

    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    BluetoothDevice robot;
    private BluetoothSocket socket = null;
    private InputStream receiveStream = null;// Canal de réception
    private OutputStream sendStream = null;// Canal d'émission
    private static BluetoothResponseHandler mHandler;
    static ConnectThread connectThread;
    static ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        //mClockView = (TextView) findViewById(R.id.clock);
        tv = (TextView) findViewById(R.id.tv);

        onCreate = true;

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
                    if(device.getName().contains("raspberrypi")) {

                        robot = device;

                        //if(connectThread==null)
                            connectThread = new ConnectThread(robot,UUID.fromString("00001200-0000-1000-8000-00805f9b34fb"), mBluetoothAdapter);
                        connectThread.run();

                    }
                }


            }
        }





        /*Button button = (Button) findViewById(R.id.button_send);
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
        });*/

        Button button_connect = (Button) findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /*Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                for (BluetoothDevice device : pairedDevices) {
                    if(device.getName().contains("HC-05")) {

                        robot = device;
                        if(connectThread==null)
                            connectThread = new ConnectThread(robot,UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"), mBluetoothAdapter);
                        connectThread.run();

                    }
                }*/
            }
        });

        Button button_stop = (Button) findViewById(R.id.button_stop);
        button_stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                StopSending = !StopSending;
                Toast.makeText(getApplicationContext(), !StopSending ? "Envoi activé" : "Envoi stoppé", Toast.LENGTH_SHORT).show();

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
        stop();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothAdapter.cancelDiscovery();

       // connectThread.cancel();
       // connectedThread.cancel();
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

        if(onCreate) {
            XOrientation1 = event.values[2];
            YOrientation1 = event.values[1];
            onCreate = false;
        }

        if(!StopSending) {
            XOrientation2 = event.values[2];
            YOrientation2 = event.values[1];

            if(YOrientation2 - YOrientation1 >= DELTA){

                sendValue("H");
            }

            if(XOrientation1 - XOrientation2 >= DELTA){
                sendValue("D");
            }

            if(YOrientation2 - YOrientation1 < -DELTA){
                sendValue("B");
            }

            if(XOrientation1 - XOrientation2 < -DELTA){
                sendValue("G");
            }
        }



        //else it will output the Roll, Pitch and Yawn values
        tv.setText("Orientation X  :"+ Float.toString(event.values[2]) +"\n"+
                "Orientation Y  :"+ Float.toString(event.values[1]) );

      //  Log.d("orientation",Float.toString(event.values[2]) +" " +  Float.toString(event.values[1]) + " " + Float.toString(event.values[0]));
    }


    private void sendValue(String value) {
        if(connectedThread != null && getState() == MainActivity.STATE_CONNECTED) {
            Log.d("SendValue", "sending "+value);
            byte[] command = value.getBytes();
            write(command);
        }
    }

    private synchronized void setState(int state) {
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }


    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    public synchronized void connected(BluetoothSocket socket) {
        Log.d("Connexion", "Connected to "+socket.getRemoteDevice().getName());
        Toast.makeText(this, "Connecté",
                Toast.LENGTH_LONG).show();

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTED);

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME, socket.getRemoteDevice().getName());
        mHandler.sendMessage(msg);

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        //connectedThread.start();
    }


    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }


    public void write(byte[] data) {
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = connectedThread;
        }

        // Perform the write unsynchronized
        if (data.length == 1) r.write(data[0]);
        else r.writeData(data);
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

            if(mmSocket == null) {
                connectionFailed();
                return;
            }

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                connectionFailed();
                return;
            }


            // Reset the ConnectThread because we're done
            synchronized (MainActivity.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
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

        public void write(byte command) {
            byte[] buffer = new byte[1];
            buffer[0] = command;

            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e("write", "Exception during write", e);
                connectionLost();
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void writeData(byte[] chunk) {

            try {
                mmOutStream.write(chunk);
                mmOutStream.flush();
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, chunk).sendToTarget();
            } catch (IOException e) {
                Log.e("writeData", "Exception during write", e);
                connectionLost();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public BluetoothResponseHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        public void setTarget(MainActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<MainActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }

}

