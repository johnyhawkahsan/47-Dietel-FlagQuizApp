// SettingsActivityFragment.java
// Subclass of PreferenceFragment for managing app settings
package com.ahsan.a47_dietel_flagquizapp;

import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsActivityFragment extends PreferenceFragment {
    // creates preferences GUI from preferences.xml file in res/xml
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.preferences); // load from XML
    }
}
