package com.idevicesinc.sweetblue.util;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.ScanOptions;


public class MainActivity extends Activity
{

    private BleManager m_manager;
    private BleManagerConfig m_config;

    private Button m_startScan;
    private Button m_stopScan;
    private Spinner m_apiSpinner;
    private TextView m_scanStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_config = new BleManagerConfig();
        m_config.runOnMainThread = false;

        m_manager = BleManager.get(this, m_config);

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
                d
                m_manager.startScan(options);
            }
        });

        m_stopScan = find(R.id.stopScan);
        m_apiSpinner = find(R.id.apiSpinner);
        m_scanStatus = find(R.id.scanStatus);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    <T extends View> T find(int id)
    {
        return (T) findViewById(id);
    }

    private final class DeviceDiscovery implements BleManager.DiscoveryListener
    {

        @Override public void onEvent(DiscoveryEvent e)
        {

        }
    }
}
