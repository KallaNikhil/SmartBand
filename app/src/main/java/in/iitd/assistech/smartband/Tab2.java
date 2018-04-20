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
    private Button addSoundButton;
    private ListView historyListView;

    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    private final String address = "00:06:66:66:33:89";

    // The thread that does all the work
    BluetoothThread btt;

    // Handler for writing messages to the Bluetooth connection
    Handler writeHandler;

    private boolean recording;
    private String fileName;

    static ArrayList<String> recordedSoundListItems;
    static ArrayList<Boolean> recordedSoundSwitchState;

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
        addSoundButton = (Button)view.findViewById(R.id.addSoundButton);

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
        addSoundButton.setOnClickListener(this);

        return view;
    }

    public void clickStopButton(){
        stopButton.performClick();
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
            case R.id.addSoundButton:
                addSound();
                break;
        }
    }


    private void addSound() {
        if (recording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        recording = true;
        Button recordButton = (Button) view.findViewById(R.id.addSoundButton);
        recordButton.setText("Stop Recording");
        mListener.onButtonClick("StartSavingSound");
    }

    private void stopRecording() {
        recording = false;
        Button recordButton = (Button) view.findViewById(R.id.addSoundButton);
        recordButton.setText("Record Sound");


        final EditText input = new EditText(getActivity());
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(input)
                .setTitle("Enter a name for the sound")
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        fileName = input.getText().toString().trim();
                        if (fileName.length() == 0)
                            Toast.makeText(getActivity(), "Field empty", Toast.LENGTH_SHORT).show();
                        else if (recordedSoundListItems.contains(fileName))
                            Toast.makeText(getActivity(), "Name taken", Toast.LENGTH_SHORT).show();
                        else {

                            mListener.onButtonClick("StopSavingSound"+" "+fileName);
                            Toast.makeText(getActivity(), fileName+" added", Toast.LENGTH_SHORT);
                            recordedSoundListItems.add(fileName);
                            recordedSoundSwitchState.add(true);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });
        dialog.show();

        final AlertDialog dialog1 = new AlertDialog.Builder(getActivity())
                .setTitle("Do you want to upload \"doorbell.wav\" to the server?")
                .setPositiveButton(android.R.string.yes, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.no, null) //Set to null. We override the onclick
                .create();

        dialog1.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button_p = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_POSITIVE);
                button_p.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        //TODO: Upload file to the server
                        dialog1.dismiss();
                    }
                });

                Button button_n = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_NEGATIVE);
                button_n.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        dialog1.dismiss();
                    }
                });
            }
        });
        dialog.show();

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