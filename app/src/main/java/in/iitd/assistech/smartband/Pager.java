package in.iitd.assistech.smartband;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

//Extending FragmentStatePagerAdapter
public class Pager extends FragmentStatePagerAdapter {

    //integer to count number of tabs
    int tabCount;
    private Tab1 tab1;
    private Tab2 tab2;
    private Tab3 tab3;

    //Constructor to the class
    public Pager(FragmentManager fm, int tabCount) {
        super(fm);
        //Initializing tab count
        this.tabCount= tabCount;
    }

    //Overriding method getItem
    @Override
    public Fragment getItem(int position) {
        //Returning the current tabs
        switch (position) {
            case 0:
                tab1 = new Tab1();
                return tab1;
            case 1:
                tab2 = new Tab2();
                return tab2;
            case 2:
                tab3 = new Tab3();
                return tab3;
            default:
                return null;
        }
    }

    //Overriden method getCount to get the number of tabs
    @Override
    public int getCount() {
        return tabCount;
    }

    public void editTab2Text(double[] prob, boolean[] notifState){
        tab2.editValue(prob, notifState);
    }

    public boolean[] getInitialNotifListState(){
        if (tab3 != null){
            return tab3.getFinalNotifState();
        }
        return null;
    }

    public boolean[] getInitialSoundListState(){
        if (tab3 != null){
            return tab3.getFinalSoundState();
        }
        return null;
    }

    public boolean[] getInitialServiceListState(){
        if (tab3 != null){
            return tab3.getFinalServiceState();
        }
        return null;
    }

    public void simulateSoundRecordClickTab2(){
        tab2.simulateSoundRecordClick();
    }
}
