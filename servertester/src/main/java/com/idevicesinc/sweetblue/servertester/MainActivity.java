package com.idevicesinc.sweetblue.servertester;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleAdvertisingPacket;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements BleServer.IncomingListener, BleServer.OutgoingListener
{

    private BleManager m_manager;
    private BleServer m_server;
    private Button m_startAdvertising;
    private Button m_stopAdvertising;
    private TextView m_textView;

    private boolean connectableFlag = true;


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

        m_textView = findViewById(R.id.textView);

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
                    startAdvertising(connectableFlag);
                    connectableFlag = !connectableFlag;
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

    private void startAdvertising(boolean connectable)
    {
        m_server = m_manager.getServer(this, m_db, null);
        m_server.setListener_Outgoing(this);
        BleAdvertisingPacket advPacket;

        if (connectable)
            advPacket = new BleAdvertisingPacket(Uuids.BATTERY_SERVICE_UUID);
        else
            advPacket = new BleAdvertisingPacket(Uuids.BATTERY_SERVICE_UUID, BleAdvertisingPacket.Option.INCLUDE_NAME);

        m_server.startAdvertising(advPacket);
        m_stopAdvertising.setEnabled(true);
        m_startAdvertising.setEnabled(false);
    }

    @Override
    public Please onEvent(final IncomingEvent e)
    {
        switch (e.type())
        {
            case READ:
                final String msg = Utils_String.makeString("READ request for: ", e.charUuid(), "\n");
                if (msg != null)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            m_textView.append(msg);
                        }
                    });
                }
                byte[] data;
                Random r = new Random();
                if (e.charUuid().equals(Uuids.BATTERY_LEVEL))
                {
                    data = new byte[1];
                }
                else
                {
                    data = new byte[20];
                }
                r.nextBytes(data);
                return Please.respondWithSuccess(data);
            case WRITE:
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        m_textView.append(Utils_String.makeString("WRITE request for: ", e.charUuid(), " Data: ", Utils_Byte.bytesToHexString(e.data_received()), "\n"));
                    }
                });
                return Please.respondWithSuccess();
        }
        return Please.respondWithSuccess();
    }

    @Override
    public void onEvent(OutgoingEvent e)
    {

    }
}
