package in.iitd.assistech.smartband;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static java.lang.Thread.sleep;

public class ExpandableListAdapterButton extends BaseExpandableListAdapter {

//    private Context _context;
//    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
//    private HashMap<String, List<String>> _listDataChild;

    String[] names;
    String header;
    Context context;
    LayoutInflater inflter;
    String value;
    final static String TAG="Debug";
    Intent bluetoothServiceIntent;

    public ExpandableListAdapterButton(Context context, String[] names, String header) {
        this.context = context;
        this.names = names;
        this.header = header;
        inflter = (LayoutInflater.from(context));
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View view, ViewGroup parent)  {
        view = inflter.inflate(R.layout.notif_list_row_button, null);
        final TextView notifTextView = (TextView) view.findViewById(R.id.notif_row_text);
        final Button notifButton = (Button) view.findViewById(R.id.notif_row_button);
        notifTextView.setText(names[childPosition]);

        notifButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(header.equals("SoundTypes")) {
                    // Code here executes on main thread after user presses button
                    String soundName = ((Button) (v.findViewById(R.id.notif_row_button))).getText().toString();
                    String fname = Environment.getExternalStorageDirectory().getPath() + "/" + soundName + SoundProcessing.AUDIO_RECORDER_FILE_EXT_WAV;
                    File file = new File(fname, SoundProcessing.AUDIO_RECORDER_FOLDER);
                    if(file.exists()){
                        file.delete();
                        Toast.makeText(context, "Sound Deleted", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(context, "Sound not Present", Toast.LENGTH_SHORT).show();
                    }
                    Tab3.getInstance().updateSoundList(Tab3.getSoundListItems());
                }
            }
        });

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    @Override
    public int getChildrenCount(int groupPosition) {
        return names.length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.collapsible_list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(header);

        return convertView;
    }
}
