package com.example.jazz.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.Pair;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends WearableActivity implements SensorEventListener {


    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private GraphView mDrawView;
    private TextView tv;
    //the Sensor Manager
    private SensorManager sManager;
    private GraphDrawing callback;

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

    ControlBDD bdd;
    String currentDir = null;
    long avant, apres;
    long first,second;
    long time = 0;
    int cpt_droite=0,cpt_gauche=0,cpt_zero=0;
    private boolean sameDirection = false;
    boolean reboot = false;

    public static ArrayList<String> MemoList = new ArrayList<String>();
    public ArrayList<String> MemoListCmp = new ArrayList<String>();


    List<Pair<String, Long>> parcours;
    Stack<Pair<String, Long>> parcours2;
    private boolean parcoursArriere = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        mDrawView = (GraphView)findViewById(R.id.GraphView);
        // rend visible la vue
        mDrawView.setVisibility(View.VISIBLE);

        mDrawView.setKeepScreenOn(true);

        callback = mDrawView;

        MemoList.add("X");
        MemoList.add("X");
        MemoList.add("X");
        MemoList.add("X");

        parcours = new ArrayList<>();
        parcours2= new Stack<>();

        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        //mClockView = (TextView) findViewById(R.id.clock);
        tv = (TextView) findViewById(R.id.tv);

        onCreate = true;

        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bdd = new ControlBDD(this);

        bdd.open();

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        if (mBluetoothAdapter == null)
            Toast.makeText(this, "La montre ne supporte pas le Bluetooth",
                    Toast.LENGTH_SHORT).show();
        else {

            if(!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();

           /* Toast.makeText(this, "Bluetooth detecté et activé",
                    Toast.LENGTH_SHORT).show();*/

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {

                    Log.d("Appaired with :","-> "+device.getName());
                    if(device.getName().contains("HC-05")) {

                        robot = device;

                        //if(connectThread==null)
                            connectThread = new ConnectThread(robot,UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"), mBluetoothAdapter);
                        connectThread.run();

                    }
                }
            }
        }


        findViewById(R.id.container).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Toast.makeText(MainActivity.this, "Parcours arrière", Toast.LENGTH_SHORT).show();

                parcoursArriere = true;

                //System.out.println("List : "+MemoList.toString());


                LongOperation send = new LongOperation();
                send.execute(MemoList.toString());


                MemoList.clear();
                parcoursArriere = false;

                MemoList.add("X");
                MemoList.add("X");
                MemoList.add("X");
                MemoList.add("X");

                return true;
            }
        });

    }

    public class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... para) {

            //System.out.println("Test ->  "+para[0]);


            para[0]=para[0].replace("[","");
            para[0]=para[0].replace(" ","");
            para[0]=para[0].replace(",","");
            para[0]=para[0].replace("]","");

            char[] ipara = para[0].toCharArray();



         /*       new Thread(new Runnable() {
                        public void run() {*/

            for (int i=0; i<para[0].length();i++) {
                // Toast.makeText(MainActivity.this,"Caractère -> "+ ipara[((para[0].length()-1)-i)], Toast.LENGTH_SHORT).show();
               // System.out.println("Caractère -> "+ ipara[((para[0].length()-1)-i)]+" indice : "+((para[0].length()-1)-i));


                switch (ipara[((para[0].length()-1)-i)]) {
                    case 'H':
                        sendValue("B");
                        sendValue("B");

                        // System.out.println("List : B"+" indice : "+ ((MemoList.size()-1)-i));
                        break;
                    case 'B':
                        sendValue("H");
                        sendValue("H");
                        // System.out.println("List : H"+" indice : "+ ((MemoList.size()-1)-i));
                        break;
                    case 'D':
                        sendValue("G");
                        sendValue("G");
                        // System.out.println("List : G"+" indice : "+ ((MemoList.size()-1)-i));
                        break;
                    case 'G':
                        sendValue("D");
                        sendValue("D");
                        // System.out.println("List : D"+" indice : "+ ((MemoList.size()-1)-i));
                        break;
                                 /*   case "X":
                                        sendValue("X");
                                        System.out.println("List : X"+" indice : "+ ((MemoList.size()-1)-i));
                                        break;*/
                    default:
                        break;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
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

        sendValue("X");

        //unregister the sensor listener
        sManager.unregisterListener(this);
        stop();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        sendValue("X");

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

            if(YOrientation2 - YOrientation1 >= DELTA && YOrientation2 - YOrientation1 <= DELTA*2){
                sendValue("H");
/*
                if(second-first<=0 || second-first>5625){
*/
                    callback.drawGraph(0);
              /*  }else if(second-first>0 && second-first<=2500 ){
                    callback.drawGraph(4);
                }else if(second-first>2500 && second-first<=3000){
                    callback.drawGraph(1);
                }else if(second-first>3000 && second-first<=3750){
                    callback.drawGraph(5);
                }else if(second-first>3750 && second-first<=4500){
                    callback.drawGraph(2);
                }else if(second-first>4500 && second-first<=4870){
                    callback.drawGraph(6);
                }else if(second-first>4870 && second-first<=5250){
                    callback.drawGraph(3);
                }else if(second-first>5250 && second-first<=5625){
                    callback.drawGraph(7);
                }*/
            }else if(XOrientation1 - XOrientation2 >= DELTA){
                sendValue("D");
                callback.drawGraph(1);

              /*  if(cpt_droite==0){
                    first = System.currentTimeMillis();
                    cpt_droite++;
                    cpt_zero=0;
                }*/
            }else if(YOrientation2 - YOrientation1 <= -DELTA && YOrientation2 - YOrientation1 >= -(DELTA*2)){
                sendValue("B");
/*
                if(second-first<=0 || second-first>5625){
*/
                    callback.drawGraph(2);
               /* }else if(second-first>0 && second-first<=2500 ){
                    callback.drawGraph(6);
                }else if(second-first>2500 && second-first<=3000){
                    callback.drawGraph(3);
                }else if(second-first>3000 && second-first<=3750){
                    callback.drawGraph(7);
                }else if(second-first>3750 && second-first<=4500){
                    callback.drawGraph(0);
                }else if(second-first>4500 && second-first<=4870){
                    callback.drawGraph(4);
                }else if(second-first>4870 && second-first<=5250){
                    callback.drawGraph(1);
                }else if(second-first>5250 && second-first<=5625){
                    callback.drawGraph(5);
                }*/
            }else if(XOrientation1 - XOrientation2 < -DELTA) {
                sendValue("G");
              /*  if(cpt_gauche==0){
                    first = System.currentTimeMillis();
                    cpt_gauche++;
                    cpt_zero=0;
                }*/
                callback.drawGraph(3);

            }else if(YOrientation2 - YOrientation1 >= DELTA*2){
                sendValue("T");
                sendValue("H");
                callback.drawGraph(4);

            }else if(YOrientation2 - YOrientation1 <= -(DELTA*2)){
                sendValue("L");
                sendValue("B");
                callback.drawGraph(5);

            }else{
                sendValue("X");
                callback.drawGraph(8);
              /*  if((cpt_zero==0 && cpt_droite!=0) || (cpt_zero==0 && cpt_gauche!=0)) {
                    second = System.currentTimeMillis();
                    cpt_zero++;
                    cpt_droite=0;
                    cpt_gauche=0;
                }*/
            }
        }



        //else it will output the Roll, Pitch and Yawn values
//        tv.setText("Orientation X  :"+ Float.toString(event.values[2]) +"\n"+
  //              "Orientation Y  :"+ Float.toString(event.values[1]) );

      //  Log.d("orientation",Float.toString(event.values[2]) +" " +  Float.toString(event.values[1]) + " " + Float.toString(event.values[0]));
    }


    public ArrayList<String> ArrayListCmp(){return MemoList;}

    public void sendValue(String value) {
        if(connectedThread != null && getState() == MainActivity.STATE_CONNECTED) {
          // Log.d("SendValue", "sending "+value);

/*            if(!parcoursArriere) {
                if(time == 0 && !value.equals("X")) {
                    time = System.currentTimeMillis();
                    System.out.println("avant : "+time);
                }

                if(currentDir == null) currentDir = value;

                //if(!(sameDirection = sameDirection(value))) {
                    apres = System.currentTimeMillis();
                    System.out.println("apres : "+apres);

                    time = apres - time;

                    System.out.println("time : "+time);

                    if(time != 0) parcours2.push(new Pair<>(currentDir, time));//parcours.add(new Pair<>(currentDir, time));

                    // bdd.insertDirection(currentDir, time);

                    currentDir = value;
                    time = 0;
               // }
            }*/


            if(!parcoursArriere && !value.equals("X")) {



                MemoList.add(value);

            }

            byte[] command = value.getBytes();
            write(command);
        }
    }

    private boolean sameDirection(String value) {
        return value.equals(currentDir);
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

       // callback.drawGraph(6);



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

    public interface GraphDrawing {
        void drawGraph(int dir);
    }

}

