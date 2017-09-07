package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import com.idevicesinc.sweetblue.BleDevice.StateListener;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.BleManager.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.util.UUID;

/**
 * An enumeration of the various states that a {@link BleDevice} can be in.
 * Note that a device can and usually will be in multiple states simultaneously.
 * Use {@link BleDevice#setListener_State(StateListener)} to be notified of state changes.
 * 
 * @see BleDevice.StateListener
 */
public enum BleDeviceState implements State
{
	/**
	 * Dummy value returned from any method that would otherwise return Java's built-in <code>null</code>.
	 * A normal {@link BleDevice} will never be in this state, but this will be the sole state of {@link BleDevice#NULL}.
	 */
	NULL,
	
	/**
	 * The device has been undiscovered and you should have been notified through {@link BleManager.DiscoveryListener#onEvent(DiscoveryEvent)}.
	 * This means the object is effectively dead. {@link BleManager} has removed all references to it and you should do the same.
	 */
	UNDISCOVERED,
	
	/**
	 * If {@link BleNodeConfig#reconnectFilter} is set appropriately and the device implicitly disconnects, either through going out of range,
	 * signal disruption, or whatever, then the device will enter this state. It will continue in this state until you return
	 * {@link BleNodeConfig.ReconnectFilter.Please#stopRetrying()} from {@link BleNodeConfig.ReconnectFilter#onEvent(BleNodeConfig.ReconnectFilter.ReconnectEvent)}
	 * or call {@link BleDevice#disconnect()} or when the device actually successfully reconnects.
	 * 
	 * @see #RECONNECTING_SHORT_TERM
	 */
	RECONNECTING_LONG_TERM,
	
	/**
	 * If {@link BleNodeConfig#reconnectFilter} is set appropriately and the device implicitly disconnects this state will be entered.
	 * Unlike with {@link #RECONNECTING_LONG_TERM}, entering this state does not mean that the {@link BleDevice} becomes {@link #DISCONNECTED}.
	 * By all outward appearances the library treats the {@link BleDevice} as still being {@link #CONNECTED} while transparently trying
	 * to reconnect under the hood using {@link BleNodeConfig#reconnectFilter}. You can even perform
	 * {@link BleDevice#read(UUID, BleDevice.ReadWriteListener)}, {@link BleDevice#write(java.util.UUID, byte[])}, etc.
	 * and they will be queued up until the device *actually* reconnects under the hood.
	 * 
	 * @see #RECONNECTING_LONG_TERM
	 */
	@Advanced
	RECONNECTING_SHORT_TERM,

	/**
	 * This state indicates that a connect has failed, but SweetBlue will be retrying the connection again. This is useful for determining if you need
	 * to retry connecting app-side or not. In this case, the device will be {@link #DISCONNECTED}, and look like it's not doing anything, unless it's
	 * also in this state. This is not to be confused with {@link #RECONNECTING_SHORT_TERM} or {@link #RECONNECTING_LONG_TERM}, which only go into those
	 * states once the {@link BleDevice} gets into the {@link #INITIALIZED} state.
	 */
	RETRYING_BLE_CONNECTION,
	
	/**
	 * The device will always be in this state unless it becomes {@link #UNDISCOVERED}.
	 */
	DISCOVERED,
	
	/**
	 * When {@link BleDevice#getOrigin()} is {@link BleDeviceOrigin#FROM_DISCOVERY}, a device will always be in this state while {@link #CONNECTED}
	 * is not active. Note that this doesn't *necessarily* mean that the actual physical device is advertising, just that it was {@link LifeCycle#DISCOVERED}
	 * or {@link LifeCycle#REDISCOVERED} through a {@link BleManager#startScan()}, so it is still assumed to be advertising.
	 */
	ADVERTISING,
	
	/**
	 * The device will always be in this state while {@link #CONNECTED} is not active. Analogous to {@link BluetoothProfile#STATE_DISCONNECTED}.
	 */
	DISCONNECTED,
	
	/**
	 * Analogous to {@link BluetoothDevice#BOND_NONE}. May not be relevant for your application if you don't use encrypted characteristics.
	 */
	UNBONDED,
	
	/**
	 * Analogous to {@link BluetoothDevice#BOND_BONDING}. May not be relevant for your application if you don't use encrypted characteristics.
	 * From this state, a device will either become {@link BleDeviceState#BONDED} (if successful) or {@link BleDeviceState#UNBONDED}.
	 * If the latter, use {@link BleDevice.BondListener} to get further information on what happened.
	 */
	BONDING,
	
	/**
	 * Analogous to {@link BluetoothDevice#BOND_BONDED}. May not be relevant for your application if you don't use encrypted characteristics.
	 */
	BONDED,
	
	/**
	 * A convenience flag for checking if the device is connecting in an overall sense. This state is active if any one of {@link #CONNECTING},
	 * {@link #DISCOVERING_SERVICES}, {@link #AUTHENTICATING}, or {@link #INITIALIZING} is also active. It is suggested to have {@link #BONDING}
	 * be a part of this also using {@link BleDeviceConfig#bondFilter} but technically {@link #BONDING} can be done outside of this state.
	 */
	CONNECTING_OVERALL,
	
	/**
	 * Analogous to {@link BluetoothProfile#STATE_CONNECTING}. If this state is active then we're in the middle of establishing an actual BLE connection.
	 */
	CONNECTING,
	
	/**
	 * Analogous to {@link BluetoothProfile#STATE_CONNECTED}. Once this state becomes active we don't consider ourselves "fully" connected
	 * because we still generally have to discover services and maybe do a few reads or writes to initialize things. So generally speaking
	 * no significant action should be taken when the {@link BleDevice} becomes {@link #CONNECTED}. Instead it's best to listen for {@link #INITIALIZED}.
	 */
	CONNECTED,
	
	/**
	 * This state is active while we request a list of services from the native stack after becoming {@link #CONNECTED}.
	 */
	DISCOVERING_SERVICES,
	
	/**
	 * This state is active after {@link #DISCOVERING_SERVICES} completes successfully.
	 */
	SERVICES_DISCOVERED,
	
	/**
	 * This state can only become active if you use {@link BleDevice#connect(BleTransaction.Auth)} or {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)}
	 * to start a connection with an authentication transaction.
	 */
	AUTHENTICATING,
	
	/**
	 * This state becomes active either if the {@link BleTransaction} provided to {@link BleDevice#connect(BleTransaction.Auth)} or
	 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} succeeds with {@link BleTransaction#succeed()}, OR if you use 
	 * {@link BleDevice#connect()} or {@link BleDevice#connect(BleTransaction.Init)} - i.e. you connect without authentication.
	 * In the latter case the {@link #AUTHENTICATING} state is skipped and we go straight to being implicitly {@link #AUTHENTICATED}.
	 */
	AUTHENTICATED,
	
	/**
	 * This state can only become active if you use {@link BleDevice#connect(BleTransaction.Init)} or {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)}
	 * to start a connection with an initialization transaction.
	 */
	INITIALIZING,
	
	/**
	 * This is generally the state you want to listen for to consider your {@link BleDevice} "fully" connected and ready to go, instead of
	 * basing it off of just {@link #CONNECTED}.
	 * <br><br>
	 * This state becomes active either if the {@link BleTransaction} provided to {@link BleDevice#connect(BleTransaction.Init)} or
	 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} succeeds with {@link BleTransaction#succeed()}, OR if you use 
	 * {@link BleDevice#connect()} or {@link BleDevice#connect(BleTransaction.Auth)} or etc.- i.e. you connect without an initialization
	 * transaction. In the latter case the {@link #INITIALIZING} state is skipped and we go straight to being implicitly {@link #INITIALIZED}.
	 */
	INITIALIZED,
	
	/**
	 * This state for "over-the-air" updates becomes active when you call {@link BleDevice#performOta(BleTransaction.Ota)} and remains active until the provided
	 * {@link BleTransaction} calls {@link BleTransaction#succeed()} or {@link BleTransaction#fail()} (or of course if your {@link BleDevice}
	 * becomes {@link #DISCONNECTED}).
	 */
	PERFORMING_OTA;
	
	static final int PURGEABLE_MASK = DISCOVERED.bit() | DISCONNECTED.bit() | UNBONDED.bit() | BONDING.bit() | BONDED.bit() | ADVERTISING.bit();
	
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

	/**
	 * Returns same as {@link #values()}, but for performance reasons this does not return a new array every time.
	 */
	public static BleDeviceState[] VALUES()
	{
		s_values = s_values != null ? s_values : values();
		
		return s_values;
	}
	private static BleDeviceState[] s_values = null;
	
	/**
	 * Full bitwise mask made by ORing all {@link BleDeviceState} instances together.
	 */
	public static final int FULL_MASK = Utils_Byte.toBits(VALUES());
	
	/**
	 * A convenience for UI purposes, this returns the "highest" connection state representing
	 * a transition from one state to another, so something with "ING" in the name (except {@link #PERFORMING_OTA}).
	 * Chronologically this method returns {@link #CONNECTING}, {@link #DISCOVERING_SERVICES},
	 * {@link #AUTHENTICATING} (if {@link BleDevice#connect(BleTransaction.Auth)} or 
	 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} is called), {@link #BONDING} (if relevant),
	 * and {@link #INITIALIZING}  (if {@link BleDevice#connect(BleTransaction.Init)} or 
	 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} is called).
	 * 
	 * @param stateMask Generally the value returned by {@link BleDevice#getStateMask()}.
	 */
	public static BleDeviceState getTransitoryConnectionState(int stateMask)
	{
		if( CONNECTED.overlaps(stateMask) )
		{
			if( INITIALIZING.overlaps(stateMask) )			return INITIALIZING;
			if( BONDING.overlaps(stateMask) )				return BONDING;
			if( AUTHENTICATING.overlaps(stateMask) )		return AUTHENTICATING;
			if( DISCOVERING_SERVICES.overlaps(stateMask) )	return DISCOVERING_SERVICES;
		}
		else
		{
			if( BONDING.overlaps(stateMask) )				return BONDING;
			if( CONNECTING.overlaps(stateMask) )			return CONNECTING;
		}
		
		if( CONNECTING_OVERALL.overlaps(stateMask) )		return CONNECTING_OVERALL;
		
		return NULL;
	}
	
	int getConnectionOrdinal()
	{
		switch(this)
		{
			case CONNECTING:				return  0;
			case DISCOVERING_SERVICES:		return  1;
			case AUTHENTICATING:			return  2;
			case BONDING:					return  3;
			case INITIALIZING:				return  4;
			default:						return -1;
		}
	}

	@Override public boolean isNull()
	{
		return this == NULL;
	}
}
