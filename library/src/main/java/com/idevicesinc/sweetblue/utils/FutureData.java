package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

/**
 * A simple interface whose implementations should be passed to methods like {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, FutureData)}
 * so you can provide time-sensitive data at the last possible moment. For example you may want to send the current time to a peripheral. If you provided
 * the time through {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, byte[])} then the operation might spend a second or two in SweetBlue's internal job
 * queue, so that by the time the write was actually sent off, the time would be a few seconds behind. Using this class, the data will be requested right before
 * it actually gets sent off.
 */
public interface FutureData
{
	/**
	 * Convenience constant implementation that just returns an empty byte array.
	 *
	 * @deprecated
	 */
	@Deprecated
	public static FutureData EMPTY = new FutureData()
	{
		@Override public byte[] getData()
		{
			return P_Const.EMPTY_BYTE_ARRAY;
		}
	};

	/**
	 * Return the data that should (for example) be sent to a peripheral through {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, FutureData)}.
	 */
	byte[] getData();
}
