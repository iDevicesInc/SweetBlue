package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.fragment.WriteValueLoadFragment;
import com.idevicesinc.sweetblue.toolbox.fragment.WriteValueNewFragment;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WriteValueActivity extends BaseActivity
{
    // What are we writing to?
    private BleManager mBleManager;
    private BleDevice mDevice;
    private BluetoothGattService mService;
    private BluetoothGattCharacteristic mCharacteristic;

    // UI configuration
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private DetailsTabsAdaptor mPagerAdapter;
    private ArrayList<BleServicesActivity.Listener> mListeners;

    private final String SHARED_PREFERENCES_FILE_NAME = "SAVED_VALUES";

    public static final Uuids.GATTFormatType sAllowedFormats[] =
        {
            Uuids.GATTFormatType.GCFT_boolean,
            Uuids.GATTFormatType.GCFT_2bit,
            Uuids.GATTFormatType.GCFT_nibble,
            Uuids.GATTFormatType.GCFT_uint8,
            Uuids.GATTFormatType.GCFT_uint12,
            Uuids.GATTFormatType.GCFT_uint16,
            Uuids.GATTFormatType.GCFT_uint24,
            Uuids.GATTFormatType.GCFT_uint32,
            Uuids.GATTFormatType.GCFT_uint48,
            Uuids.GATTFormatType.GCFT_uint64,
            Uuids.GATTFormatType.GCFT_uint128,
            Uuids.GATTFormatType.GCFT_sint8,
            Uuids.GATTFormatType.GCFT_sint12,
            Uuids.GATTFormatType.GCFT_sint16,
            Uuids.GATTFormatType.GCFT_sint24,
            Uuids.GATTFormatType.GCFT_sint32,
            Uuids.GATTFormatType.GCFT_sint48,
            Uuids.GATTFormatType.GCFT_sint64,
            Uuids.GATTFormatType.GCFT_sint128,
            Uuids.GATTFormatType.GCFT_float32,
            Uuids.GATTFormatType.GCFT_float64,
            Uuids.GATTFormatType.GCFT_utf8s,
            Uuids.GATTFormatType.GCFT_utf16s,
            Uuids.GATTFormatType.GCFT_struct
        };

    // Saved values
    public static class SavedValue implements Comparable<SavedValue>
    {
        String mName;
        String mValueString;
        Uuids.GATTFormatType mGattFormatType;

        SavedValue(String name, String value, Uuids.GATTFormatType gft)
        {
            mName = name;
            mValueString = value;
            mGattFormatType = gft;
        }

        public String getName()
        {
            return mName;
        }

        public String getValueString()
        {
            return mValueString;
        }

        public Uuids.GATTFormatType getGATTFormatType()
        {
            return mGattFormatType;
        }

        public static SavedValue parse(String name, String packed)
        {
            String splits[] = packed.split("\\|", 2);
            String lenString = splits[0];
            String leftover = splits[1];

            Integer len = Integer.parseInt(lenString);

            String value = leftover.substring(0, len);
            String typeString = leftover.substring(len);
            Uuids.GATTFormatType gft = Uuids.GATTFormatType.valueOf(typeString);

            SavedValue sv = new SavedValue(name, value, gft);
            return sv;
        }

        public void writePreference(SharedPreferences.Editor editor)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("" + mValueString.length());
            sb.append("|");
            sb.append(mValueString);
            sb.append(mGattFormatType.name());

            editor.putString(mName, sb.toString());
        }

        @Override
        public int compareTo(@NonNull SavedValue o)
        {
            return mName.compareTo(o.mName);
        }
    }

    private List<SavedValue> mSavedValueList = new ArrayList<>();
    private boolean mSavedValueListDirty = false;

    private enum Tabs
    {
        New,
        Load
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleservices);

        mListeners = new ArrayList<>(2);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final String mac = getIntent().getStringExtra("mac");
        final String serviceUUID = getIntent().getStringExtra("serviceUUID");
        final String characteristicUUID = getIntent().getStringExtra("characteristicUUID");

        mBleManager = BleManager.get(this);
        mDevice = mBleManager.getDevice(mac);
        mService = mDevice.getNativeService(UUID.fromString(serviceUUID));
        List<BluetoothGattCharacteristic> charList = mService.getCharacteristics();
        if (characteristicUUID != null)
        {
            for (BluetoothGattCharacteristic bgc : charList)
            {
                if (characteristicUUID.equals(bgc.getUuid().toString()))
                {
                    mCharacteristic = bgc;
                    break;
                }
            }
        }

        actionBar.setTitle(UuidUtil.getCharacteristicName(mCharacteristic));

        mTabLayout = find(R.id.tabLayout);
        mViewPager = find(R.id.viewPager);
        mPagerAdapter = new DetailsTabsAdaptor(getSupportFragmentManager());

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });

        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        if (mDevice.getNativeServices_List() == null || mDevice.getNativeServices_List().size() == 0)
        {
            mViewPager.setCurrentItem(1);
        }

        mDevice.setListener_State(new DeviceStateListener()
        {
            @Override
            public void onEvent(BleDevice.StateListener.StateEvent e)
            {
                if (mListeners.size() > 0)
                {
                    for (BleServicesActivity.Listener l : mListeners)
                    {
                        if (l != null)
                        {
                            l.onEvent(e);
                        }
                    }
                }
            }
        });

        loadSavedValues();
    }

    public BleDevice getDevice()
    {
        return mDevice;
    }

    public List<SavedValue> getSavedValues()
    {
        // Return a defensive copy
        List<SavedValue> l = new ArrayList<>();
        l.addAll(mSavedValueList);
        return l;
    }

    public void addSavedValue(SavedValue sv)
    {
        // Remove then add so we replace identically named values
        mSavedValueList.remove(sv);
        mSavedValueList.add(sv);
        mSavedValueListDirty = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.write_value, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.write)
        {
            if (mViewPager.getCurrentItem() == Tabs.New.ordinal())
            {
                Fragment f = mPagerAdapter.getFragmentAtPosition(Tabs.New.ordinal());
                WriteValueNewFragment wvnf = (WriteValueNewFragment)f;
                if (wvnf != null)
                {
                    String valueString = wvnf.getValueString();
                    Uuids.GATTFormatType gft = wvnf.getValueGATTFormatType();
                    String saveAsName = wvnf.getSaveAsName();
                    if (saveAsName != null)
                    {
                        SavedValue sv = new SavedValue(saveAsName, valueString, gft);
                        addSavedValue(sv);
                    }

                    writeValue(valueString, gft);
                }
                return true;
            }
            else if (mViewPager.getCurrentItem() == Tabs.Load.ordinal())
            {
                Fragment f = mPagerAdapter.getFragmentAtPosition(Tabs.Load.ordinal());
                WriteValueLoadFragment wvlf = (WriteValueLoadFragment)f;
                if (wvlf != null)
                {
                    SavedValue sv = wvlf.getSelectedValue();
                    if (sv != null)
                        writeValue(sv.getValueString(), sv.getGATTFormatType());
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onDestroy()
    {
        mDevice.setListener_State((DeviceStateListener) null);
        mListeners.clear();
        super.onDestroy();
    }

    public void registerListener(BleServicesActivity.Listener listener)
    {
        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    private class DetailsTabsAdaptor extends FragmentPagerAdapter
    {
        private WeakReference<Fragment> mFragments[] = new WeakReference[Tabs.values().length];

        public DetailsTabsAdaptor(FragmentManager fm)
        {
            super(fm);
        }

        public Fragment getFragmentAtPosition(int position)
        {
            try
            {
                return mFragments[position].get();
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public Fragment getItem(int position)
        {
            Fragment f = null;
            if (position == Tabs.New.ordinal())
                f = new WriteValueNewFragment();
            else if (position == Tabs.Load.ordinal())
            {
                WriteValueLoadFragment wvlf = new WriteValueLoadFragment();
                wvlf.setSavedValues(getSavedValues());
                f = wvlf;
            }
            mFragments[position] = new WeakReference<>(f);
            return f;
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if (position == Tabs.New.ordinal())
                return getString(R.string.write_value_new_tab);
            else if (position == Tabs.Load.ordinal())
                return getString(R.string.write_value_load_tab);
            return null;
        }
    }

    public interface Listener extends DeviceStateListener
    {
    }

    @Override
    public boolean onNavigateUp()
    {
        saveSavedValues();
        finish();
        return true;
    }

    private void loadSavedValues()
    {
        mSavedValueList.clear();

        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        Set<String> keySet = sp.getAll().keySet();

        for (String key : keySet)
        {
            String s = sp.getString(key, null);

            SavedValue sv = SavedValue.parse(key, s);

            mSavedValueList.add(sv);
        }

        Collections.sort(mSavedValueList);
    }

    private void saveSavedValues()
    {
        if (!mSavedValueListDirty)
            return;

        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        for (SavedValue sv : mSavedValueList)
            sv.writePreference(editor);

        editor.commit();
    }

    private void writeValue(final String valueString, final Uuids.GATTFormatType gft)
    {
        try
        {
            byte rawVal[] = gft.stringToByteArray(valueString);

            final Dialog d = ProgressDialog.show(WriteValueActivity.this, getString(R.string.write_value_writing_dialog_title), getString(R.string.write_value_writing_dialog_message));

            mDevice.write(mCharacteristic.getUuid(), rawVal, new BleDevice.ReadWriteListener()
            {
                @Override
                public void onEvent(ReadWriteEvent e)
                {
                    if (e.wasSuccess())
                    {
                        Toast.makeText(getApplicationContext(), R.string.write_value_writing_success_toast, Toast.LENGTH_LONG).show();
                        saveSavedValues();
                        finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), R.string.write_value_writing_fail_toast, Toast.LENGTH_LONG).show();

                    d.dismiss();
                }
            });
        }
        catch (Uuids.GATTCharacteristicFormatTypeConversionException e)
        {
            //FIXME:  Add toast telling user the write failed
            Toast.makeText(getApplicationContext(), getString(R.string.write_value_invalid_value_toast) + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
