package com.idevicesinc.sweetblue.toolbox;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.View;

import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.utils.Interval;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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

            BleManagerConfig cfg = new BleManagerConfig();

            try
            {
                buildSettingsForObject(cfg);
            }
            catch (IllegalAccessException e)
            {

            }
            Log.d("done", "done");
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
                    etp.getEditText().setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    etp.setDefaultValue(val.toString());
                    etp.setSummary(val.toString());
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

                                etp.setSummary(d.toString());
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

            // JSON sample code
            try
            {
                JSONObject jo = JSONUtil.settingsObjectToJSON(o);
                String s = jo.toString();
                Log.d("++JSON", "Object is " + s);

                JSONUtil.applyJSONToSettingsObject(o, jo);

                Log.d("++JSON", "applied options back");
            }
            catch (Exception e)
            {

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

    static final Class s_allowedClasses[] =
        {
            boolean.class,
            Boolean.class,
            short.class,
            Short.class,
            int.class,
            Integer.class,
            long.class,
            Long.class,
            float.class,
            Float.class,
            String.class,
            Interval.class
        };

    public static class JSONUtil
    {

        public static Set<Class> getAllowedClassSet()
        {
            Set<Class> s = new HashSet<>();

            for (Class c : s_allowedClasses)
                s.add(c);

            return s;
        }

        // Utility methods
        public static JSONObject settingsObjectToJSON(Object settingsObject) throws IllegalAccessException, JSONException
        {
            JSONObject jo = new JSONObject();

            Set<Class> allowedClasses = getAllowedClassSet();

            Class c = settingsObject.getClass();
            Field[] fields = c.getFields();
            for (Field f : fields)
            {
                int modifiers = f.getModifiers();

                // Skip anything static or final, we only care about mutable instance fields
                if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
                    continue;

                Type t = f.getGenericType();

                // See if the type is supported?
                if (!allowedClasses.contains(t))
                {
                    Log.d("++JSON", "Skippking unknown class " + t);
                    continue;
                }

                if (t == Interval.class)
                {  // Special handling of interval, save it as a double
                    Interval i = (Interval)f.get(settingsObject);
                    jo.put(f.getName(), i.secs());
                }
                else
                    jo.put(f.getName(), f.get(settingsObject));
            }

            return jo;
        }

        public static void applyJSONToSettingsObject(Object settingsObject, JSONObject jo) throws IllegalAccessException, JSONException
        {
            Set<Class> allowedClasses = getAllowedClassSet();

            Class c = settingsObject.getClass();
            Field[] fields = c.getFields();
            for (Field f : fields)
            {
                int modifiers = f.getModifiers();

                // Skip anything static or final, we only care about mutable instance fields
                if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
                    continue;

                Type t = f.getGenericType();

                // See if the type is supported?
                if (!allowedClasses.contains(t))
                {
                    Log.d("++JSON", "Skippking unknown class " + t);
                    continue;
                }

                if (t == Interval.class)
                {  // Special handling of interval, save it as a double
                    Double val = jo.opt(f.getName()) != null ? jo.getDouble(f.getName()) : null;
                    if (val != null)
                    {
                        Interval i = Interval.secs(val);
                        f.set(settingsObject, i);
                    }
                }
                else
                {
                    Object val = jo.opt(f.getName());
                    if (val != null)
                        f.set(settingsObject, val);
                }
            }
        }
    }
}
