package uml_robotics.robotnexus;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

public class ControllerService extends Service {
    private ServiceLooper serviceLooper; //looper to maintain a service thread
    private Handler serviceHandler; // handler for posting to looper
    private static ArrayList<Robot> theModel; // the model of the system -> only manipulate through its methods
    private static ReentrantLock theModelLock; // keeps mutual exclusion of model tampering
    private ArrayList<Robot> model; // controller's copy of the model
    private ReentrantLock modelLock; // mutex for tampering with copt of the model
    private Boolean btInitOff = false; //used for checking initial state of user's bluetooth
    private BluetoothAdapter btAdapter;  //Adapter used for most bluetoothy stuff
    // callback that triggers when le devices are found by startLeScan()
    private BluetoothAdapter.LeScanCallback leCallback;
    // callback for gattserver events
    private BluetoothGattCallback btGattCallback;
    private boolean isScanning = false; //for tracking if app is scanning or not
    private UUID uuidOfInterest; //UUID that specifies this is a robot
    private HashMap<String, String> supportedServices;//known services
    private HashMap<String, String> supportedCharas; //known characteristics

    public ControllerService() {
    }


    @Override
    public void onCreate() {
        final String TAG = "Service.onCreate()";
        Log.i(TAG, "Service created");

        // create the service thread
        serviceLooper = new ServiceLooper();
        serviceLooper.start();

        // make the model and its lock
        theModel = new ArrayList<Robot>();
        theModelLock = new ReentrantLock();

        // sleeping for one second to give looper a chance to make handler
        try {
            sleep(1000);
        } catch (Exception ex) {
            Log.i(TAG, "Failed sleep to give service thread time.");
        }

        // post to service message queue
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {

                //Getting app's btAdapter
                btAdapter = BluetoothAdapter.getDefaultAdapter();

                //Turning on bluetooth if it is currently off
                if (!btAdapter.isEnabled()) {

                    btAdapter.enable();
                    // user did not have bt on
                    btInitOff = true;

                }

                //setting our robot uuid (this is sent in the advertisement)
                //uuidOfInterest = UUID.fromString("0000ec00-0000-1000-8000-00805f9b34fb");
                //uuidOfInterest = UUID.fromString("00001800-30de-4630-9b59-27228d45bf11");
                uuidOfInterest = UUID.fromString("11bf458d-2227-599b-3046-de3000180000");

                //Populating supportedServices and supportedCharas maps
                supportedServices = new HashMap<String, String>();
                supportedServices.put("00001800-0000-1000-8000-00805f9b34fb", "Generic Access");
                supportedServices.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute");
                supportedServices.put("00001800-30de-4630-9b59-27228d45bf11", "Image Receive");
                supportedServices.put("00001801-30de-4630-9b59-27228d45bf11", "Image Send");

                supportedCharas = new HashMap<String, String>();
                supportedCharas.put("00002a00-0000-1000-8000-00805f9b34fb", "Device Name");
                supportedCharas.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
                supportedCharas.put("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed");
                supportedCharas.put("00002a10-30de-4630-9b59-27228d45bf11", "Packet Read");
                supportedCharas.put("00002a11-30de-4630-9b59-27228d45bf11", "Missing Packet Write");
                supportedCharas.put("00002a12-30de-4630-9b59-27228d45bf11", "Packet Write");
                supportedCharas.put("00002a13-30de-4630-9b59-27228d45bf11", "Missing Packet Read");
                supportedCharas.put("00002a14-30de-4630-9b59-27228d45bf11", "Total Number of Packets");



            }
        }); // end handler post
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void onStartCommandSeparateThread(Intent intent, int flags, int startId) {
        final String TAG = "onStartSeparateThread";
    }

    @Override
    public void onDestroy() {

        Log.i("Service.onDestroy()", "Destroyed");
    }

    /**
     *returns null -> this is a pure started service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * responsible for keeping our service thread alive and referable through serviceHandler
     */
    private class ServiceLooper extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            serviceHandler = new Handler();
            Looper.loop();
        }
    }


    /**
     * **************************************
     * ** MODEL METHODS BELOW THIS COMMENT **
     * **************************************
     */


    /**
     * @return a copy of the current model
     */
    public static ArrayList<Robot> getModel() {
        return new ArrayList<Robot>(theModel);
    }
}

