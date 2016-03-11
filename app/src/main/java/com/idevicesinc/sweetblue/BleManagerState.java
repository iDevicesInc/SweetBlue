package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;

import com.idevicesinc.sweetblue.annotations.UnitTest;
import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_Byte;

/**
 * An enumeration of the various states that a {@link BleManager} can be in.
 * The manager can be in multiple states simultaneously.
 * 
 * @see BleManager.StateListener
 * @see BleManager.NativeStateListener
 * @see BleManager#is(BleManagerState)
 * @see BleManager#getStateMask()
 */
public enum BleManagerState implements State
{
	/**
	 * Analogous to {@link BluetoothAdapter#STATE_OFF}.
	 */
	OFF					(BluetoothAdapter.STATE_OFF),
	
	/**
	 * Analogous to {@link BluetoothAdapter#STATE_TURNING_ON}.
	 */
	TURNING_ON			(BluetoothAdapter.STATE_TURNING_ON),
	
	/**
	 * Analogous to {@link BluetoothAdapter#STATE_ON}.
	 */
	ON					(BluetoothAdapter.STATE_ON),
	
	/**
	 * Analogous to {@link BluetoothAdapter#STATE_TURNING_OFF}.
	 */
	TURNING_OFF			(BluetoothAdapter.STATE_TURNING_OFF),

	/**
	 * This is the state that {@link BleManager} is in after calling {@link BleManager#startScan()} or related overloads. The {@link BleManager}
	 * will only be in this state for a very short period before moving to {@link #SCANNING}.
	 *
	 * @see BleManager#startScan()
	 */
	STARTING_SCAN,
	
	/**
	 * This is the state that {@link BleManager} is in when scanning actually starts.
	 *
	 */
	SCANNING,
	
	/**
	 * This is the state that {@link BleManager} is in after calling {@link BleManager#reset()}.
	 */
	RESETTING;
	
	private final int m_nativeCode;
	
	static BleManagerState[] VALUES()
	{
		s_values = s_values != null ? s_values : values();
		
		return s_values;
	}
	private static BleManagerState[] s_values = null;
	
	/**
	 * Full bitwise mask made by ORing all {@link BleManagerState} instances together.
	 */
	public static final int FULL_MASK = Utils_Byte.toBits(VALUES());
	
	private BleManagerState()
	{
		m_nativeCode = 0;
	}
	
	private BleManagerState(int nativeCode)
	{
		m_nativeCode = nativeCode;
	}

	private BleScanMode m_mode;
	private BleScanPower m_power;


	@UnitTest
	BleManagerState setScanMode(BleScanMode mode)
	{
		m_mode = mode;
		return this;
	}

	@UnitTest
	BleManagerState setPower(BleScanPower power)
	{
		m_power = power;
		return this;
	}

	@UnitTest
	BleScanMode getScanMode()
	{
		return m_mode;
	}

	@UnitTest
	BleScanPower getScanPower()
	{
		return m_power;
	}
	
	/**
	 * Returns the analogous native code, if applicable. For example {@link BluetoothAdapter#STATE_OFF},
	 * {@link BluetoothAdapter#STATE_ON}, etc. {@link #RESETTING} and {@link #SCANNING} do not have a native
	 * code equivalent and will return 0.
	 */
	public int getNativeCode()
	{
		return m_nativeCode;
	}
	
	static BleManagerState get(int nativeCode)
	{
		for( int i = 0; i < VALUES().length; i++ )
		{
			if( VALUES()[i].getNativeCode() == nativeCode )
			{
				return VALUES()[i];
			}
		}
		
		return null;
	}

	@Override public int bit()
	{
		return 0x1 << ordinal();
	}
	
	@Override public boolean overlaps(int mask)
	{
		return (bit() & mask) != 0x0;
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

	@Override public boolean isNull()
	{
		return false;
	}
}
