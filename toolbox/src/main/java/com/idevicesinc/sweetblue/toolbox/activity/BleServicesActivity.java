package com.idevicesinc.sweetblue.toolbox.activity;


import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.fragment.BleDetailsFragment;
import com.idevicesinc.sweetblue.toolbox.fragment.BleServicesFragment;
import com.idevicesinc.sweetblue.toolbox.view.BleServiceAdapter;
import com.idevicesinc.sweetblue.toolbox.view.ReselectableSpinner;
import java.util.ArrayList;


public class BleServicesActivity extends BaseActivity
{

    private BleDevice m_device;



    private TabLayout m_tabLayout;
    private ViewPager m_viewPager;
    private DetailsTabsAdaptor m_pagerAdapter;
    private Listener m_currentListener;


    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleservices);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        String mac = getIntent().getStringExtra("mac");
        m_device = BleManager.get(this).getDevice(mac);

        setTitle(m_device.getName_normalized());

        m_tabLayout = find(R.id.tabLayout);
        m_viewPager = find(R.id.viewPager);
        m_pagerAdapter = new DetailsTabsAdaptor(getSupportFragmentManager());

        m_viewPager.setAdapter(m_pagerAdapter);
        m_tabLayout.setupWithViewPager(m_viewPager);

        if (m_device.getNativeServices_List() == null || m_device.getNativeServices_List().size() == 0)
        {
            m_viewPager.setCurrentItem(1);
        }

        m_device.setListener_State(new DeviceStateListener()
        {
            @Override
            public void onEvent(BleDevice.StateListener.StateEvent e)
            {
                if (m_currentListener != null)
                {
                    m_currentListener.onEvent(e);
                }
            }
        });
    }

    public BleDevice getDevice()
    {
        return m_device;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.connect:
                m_device.connect();
                break;
            case R.id.disconnect:
                m_device.disconnect();
                break;
            case R.id.bond:
                m_device.bond();
                break;
            case R.id.unbond:
                m_device.unbond();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        getMenuInflater().inflate(getMenuResId(), menu);
        return true;
    }

    @Override
    public int getMenuResId()
    {
        if (m_device != null)
        {
            boolean connected = !m_device.is(BleDeviceState.DISCONNECTED);
            boolean bonded = m_device.is(BleDeviceState.BONDED);
            if (connected)
            {
                if (bonded)
                {
                    return R.menu.connected_bonded;
                }
                return R.menu.connected_unbonded;
            }
            if (bonded)
            {
                return R.menu.disconnected_bonded;
            }
            return R.menu.disconnected_unbonded;
        }
        return super.getMenuResId();
    }

    @Override protected void onDestroy()
    {
        m_device.setListener_State((DeviceStateListener) null);
        super.onDestroy();
    }

    public void registerListener(Listener listener)
    {
        m_currentListener = listener;
    }

    private class DetailsTabsAdaptor extends FragmentPagerAdapter
    {

        public DetailsTabsAdaptor(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if (position == 0)
            {
                return new BleServicesFragment();
            }
            return new BleDetailsFragment();
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if (position == 0)
            {
                return getString(R.string.services);
            }
            return getString(R.string.details);
        }
    }

    public interface Listener extends DeviceStateListener
    {
    }
}
