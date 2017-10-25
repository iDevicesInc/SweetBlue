package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import java.util.concurrent.Semaphore;


public class BleServerTest extends BaseTester<MainActivity>
{


    @Test
    public void testServerAddServices() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        GattDatabase db = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
                .addCharacteristic(Uuids.BATTERY_LEVEL).setProperties().read().setPermissions().read().completeService()
                .addService(Uuids.CURRENT_TIME_SERVICE)
                .addCharacteristic(Uuids.CURRENT_TIME).setProperties().read().setPermissions().read().completeService()
                .addService(Uuids.HEART_RATE_SERVICE_UUID)
                .addCharacteristic(Uuids.HEART_RATE_MAX).setProperties().readWrite().setPermissions().readWrite().completeChar()
                .addCharacteristic(Uuids.HEART_RATE_MEASUREMENT).setProperties().read().setPermissions().read().completeService();
        BleServer server = mgr.getServer(db);
        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID, new BleServer.AdvertisingListener()
        {
            @Override
            public void onEvent(AdvertisingEvent e)
            {
                s.release();
            }
        });
        s.acquire();
    }


    @Override
    Class<MainActivity> getActivityClass()
    {
        return MainActivity.class;
    }

    @Override
    BleManagerConfig getInitialConfig()
    {
        return new BleManagerConfig(true);
    }
}
