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
 * @see ManagerStateListener
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
	 * This is the state that {@link BleManager} is in when the update loop tick interval has been lowered, due to there being no
	 * tasks in the queue for a time dictated by {@link BleManagerConfig#minTimeToIdle}.
	 */
	IDLE,

	/**
	 * This is the state that {@link BleManager} is in after calling {@link BleManager#startScan()} or related overloads. The {@link BleManager}
	 * will only be in this state for a very short period before moving to {@link #SCANNING}.
	 *
	 * @see BleManager#startScan()
	 */
	STARTING_SCAN,

	/**
	 * This is the state that {@link BleManager} is in when {@link BleManagerConfig#scanClassicBoostLength} is not <code>null</code>, or
	 * {@link Interval#DISABLED}. No devices will be discovered when in this state, it's simply here to make the BLE scan give more
	 * reliable results, especially when looking for many devices. If you explicitly start a Bluetooth Classic scan, the manager will
	 * <b>not</b> enter this state.
	 */
	BOOST_SCANNING,
	
	/**
	 * This is the state that {@link BleManager} is in when scanning actually starts.
	 *
	 */
	SCANNING,

	/**
	 * This is the state that {@link BleManager} is in when scanning has been paused (but will resume). This is mainly used with any of the startPeriodicScan
	 * methods in {@link BleManager}.
	 */
	SCANNING_PAUSED,

	/**
	 * This state dictates that all permissions (if needed) have been granted to allow BLE scanning to return results. See {@link com.idevicesinc.sweetblue.utils.BluetoothEnabler}.
	 */
	BLE_SCAN_READY,
	
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
