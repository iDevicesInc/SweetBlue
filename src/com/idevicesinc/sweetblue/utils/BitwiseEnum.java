package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleState;

/**
 * Implementations are {@link BleDeviceState} and {@link BleState}.
 * Not intended for implementations outside this library.
 * 
 * @author dougkoellmer
 */
public interface BitwiseEnum
{
	/**
	 * Returns the bit (0x1, 0x2, 0x4, etc.) this enum represents based on the {@link #ordinal()}.
	 */
	int bit();
	
	/**
	 * Convenience method for checking if <code>({@link #bit()} & mask) != 0x0</code>.
	 */
	boolean overlaps(int mask);
	
	/**
	 * Same as {@link Enum#ordinal()}.
	 */
	int ordinal();
	
	/**
	 * Same as {@link Enum#name()}.
	 */
	String name();
}
