package in.iitd.assistech.smartband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by nikhil on 12/5/18.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {

    public static String CONTENT_ACTION = "in.iitd.assistech.smartband.MyBroadcastReceiver.ContentAction";
    public static String INCORRECT_ACTION = "in.iitd.assistech.smartband.MyBroadcastReceiver.IncorrectAction";
    public static String CORRECT_ACTION = "in.iitd.assistech.smartband.MyBroadcastReceiver.CorrectAction";

    private static final String TAG = "MyBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // remove notification
        if(BluetoothService.isInstanceCreated()){
            BluetoothService.getInstance().removeNotification(BluetoothService.NOTIFICATION_ID_RESULT);
        }

        if(CONTENT_ACTION.equals(action)) {
//            Log.v(TAG,"Pressed Content");
//
//            String soundCategory = intent.getExtras().getString("SoundResultCategory","No Result");
//            Log.d(TAG, soundCategory);
//            if(!MainActivity.isRunning()) {
//                Intent contentIntent = new Intent(context, MainActivity.class);
//                contentIntent.putExtra("DisplaySoundDetectionDialog", true);
//                contentIntent.putExtra("SoundDetectionResult", soundCategory);
//                context.startActivity(contentIntent);
//            }else{
//                MainActivity.getInstance().showDialog(MainActivity.getInstance(), soundCategory, false);
//            }

        } else if(INCORRECT_ACTION.equals(action)) {
            Log.v(TAG,"Pressed NO");

            if(!MainActivity.isRunning()) {
                Intent contentIntent = new Intent(context, MainActivity.class);
                contentIntent.putExtra("IncorrectDetection", true);
                context.startActivity(contentIntent);
            }else{
                MainActivity.getInstance().showIncorrectDetectionDialog(MainActivity.getInstance(), false);
            }

        } else if(CORRECT_ACTION.equals(action)) {
            Log.v(TAG,"Pressed YES");

            String soundCategory = intent.getExtras().getString("SoundResultCategory","No Result");
            //TODO
        }
    }
}
