package uml_robotics.robotnexus;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
    //Holds bluetooth devices we don't care about
    private List<BluetoothDevice> rejectedDeviceList = null;
    // holds bluetooth devices identified as robots
    private HashMap<BluetoothDevice, Integer> robotsAsBTDevices;
    private HashMap<Integer, String> packetsFound = null; // map multi packet reads
    // blocks makeRobot() from finishing until descriptor has been written - **Hack-Fix**
    private BlockingQueue<Integer> makeRobotBlock = null;
    // lock for sequencing statusReview and handleJPEG()
    private ReentrantLock transferLock;
    // queue for scan callbacks
    //private ArrayList<ScanCallbackPackage> scanCallbackPackages;
    // boolean for stating if we're connected to a robot or not
    private boolean isConnected = false;

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

                //List of rejected bluetooth devices
                rejectedDeviceList = new ArrayList<BluetoothDevice>();

                // non-connected btDevices(that are robots) with their average rssis
                robotsAsBTDevices = new HashMap<BluetoothDevice, Integer>();

                // initializing packetFound map for multi packet reads.
                packetsFound = new HashMap<>();

                // creating a lock for our transfer process
                transferLock = new ReentrantLock();

                // creating a lock for scanning callback process
                //instantiating block for notify completion wait
                makeRobotBlock = new ArrayBlockingQueue<>(1);

                // getting copy of model
                model = getModel();

                // create job queue for scan callbacks
                //scanCallbackPackages = new ArrayList<ScanCallbackPackage>();

                // implementing callback for startLeScan()
                leCallback = new BluetoothAdapter.LeScanCallback() {

                    @Override
                    public synchronized void onLeScan(final BluetoothDevice device, final int rssi,
                                                      final byte[] scanRecord) {

                        //Log.i("leCallback", "Start");

                        //enqueue job for handleScanningCallback to work with
                        //scanCallbackPackages.add(new ScanCallbackPackage(device, rssi, scanRecord));

                        // get on service thread
                        serviceHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                handleScanningCallback(device, rssi, scanRecord);
                            }
                        });
                    }
                };
            }
        }); // end handler post
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        // go on service thread
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                onStartCommandSeparateThread(intent, flags, startId);
            }
        });

        return Service.START_STICKY;
    }

    private void onStartCommandSeparateThread(Intent intent, int flags, int startId) {
        final String TAG = "onStartSeparateThread";

        // enabling bluetooth is not a blocking call so we need to make sure bt is on
        while (!btAdapter.isEnabled()) {
            try {
                Log.v(TAG, "Putting service thread to sleep");
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Log.v(TAG, "Service thread failed to sleep.");
            }
        }
        //start scanning
        btAdapter.startLeScan(leCallback);
        isScanning = true;
    }

    @Override
    public void onDestroy() {


        //stop scanning
        if (isScanning) {
            serviceHandler.post(new Runnable() {
                @Override
                public void run() {
                    btAdapter.stopLeScan(leCallback);
                }
            });
        }

        //turn off user's bluetooth if it was off before app launch
        if (btInitOff) {
            btAdapter.disable();
        }

        // cleaning up
        btAdapter = null;
        uuidOfInterest = null;
        leCallback = null;
        btGattCallback = null;
        supportedServices = null;
        supportedCharas = null;
        serviceHandler = null;
        serviceLooper = null;
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
     * instances help enqueueing scan callback jobs
     */
    private class ScanCallbackPackage {
        private BluetoothDevice device;
        private int rssi;
        private byte[] scanRecord;

        public ScanCallbackPackage(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public int getRssi() {
            return rssi;
        }

        public byte[] getScanRecord() {
            return scanRecord;
        }
    }

    /**
     * Worker method for handling both leCallback and scanCallback
     *
     * @param device     is the bt device that triggered the callback
     * @param rssi       is the signal strength value of the advertisement
     * @param scanRecord is the payload
     */
    private void handleScanningCallback(final BluetoothDevice device, final int rssi,
                                        final byte[] scanRecord) {
        final String TAG = "handleScanningCallback()";

        // safety-net : if uuid is null then app should not proceed.
        if (uuidOfInterest == null) {
            Log.i(TAG, "Called when uuidOfInterest is null");
            try {
                (Looper.myLooper()).getThread().sleep(200);
                Thread.currentThread().interrupt();
            } catch (InterruptedException ex) {
                Log.i(TAG, "Failed to sleep or interrupt");
            }
            return;
        }

        // If device has been added to rejected list then go no further.
        if (rejectedDeviceList.contains(device)) {
            //Log.i(TAG, "Rejected " + device.getAddress());
            return;
        }

        // so this robot won't be added to robotsAsBTDevices without connecting
        if (isConnected) {
            return;
        }

        // handling dismissed robots
        // if there's a dismissed robot that has the same bt device in callback then restart the
        // dismissedRobot's timer
        //for (DismissedRobot dismissedRobot : dismissedList) {
            //if (dismissedRobot.getBluetoothDevice().equals(device)) {
                //Log.i(TAG, "found dismissed robot");
                //dismissedRobot.restartTimer();
                //return;
            //}
        //}

        if (!robotsAsBTDevices.containsKey(device)) {

            List<String> listOfStructures = parseScanRecord(scanRecord);
            //for (String s : listOfStructures) {
            //Log.i("leCallBack", s);
            //}

            if (uuidOfInterest.equals(getAdUuidOfPeripheral(listOfStructures))) {
                //DeviceUtilities item = new DeviceUtilities(device, rssi);
                robotsAsBTDevices.put(device, rssi);
                //initial connection
                connect(device);
            } else {
                // Add to rejected list
                rejectedDeviceList.add(device);
            }

        } else { //this is one of our robots

            Log.i(TAG, "Heard one of our robots");

            // update rssi
            robotsAsBTDevices.put(device, rssi);

            /*
            // update rssi values
            for (BluetoothDevice dev : robotsAsBTDevices.keySet()) {

                //DeviceUtilities dev = (arAdapter.getItem(i));

                //if (((dev.getItem()).getAddress()).equals(device.getAddress())) {
                if (dev.getAddress().equals(device.getAddress())) {

                    //dev.addToRssiList(rssi);

                    robotsAsBTDevices.get(dev).add(rssi);

                    if (robotsAsBTDevices.get(dev).size() > 9) {
                        /*
                        int n = 0;
                        for (Integer x :robotsAsBTDevices.get(dev)) {
                            n += x;
                        }
                        n = n / 10;
                        dev.setRssi(n);
                        //arAdapter.notifyDataSetChanged();
                        dev.clearRssiList();

                        smartConnect();
                    }
                    break;
                }
            }*/
        }
    }

    /**
     * @param scanRecord is  payload
     * @return arraylist of all data structures in payload
     */
    private List<String> parseScanRecord(byte[] scanRecord) {
        // Contains info in payload
        List<String> GAPStructures = new ArrayList<>();

        // Keeping track of which byte we're on
        int index = 0;

        // Loop through whole payload
        while (index < scanRecord.length) {

            int lengthOfGAPStructure = scanRecord[index];

            // checking to see if payload is not full
            if (lengthOfGAPStructure == 0) {
                // if here then payload no longer contains any ad structures.
                break;
            }
            index++;

            //checking to see if GAP type is 0
            if (scanRecord[index] == 0) {
                // if here then no specified GAP type was used
                break;
            }


            //String structure = (Arrays.copyOfRange(scanRecord, index, ((index+lengthOfGAPStructure)-1))).toString();
            //loop through each byte and get it's binary representation in string form******
            byte[] adData = Arrays.copyOfRange(scanRecord, index, ((index + lengthOfGAPStructure)));
            String structure = "";
            for (int i = 0; i < adData.length; i++) {
                structure += ((Integer.toBinaryString((adData[i] & 0b11111111) + 256)).substring(1) + " ");
            }

            GAPStructures.add(structure);
            index += (lengthOfGAPStructure);

        }
        return GAPStructures;
    }


    /**
     * @param listOfStructures is an arraylist with each element being a complete payload structure
     * @return the uuid found at 0x06 gap data structure. if structure not found then a null uuid is returned
     */
    private UUID getAdUuidOfPeripheral(List<String> listOfStructures) {

        UUID uuidTemp = null;
        boolean incListOfUuidFound = false;
        String strOfUuidElements = "";

        for (String s : listOfStructures) {

            //Log.i("getAdUuidOfPeripheral()", "Checking = " + s.substring(0, 8));

            if ((s.substring(0, 8)).equals("00000110")) {

                incListOfUuidFound = true;
                strOfUuidElements = s.substring(9);
                //Log.i("getAdUuidOfPeripheral()", "Found incomplete list of 128 bit UUIDs");
                //Log.i("getAdUuidOfPeripheral()", "Bytes of UUID = " + strOfUuidElements);
                //Log.i("getAdUuidOfPeripheral()", "Length = " + byteStringOfUuid);
                break;

            }
        }

        if (!incListOfUuidFound) {
            return null;
        }

        strOfUuidElements = changeByteOrder(strOfUuidElements);
        //Log.i("getAdUuidOfPeripheral()", "Reordered bytes of UUID = " + strOfUuidElements);

        strOfUuidElements = binaryStrToUuidStr(strOfUuidElements);
        //Log.i("getAdUuidOfPeripheral()", "UUID = " + strOfUuidElements);

        uuidTemp = UUID.fromString(strOfUuidElements);
        return uuidTemp;
    }

    /**
     * @param byteString is a uuid Byte String in little endian
     * @return uuid byte string in big endian
     */
    private String changeByteOrder(String byteString) {

        String result = "";


        int end = byteString.length() - 5;
        int start = end - 4;

        //if even then account for a space
        int whiteSpaceTracker = 1;

        for (int i = 0; i < 32; i++) {

            result = (result + byteString.substring(start, end) + " ");

            if ((whiteSpaceTracker % 2) == 1) {
                start = end;
                end = end + 4;
            } else {

                start = start - 13;
                end = start + 4;
            }
            whiteSpaceTracker++;
        }

        return result;


    }

    /**
     * @param binaryString big endian byte string of uuid
     * @return hex string of uuid
     */
    private String binaryStrToUuidStr(String binaryString) {

        String result = "";

        int start = 0;
        int end = 4;

        for (int i = 0; i < 32; i++) {

            result += Integer.toHexString(Integer.parseInt(binaryString.substring(start, end), 2));
            if (i == 7 || i == 11 | i == 15 || i == 19) {
                result += "-";
            }
            start = end + 1;
            end = end + 5;
        }

        return result;

    }



    /**
     * stops scanning and connects to passed device
     */
    private void connect(BluetoothDevice device) {

        //stop scanning
        if (isScanning) {
            btAdapter.stopLeScan(leCallback);
        }

        // if we're already connected get out
        if (isConnected) {
            return;
        }

        Log.i("Service.connect()", "connecting to " + device.getAddress());

        try {
            Method connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
            connectGattMethod.invoke(device, ControllerService.this,
                    false, btGattCallback, BluetoothDevice.TRANSPORT_LE);
        } catch (Exception ex) {
            Log.i("Service.connect()", "failed connection");
        }

        isConnected = true;

        /*
        Log.i("smartConnect()", "Just got in");
        int bestRssi = (arAdapter.getItem(0)).getRssi();
        int index = 0;
        //int[] arOfRssi = new int [arAdapter.getCount()];
        for (int i = 0; i < arAdapter.getCount(); i++) {

            //maxRssi = (arAdapter.getItem(i)).getRssi();
            if (bestRssi < (arAdapter.getItem(i)).getRssi()) {
                bestRssi = (arAdapter.getItem(i)).getRssi();
                index = i;
            }
        }
        //if (bestRssi >= -65) {

        if (robot.getGattServer() == null) {

            // checking for android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //device is running marshmallow or higher
                serviceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            leScanner.stopScan(scanCallback);
                        }
                    }
                });
            } else {
                //device is running either jellybean, kitkat, or lollipop
                serviceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        btAdapter.stopLeScan(leCallback);
                    }
                });
            }
            isScanning = false;

            // using reflection to get internal connectGatt
            BluetoothDevice device = ((arAdapter.getItem(index)).getItem());
            try {
                Method connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                robot.setGattServer((BluetoothGatt) connectGattMethod.invoke(device, LinkService.this,
                        false, btGattCallback, BluetoothDevice.TRANSPORT_LE));
            } catch (Exception ex) {

            }


            Log.i("smartConnect()", "After connectGatt");

        }
        //} else {
        // notify to move closer
        //broadcastStatus(1);
        //}

        */
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

