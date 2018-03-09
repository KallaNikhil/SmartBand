package in.iitd.assistech.smartband;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.facebook.FacebookSdk.getApplicationContext;
import static in.iitd.assistech.smartband.MainActivity.EMAIL;
import static in.iitd.assistech.smartband.MainActivity.NAME;
import static in.iitd.assistech.smartband.MainActivity.PHOTO;
import static in.iitd.assistech.smartband.MainActivity.PHOTOURI;
import static in.iitd.assistech.smartband.MainActivity.SIGNED;

public class Tab3 extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{

    public View view;
    private static final String TAG = "Tab3";


    private ExpandableListAdapter notifListAdapter;
    private ExpandableListAdapter soundListAdapter;
    private ExpandableListView notifListView;
    private ExpandableListView soundListView;
    static final String[] notificationListItems = {"Vibration", "Sound", "Flashlight", "Flash Screen"};
    static final String[] soundListItems = {"Vehicle Horn", "Dog Bark"};

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


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int state = notificationListItems.length+soundListItems.length;
        boolean[] notifSwitchState = new boolean[state];
//        boolean[] soundSwitchState = new boolean[soundListItems.length];

        Log.e(TAG, "FUCK YOU!!!!!!!!!!!!!!!!!!!!!!");
        for (int i=0; i<notificationListItems.length; i++){
            notifSwitchState[i] = notifListAdapter.getCheckedState(i);
        }
        for (int i=notificationListItems.length; i<state; i++){
            notifSwitchState[i] = soundListAdapter.getCheckedState(i-notificationListItems.length);
//            soundSwitchState[i] = soundListAdapter.getCheckedState(i);
        }

//        Log.e(TAG, "SoundStare 0 and 1 " + soundSwitchState[0] + ", " + soundSwitchState[1]);

        outState.putBooleanArray("notifState", notifSwitchState);
//        outState.putBooleanArray("soundState", soundSwitchState);
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }


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

        view.findViewById(R.id.signOutButton).setOnClickListener(this);
        view.findViewById(R.id.revokeButton).setOnClickListener(this);

        // UID specific to the provider
        uid = user.getUid();
        updateUI();

        /**--------------------------------**/
        notifListView = (ExpandableListView) view.findViewById(R.id.notificationListView);
        soundListView = (ExpandableListView) view.findViewById(R.id.soundListView);
        if(savedInstanceState != null){
            boolean[] switchState = savedInstanceState.getBooleanArray("notifState");
            boolean[] notifSwitchState = Arrays.copyOf(switchState, notificationListItems.length);
            boolean[] soundSwitchState = new boolean[soundListItems.length];
            for (int i=0; i<soundListItems.length; i++){
                soundSwitchState[i] = switchState[notificationListItems.length+i];
            }
//            boolean[] soundSwitchState = savedInstanceState.getBooleanArray("soundState");
            try{
//                notifListAdapter = new NotifListAdapter(getContext(), notificationListItems, notifSwitchState);
                notifListAdapter = new ExpandableListAdapter(getContext(), notificationListItems, "Notification", notifSwitchState);
//                ListView notifListView = (ListView) view.findViewById(R.id.notificationListView);
                notifListView.setAdapter(notifListAdapter);

                soundListAdapter = new ExpandableListAdapter(getContext(), soundListItems, "Sound Types", soundSwitchState);
                soundListView.setAdapter(soundListAdapter);
            }catch(Exception e){
                Log.e(TAG, e.toString());
                Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }
        } else{
            boolean[] startNotifState = MainActivity.getStartNotifListState();
            boolean[] startSoundState = MainActivity.getStartSoundListState();
            notifListAdapter = new ExpandableListAdapter(getContext(), notificationListItems, "Notification", startNotifState);
            notifListView.setAdapter(notifListAdapter);

            soundListAdapter = new ExpandableListAdapter(getContext(), soundListItems, "Sound Types", startSoundState);
            soundListView.setAdapter(soundListAdapter);
        }
        /**----------------------------------------------**/

        notifListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
//                Toast.makeText(getApplicationContext(), " Expanded",
//                        Toast.LENGTH_SHORT).show();
            }
        });
        /**-------------------------------**/

        return view;
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
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
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
            notifSwitchState[i] = notifListAdapter.getCheckedState(i);
        }
        return notifSwitchState;
    }

    public boolean[] getFinalSoundState(){
        boolean[] soundSwitchState = new boolean[soundListItems.length];
        for (int i=0; i<soundListItems.length; i++){
            soundSwitchState[i] = notifListAdapter.getCheckedState(i);
        }
        return soundSwitchState;
    }

}
