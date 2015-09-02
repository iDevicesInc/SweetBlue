package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleDevice.BondListener;
import com.idevicesinc.sweetblue.BleDevice.StateListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * An enumeration of the various states that a {@link BleServer} can be in on a per-client (mac address) basis.
 * Note that unlike a {@link BleDevice}, a {@link BleServer} can only be in one state at a time for a given client
 * Use {@link BleServer#setListener_State(BleServer.StateListener)} to be notified of state changes.
 * 
 * @see BleServer.StateListener
 */
public enum BleServerState implements State
{
	/**
	 * Dummy value returned from any method that would otherwise return Java's built-in <code>null</code>.
	 * A normal {@link BleDevice} will never be in this state, but this will be the sole state of {@link BleServer#NULL}.
	 */
	NULL,

	DISCONNECTED,


	CONNECTING,
	

	CONNECTED;


	public static int toBits(BleServerState ... states)
	{
		return Utils.toBits(states);
	}
	
	@Override public boolean overlaps(int mask)
	{
		return (bit() & mask) != 0x0;
	}
	
	@Override public int bit()
	{
		return 0x1 << ordinal();
	}
	
	@Override public boolean didEnter(int oldStateBits, int newStateBits)
	{
		return !this.overlaps(oldStateBits) && this.overlaps(newStateBits);
	}
	
	@Override public boolean didExit(int oldStateBits, int newStateBits)
	{
		return this.overlaps(oldStateBits) && !this.overlaps(newStateBits);
	}
	
	@Override public int or(BitwiseEnum state)
	{
		return this.bit() | state.bit();
	}
	
	@Override public int or(int bits)
	{
		return this.bit() | bits;
	}
	
	static BleServerState[] VALUES()
	{
		s_values = s_values != null ? s_values : values();
		
		return s_values;
	}
	private static BleServerState[] s_values = null;
	
	/**
	 * Full bitwise mask made by ORing all {@link BleServerState} instances together.
	 */
	public static final int FULL_MASK = Utils.calcFullMask(VALUES());

	@Override public boolean isNull()
	{
		return this == NULL;
	}
}
