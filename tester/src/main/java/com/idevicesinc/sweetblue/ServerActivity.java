package com.idevicesinc.sweetblue;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.idevicesinc.sweetblue.tester.R;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.UUID;

public class ServerActivity extends Activity
{

    private BleManager m_manager;
    private BleServer m_server;


    private static final UUID mFakeService = UUID.randomUUID();
    private static final UUID mFakeChar = UUID.randomUUID();
    private static final UUID mAdvService = Uuids.fromInt("1234");


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        BleManagerConfig config = new BleManagerConfig(true);
        config.runOnMainThread = false;

        m_manager = BleManager.get(this, config);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter() {
            @Override
            public Please onEvent(BluetoothEnablerEvent e)
            {
                if (e.isDone())
                {
                    startUpServer();
                }
                return super.onEvent(e);
            }
        });

    }

    private void startUpServer()
    {
        GattDatabase db = new GattDatabase().addService(mFakeService)
                .addCharacteristic(mFakeChar).setValue(new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8 }).setProperties().readWrite().setPermissions().readWrite().completeService();

        m_server = m_manager.getServer(new BleServer.IncomingListener()
        {
            @Override
            public Please onEvent(IncomingEvent e)
            {
                return Please.respondWithSuccess();
            }
        }, db, new BleServer.ServiceAddListener()
        {
            @Override
            public void onEvent(ServiceAddEvent e)
            {
                if (!e.wasSuccess())
                {
                    Toast.makeText(ServerActivity.this, "Service with UUID " + e.serviceUuid() + " failed to get added!", Toast.LENGTH_LONG).show();
                }
                else
                {
                    BleAdvertisingPacket packet = new BleAdvertisingPacket(mAdvService);
                    m_server.startAdvertising(packet, new BleServer.AdvertisingListener()
                    {
                        @Override
                        public void onEvent(AdvertisingEvent e)
                        {
                            if (e.wasSuccess())
                            {
                                Log.e("Server", "Advertising as " + m_server.getManager().getNativeAdapter().getName());
                            }
                            else
                            {
                                Toast.makeText(ServerActivity.this, "Unable to start advertising! Error: " + e.status().name(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

}
