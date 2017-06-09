package com.idevicesinc.sweetblue.toolbox;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsActivity extends PreferenceActivity
{
    public static class Prefs1Fragment extends PreferenceFragment
    {
        Context mContext;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            //initPreferences();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState)
        {
            super.onViewCreated(view, savedInstanceState);

            initPreferences();
        }

        public void setContext(Context c)
        {
            mContext = c;
        }

        void initPreferences()
        {
            Object o = getPreferenceManager();

            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mContext);
            setPreferenceScreen(screen);

            PreferenceCategory category = new PreferenceCategory(mContext);
            category.setTitle("category name");

            screen.addPreference(category);

            for (int i = 0; i < 15; ++i)
            {
                CheckBoxPreference checkBoxPref = new CheckBoxPreference(mContext);
                checkBoxPref.setTitle("title" + i);
                checkBoxPref.setSummary("summary");
                checkBoxPref.setChecked(true);


                category.addPreference(checkBoxPref);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.settings_title));

        //init();


        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Prefs1Fragment f = new Prefs1Fragment();
        f.setContext(this);
        fragmentTransaction.add(R.id.fragment_container, f, "HELLO");
        fragmentTransaction.commit();

    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }

    void init()
    {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle("category name");

        screen.addPreference(category);

        CheckBoxPreference checkBoxPref = new CheckBoxPreference(this);
        checkBoxPref.setTitle("title");
        checkBoxPref.setSummary("summary");
        checkBoxPref.setChecked(true);

        category.addPreference(checkBoxPref);
        setPreferenceScreen(screen);
    }
}
