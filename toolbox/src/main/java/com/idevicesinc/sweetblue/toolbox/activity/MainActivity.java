package com.idevicesinc.sweetblue.toolbox.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.toolbox.BuildConfig;
import com.idevicesinc.sweetblue.toolbox.util.DebugLog;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UpdateManager;
import com.idevicesinc.sweetblue.toolbox.view.ReselectableSpinner;
import com.idevicesinc.sweetblue.toolbox.view.ScanAdapter;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class MainActivity extends BaseActivity
{

    private BleManager m_manager;

    private Button m_startScan;
    private Button m_stopScan;
    private ReselectableSpinner m_apiSpinner;
    private TextView m_scanStatus;
    private RecyclerView m_deviceRecycler;
    private ScanAdapter m_adapter;
    private ArrayList<BleDevice> m_deviceList;

    private DrawerLayout m_drawerLayout;
    private View m_navDrawerLayout;
    private ActionBarDrawerToggle m_drawerToggler;

    public static BleManagerConfig getDefaultConfig()
    {
        BleManagerConfig cfg = new BleManagerConfig();
        cfg.runOnMainThread = false;
        cfg.loggingEnabled = true;
        cfg.connectFailRetryConnectingOverall = true;
        cfg.logger = DebugLog.getDebugger();
        return cfg;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        m_manager = BleManager.get(this, getDefaultConfig());

        m_manager.setListener_Discovery(new DeviceDiscovery());

        m_manager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    m_scanStatus.setText(R.string.scanning);
                }
                else if (e.didExit(BleManagerState.SCANNING))
                {
                    m_scanStatus.setText(R.string.not_scanning);
                }
            }
        });

        m_startScan = find(R.id.startScan);
        m_startScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                ScanOptions options = new ScanOptions();
                options.scanInfinitely();
                m_manager.startScan(options);
            }
        });

        m_stopScan = find(R.id.stopScan);
        m_stopScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                m_manager.stopAllScanning();
            }
        });
        m_apiSpinner = find(R.id.apiSpinner);
        m_scanStatus = find(R.id.scanStatus);

        m_deviceList = new ArrayList<>();

        m_deviceRecycler = find(R.id.recyclerView);

        m_adapter = new ScanAdapter(this, m_deviceList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        m_deviceRecycler.setAdapter(m_adapter);
        m_deviceRecycler.setLayoutManager(layoutManager);

        m_apiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                BleManagerConfig cfg = m_manager.getConfigClone();
                String option = (String) parent.getItemAtPosition(position);
                switch (option)
                {
                    case "Classic":
                        cfg.scanApi = BleScanApi.CLASSIC;
                        break;
                    case "Pre-Lollipop":
                        cfg.scanApi = BleScanApi.PRE_LOLLIPOP;
                        break;
                    case "Post-Lollipop":
                        cfg.scanApi = BleScanApi.POST_LOLLIPOP;
                        break;
                    default:
                        cfg.scanApi = BleScanApi.AUTO;
                        break;
                }
                m_manager.setConfig(cfg);
            }

            @Override public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });
        m_startScan.setEnabled(false);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
            @Override public Please onEvent(BluetoothEnablerEvent e)
            {
                if (e.isDone())
                {
                    m_startScan.setEnabled(true);
                }
                return super.onEvent(e);
            }
        });

        setupNavDrawer();
    }

    @Override protected void onDestroy()
    {
        if (isFinishing())
        {
            m_manager.shutdown();
        }
        super.onDestroy();
    }

    private final class DeviceDiscovery implements BleManager.DiscoveryListener, UpdateManager.UpdateListener
    {
        ConcurrentHashMap<String, DiscoveryEvent> m_rediscoverMap = new ConcurrentHashMap<>();

        DeviceDiscovery()
        {
            // Register for updates every 3 seconds
            UpdateManager.getInstance().subscribe(this, 3.0);
        }

        @Override public void onEvent(DiscoveryEvent de)
        {
            if (!processEvent(de, false))
                m_rediscoverMap.put(de.macAddress(), de);
        }

        @Override
        public void onUpdate()
        {
            if (m_rediscoverMap.size() > 0)
            {
                m_rediscoverMap.clear();
                m_adapter.notifyDataSetChanged();
            }
        }

        boolean processEvent(DiscoveryEvent de, boolean processRediscovers)
        {
            if (de.was(LifeCycle.DISCOVERED))
            {
                m_deviceList.add(de.device());
                m_adapter.notifyItemInserted(m_deviceList.size() - 1);
            }
            else if (de.was(LifeCycle.REDISCOVERED))
            {
                if (!processRediscovers)
                    return false;

                // If the device was rediscovered, then we have an updated rssi value, so inform the adapter that the data has changed
                // for this device
                int index = m_deviceList.indexOf(de.device());
                if (index != -1)
                {
                    m_adapter.notifyItemChanged(index);
                }
            }

            // True because we processed the event
            return true;
        }
    }

    private void closeNavDrawer()
    {
        m_drawerLayout.closeDrawer(m_navDrawerLayout, true);
    }

    private void setupNavDrawer()
    {
        // Grab the drawer
        m_drawerLayout = find(R.id.drawerLayout);
        m_navDrawerLayout = find(R.id.navigationDrawer);

        m_drawerToggler = new ActionBarDrawerToggle(this, m_drawerLayout, R.string.drawer_open, R.string.drawer_close)
        {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView)
            {
                int i;
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view)
            {
                int i;
            }
        };

        m_drawerLayout.addDrawerListener(m_drawerToggler);

        m_drawerLayout.post(new Runnable()
        {
            @Override
            public void run()
            {
                m_drawerToggler.syncState();
            }
        });

        m_drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener()
        {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                m_drawerToggler.syncState();
            }

            @Override
            public void onDrawerClosed(View drawerView)
            {
                m_drawerToggler.syncState();
            }

            @Override
            public void onDrawerStateChanged(int newState)
            {
            }
        });

        // For hamburger menu
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView iv = find(R.id.closeButton);
        iv.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();
            }
        });

        LinearLayout ll = find(R.id.logger);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();
                Intent intent = new Intent(MainActivity.this, LoggerActivity.class);
                startActivity(intent);
            }
        });

        ll = find(R.id.deviceInfo);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                Intent intent = new Intent(MainActivity.this, DeviceInformationActivity.class);
                startActivity(intent);
            }
        });

        ll = find(R.id.about);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                launchWebsite();
            }
        });

        ll = find(R.id.settings);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        ll = find(R.id.feedback);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                sendFeedbackEmail();
            }
        });

        TextView tv = find(R.id.appVersion);
        tv.setText("App Version - " + BuildConfig.VERSION_NAME);
    }

    private void launchWebsite()
    {
        // Launch a web view
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.visite_website_url)));

        startActivity(browserIntent);
    }

    private void sendFeedbackEmail()
    {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));

        emailIntent.setType("message/rfc822");

        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.send_feedback_email_address) });

        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject));

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.send_feedback_email_body));

        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_send_mail)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (m_drawerToggler.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }
}
