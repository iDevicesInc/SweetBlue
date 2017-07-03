package com.idevicesinc.sweetblue.toolbox.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
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
import com.idevicesinc.sweetblue.toolbox.view.DialogHelper;
import com.idevicesinc.sweetblue.toolbox.view.ScanAdapter;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;


public class MainActivity extends BaseActivity
{

    private BleManager m_manager;

    private RecyclerView m_deviceRecycler;
    private ScanAdapter m_adapter;
    private ArrayList<BleDevice> m_deviceList;

    private TextView m_scanTextView;
    private ImageView m_scanImageView;

    private DrawerLayout m_drawerLayout;
    private View m_navDrawerLayout;
    private ActionBarDrawerToggle m_drawerToggler;

    private RssiComparator rssiComparator = new RssiComparator();
    private NameComparator nameComparator = new NameComparator();

    private Comparator<BleDevice> m_currentComparator = rssiComparator;


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

        setTitle("");

        m_manager = BleManager.get(this, getDefaultConfig());

        m_manager.setListener_Discovery(new DeviceDiscovery());

        m_manager.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    m_scanImageView.setImageResource(R.drawable.icon_cancel);
                }
                else if (e.didExit(BleManagerState.SCANNING))
                {
                    m_scanImageView.setImageResource(R.drawable.icon_scan);
                }
                else
                {
                    if (!m_manager.isScanningReady())
                    {
                        m_scanImageView.setImageResource(R.drawable.icon_alert);
                        m_scanTextView.setText(R.string.not_ready_to_scan);
                    }
                }
            }
        });

        m_scanTextView = find(R.id.scanTextView);
        m_scanImageView = find(R.id.scanImageView);

        LinearLayout ll = find(R.id.scanLayout);
        ll.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (m_manager.isScanning())
                {
                    m_manager.stopAllScanning();
                    m_scanTextView.setText(R.string.start_scan);
                    m_scanImageView.setImageResource(R.drawable.icon_scan);
                }
                else if (m_manager.isScanningReady())
                {
                    ScanOptions options = new ScanOptions();
                    options.scanInfinitely();
                    m_manager.startScan(options);
                    m_scanTextView.setText(R.string.scanning);
                    m_scanImageView.setImageResource(R.drawable.icon_cancel);
                }
                else
                {
                    BluetoothEnabler.start(MainActivity.this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
                        @Override
                        public Please onEvent(BluetoothEnablerEvent e)
                        {
                            if (e.isDone())
                            {
                                m_scanTextView.setText(R.string.start_scan);
                                m_scanImageView.setImageResource(R.drawable.icon_scan);
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });


        m_deviceList = new ArrayList<>();

        m_deviceRecycler = find(R.id.recyclerView);

        m_adapter = new ScanAdapter(this, m_deviceList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        m_deviceRecycler.setAdapter(m_adapter);
        m_deviceRecycler.setLayoutManager(layoutManager);


        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
            @Override public Please onEvent(BluetoothEnablerEvent e)
            {
                if (e.isDone())
                {
                    m_scanTextView.setText(R.string.start_scan);
                    m_scanImageView.setImageResource(R.drawable.icon_scan);
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
                Collections.sort(m_deviceList, m_currentComparator);
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
                    Collections.sort(m_deviceList, m_currentComparator);
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
        tv.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));
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
        switch (item.getItemId())
        {
            case R.id.scanOptions:
                openScanOptionsDialog();
                return true;
            case R.id.sortOptions:
                openSortOptionsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSortOptionsDialog()
    {
        final String[] choices = getResources().getStringArray(R.array.sort_options);
        int current = (m_currentComparator instanceof NameComparator) ? 0 : 1;
        DialogHelper.showRadioGroupDialog(this, getString(R.string.sort_by), null, choices, current, new DialogHelper.RadioGroupListener()
        {
            @Override
            public void onChoiceSelected(String choice)
            {
                if (choice.equals("Name"))
                {
                    m_currentComparator = nameComparator;
                }
                else
                {
                    m_currentComparator = rssiComparator;
                }
                Collections.sort(m_deviceList, m_currentComparator);
                m_adapter.notifyDataSetChanged();
            }

            @Override
            public void onCanceled()
            {
            }
        });
    }

    private void openScanOptionsDialog()
    {
        String[] choices = getResources().getStringArray(R.array.scan_apis);
        int current;
        switch (m_manager.getConfigClone().scanApi)
        {
            case CLASSIC:
                current = 1;
                break;
            case PRE_LOLLIPOP:
                current = 2;
                break;
            case POST_LOLLIPOP:
                current = 3;
                break;
            default: // Auto
                current = 0;
        }
        DialogHelper.showRadioGroupDialog(this, getString(R.string.select_scan_api), null, choices, current, new DialogHelper.RadioGroupListener()
        {
            @Override
            public void onChoiceSelected(String choice)
            {
                BleManagerConfig cfg = m_manager.getConfigClone();
                switch (choice)
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

            @Override
            public void onCanceled()
            {
            }
        });

    }

    private final static class RssiComparator implements Comparator<BleDevice>
    {

        @Override
        public int compare(BleDevice o1, BleDevice o2)
        {
            return o2.getRssi() - o1.getRssi();
        }
    }

    private final static class NameComparator implements Comparator<BleDevice>
    {

        @Override
        public int compare(BleDevice o1, BleDevice o2)
        {
            return o1.getName_normalized().compareTo(o2.getName_normalized());
        }
    }
}
