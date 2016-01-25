package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener;
import com.idevicesinc.sweetblue.BleTransaction.EndReason;

interface PI_EndListener
{
	void onTransactionEnd(BleTransaction txn, EndReason reason, ReadWriteListener.ReadWriteEvent failReason);
}