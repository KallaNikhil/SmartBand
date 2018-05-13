package in.iitd.assistech.smartband;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by nikhil on 20/2/18.
 */

public class BluetoothService extends Service {


    private static BluetoothAdapter mBluetoothAdapter;
    private ConnectThread connectThread;
    public static BluetoothDevice bluetoothDevice;

    private static final String TAG = "BluetoothService";
    private static final String DeviceName = "HC-05";
    static final int NOTIFICATION_ID_START = 1;
    static final int NOTIFICATION_ID_RESULT = 2;

    /*
    * Variables to maintain time difference between successive detection of loud sounds
    * */
    private long prevTimeSoundLabelSent;
    private final long timeDiffToBeMaintained = 10000;

    private static BluetoothService instance = null;

    // Bluetooth variables
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    ProgressDialog dialogDetectThreadEnd, dialogDetectThreadStart;

    private NotificationManagerCompat notificationManager;

    /*
    * Check if the instance of this bluetooth service is already created
    * */
    public static boolean isInstanceCreated() {
        return instance != null;
    }

    static{
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /*
    * Check if the instance of this bluetooth service is already created
    * */
    public static BluetoothService getInstance() {
        return instance;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(MainActivity.isRunning()) {
            dialogDetectThreadStart = ProgressDialog.show(MainActivity.getInstance(), "",
                    "Starting Bluetooth Service Please wait...", true);
        }

        bluetoothDevice = searchDevice();

        // TODO: if could not find bluetooth device
        if(bluetoothDevice == null){
            showNotificationMsg("Could not find Bluetooth device, Please Pair the device and the restart the Bluetooth service");
            return Service.START_STICKY;
        }

        notificationManager = NotificationManagerCompat.from(this);

        // create the connection thread
        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();

        return Service.START_STICKY;
    }

    public static BluetoothDevice searchDevice(){

        // Check if bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            // bluetooth is disabled
            return null;
        }

        // Search for the bluetooth Device
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothAdapter.cancelDiscovery();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName(); // Name
                if(deviceName.equals(DeviceName)){
                    // connect to this device
                    Log.d(TAG, DeviceName+" detected");

                    return device;
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        dismissDialog(dialogDetectThreadStart);
        instance = null;
    }

    private void dismissDialog(ProgressDialog dialog){
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean stopBluetoothService(){
        dialogDetectThreadEnd = ProgressDialog.show(MainActivity.getInstance(), "",
                "Closing Bluetooth Service Please wait...", true);
        if(SoundProcessing.isServiceRecordingSound){
            SoundProcessing.stopRecording();
        }
        connectThread.cancel();
        return true;
    }


    /*
    * Call this function after a loud sound has been detected, and we want to know the label of the sound
    * */
    private void detectSound() {

        // Send sound label only if the prev time the sound label sent is atleast the value "timeDiffToBeMaintained"
        long time = System.currentTimeMillis();
        if(time - prevTimeSoundLabelSent > timeDiffToBeMaintained) {
            prevTimeSoundLabelSent = time;
            showSoundDetectionStartNotification();
            SoundProcessing.startRecording(1, true);
        }
    }

    /*
    * This functions send the sound label (input parameter) to the smart device
    * */
    public boolean sendSoundLabelToDevice(int soundLabel){
        try {
            mmOutStream.write(soundLabel);
            Log.d(TAG, "Data written to the bluetooth device - " + soundLabel);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred when sending data", e);
            return false;
        }
        return true;
    }

    public void showSoundResultNotification(String resultSoundCategory){

        // Create pending intent, mention the Activity which needs to be
        //triggered when user clicks on notification

        Intent contentIntent = new Intent(this, MyBroadcastReceiver.class);
        contentIntent.setAction(MyBroadcastReceiver.CONTENT_ACTION);
        contentIntent.putExtra("SoundResultCategory", resultSoundCategory);
        PendingIntent pendingContentIntent = PendingIntent.getBroadcast(this, 0, contentIntent, 0);


        // incorrect detection button presses
        Intent incorrectButtonIntent = new Intent(this, MyBroadcastReceiver.class);
        incorrectButtonIntent.setAction(MyBroadcastReceiver.INCORRECT_ACTION);
        PendingIntent pendingIncorrectIntent = PendingIntent.getBroadcast(this, 0, incorrectButtonIntent, 0);

        // correct detection button presses
        Intent correctButtonIntent = new Intent(this, MyBroadcastReceiver.class);
        correctButtonIntent.setAction(MyBroadcastReceiver.CORRECT_ACTION);
        correctButtonIntent.putExtra("SoundResultCategory", resultSoundCategory);
        PendingIntent correctIntent = PendingIntent.getBroadcast(this, 0, correctButtonIntent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Smart Band")
                .setContentText("Sound Detected - " + resultSoundCategory)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingContentIntent)
                .addAction(R.drawable.cross_icon, "Wrong Detection", pendingIncorrectIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_light, "Correct Detection", correctIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[] { 1000, 1000});

        Notification notif = mBuilder.build();

        notif.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID_RESULT, notif);
    }

    public void removeNotification(int id){
        if(notificationManager != null) {
            notificationManager.cancel(id);
        }
    }

    public void showSoundDetectionStartNotification(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Smart Band")
                .setContentText("Bluetooth Service - Started Detecting Sound")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID_START, mBuilder.build());
    }

    public void showNotificationMsg(String msg){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notif_icon)
                .setContentTitle("Smart Band")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID_START, mBuilder.build());
    }


    /*
    * This class is private to Bluetooth Service
    * This thread deals with the bluetooth variables connecting to the device
    * */
    private class ConnectThread extends Thread {

        private int DelayDetectionOfDevice = 100;
        private UUID MY_UUID;
        private boolean isInterrputed;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            MY_UUID = mmDevice.getUuids()[0].getUuid();
            isInterrputed = false;
        }

        public void run() {
            dismissDialog(dialogDetectThreadStart);

            // Debug
            Thread t = new Thread(){
                public void run(){
                    try {
                        sleep(1000);
                    }catch (Exception ex){
                        Log.d(TAG, "could not sleep");
                    }
                    showSoundResultNotification("kkkk");
                }
            };
            t.start();

            while(!isInterrputed) {

                mmSocket = null;

                // loop till you connect to the required device
                while(!isInterrputed) {

                    try {
                        // Get a BluetoothSocket to connect with the given BluetoothDevice.
                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        Log.d(TAG, "Socket created");

                        // Connect to the remote device through the socket. This call blocks
                        // until it succeeds or throws an exception.
                        try {
                            mmSocket.connect();
                            break;
                        } catch (IOException connectException) {
                            // Unable to connect; close the socket and return.
                            Log.d(TAG, "could not connect to the socket");
                            try {
                                mmSocket.close();
                            } catch (IOException closeException) {
                                Log.e(TAG, "Could not close the client socket", closeException);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Socket's create() method failed", e);
                    }

                    //  sleep for sometime if you could not connect to the bluetooth device
                    try {
                        sleep(DelayDetectionOfDevice);
                    }catch (Exception ex){
                        Log.d(TAG, "could not sleep");
                    }
                }

                if(isInterrputed) {
                    break;
                }

                // The connection attempt succeeded.
                Log.d(TAG, "Connected to socket");

                // Get the input and output streams; using temp objects because
                // member streams are final.
                mmInStream = null;
                mmOutStream = null;

                try {
                    mmInStream = mmSocket.getInputStream();
                    mmOutStream = mmSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }

                // read buffer
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                Log.d(TAG, "Started Parsing data");

                // Keep listening to the InputStream until an exception occurs.
                while (!isInterrputed) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);
                        String inbuf = new String(mmBuffer).substring(0, numBytes).toLowerCase();
                        Log.d(TAG, inbuf);

                        // TODO: refine the detection process
                        CharSequence checkSeq = "l";
                        if (inbuf.contains(checkSeq)) {
                            Log.d(TAG, "Loud sound detected");

                            // get the sound label from the app
                            detectSound();
                        }

                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        closeAllSockets();
                        break;
                    }
                }
            }
            Log.d(TAG, "Connect Thread Run Completed");

            dismissDialog(dialogDetectThreadEnd);
            stopSelf();
        }

        public void cancel(){
            isInterrputed = true;
            closeAllSockets();
        }

        private void closeAllSockets(){
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
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


}

