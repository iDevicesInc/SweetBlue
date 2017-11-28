package com.idevicesinc.sweetblue.servertester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.idevicesinc.sweetblue.BleAdvertisingPacket;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Uuids;

public class MainActivity extends AppCompatActivity implements BleServer.IncomingListener, BleServer.OutgoingListener
{

    private BleManager m_manager;
    private BleServer m_server;
    private Button m_startAdvertising;
    private Button m_stopAdvertising;

    private GattDatabase m_db = new GattDatabase()
            .addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().completeService()

            .addService(Uuids.USER_DATA_SERVICE_UUID)
            .addCharacteristic(Uuids.USER_CONTROL_POINT).setPermissions().readWrite().setProperties().readWrite()

            .completeService();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_startAdvertising = findViewById(R.id.startAdvertising);
        m_stopAdvertising = findViewById(R.id.stopAdvertising);

        BleManagerConfig config = new BleManagerConfig();
        config.runOnMainThread = false;
        config.loggingEnabled = true;

        m_manager = BleManager.get(this, config);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
            @Override
            public Please onEvent(BluetoothEnablerEvent e)
            {
                Please p = super.onEvent(e);
                if (e.isDone())
                {
                    m_startAdvertising.setEnabled(true);
                }
                return p;
            }
        });

        m_startAdvertising.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (m_manager != null && m_manager.isScanningReady())
                {
                    startAdvertising();
                }
            }
        });

        m_stopAdvertising.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (m_server.isAdvertising())
                {
                    m_server.stopAdvertising();
                }
                m_startAdvertising.setEnabled(true);
                m_stopAdvertising.setEnabled(false);
            }
        });
    }

    private void startAdvertising()
    {
        m_server = m_manager.getServer(this, m_db, null);
        m_server.setListener_Outgoing(this);
        m_server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);
        m_stopAdvertising.setEnabled(true);
        m_startAdvertising.setEnabled(false);
    }

    @Override
    public Please onEvent(IncomingEvent e)
    {
        return Please.respondWithSuccess();
    }

    @Override
    public void onEvent(OutgoingEvent e)
    {

    }
}
