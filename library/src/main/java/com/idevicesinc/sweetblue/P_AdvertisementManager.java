package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


class P_AdvertisementManager
{

    private final BleServer m_server;
    // Cached packet
    private BleAdvertisingPacket m_advPacket;

    private boolean m_isAdvertising = false;

    private BleServer.AdvertisingListener m_advertisingListener;


    P_AdvertisementManager(BleServer server)
    {
        m_server = server;
    }


    final void setListener_Advertising(BleServer.AdvertisingListener listener)
    {
        m_advertisingListener = listener;
    }

    final BleServer.AdvertisingListener getListener_advertising()
    {
        return m_advertisingListener;
    }

    final boolean isAdvertising()
    {
        return m_isAdvertising;
    }

    final boolean isAdvertising(UUID serviceUuid)
    {
        if (Utils.isLollipop() && m_advPacket != null)
        {
            return m_advPacket.hasUuid(serviceUuid);
        }
        return false;
    }

    final BleServer.AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advertisePacket, BleAdvertisingSettings settings, BleServer.AdvertisingListener listener)
    {
        if (m_server.isNull())
        {
            getManager().getLogger().e(BleServer.class.getSimpleName() + " is null!");

            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.NULL_SERVER);
        }

        if (!isAdvertisingSupportedByAndroidVersion())
        {
            getManager().getLogger().e("Advertising NOT supported on android OS's less than Lollipop!");

            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.ANDROID_VERSION_NOT_SUPPORTED);
        }

        if (!isAdvertisingSupportedByChipset())
        {
            getManager().getLogger().e("Advertising NOT supported by current device's chipset!");

            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.CHIPSET_NOT_SUPPORTED);
        }

        if (!getManager().is(BleManagerState.ON))
        {
            getManager().getLogger().e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.BLE_NOT_ON);
        }

        if (m_isAdvertising)
        {
            getManager().getLogger().w(BleServer.class.getSimpleName() + " is already advertising!");

            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.ALREADY_STARTED);
        }
        else
        {
            getManager().ASSERT(!getManager().getTaskQueue().isCurrentOrInQueue(P_Task_Advertise.class, getManager()));

            getManager().getTaskQueue().add(new P_Task_Advertise(m_server, advertisePacket, settings, listener));
            return new BleServer.AdvertisingListener.AdvertisingEvent(m_server, BleServer.AdvertisingListener.Status.NULL);
        }
    }

    final void stopAdvertising()
    {
        if (Utils.isLollipop())
        {

            final P_Task_Advertise adTask = getManager().getTaskQueue().get(P_Task_Advertise.class, getManager());
            if (adTask != null)
            {
                adTask.stopAdvertising();
                adTask.clearFromQueue();
            }
            else
            {
                // We don't leave the advertising task in the queue, so this is what's going to be called 99% of the time.
                getManager().managerLayer().stopAdvertising();
            }
            onAdvertiseStop();
        }
    }

    final void onAdvertiseStart(BleAdvertisingPacket packet)
    {
        getManager().getLogger().d("Advertising started successfully.");
        m_advPacket = packet;
        m_isAdvertising = true;
    }

    final void onAdvertiseStartFailed(BleServer.AdvertisingListener.Status result)
    {
        getManager().getLogger().e("Failed to start advertising! Result: " + result);
        onAdvertiseStop();
    }

    final void onAdvertiseStop()
    {
        getManager().getLogger().d("Advertising stopped.");
        m_advPacket = null;
        m_isAdvertising = false;
    }

    /**
     * Checks to see if the device is running an Android OS which supports
     * advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByAndroidVersion()}.
     */
    final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return getManager().isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
     */
    final boolean isAdvertisingSupportedByChipset()
    {
        return getManager().isAdvertisingSupportedByChipset();
    }

    /**
     * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
     */
    final boolean isAdvertisingSupported()
    {
        return getManager().isAdvertisingSupported();
    }


    private BleManager getManager()
    {
        return m_server.getManager();
    }

}
