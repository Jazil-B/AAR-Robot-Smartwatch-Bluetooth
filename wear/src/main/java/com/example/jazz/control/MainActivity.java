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
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


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

    BluetoothAdapter mBluetoothAdapter;

    private int mState;

    // Constantes qui indiquent l'état de la connexion
    public static final int STATE_NONE = 0;       // On ne fait rien
    public static final int STATE_CONNECTED = 2;  // Connecté à un device

    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    BluetoothDevice robot;
    private static BluetoothResponseHandler mHandler;
    static ConnectThread connectThread;
    static ConnectedThread connectedThread;

    ControlBDD bdd;
    String currentDir = null;

    public static ArrayList<String> MemoList = new ArrayList<String>();
    public ArrayList<String> MemoListCmp = new ArrayList<String>();

    private boolean parcoursArriere = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        mDrawView = (GraphView)findViewById(R.id.GraphView);

        // rend visible la vue
        mDrawView.setVisibility(View.VISIBLE);

        //Désactive la mise en veille de la montre
        mDrawView.setKeepScreenOn(true);

        callback = mDrawView;


        //initialise la ArrayList afin d'arrêter le robot une fois le retour à sa position activé
        MemoList.add("X");
        MemoList.add("X");
        MemoList.add("X");
        MemoList.add("X");

        setAmbientEnabled();

        onCreate = true;

        //On récupère le SensorManager de la montre pour le gyroscope
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //On récupère l'adaptateur Bluetooth de la montre
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        if (mBluetoothAdapter == null)
            Toast.makeText(this, "La montre ne supporte pas le Bluetooth",
                    Toast.LENGTH_SHORT).show();
        else {

            if(!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();

            //On boucle parmi la liste des devices appairé à la montre
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // S'il existe des appareils appairés
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    Log.d("Appaired with :","-> "+device.getName());
                    //On selectionne le robot parmi la liste
                    if(device.getName().contains("HC-05")) {

                        robot = device;

                        /*  Une fois que le robot est reconnu parmi la liste des appareils appairée, on démarre le thread de connexion, en passant en
                            paramètre le BluetoothDevice contenant le robot, et l'UUID de la connexion à effectuer
                         */
                        connectThread = new ConnectThread(robot,UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"), mBluetoothAdapter);
                        connectThread.run();

                    }
                }
            }
        }


        /* On enregistre un listener qui va écouter si un appui long sur l'écran de la montre à
            été fait. Cela aura pour conséquence de démarrer le parcours arrière du robot, afin
            qu'il revienne à son point de départ.
         */
        findViewById(R.id.container).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Toast.makeText(MainActivity.this, "Parcours arrière", Toast.LENGTH_SHORT).show();

                parcoursArriere = true;

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

    /*  AsyncTask qui permettra de boucler parmi la liste des caractères
        envoyés au robot enregistrés dans une liste, et de les renvoyer
        à l'inverse au robot lors du parcours arrière
     */
    public class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... para) {

            para[0]=para[0].replace("[","");
            para[0]=para[0].replace(" ","");
            para[0]=para[0].replace(",","");
            para[0]=para[0].replace("]","");

            char[] ipara = para[0].toCharArray();

            for (int i=0; i<para[0].length();i++) {

                switch (ipara[((para[0].length()-1)-i)]) {
                    case 'H':
                        sendValue("B");
                        sendValue("B");
                        break;
                    case 'B':
                        sendValue("H");
                        sendValue("H");
                        break;
                    case 'D':
                        sendValue("G");
                        sendValue("G");
                        break;
                    case 'G':
                        sendValue("D");
                        sendValue("D");
                        break;
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
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

    // Appelé quand l'Activity démarre.
    @Override
    protected void onResume()
    {
        super.onResume();

        /*  Enregistre le Sensor Listener et lui ordonne d'écouter le capteur du gyroscope,
            et de récupérer ses valeurs aussi vite que possible.
         */
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

   //Quand l'Activity n'est plus du tout visible
    protected void onStop()
    {

        sendValue("X");

        //Déréférence le sensor listener.
        sManager.unregisterListener(this);
        stop();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        sendValue("X");

        mBluetoothAdapter.cancelDiscovery();
    }

    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    public void onSensorChanged(SensorEvent event)
    {
        //Si le sensor n'est pas assez précis, on ne l'utilise pas
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        //Après démarrage de l'application, on récupère les premières valeurs renvoyés par le gyroscope
        if(onCreate) {
            XOrientation1 = event.values[2];
            YOrientation1 = event.values[1];
            onCreate = false;
        }

        /*
            Pour chaque changement de valeurs du gyroscope, nous enregistrons ces nouvelles valeurs
            et faisons la différence entre les premières valeurs et ces nouvelles valeurs. En fonction
            de la valeur de cette différence, nous envoyons un caractère associé à une certaine
            direction pour le robot.
         */
        if(!StopSending) {
            XOrientation2 = event.values[2];
            YOrientation2 = event.values[1];

            if(YOrientation2 - YOrientation1 >= DELTA && YOrientation2 - YOrientation1 <= DELTA*2){
                sendValue("H");
                callback.drawGraph(0);

            }else if(XOrientation1 - XOrientation2 >= DELTA){
                sendValue("D");
                callback.drawGraph(1);
            }else if(YOrientation2 - YOrientation1 <= -DELTA && YOrientation2 - YOrientation1 >= -(DELTA*2)){
                sendValue("B");

                callback.drawGraph(2);

            }else if(XOrientation1 - XOrientation2 < -DELTA) {
                sendValue("G");
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
                // Nous envoyons un "X" pour que le robot reste immobile
                sendValue("X");
                callback.drawGraph(8);
            }
        }
    }


    public ArrayList<String> ArrayListCmp(){return MemoList;}

    public void sendValue(String value) {
        if(connectedThread != null && getState() == MainActivity.STATE_CONNECTED) {
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

        // On annule le thread qui à complété la connexion
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTED);

        // On envoie le nom du device connecté au thread principal
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME, socket.getRemoteDevice().getName());
        mHandler.sendMessage(msg);

        //Démarre le thread qui gérera la connexion et s'occupera de la transmission
        connectedThread = new ConnectedThread(socket);
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
        // On synchronise une copie du ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = connectedThread;
        }

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
            //On utilise un objet temporaire, assigné plus tard à mmSocket
            //parce que mmSocket est final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // On essaye d'obtenir un BluetoothSocket pour le connecter
            // avec le BluetoothDevice donné
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            /*  Si la recherche d'appareils Bluetooth est toujours en cours
                on l'annule car cela ralenti la connexion
              */
            adapter.cancelDiscovery();

            if(mmSocket == null) {
                connectionFailed();
                return;
            }

            try {
                /*  Connecte l'appareil à travers le socket. Cet appel bloquera
                    jusqu'à ce que la connexion soit acceptée, ou alors renverra
                    une erreur
                 */
                mmSocket.connect();
            } catch (IOException connectException) {
                // Impossible de se connecter, on ferme le socket et on sort
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                connectionFailed();
                return;
            }


            // Reset le ConnectThread parce que nous en avons plus besoin
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

        /* On l'appelle depuis le thread principal pour envoyer les données au robot */
        public void writeData(byte[] chunk) {

            try {
                mmOutStream.write(chunk);
                mmOutStream.flush();

                // On rapporte au thread principal le message
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, chunk).sendToTarget();
            } catch (IOException e) {
                Log.e("writeData", "Exception during write", e);
                connectionLost();
            }
        }

        /* On l'appelle depuis le thread principal pour stopper la connection */
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

