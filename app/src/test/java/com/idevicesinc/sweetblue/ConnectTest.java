package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{

    @Override public P_NativeManagerLayer getManagerLayer()
    {
        return new P_UnitTestManagerLayer();
    }


    @Test
    public void connectThenDropTest() throws Exception
    {
        BleDevice device = m_mgr.newDevice("00:11:22:33:44:55");
        device.connect(new BleDevice.StateListener()
        {
            @Override public void onEvent(StateEvent e)
            {
                if (e.didEnter(BleDeviceState.CONNECTED))
                {
                    int i = 0;
                    i++;
                }
            }
        });
    }


    private class GattLayer extends P_UnitGatt
    {
        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            return super.connect(device, context, useAutoConnect, callback);
        }
    }


    private class DeviceLayer extends P_UnitDevice
    {
        @Override public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            return super.connect(context, useAutoConnect, callback);
        }
    }

    private class ManagerLayer extends P_UnitTestManagerLayer
    {

        @Override public int getConnectionState(P_NativeDeviceLayer device)
        {
            return super.getConnectionState(device);
        }
    }

}
