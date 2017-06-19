package com.idevicesinc.sweetblue.toolbox;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.toolbox.view.ReselectableSpinner;
import com.idevicesinc.sweetblue.toolbox.view.ScanAdapter;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends BaseActivity
{

    private BleManager m_manager;
    private BleManagerConfig m_config;

    private Button m_startScan;
    private Button m_stopScan;
    private ReselectableSpinner m_apiSpinner;
    private TextView m_scanStatus;
    private RecyclerView m_deviceRecycler;
    private ScanAdapter m_adapter;
    private ArrayList<BleDevice> m_deviceList;

    private DrawerLayout m_drawerLayout;
    private View m_navDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_config = new BleManagerConfig();
        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.connectFailRetryConnectingOverall = true;
        m_config.logger = DebugLog.getDebugger();
        //m_config.updateLoopCallback = UpdateManager.getInstance();

        m_manager = BleManager.get(this, m_config);

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
                String option = (String) parent.getItemAtPosition(position);
                switch (option)
                {
                    case "Classic":
                        m_config.scanApi = BleScanApi.CLASSIC;
                        break;
                    case "Pre-Lollipop":
                        m_config.scanApi = BleScanApi.PRE_LOLLIPOP;
                        break;
                    case "Post-Lollipop":
                        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
                        break;
                    default:
                        m_config.scanApi = BleScanApi.AUTO;
                        break;
                }
                m_manager.setConfig(m_config);
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
            // Burn through the rediscovery events
            Log.d("upd", "Processing " + m_rediscoverMap.size() + " events");
            /*for (String key : m_rediscoverMap.keySet())
            {
                DiscoveryEvent de = m_rediscoverMap.get(key);
                if (de == null)
                    continue;

                processEvent(de, true);
                m_rediscoverMap.remove(key);
            }*/
            if (m_rediscoverMap.size() > 0)
            {
                m_rediscoverMap.clear();
                m_adapter.notifyDataSetChanged();
            }
            Log.d("upd", "Done processing events");
        }

        boolean processEvent(DiscoveryEvent de, boolean processRediscovers)
        {
            if (de.was(LifeCycle.DISCOVERED))
            {
                m_deviceList.add(de.device());
                m_adapter.notifyItemInserted(m_deviceList.size() - 1);

                Log.d("evt", "Discover event for " + de.device().getMacAddress());
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

                Log.d("evt", "Rediscover event for " + de.device().getMacAddress());
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

        LinearLayout ll = null;

        ll = find(R.id.loggerLinearLayout);
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

        ll = find(R.id.deviceInformationLinearLayout);
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

        ll = find(R.id.settingsLinearLayout);
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

        ll = find(R.id.websiteLinearLayout);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                launchWebsite();
            }
        });

        ll = find(R.id.sendFeedbackLinearLayout);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                closeNavDrawer();

                sendFeedbackEmail();
            }
        });
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
}
