package in.iitd.assistech.smartband;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static in.iitd.assistech.smartband.Tab3.notificationListItems;
import static in.iitd.assistech.smartband.Tab3.servicesListItems;
import static in.iitd.assistech.smartband.Tab3.soundListItems;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener,
        OnTabEvent{

    private static final String TAG = "MainActivity";
    public static String NAME;
    public static String EMAIL;
    public static String PHOTO;
    public static String PHOTOURI;
    public static String SIGNED;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    static Pager adapter;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static int REQUEST_MICROPHONE = 101;
    static String[] warnMsgs = {"Car Horn Detected", "Dog Bark Detected", "Ambient"};
    static int[] warnImgs = new int[] {R.drawable.car_horn, R.drawable.dog_bark, 0};

    public AlertDialog myDialog;
    public AlertDialog incorrectDetDialog;

    private static boolean[] startNotifListState;
    private static boolean[] startSoundListState;
    private static boolean[] startServiceListState;

    /*
    * Check if this activity is running
    * */
    private static boolean isActive = false;
    private static MainActivity instance;

    /*
    * Bluetooth Variables
    * */
    private BluetoothAdapter mBluetoothAdapter;
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_BT_PressSwitch = 2;

    /*
    public AlertDialog.Builder warnDB;
    public AlertDialog warnDialog;
    */


    private static final int RC_OVERLAY = 1;

    public static boolean isRunning(){
        return isActive;
    }

    public static MainActivity getInstance(){
        return instance;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    // TODO
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT_PressSwitch) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Tab3.getInstance().switchBluetoothServiceOn();
            }
        }
    }

    /*
        * Switch the bluetooth on
        * */
    public void switchBluetoothOn(boolean pressStartServiceSwitch){

        // Check if the device supports bluetooth
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            throw new RuntimeException("Device doesn't support Bluetooth");
        }

        // Check if bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, pressStartServiceSwitch ? REQUEST_ENABLE_BT_PressSwitch : REQUEST_ENABLE_BT);
        }
    }

    public boolean isBluetoothOn(){
        return mBluetoothAdapter.isEnabled();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        NAME = getIntent().getStringExtra("NAME");
        EMAIL = getIntent().getStringExtra("EMAIL");
        PHOTO = getIntent().getStringExtra("PHOTO");
        PHOTOURI = getIntent().getStringExtra("PHOTOURI");
        SIGNED = getIntent().getStringExtra("SIGNED");

        //TODO: Hardware Acceleration
//        getWindow().setFlags(
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);


        //Adding toolbar to the activity
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.pager);
        tabLayout.setupWithViewPager(viewPager);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        //Creating our pager adapter
        adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());
        //Adding adapter to pager
        viewPager.setAdapter(adapter);

        tabLayout.getTabAt(0).setText("Chat");
        tabLayout.getTabAt(1).setText("Sound");
        tabLayout.getTabAt(2).setText("Settings");

        //Adding onTabSelectedListener to swipe views
        tabLayout.addOnTabSelectedListener(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);

        }

        viewPager.setCurrentItem(1);

        int writeExtPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (writeExtPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        //TODO: @Link https://github.com/OmarAflak/Bluetooth-Library

    }

    @Override
    protected void onStart() {
        super.onStart();
        instance = this;

        // check if the Bluetooth service is switched on
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(servicesListItems[0], false)) {
            switchBluetoothOn(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // update soundListItems
        soundListItems = Tab3.getSoundListItems();

        boolean[] notifState = new boolean[notificationListItems.length];
        for (int i=0; i<notificationListItems.length; i++){
            notifState[i] = app_preferences.getBoolean(notificationListItems[i], true);
        }
        boolean[] soundState =  new boolean[soundListItems.size()];
        for (int i=0; i<soundListItems.size(); i++){
            soundState[i] = app_preferences.getBoolean(soundListItems.get(i), true);
        }

        boolean[] serviceState =  new boolean[servicesListItems.length];
        for (int i=0; i<servicesListItems.length; i++){
            serviceState[i] = app_preferences.getBoolean(servicesListItems[i], false);
        }

        startNotifListState = notifState;
        startSoundListState = soundState;
        startServiceListState = serviceState;

        isActive = true;

        Boolean incorrectDetection = getIntent().getExtras().getBoolean("incorrectDetection", false);
        if(incorrectDetection){
            showIncorrectDetectionDialog(getApplicationContext(), true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = app_preferences.edit();
        if(adapter.getInitialNotifListState() != null){
            boolean[] notifState = adapter.getInitialNotifListState();
            for (int i=0; i<notificationListItems.length; i++){
                editor.putBoolean(notificationListItems[i], notifState[i]);
            }
            editor.commit();
        }

        if(adapter.getInitialSoundListState() != null){
            boolean[] soundState = adapter.getInitialSoundListState();
            for (int i=0; i<soundListItems.size(); i++){
                editor.putBoolean(soundListItems.get(i), soundState[i]);
            }
            editor.commit();
        }

        if(adapter.getInitialServiceListState() != null){
            boolean[] serviceState = adapter.getInitialServiceListState();
            for (int i=0; i<servicesListItems.length; i++){
                editor.putBoolean(servicesListItems[i], serviceState[i]);
            }
            editor.commit();
        }

        isActive = false;

        // stop sound recording in case the application gets paused
        if(SoundProcessing.isActivityRecordingSound) {
            if (Tab2.getInstance() != null) {
                Tab2.getInstance().clickStopButton();
            }else{
                SoundProcessing.stopRecording();
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        instance = null;
    }

    @Override
    public void onButtonClick(String text) {

        String fname = "", filepath;
        File file;
        if (text.startsWith("StopSavingSound")) {
            String[] splitted = text.split("\\s+");
            filepath = Environment.getExternalStorageDirectory().getPath();
            file = new File(filepath,SoundProcessing.AUDIO_RECORDER_FOLDER);
            if (!file.exists()) {
                file.mkdirs();
            }
            fname = file.getAbsolutePath() + "/" + splitted[1] + SoundProcessing.AUDIO_RECORDER_FILE_EXT_WAV;
            text = "StopSavingSound";
        }

        Thread t;
        switch(text){
            case "MicReadButton":
                t = new Thread(){
                    public void run(){
                        SoundProcessing.startRecording(1, false);
                    }
                };
                t.start();
                break;
            case "StopRecordButton":
                t = new Thread(){
                    public void run(){
                        SoundProcessing.stopRecording();
                    }
                };
                t.start();
                break;
            case "SoundRecordButton":
//                Log.e(TAG, "Record}");
//                startRecording(2);
                break;
            case "StopRecord":
//                stopRecording();
//                copyWaveFile(getTempFilename(),getFilename());
//                deleteTempFile();
                break;
            case "StartSavingSound":
                //TODO
                SoundProcessing.startRecording(2, false);
                break;
            case "StopSavingSound":
                //TODO
                SoundProcessing.stopRecording();
                SoundProcessing.copyWaveFile(SoundProcessing.getTempFilename(),fname);
                SoundProcessing.deleteTempFile();
                break;
            case "StartTargetFingerPrint":
                //TODO
                SoundProcessing.startRecording(2, false);
                break;
            case "StopTargetFingerPrint":
                //TODO
                t = new Thread(){
                    public void run(){
                        SoundProcessing.stopRecording();
                        SoundProcessing.copyWaveFile(SoundProcessing.getTempFilename(),SoundProcessing.getFilename());
                        SoundProcessing.deleteTempFile();
                    }
                };
                t.start();
                break;
        }

    }

    public void showIncorrectDetectionDialog(Context context, final Boolean stopMainActivity){
        if (incorrectDetDialog != null && incorrectDetDialog.isShowing()) return;

        ArrayList<String> items = Tab3.getSoundListItems();

        CharSequence[] cItems = new CharSequence[items.size()];
        for(int i=0; i<items.size(); i++)
            cItems[i] = items.get(i);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Give Sound Category");
        ImageView wrnImg = new ImageView(MainActivity.this);
        builder.setView(wrnImg);

        builder.setItems(cItems, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //TODO: Do something with the selection

                dialog.dismiss();
            }
        });

        builder.setCancelable(true);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(stopMainActivity)
                    MainActivity.getInstance().finish();
            }
        });

        incorrectDetDialog = builder.create();
        incorrectDetDialog.show();
        final Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            public void run() {
                incorrectDetDialog.dismiss();
                Log.e(TAG, "TIMER Running");
                timer2.cancel(); //this will cancel the timer of the system
            }
        }, 15000); // the timer will count 15 seconds....
    }

    public void showDialog(final Context context, String resultSoundCategory) {
        if (myDialog != null && myDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(resultSoundCategory);
        ImageView wrnImg = new ImageView(MainActivity.this);
        //TODO No Text clickable button for dog bark case dialog
//        ViewGroup.LayoutParams imgParams = wrnImg.getLayoutParams();
//        imgParams.width = 60;
//        imgParams.height = 60;
//        wrnImg.setLayoutParams(imgParams);
//        wrnImg.setImageResource(warnImgs[idx]);
        builder.setView(wrnImg);
//        builder.setMessage("Message");
//        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int arg1) {
//                dialog.dismiss();
//            }});
        builder.setPositiveButton("Ok, Thank You!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Wrong Detection", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                showIncorrectDetectionDialog(context, true);
            }
        });

        builder.setCancelable(true);
        myDialog = builder.create();
        myDialog.show();
        final Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            public void run() {
                myDialog.dismiss();
                Log.e(TAG, "TIMER Running");
                timer2.cancel(); //this will cancel the timer of the system
            }
        }, 15000); // the timer will count 15 seconds....
    }


    /**-----------For Tab Layout--------------**/
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
        tabLayout.getTabAt(tab.getPosition()).select();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    static boolean[] getStartNotifListState(){
        return startNotifListState;
    }

    static boolean[] getStartSoundListState(){
        return startSoundListState;
    }

    static boolean[] getStartServiceListState(){
        return startServiceListState;
    }

}