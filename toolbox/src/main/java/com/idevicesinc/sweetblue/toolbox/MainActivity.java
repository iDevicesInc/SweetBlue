package com.idevicesinc.sweetblue.toolbox;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
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


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_config = new BleManagerConfig();
        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.connectFailRetryConnectingOverall = true;

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
    }

    @Override protected void onDestroy()
    {
        if (isFinishing())
        {
            m_manager.shutdown();
        }
        super.onDestroy();
    }

    private final class DeviceDiscovery implements BleManager.DiscoveryListener
    {

        @Override public void onEvent(DiscoveryEvent e)
        {
            if (e.was(LifeCycle.DISCOVERED))
            {
                m_deviceList.add(e.device());
                m_adapter.notifyItemInserted(m_deviceList.size() - 1);
            }
            else if (e.was(LifeCycle.REDISCOVERED))
            {
                // If the device was rediscovered, then we have an updated rssi value, so inform the adapter that the data has changed
                // for this device
                int index = m_deviceList.indexOf(e.device());
                if (index != -1)
                {
                    m_adapter.notifyItemChanged(index);
                }
            }
        }
    }
}
