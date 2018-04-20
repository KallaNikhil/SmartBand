package in.iitd.assistech.smartband;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

//import com.bumptech.glide.Glide;
import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.facebook.FacebookSdk.getApplicationContext;
import static in.iitd.assistech.smartband.MainActivity.EMAIL;
import static in.iitd.assistech.smartband.MainActivity.NAME;
import static in.iitd.assistech.smartband.MainActivity.PHOTO;
import static in.iitd.assistech.smartband.MainActivity.PHOTOURI;
import static in.iitd.assistech.smartband.MainActivity.SIGNED;

public class Tab3 extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{

    //--------------------------------
    Button buttonStart, buttonStop, buttonPlayLastRecordAudio,
            buttonStopPlayingRecording ;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder ;
    Random random ;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer ;
    //--------------------------------

    public View view;
    private static final String TAG = "Tab3";
    private static Tab3 instance;
    /*
    * In List the order is as follows
    * 0 - Notifications
    * 1 - Sound types
    * 2 - Services
    * */
    private ExpandableListAdapter[] listAdapters;
    private ExpandableListView[] listViews;
    private static final int ListSize = 3;
    static final String[] notificationListItems = {"Vibration", "Sound", "Flashlight", "Flash Screen"};
    static ArrayList<String> soundListItems = new ArrayList<>();
    static final String[] servicesListItems = {"BluetoothService"};

    static Boolean soundAdded = false;

    private CircleImageView userProfileImage;
    private TextView userName;
    private TextView userEmail;

    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;
    private String providerId;
    private String uid;
    private String name;
    private String email;

    private boolean recording;
    private String fileName;

    private OnTabEvent mListener;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, ""+soundListItems.size());

        int state = notificationListItems.length + soundListItems.size() + servicesListItems.length;
        boolean[] switchState = new boolean[state];

        int groupIndex = 0;
        int childIndex = 0;
        for(int i=0; i<state; i++){
            if((groupIndex == 0 && childIndex == notificationListItems.length) ||
                    (groupIndex == 1 && childIndex == soundListItems.size()) ||
                    (groupIndex == 2 && childIndex == servicesListItems.length)){

                groupIndex++;
                childIndex = 0;
            }

            if(soundAdded && groupIndex == 1 && childIndex == soundListItems.size() - 1){
                switchState[i] = true;
            }else{
                switchState[i] = listAdapters[groupIndex].getCheckedState(childIndex);
            }

            childIndex++;
        }

//        Log.e(TAG, "SoundStare 0 and 1 " + soundSwitchState[0] + ", " + soundSwitchState[1]);

        outState.putBooleanArray("notifState", switchState);
        outState.putInt("soundListItemsSize", soundListItems.size());
//        outState.putBooleanArray("soundState", soundSwitchState);
    }

    public static Tab3 getInstance(){
        return instance;
    }

    public static ArrayList<String> getSoundListItems(){

        ArrayList<String> soundListItems = new ArrayList<>(Arrays.asList("Vehicle Horn", "Dog Bark"));

        String filepath = Environment.getExternalStorageDirectory().getPath();
        File directory = new File(filepath,"SmartBand");
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if(files != null) {
                for (int i = 0; i < files.length; i++) {
                    soundListItems.add(files[i].getName());
                }
            }
        }

        return soundListItems;
    }

    public void switchBluetoothServiceOn(){
        // set start services switch to true
        ((Switch) view.findViewById(R.id.notif_row_switch)).setChecked(true);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        view = inflater.inflate(R.layout.tab3, container, false);
//        userProfileImage = (ImageView)view.findViewById(R.id.mUserProfilePic);
        userName = (TextView) view.findViewById(R.id.userName);
        userEmail = (TextView) view.findViewById(R.id.userEmail);

        userProfileImage = (CircleImageView)view.findViewById(R.id.mUserProfilePic);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        Log.e(TAG, "Provider ID : " + providerId);
        for (UserInfo user: FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
            Log.e(TAG, user.getProviderId());
            if (user.getProviderId().equals("facebook.com")) {
                System.out.println("User is signed in with Facebook");
            } else {
                if(mGoogleApiClient == null){
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .build();
                    mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                            .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                            .build();
                }
            }
        }

        // set onClickListener to signOutButton
        view.findViewById(R.id.signOutButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                signOut();
            }
//                switch (view.getId()){
//                    case R.id.signOutButton:
//                        signOut();
//                        break;
//                    case R.id.revokeButton:
//                        revokeAccess();
//                        break;
//                }
            //}

        });

        // set onClickListener to revokeButton
        view.findViewById(R.id.revokeButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                revokeAccess();
            }
//                switch (view.getId()){
//                    case R.id.signOutButton:
//                        signOut();
//                        break;
//                    case R.id.revokeButton:
//                        revokeAccess();
//                        break;
//                }
            //}

        });

        // set onClickListener to aboutUs button
        view.findViewById(R.id.aboutus).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity(),AlertDialog.THEME_HOLO_LIGHT).create(); //Read Update
                alertDialog.setTitle("About Us");
                alertDialog.setMessage("We are group of students at Assitech IIT Delhi working for smart applications for hearing impaired people.\n" +
                        "Kindly visit (http://assistech.iitd.ernet.in) for further details.");

                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

//                alertDialog.setOnShowListener( new OnShowListener() {
//                    @Override
//                    public void onShow(DialogInterface arg0) {
//                        dialog.getButton(AlertDialog.BUTTON).setTextColor(WHITE);
//                    }
//                });
                alertDialog.show();  //<-- See This!
                //alertDialog.getWindow().setLayout(600, 800);
            }

        });

        // set onClickListener to howToUse button
        view.findViewById(R.id.howtouse).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity(),AlertDialog.THEME_HOLO_LIGHT).create(); //Read Update
                alertDialog.setTitle("How to Use");
                alertDialog.setMessage("The app contains three tabs.\n" +
                        "1.First tab for seeking help from friends through message or recording.\n" +
                        "2.Second tab for recording and classifying sound.\n" +
                        "3.Third tab for user settings.\n" +
                        "4.Delete User options removes your account.\n" +
                        "5.Service should be on to setup the communication between band and the application.\n");

                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

                alertDialog.show();  //<-- See This!
                //alertDialog.getWindow().setLayout(600, 800);
            }

        });

        view.findViewById(R.id.addSoundButton).setOnClickListener(this);

        // UID specific to the provider
        uid = user.getUid();
        updateUI();

        /**--------------------------------**/
        listViews = new ExpandableListView[ListSize];
        listAdapters = new ExpandableListAdapter[ListSize];

        listViews[0] = (ExpandableListView) view.findViewById(R.id.notificationListView);
        listViews[1] = (ExpandableListView) view.findViewById(R.id.soundListView);
        listViews[2] = (ExpandableListView) view.findViewById(R.id.serviceListView);

        soundListItems = getSoundListItems();

        if(savedInstanceState != null){
            Log.d(TAG, "check if soundListItems is in sync - present : " + soundListItems.size()+", stored : "+ savedInstanceState.getInt("soundListItemsSize", 0));

            boolean[] switchState = savedInstanceState.getBooleanArray("notifState");
            int[] cummListItemSizes = new int[ListSize];

            cummListItemSizes[0] = notificationListItems.length;
            boolean[] notifSwitchState = Arrays.copyOfRange(switchState,0, cummListItemSizes[0]);

            cummListItemSizes[1] = cummListItemSizes[0] + soundListItems.size();
            boolean[] soundSwitchState = Arrays.copyOfRange(switchState, cummListItemSizes[0], cummListItemSizes[1]);

            cummListItemSizes[2] = cummListItemSizes[1] + servicesListItems.length;
            boolean[] serviceSwitchState = Arrays.copyOfRange(switchState, cummListItemSizes[1], cummListItemSizes[2]);

            try{
                listAdapters[0] = new ExpandableListAdapter(getContext(), notificationListItems, "Notification", notifSwitchState);
                listAdapters[1] = new ExpandableListAdapter(getContext(), soundListItems.toArray(new String[soundListItems.size()]), "Sound Types", soundSwitchState);
                listAdapters[2] = new ExpandableListAdapter(getContext(), servicesListItems, "Service Types", serviceSwitchState);

                for(int i=0; i<ListSize; i++){
                    listViews[i].setAdapter(listAdapters[i]);
                }

            }catch(Exception e){
                Log.e(TAG, e.toString());
                Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }
        } else{
            boolean[] startNotifState = MainActivity.getStartNotifListState();
            boolean[] startSoundState = MainActivity.getStartSoundListState();
            boolean[] startServiceState = MainActivity.getStartServiceListState();

            listAdapters[0] = new ExpandableListAdapter(getContext(), notificationListItems, "Notification", startNotifState);
            listAdapters[1] = new ExpandableListAdapter(getContext(), soundListItems.toArray(new String[soundListItems.size()]), "Sound Types", startSoundState);
            listAdapters[2] = new ExpandableListAdapter(getContext(), servicesListItems, "Service Types", startServiceState);

            for(int i=0; i<ListSize; i++){
                listViews[i].setAdapter(listAdapters[i]);
            }
        }
        /**----------------------------------------------**/

        listViews[0].setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
//                Toast.makeText(getApplicationContext(), " Expanded",
//                        Toast.LENGTH_SHORT).show();
            }
        });
        /**-------------------------------**/

        //------------------------------------------------------

        buttonStart = (Button) view.findViewById(R.id.button);
        buttonStop = (Button) view.findViewById(R.id.button2);
        buttonPlayLastRecordAudio = (Button) view.findViewById(R.id.button3);
        buttonStopPlayingRecording = (Button)view.findViewById(R.id.button4);

        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(false);
        buttonStopPlayingRecording.setEnabled(false);

        random = new Random();

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(checkPermission()) {

                    AudioSavePathInDevice =
                            Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                                    CreateRandomAudioFileName(5) + "AudioRecording.mp4";
                    System.out.println("-----------------------" + "AudioSavePathInDevice : " + AudioSavePathInDevice + "-----------------------");
                    MediaRecorderReady();

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    buttonStart.setEnabled(false);
                    buttonStop.setEnabled(true);

                    /*Toast.makeText(MainActivity.this, "Recording started",
                            Toast.LENGTH_LONG).show();*/
                } else {
                    requestPermission();
                }

            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                buttonStop.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);

                //Toast.makeText(MainActivity.this, "Recording Completed", Toast.LENGTH_LONG).show();
            }
        });

        buttonPlayLastRecordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {

                buttonStop.setEnabled(false);
                buttonStart.setEnabled(false);
                buttonStopPlayingRecording.setEnabled(true);

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(AudioSavePathInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                /*Toast.makeText(MainActivity.this, "Recording Playing",
                        Toast.LENGTH_LONG).show();*/
            }
        });

        buttonStopPlayingRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);

                if(mediaPlayer != null){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    MediaRecorderReady();
                }

                Intent i = new Intent(getActivity(), UploadActivity.class);
                i.putExtra("filePath", AudioSavePathInDevice);
                i.putExtra("isImage", false);
                startActivity(i);
            }
        });
        //------------------------------------------------------

        return view;
    }

    public void MediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    public String CreateRandomAudioFileName(int string){
        StringBuilder stringBuilder = new StringBuilder( string );
        int i = 0 ;
        while(i < string ) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++ ;
        }
        return stringBuilder.toString();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(), new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        System.out.println("Permission Granted");
                       /* Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();*/
                    } else {
                        System.out.println("Permission Denied");
                        // Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signOutButton:
                signOut();
                break;
            case R.id.revokeButton:
                revokeAccess();
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


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnTabEvent) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTabEvent interface");
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
                        else if (soundListItems.contains(fileName))
                            Toast.makeText(getActivity(), "Name taken", Toast.LENGTH_SHORT).show();
                        else {

                            mListener.onButtonClick("StopSavingSound"+" "+fileName);

                            Toast.makeText(getActivity(), fileName+" added", Toast.LENGTH_SHORT);
                            soundListItems.add(fileName);

                            //update sound sound list
                            boolean[] soundSwitchState = new boolean[soundListItems.size()];
                            for(int i=0; i<soundListItems.size()-1; i++){
                                soundSwitchState[i] = listAdapters[1].getCheckedState(i);
                            }
                            soundSwitchState[soundListItems.size()-1] = true;
                            listAdapters[1] = new ExpandableListAdapter(getContext(), soundListItems.toArray(new String[soundListItems.size()]), "Sound Types", soundSwitchState);
                            listViews[1].setAdapter(listAdapters[1]);

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
//        dialog1.show();

    }


    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
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

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        if(SIGNED.equals("GOOGLE")){
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            //TODO: updateUI(null);
                            Log.e(TAG, "Google SignOut1" + status.toString());
                        }
                    });
            Log.e(TAG, "Google SignOut");
        } else if (SIGNED.equals("FB")){
            LoginManager.getInstance().logOut();
        }

        if(mAuth.getCurrentUser() == null){
            Log.e(TAG, "Google SignOut258");
            Intent intent = new Intent(getActivity(), SignInActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        if(SIGNED.equals("GOOGLE")){
            // Google revoke access
            Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            //TODO: updateUI(null);
                        }
                    });
            Log.e(TAG, "Google SignOut");
        } else if (SIGNED.equals("FB")){
            LoginManager.getInstance().logOut();
        }

        if(mAuth.getCurrentUser() == null){
            Log.e(TAG, "Google SignOut259");
            Intent intent = new Intent(getActivity(), SignInActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void updateUI(){
//        name = user.getDisplayName();
//        email = user.getEmail();
//        photoUrl = user.getPhotoUrl();
//        String mUserprofileUrl = photoUrl.toString();
        name = NAME;
        email = EMAIL;
        String mUserprofileUrl = PHOTOURI;

        userName.setText(name);
        userEmail.setText(email);
        try{
            Glide
                    .with(getContext())
                    .load(mUserprofileUrl)
                    .into(userProfileImage);//.placeholder(R.mipmap.ic_launcher).fitCenter()
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }

        Log.d(TAG, name + email);
        Log.e(TAG, "Photo : " + mUserprofileUrl);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.e(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(getActivity(), "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    /**--------------------------------------------**/
    public boolean[] getFinalNotifState(){
        boolean[] notifSwitchState = new boolean[notificationListItems.length];
        for (int i=0; i<notificationListItems.length; i++){
            notifSwitchState[i] = listAdapters[0].getCheckedState(i);
        }
        return notifSwitchState;
    }

    public boolean[] getFinalSoundState(){
        boolean[] soundSwitchState = new boolean[soundListItems.size()];
        for (int i=0; i<soundListItems.size(); i++){
            soundSwitchState[i] = listAdapters[1].getCheckedState(i);
        }
        return soundSwitchState;
    }

    public boolean[] getFinalServiceState(){
        boolean[] serviceSwitchState = new boolean[servicesListItems.length];
        for (int i=0; i<servicesListItems.length; i++){
            serviceSwitchState[i] = listAdapters[2].getCheckedState(i);
        }
        return serviceSwitchState;
    }


}