package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class P_StripedWriteTransaction extends BleTransaction
{


    private final FutureData m_data;
    private final BluetoothGattCharacteristic m_characteristic;
    private final boolean m_requiresBonding;
    private final BleDevice.ReadWriteListener.Type m_writeType;
    private final BleDevice.ReadWriteListener m_listener;
    private final List<P_Task_Write> m_writeList;
    private final WriteListener m_internalListener;
    private final DescriptorFilter m_descriptorFilter;


    P_StripedWriteTransaction(FutureData data, BluetoothGattCharacteristic characteristic, boolean requiresBonding, DescriptorFilter filter, BleDevice.ReadWriteListener.Type writeType, BleDevice.ReadWriteListener listener)
    {
        m_data = data;
        m_characteristic = characteristic;
        m_requiresBonding = requiresBonding;
        m_listener = listener;
        m_descriptorFilter = filter;
        m_writeType = writeType;
        m_writeList = new ArrayList<>();
        m_internalListener = new WriteListener();
    }


    @Override protected final void start(BleDevice device)
    {
        final byte[] allData = m_data.getData();
        int curIndex = 0;
        FutureData curData;
        while (curIndex < allData.length)
        {
            int end = Math.min(allData.length, curIndex + device.getEffectiveWriteMtuSize());
            curData = new PresentData(Arrays.copyOfRange(allData, curIndex, end));
            final P_Task_Write task;
            if (m_descriptorFilter == null)
            {
                task = new P_Task_Write(device, m_characteristic, curData, m_requiresBonding, m_writeType, m_internalListener, device.m_txnMngr.getCurrent(), device.getOverrideReadWritePriority());
            }
            else
            {
                task = new P_Task_Write(device, m_characteristic.getService().getUuid(), m_characteristic.getUuid(), m_descriptorFilter, curData, m_requiresBonding, m_writeType, m_internalListener, device.m_txnMngr.getCurrent(), device.getOverrideReadWritePriority());
            }
            m_writeList.add(task);

            curIndex = end;
        }
        device.queue().add(m_writeList.remove(0));
    }

    private final class WriteListener implements BleDevice.ReadWriteListener
    {

        @Override public final void onEvent(BleDevice.ReadWriteListener.ReadWriteEvent e)
        {
            if (e.wasSuccess())
            {
                if (m_writeList.size() > 0)
                {
                    getDevice().queue().add(m_writeList.remove(0));
                }
                else
                {
                    succeed();
                    if (m_listener != null)
                    {
                        m_listener.onEvent(e);
                    }
                }
            }
            else
            {
                fail();
                if (m_listener != null)
                {
                    m_listener.onEvent(e);
                }
            }
        }
    }
}
