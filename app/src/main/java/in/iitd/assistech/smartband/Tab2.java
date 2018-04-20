package in.iitd.assistech.smartband;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.ParcelFileDescriptor.MODE_APPEND;
import static com.facebook.FacebookSdk.getApplicationContext;

public class Tab2 extends Fragment implements View.OnClickListener{

    private static final String TAG = "TAB2";

    private View view;
    private Button startFPButton;
    private Button stopFPButton;
    private Button soundRecordButton;
    private Button stopSoundRecord;
    private ImageButton startButton;
    private ImageButton stopButton;
    private ListView historyListView;

    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    private final String address = "00:06:66:66:33:89";

    // The thread that does all the work
    BluetoothThread btt;

    // Handler for writing messages to the Bluetooth connection
    Handler writeHandler;

    private static Tab2 instance;

    private OnTabEvent mListener;

    public Tab2(){
    }

    public static Tab2 getInstance(){
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.tab2, container, false);

        startFPButton = (Button)view.findViewById(R.id.startFPButton);
        stopFPButton = (Button)view.findViewById(R.id.stopFPButton);
        soundRecordButton = (Button)view.findViewById(R.id.startsoundRecord);
        stopSoundRecord = (Button)view.findViewById(R.id.stopSoundRecord);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getRealSize(screenSize);
        int size = Math.min(screenSize.x, screenSize.y);
        int buttonSize = Math.round(size * 0.75f);

        startButton = (ImageButton) view.findViewById(R.id.start_button);
        stopButton = (ImageButton) view.findViewById(R.id.stop_button);
//        historyListView = (ListView)view.findViewById(R.id.historyListView);

        startButton.setMaxWidth(buttonSize);
        startButton.setMaxHeight(buttonSize);
        stopButton.setMaxWidth(buttonSize);
        stopButton.setMaxHeight(buttonSize);

        startFPButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopFPButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        soundRecordButton.setOnClickListener(this);
        stopSoundRecord.setOnClickListener(this);

        return view;
    }

    public void clickStopButton(){
//        Thread t = new Thread(){
//            public void run(){
//                stopButton.performClick();
//            }
//        };
//        t.start();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startFPButton:
                //TODO
                mListener.onButtonClick("StartTargetFingerPrint");
                stopFPButton.setVisibility(View.VISIBLE);
                startFPButton.setVisibility(View.GONE);
                break;
            case R.id.stopFPButton:
                //TODO
                mListener.onButtonClick("StopTargetFingerPrint");
                stopFPButton.setVisibility(View.GONE);
                startFPButton.setVisibility(View.VISIBLE);
                break;

            case R.id.start_button:
                //TODO
                mListener.onButtonClick("MicReadButton");
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                break;
            case R.id.stop_button:
                //TODO
                mListener.onButtonClick("StopRecordButton");
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                break;
            case R.id.startsoundRecord:
                Log.e(TAG, "Record}");
                mListener.onButtonClick("SoundRecord");
                break;
            case R.id.stopSoundRecord:
                mListener.onButtonClick("StopRecord");
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTabEvent) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTabEvent interface");
        }
    }

    public void simulateSoundRecordClick(){

        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startButton.performClick();
            }
        });
    }
}