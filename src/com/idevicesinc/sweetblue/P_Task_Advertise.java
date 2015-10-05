package com.idevicesinc.sweetblue;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.ParcelUuid;
import com.idevicesinc.sweetblue.utils.BleAdvertiseConfig;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.Map;
import java.util.UUID;


public class P_Task_Advertise extends PA_Task_RequiresBleOn {

    private final BleAdvertiseConfig m_info;
    private final BleServer.AdvertiseListener m_listener;
    private final boolean start;


    public P_Task_Advertise(BleServer server, BleAdvertiseConfig info, BleServer.AdvertiseListener listener, boolean start)
    {
        super(server, null);
        this.start = start;
        m_info = info;
        m_listener = listener;
    }

    public boolean isStarting()
    {
        return start;
    }

    @Override
    protected BleTask getTaskType()
    {
        if (start)
        {
            return BleTask.START_ADVERTISING;
        }
        else
        {
            return BleTask.STOP_ADVERTISING;
        }
    }

    @Override
    void execute()
    {
        if (Utils.isLollipop())
        {
            if (start)
            {
                startAdvertising(m_info, m_listener);
            }
            else
            {
                stopAdvertising(m_listener);
            }
        }
        else
        {
            fail();
        }
    }

    private void stopAdvertising(final BleServer.AdvertiseListener m_listener)
    {
        BluetoothLeAdvertiser ad = getManager().getNativeAdapter().getBluetoothLeAdvertiser();
        if (ad != null)
        {
            ad.stopAdvertising(new AdvertiseCallback()
            {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect)
                {
                    super.onStartSuccess(settingsInEffect);
                    if (m_listener != null)
                    {
                        m_listener.onEvent(new BleServer.AdvertiseListener.AdvertiseEvent(getServer(), BleServer.AdvertiseListener.AdvertiseResult.SUCCESS));
                        succeed();
                    }
                }

                @Override
                public void onStartFailure(int errorCode)
                {
                    super.onStartFailure(errorCode);
                    if (m_listener != null)
                    {
                        BleServer.AdvertiseListener.AdvertiseResult res = BleServer.AdvertiseListener.AdvertiseResult.fromNativeBit(errorCode);
                        m_listener.onEvent(new BleServer.AdvertiseListener.AdvertiseEvent(getServer(), res));
                        fail();
                    }
                }
            });
        }
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.MEDIUM;
    }

    private void startAdvertising(BleAdvertiseConfig info, final BleServer.AdvertiseListener listener)
    {
        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
        settings.setAdvertiseMode(info.getMode().getNativeBit());
        settings.setTxPowerLevel(info.getPower().getNativeBit());
        settings.setConnectable(info.isConnectable());
        if (info.getTimeout() != null)
        {
            settings.setTimeout((int) info.getTimeout().millis());
        }
        AdvertiseData.Builder data = new AdvertiseData.Builder();
        for (UUID id : info.getUuids())
        {
            data.addServiceUuid(new ParcelUuid(id));
        }
        if (info.getManufacturerId() != Integer.MIN_VALUE && info.getManufacturerData() != null)
        {
            data.addManufacturerData(info.getManufacturerId(), info.getManufacturerData());
        }
        if (info.getServiceData() != null && info.getServiceData().size() > 0)
        {
            Map<UUID, byte[]> sdata = info.getServiceData();
            for (UUID dataUuid : sdata.keySet())
            {
                data.addServiceData(new ParcelUuid(dataUuid), sdata.get(dataUuid));
            }
        }
        data.setIncludeDeviceName(info.includeDeviceName());
        data.setIncludeTxPowerLevel(info.includeTxPowerLevel());
        BluetoothLeAdvertiser advert = getManager().getNativeAdapter().getBluetoothLeAdvertiser();
        if (advert != null)
        {
            advert.startAdvertising(settings.build(), data.build(), new AdvertiseCallback()
            {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect)
                {
                    getLogger().d("Advertising started successfully.");
                    if (listener != null)
                    {
                        getServer().onAdvertiseResult(BleServer.AdvertiseListener.AdvertiseResult.SUCCESS, listener);
                    }
                    else
                    {
                        getServer().onAdvertiseResult(BleServer.AdvertiseListener.AdvertiseResult.SUCCESS);
                    }
                    succeed();
                }

                @Override
                public void onStartFailure(int errorCode)
                {
                    BleServer.AdvertiseListener.AdvertiseResult result = BleServer.AdvertiseListener.AdvertiseResult.fromNativeBit(errorCode);
                    getLogger().e("Failed to start advertising! Result: " + result);
                    if (listener != null)
                    {
                        getServer().onAdvertiseResult(result, listener);
                    }
                    else
                    {
                        getServer().onAdvertiseResult(result);
                    }
                    fail();
                }
            });
        }
    }

    static boolean canAdvertise(BleManager man)
    {
        if (Build.VERSION.SDK_INT >= 21)
        {
            return man.getNativeAdapter().isMultipleAdvertisementSupported();
        }
        else
        {
            return false;
        }
    }
}
