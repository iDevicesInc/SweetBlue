package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


final class P_StripedWriteDescriptorTransaction extends BleTransaction
{

    private final FutureData m_data;
    private final BluetoothGattDescriptor m_descriptor;
    private final boolean m_requiresBonding;
    private final BleDevice.ReadWriteListener m_listener;
    private final List<P_Task_WriteDescriptor> m_writeList;
    private final WriteListener m_internalListener;


    P_StripedWriteDescriptorTransaction(FutureData data, BluetoothGattDescriptor descriptor, boolean requiresBonding, BleDevice.ReadWriteListener listener)
    {
        m_data = data;
        m_descriptor = descriptor;
        m_requiresBonding = requiresBonding;
        m_listener = listener;
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
            m_writeList.add(new P_Task_WriteDescriptor(device, m_descriptor, curData, m_requiresBonding, m_internalListener, device.m_txnMngr.getCurrent(), device.getOverrideReadWritePriority()));
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
                    m_listener.onEvent(e);
                }
            }
            else
            {
                fail();
                m_listener.onEvent(e);
            }
        }
    }
}
