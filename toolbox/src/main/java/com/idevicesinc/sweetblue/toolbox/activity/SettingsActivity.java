package com.idevicesinc.sweetblue.toolbox.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.idevicesinc.sweetblue.toolbox.R;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_JSONUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class SettingsActivity extends BaseActivity
{
    private final int REQUEST_CODE_EXTERNAL_STORAGE = 101;

    private final int REQUEST_CODE_IMPORT_SETTINGS = 102;

    protected Object mSettingsObject = null;

    protected PrefsFragment mCurrentFragment = null;

    public static class PrefsFragment extends PreferenceFragment
    {
        protected Context mContext;

        protected Object mSettingsObject = null;

        protected boolean mDirty = false;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState)
        {
            super.onViewCreated(view, savedInstanceState);

            initPreferences();
        }

        public void setConfig(Context c, Object settingsObject)
        {
            mContext = c;
            mSettingsObject = settingsObject;
        }

        void initPreferences()
        {
            try
            {
                buildSettingsForObject(mSettingsObject);
            }
            catch (IllegalAccessException e)
            {

            }
        }

        private Preference createPreferenceForField(final Object o, final Field f)
        {
            Preference p = null;

            try
            {
                int modifiers = f.getModifiers();

                if (Modifier.isFinal(modifiers))
                    return null;

                final Type t = f.getGenericType();

                if (t == boolean.class || t == Boolean.class)
                {
                    CheckBoxPreference cbp = new CheckBoxPreference(mContext);
                    boolean b = f.getBoolean(o);
                    cbp.setChecked(b);
                    cbp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            try
                            {
                                Boolean val = (Boolean)newValue;
                                if (t == boolean.class)
                                    f.setBoolean(o, val);
                                else
                                    f.set(o, val);
                                mDirty = true;
                            }
                            catch (Exception e)
                            {
                                return false;
                            }
                            return true;
                        }
                    });

                    p = cbp;
                }
                else if (t == short.class || t == Short.class)
                {
                    //TODO
                    Log.d("settings++", "Would add short preference for " + f);
                }
                else if (t == int.class || t == Integer.class)
                {
                    final EditTextPreference etp = new EditTextPreference(mContext);
                    etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
                    etp.setDefaultValue(f.get(o).toString());
                    etp.setSummary(f.get(o).toString());
                    etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            try
                            {
                                Integer i = Integer.valueOf((String)newValue);
                                if (t == int.class)
                                    f.setInt(o, i);
                                else
                                    f.set(o, i);

                                etp.setSummary(i.toString());

                                mDirty = true;
                            }
                            catch (Exception e)
                            {
                                return false;
                            }
                            return true;
                        }
                    });
                    p = etp;
                }
                else if (t == long.class || t == Long.class)
                {
                    //TODO
                    Log.d("settings++", "Would add long preference for " + f);
                }
                else if (t == float.class || t == Float.class)
                {
                    //TODO
                    Log.d("settings++", "Would add float preference for " + f);
                }
                else if (t == double.class || t == Double.class)
                {
                    //TODO
                    Log.d("settings++", "Would add double preference for " + f);
                }
                else if (t == Interval.class)
                {
                    Interval i = (Interval)f.get(o);
                    Double val = new Double(i.secs());
                    final EditTextPreference etp = new EditTextPreference(mContext);
                    etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    etp.setDefaultValue(val.toString());
                    etp.setSummary(val.toString() + " seconds");
                    etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            try
                            {
                                Double d = Double.valueOf((String)newValue);

                                Interval i = Interval.secs(d);
                                f.set(o, i);

                                etp.setSummary(d.toString() + " seconds");

                                mDirty = true;
                            }
                            catch (Exception e)
                            {
                                return false;
                            }
                            return true;
                        }
                    });
                    p = etp;
                }
                else
                {
                    Log.d("settings++", "Unknown type '" + t + "' for  " + f);
                }

                if (p != null)
                    p.setTitle(unHumpCamelCase(f.getName()));
            }
            catch (Exception e)
            {
                p = null;
            }

            return p;
        }

        private void buildSettingsForObject(final Object o) throws IllegalAccessException
        {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(mContext);
            setPreferenceScreen(screen);

            PreferenceCategory category = new PreferenceCategory(mContext);
            category.setTitle("Preferences for " + o.getClass().getSimpleName());

            screen.addPreference(category);

            Class c = o.getClass();
            Field[] fields = c.getFields();
            for (final Field f : fields)
            {
                Preference p = createPreferenceForField(o, f);
                if (p != null)
                    category.addPreference(p);
            }
        }

        private static String unHumpCamelCase(String camelCaseString)
        {
            if (camelCaseString == null)
                return null;
            String[] splits = camelCaseString.split("(?=\\p{Upper})");

            StringBuilder sb = new StringBuilder();
            if (splits.length > 0)
            {
                String first = splits[0];
                first = first.substring(0, 1).toUpperCase() + first.substring(1);
                sb.append(first);
                for (int i = 1; i < splits.length; ++i)
                {
                    sb.append(' ');
                    String split = splits[i];
                    split = split.substring(0, 1).toLowerCase() + split.substring(1);
                    sb.append(split);
                }
            }

            return sb.toString();
        }

        public boolean getIsDirty()
        {
            return mDirty;
        }

        public void clearDirty()
        {
            mDirty = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.settings_title));

        //init();

        BleManager manager = BleManager.get(this);
        mSettingsObject = manager.getConfigClone();

        createUI();

    }

    protected void createUI()
    {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PrefsFragment f = new PrefsFragment();
        f.setConfig(this, mSettingsObject);
        fragmentTransaction.replace(R.id.fragment_container, f);
        fragmentTransaction.commit();

        mCurrentFragment = f;
    }

    @Override
    public boolean onNavigateUp()
    {


        navigateBack();

        return true;
    }

    @Override
    public void onBackPressed()
    {
        navigateBack();
    }

    private void navigateBack()
    {
        if (mCurrentFragment != null && mCurrentFragment.getIsDirty())
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.settings_save_dialog_title));
            alertDialog.setMessage(getString(R.string.settings_save_dialog_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        finish();
                    }
                });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        BleManager manager = BleManager.get(SettingsActivity.this);
                        manager.setConfig((BleManagerConfig)mSettingsObject);
                        dialog.dismiss();
                        finish();
                    }
                });
            alertDialog.show();
        }
        else
        {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.importSettings)
            importSettings();
        else if (item.getItemId() == R.id.exportSettingsEmail)
            exportSettingsToEmail();
        else if (item.getItemId() == R.id.exportSettings)
            exportSettingsToDisk();
        else if (item.getItemId() == R.id.resetSettings)
            resetSettings();
        return super.onOptionsItemSelected(item);
    }

    private String getSettingsJSON()
    {
        //TODO:  Confirm save?  For now just do it!

        // Update the ble manager config
        BleManagerConfig cfg = (BleManagerConfig)mSettingsObject;
        BleManager.get(this, cfg);

        // Build a JSON object of the differences in the config from a standard config
        BleManagerConfig baseConfig = new BleManagerConfig();

        JSONObject jo1 = baseConfig.writeJSON();
        JSONObject jo2 = cfg.writeJSON();

        try
        {
            //TODO:  Find a way to move this into the library (the diff)
            JSONObject diff = P_JSONUtil.shallowDiffJSONObjects(jo1, jo2);
            String diffString = diff.toString();
            return diffString;
        }
        catch (Exception e)
        {

        }

        return null;
    }

    private void importSettings()
    {
        Intent intent = new Intent()
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_CODE_IMPORT_SETTINGS);
    }

    private void importSettingsFromJSON(JSONObject jo)
    {
        if (jo == null)
            return;

        mSettingsObject = new BleManagerConfig(jo);

        createUI();
    }

    private void exportSettingsToEmail()
    {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));

        emailIntent.setType("message/rfc822");

        //emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.send_feedback_email_address) });

        //emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject));

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getSettingsJSON());

        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_send_mail)));
    }

    private void exportSettingsToDisk()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_EXTERNAL_STORAGE);
            return;
        }

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Environment.getDownloadCacheDirectory();

        // Look for a file name that isn't taken
        String filenameFormat = "sweetblue%s.json";
        String filename = String.format(filenameFormat, "");
        File file = null;

        int counter = 1;
        do
        {
            file = new File(path, filename);
            if (!file.exists())
                break;
            file = null;
            filename = String.format(filenameFormat, ++counter);
        } while (true && counter < 100);  // Limit is just to avoid an insanely long loop if we somehow can't find an suitable file

        if (file == null)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.settings_toast_unable_to_save), Toast.LENGTH_LONG).show();
            return;
        }

        path.mkdirs();

        try
        {
            OutputStream os = new FileOutputStream(file);
            String JSONData = getSettingsJSON();
            byte[] data = JSONData.getBytes("US-ASCII");
            os.write(data);
            os.close();
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.settings_toast_unable_to_save), Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        i.setData(Uri.fromFile(file));
        sendBroadcast(i);

        String toastMsg = String.format(getString(R.string.settings_toast_saved), filename);
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_EXTERNAL_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    exportSettingsToDisk();
                }
                return;
            }
        }
    }

    private void resetSettings()
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.settings_reset_dialog_title));
        alertDialog.setMessage(getString(R.string.settings_reset_dialog_message));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    mSettingsObject = MainActivity.getDefaultConfig();

                    createUI();

                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_SETTINGS && resultCode == RESULT_OK)
        {
            Uri selectedfile = data.getData(); //The uri with the location of the file

            try
            {
                JSONObject jo = readJSONFromURI(selectedfile);
                importSettingsFromJSON(jo);
            }
            catch (Exception e)
            {
                String messageString = String.format(getString(R.string.settings_toast_unable_to_load), selectedfile.getLastPathSegment());
                Toast.makeText(getApplicationContext(), messageString, Toast.LENGTH_LONG).show();
                return;
            }

            String messageString = String.format(getString(R.string.settings_toast_loaded), selectedfile.getLastPathSegment());
            Toast.makeText(getApplicationContext(), messageString, Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject readJSONFromURI(Uri uri) throws IOException, JSONException
    {
        ContentResolver cr = getContentResolver();
        InputStream is = cr.openInputStream(uri);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final int kBufferSize = 1024;
        byte buf[] = new byte[kBufferSize];
        int len;
        while ((len = is.read(buf)) > 0)
        {
            os.write(buf, 0, len);
        }

        String JSONString = os.toString();
        JSONObject jo = new JSONObject(JSONString);

        return jo;
    }
}
