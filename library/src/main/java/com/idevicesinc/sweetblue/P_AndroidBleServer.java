package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.utils.P_Const;
import java.util.List;
import java.util.UUID;


public final class P_AndroidBleServer implements P_NativeServerLayer
{

    final static P_AndroidBleServer NULL = new P_AndroidBleServer(null);

    private final BluetoothGattServer m_server;


    P_AndroidBleServer(BluetoothGattServer server)
    {
        m_server = server;
    }


    @Override
    public final boolean isServerNull()
    {
        return m_server == null;
    }

    @Override
    public final boolean addService(BluetoothGattService service)
    {
        if (m_server != null)
        {
            return m_server.addService(service);
        }

        return false;
    }

    @Override
    public final void cancelConnection(BluetoothDevice device)
    {
        if (m_server != null)
        {
            m_server.cancelConnection(device);
        }
    }

    @Override
    public final void clearServices()
    {
        if (m_server != null)
        {
            m_server.clearServices();
        }
    }

    @Override
    public final void close()
    {
        if (m_server != null)
        {
            m_server.close();
        }
    }

    @Override
    public final boolean connect(BluetoothDevice device, boolean autoConnect)
    {
        if (m_server != null)
        {
            return m_server.connect(device, autoConnect);
        }
        return false;
    }

    @Override
    public final BluetoothGattService getService(UUID uuid)
    {
        if (m_server != null)
        {
            return m_server.getService(uuid);
        }
        return null;
    }

    @Override
    public final List<BluetoothGattService> getServices()
    {
        if (m_server != null)
        {
            return m_server.getServices();
        }
        return P_Const.EMPTY_SERVICE_LIST;
    }

    @Override
    public final boolean notifyCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm)
    {
        if (m_server != null)
        {
            return m_server.notifyCharacteristicChanged(device, characteristic, confirm);
        }
        return false;
    }

    @Override
    public final boolean removeService(BluetoothGattService service)
    {
        if (m_server != null)
        {
            return m_server.removeService(service);
        }
        return false;
    }

    @Override
    public final boolean sendResponse(BluetoothDevice device, int requestId, int status, int offset, byte[] value)
    {
        if (m_server != null)
        {
            return m_server.sendResponse(device, requestId, status, offset, value);
        }
        return false;
    }

    @Override
    public final BluetoothGattServer getNativeServer()
    {
        return m_server;
    }
}
