package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


final class P_StripedWriteDescriptorTransaction extends BleTransaction
{

    private final boolean m_requiresBonding;
    private final List<P_Task_WriteDescriptor> m_writeList;
    private final WriteListener m_internalListener;
    private final BleWrite m_write;


    P_StripedWriteDescriptorTransaction(BleWrite write, boolean requiresBonding)
    {
        m_write = write;
        m_requiresBonding = requiresBonding;
        m_writeList = new ArrayList<>();
        m_internalListener = new WriteListener();
    }


    @Override protected final void start(BleDevice device)
    {
        final byte[] allData = m_write.m_data.getData();
        int curIndex = 0;
        FutureData curData;
        while (curIndex < allData.length)
        {
            int end = Math.min(allData.length, curIndex + device.getEffectiveWriteMtuSize());
            curData = new PresentData(Arrays.copyOfRange(allData, curIndex, end));
            final BleWrite write = m_write.createDuplicate()
                    .setData(curData)
                    .setReadWriteListener(m_internalListener);
            m_writeList.add(new P_Task_WriteDescriptor(device, write, m_requiresBonding, device.m_txnMngr.getCurrent(), device.getOverrideReadWritePriority()));
            curIndex = end;
        }
        device.queue().add(m_writeList.remove(0));
    }

    private final class WriteListener implements ReadWriteListener
    {

        @Override public final void onEvent(ReadWriteListener.ReadWriteEvent e)
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
                    if (m_write.readWriteListener != null)
                        m_write.readWriteListener.onEvent(e);
                }
            }
            else
            {
                fail();
                if (m_write.readWriteListener != null)
                    m_write.readWriteListener.onEvent(e);
            }
        }
    }
}
