package in.iitd.assistech.smartband;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by nikhil on 20/2/18.
 */

public class BluetoothService extends Service {


    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread connectThread;
    private BluetoothDevice bluetoothDevice;
    private NotificationManagerCompat notificationManager;

    private static final String DEBUG_TAG = "debug";
    private static final String DeviceName = "HC-05";
    private static final int NOTIFICATION_ID = 1;
    private long prevTimeSoundLabelSent;
    private final long timeDiffToBeMaintained = 100;

    private static BluetoothService instance = null;

    // Bluetooth variables
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;

    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    /*
    * Check if the instance of this bluetooth service is already created
    * */
    public static boolean isInstanceCreated() {
        return instance != null;
    }

    /*
    * Check if the instance of this bluetooth service is already created
    * */
    public static BluetoothService getInstance() {
        return instance;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the device supports bluetooth
        if (mBluetoothAdapter == null) {
            Log.d(DEBUG_TAG, "Device doesn't support Bluetooth");
        }else{
            bluetoothDevice = searchDevice();

            // Check if device is null
            if(bluetoothDevice == null){
                Log.d(DEBUG_TAG, "Bluetooth is off");
            }else{
                // Default: start connecting with device when the service is created
                startCommunicationWithDevice();
            }
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        prevTimeSoundLabelSent = 0;
        instance = this;
        getBluetoothAdapter();
    }

    private void getBluetoothAdapter(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private BluetoothDevice searchDevice(){

        // Check if bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            // bluetooth is disabled
            return null;
        }

        // Search for the bluetooth Device
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName(); // Name
                if(deviceName.equals(DeviceName)){
                    // connect to this device
                    Log.d(DEBUG_TAG, DeviceName+" detected");
                    return device;
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopCommunicationWithDevice();
    }

    private void detectSound(){

        // Send sound label only if the prev time the sound label sent is atleast the value "timeDiffToBeMaintained"
        long time = System.currentTimeMillis();
        if(!MainActivity.isRunning()) {
            prevTimeSoundLabelSent = time;
            // TODO
            Intent dialogIntent = new Intent(this, MainActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);

            // Debug
            sendDataToDevice(0);
        }
    }

    public boolean sendDataToDevice(int soundLabel){
        return write(soundLabel);
    }

    // Call this from the main activity to send data to the remote device.
    public boolean write(int soundLabel) {
        try {
            mmOutStream.write(soundLabel);
            Log.d(DEBUG_TAG, "Data written to the bluetooth device");
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error occurred when sending data", e);
            return false;
        }
        return true;
    }

    public boolean startCommunicationWithDevice(){
        if(connectThread!=null && connectThread.getState()== Thread.State.RUNNABLE){
            Log.d(DEBUG_TAG, "Attempting to start bluetooth service thread already running");
        }

        // create the connection thread
        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();

        return true;
    }

    public boolean stopCommunicationWithDevice(){
        connectThread.interrupt();
        return true;
    }

    /*
    * This class is private to Bluetooth Service
    * This thread deals with the bluetooth variables connecting to the device
    * */
    private class ConnectThread extends Thread {

        private int DelayDetectionOfDevice = 100;
        private UUID MY_UUID;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            MY_UUID = mmDevice.getUuids()[0].getUuid();
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            while(!Thread.interrupted()) {

                mmSocket = null;

                // loop till you connect to the required device
                while(!Thread.interrupted()) {

                    try {
                        // Get a BluetoothSocket to connect with the given BluetoothDevice.
                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        Log.d(DEBUG_TAG, "Socket created");

                        // Connect to the remote device through the socket. This call blocks
                        // until it succeeds or throws an exception.
                        try {
                            mmSocket.connect();
                            break;
                        } catch (IOException connectException) {
                            // Unable to connect; close the socket and return.
                            Log.d(DEBUG_TAG, "could not connect to the socket");
                            try {
                                mmSocket.close();
                            } catch (IOException closeException) {
                                Log.e(DEBUG_TAG, "Could not close the client socket", closeException);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Socket's create() method failed", e);
                    }

                    //  sleep for sometime if you could not connect to the bluetooth device
                    try {
                        sleep(DelayDetectionOfDevice);
                    }catch (Exception ex){
                        Log.d(DEBUG_TAG, "could not sleep");
                    }
                }

                // The connection attempt succeeded.
                Log.d(DEBUG_TAG, "Connected to socket");

                // Get the input and output streams; using temp objects because
                // member streams are final.
                mmInStream = null;
                mmOutStream = null;

                try {
                    mmInStream = mmSocket.getInputStream();
                    mmOutStream = mmSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Error occurred when creating input stream", e);
                }

                // read buffer
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                Log.d(DEBUG_TAG, "Started Parsing data");

                // Keep listening to the InputStream until an exception occurs.
                while (!Thread.interrupted()) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);
                        String inbuf = new String(mmBuffer).substring(0, numBytes).toLowerCase();
                        Log.d(DEBUG_TAG, inbuf);

                        // TODO: refine the detection process
                        CharSequence checkSeq = "l";
                        if (inbuf.contains(checkSeq)) {
                            Log.d(DEBUG_TAG, "Loud sound detected");

                            // get the sound label from the app
                            detectSound();
                        }

                    } catch (IOException e) {
                        Log.d(DEBUG_TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }
        }


        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                if(mmSocket!=null) {
                    mmSocket.close();
                }
                if(mmInStream!=null){
                    mmInStream.close();
                }
                if(mmOutStream!=null){
                    mmOutStream.close();
                }
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Could not close the client socket", e);
            }
        }
    }

    private void addNotification(String string) {
        // create the notification
        Notification.Builder m_notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(string)
                .setSmallIcon(R.drawable.notif_icon);

        // create the pending intent and add to the notification
        Intent intent = new Intent(this, BluetoothService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        m_notificationBuilder.setContentIntent(pendingIntent);

        // send the notification
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, m_notificationBuilder.getNotification());
    }
}

