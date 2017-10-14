// MainActivity.java
// Hosts the MainActivityFragment on a phone and both the
// MainActivityFragment and SettingsActivityFragment on a tablet
package com.ahsan.a47_dietel_flagquizapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FlagQuiz Activity";

    // keys for reading data from SharedPreferences, You’ll use these to access the preference values.
    public static final String CHOICES = "pref_numberOfChoices";//Key value of ListPreference in preferences.xml
    public static final String REGIONS = "pref_regionsToInclude";//Key value of MultiSelectListPreference in preferences.xml

    private boolean phoneDevice = true; // used to force portrait mode
    private boolean preferencesChanged = true; // specifies whether the app’s preferences have changed—if so, the MainActivity’s onStart lifecycle method (Section 4.6.4) will call the MainActivityFragment’s methods updateGuessRows (Section 4.7.4) and updateRegions (Section 4.7.5) to reconfigure the quiz, based on the new settings

    // configure the MainActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//calls setContentView to set MainActivity’s GUI-NOTE: We have two contentMains, so if app is running on a devices that’s at least 700 pixels wide in landscape orientation—in that case, Android uses the version in the res/layout-sw700dp-land folder
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);//set the Toolbar defined in MainActivity’s layout as the app bar (formerly called the action bar)
        setSupportActionBar(toolbar);

        Log.i(TAG, "Choices has following values:" + CHOICES);
        Log.i(TAG, "Regions has following values:" + REGIONS);


        // set default values in the app's SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);//false indicates that the default preference values should be set only the first time this method is called.

        // register listener for SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // first you combine the value of screenLayout with Configuration.SCREENLAYOUT_SIZE_MASK using the bitwise AND (&) operator.
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE || screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE){
            phoneDevice = false;// not a phone-sized device
        }

        // if running on phone-sized device, allow only portrait orientation, so we could display settings button in menu's place
        if (phoneDevice){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }





    // listener for changes to the app's SharedPreferences
    private OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
        // called when the user changes the app's preferences
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            preferencesChanged = true;// user changed app setting

            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);//Launch our quizFragment

            if (key.equals(CHOICES)){// # of choices to display changed
                quizFragment.updateGuessRows(sharedPreferences);
                quizFragment.resetQuiz();

            }  else if (key.equals(REGIONS)) { // regions to include changed

                Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);// get the Set<String> containing the enabled regions

                if (regions != null && regions.size() > 0) {

                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();

                } else {
                    // must select one region--set North America as default
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putStringSet(REGIONS, regions);
                    editor.apply();

                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                }
            }

            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }
    };






    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged){//preferencesChanged == true
            // now that the default preferences have been set, initialize MainActivityFragment and start the quiz
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            //if preferencesChanged is true, onStart calls MainActivityFragment’s updateGuessRows (Section 4.7.4) and updateRegions (Section 4.7.5) methods to reconfigure the quiz.
            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }






    // show menu if app is running on a phone or a portrait-oriented tablet
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // get the device's current orientation
        int orientation = getResources().getConfiguration().orientation;

        //we want to show the menu only when the app is running in portrait orientation on a mobile device
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);//inflate with two arguments—the resource ID of the menu resource that populates the menu and the Menu object in which the menu items will be placed

            return true;//Returning true from onCreateOptionsMenu indicates that the menu should be displayed.

        } else {

            return false;

        }
    }




    // displays the SettingsActivity when running on a phone
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }






}
