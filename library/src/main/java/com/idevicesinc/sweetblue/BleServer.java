package com.idevicesinc.sweetblue;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.impl.DefaultServerReconnectFilter;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode;
import static com.idevicesinc.sweetblue.BleManagerState.ON;
import static com.idevicesinc.sweetblue.BleServerState.*;


/**
 * Get an instance from {@link BleManager#getServer()}. Wrapper for functionality exposed by {@link BluetoothGattServer}. For OS levels less than 5.0, this
 * is only useful by piggybacking on an existing {@link BleDevice} that is currently {@link BleDeviceState#CONNECTED}.
 * For OS levels 5.0 and up a {@link BleServer} is capable of acting as an independent peripheral.
 */
public final class BleServer extends BleNode
{
	/**
	 * Special value that is used in place of Java's built-in <code>null</code>.
	 */
	@Immutable
	public static final BleServer NULL = new BleServer(null, /*isNull=*/true);

	static final OutgoingListener NULL_OUTGOING_LISTENER = e -> {};

	private final P_ServerStateTracker m_stateTracker;
	final P_BleServer_Listeners m_listeners;
	final P_NativeServerWrapper m_nativeWrapper;
	private AdvertisingListener m_advertisingListener;
	private IncomingListener m_incomingListener;
	private OutgoingListener m_outgoingListener_default;
	private final boolean m_isNull;
	private BleNodeConfig m_config = null;
	private final P_ServerConnectionFailManager m_connectionFailMngr;
	private final P_ClientManager m_clientMngr;


	/*package*/ BleServer(final BleManager mngr, final boolean isNull)
	{
		super(mngr);

		m_isNull = isNull;

		if( isNull )
		{
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = null;
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
			m_clientMngr = new P_ClientManager(this);
		}
		else
		{
			m_stateTracker = new P_ServerStateTracker(this);
			m_listeners = new P_BleServer_Listeners(this);
			m_nativeWrapper = new P_NativeServerWrapper(this);
			m_connectionFailMngr = new P_ServerConnectionFailManager(this);
			m_clientMngr = new P_ClientManager(this);
			m_config = mngr.getConfigClone();
			m_config.reconnectFilter = new DefaultServerReconnectFilter();
		}
	}

	@Override protected final PA_ServiceManager newServiceManager()
	{
		return new P_ServerServiceManager(this);
	}

	/**
	 * Optionally sets overrides for any custom options given to {@link BleManager#get(android.content.Context, BleManagerConfig)}
	 * for this individual server.
	 */
	public final void setConfig(final BleNodeConfig config_nullable)
	{
		m_config = config_nullable == null ? null : config_nullable.clone();
	}

	@Override /*package*/ final BleNodeConfig conf_node()
	{
		return m_config != null ? m_config : conf_mngr();
	}
	
	/**
	 * Set a listener here to be notified whenever this server's state changes in relation to a specific client.
	 */
	public final void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) final ServerStateListener listener_nullable)
	{
		m_stateTracker.setListener(listener_nullable);
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable)
	{
		m_incomingListener = listener_nullable;
	}

	/**
	 * Set a listener here to override any listener provided previously and provide a default backup that will be called
	 * after any listener provided to {@link #addService(BleService, AddServiceListener)}.
	 */
	public final void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) final AddServiceListener listener_nullable)
	{
		serviceMngr_server().setListener(listener_nullable);
	}

	public final void setListener_Advertising(@Nullable(Nullable.Prevalence.NORMAL) final AdvertisingListener listener_nullable)
	{
		m_advertisingListener = listener_nullable;
	}

	public final @Nullable(Nullable.Prevalence.RARE)
	AdvertisingListener getListener_Advertise()
	{
		return m_advertisingListener;
	}

	/**
	 * Returns the listener provided to {@link #setListener_Incoming(IncomingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.RARE) IncomingListener getListener_Incoming()
	{
		return m_incomingListener;
	}

	/**
	 * This is a default catch-all convenience listener that will be called after any listener provided through
	 * the static methods of {@link IncomingListener.Please} such as {@link IncomingListener.Please#respondWithSuccess(OutgoingListener)}.
	 *
	 * @see BleManager#setListener_Outgoing(OutgoingListener)
	 */
	public final void setListener_Outgoing(final OutgoingListener listener)
	{
		m_outgoingListener_default = listener;
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_ReconnectFilter(final ServerReconnectFilter listener)
	{
		m_connectionFailMngr.setListener(listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data)
	{
		return sendIndication(macAddress, null, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendIndication(macAddress, (UUID) null, charUuid, data, listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData)
	{
		return sendIndication(macAddress, null, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendIndication(macAddress, (UUID) null, charUuid, futureData, listener);
	}

	/**
	 * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
	{
		return sendIndication(macAddress, serviceUuid, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Same as {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)} but sends an indication instead.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/true);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data)
	{
		return sendNotification(macAddress, null, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendNotification(macAddress, (UUID) null, charUuid, data, listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, data, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData)
	{
		return sendNotification(macAddress, null, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification(macAddress, (UUID) null, charUuid, futureData, listener);
	}

	/**
	 * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
	{
		return sendNotification(macAddress, serviceUuid, charUuid, futureData, (OutgoingListener) null);
	}

	/**
	 * Use this method to send a notification to the client device with the given mac address to the given characteristic {@link UUID}.
	 * If there is any kind of "early-out" issue then this method will return a {@link OutgoingListener.OutgoingEvent} in addition
	 * to passing it through the listener. Otherwise this method will return an instance with {@link OutgoingListener.OutgoingEvent#isNull()} being
	 * <code>true</code>.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/false);
	}

	private OutgoingListener.OutgoingEvent sendNotification_private(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData, final OutgoingListener listener, final boolean isIndication)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		final BluetoothDevice nativeDevice = newNativeDevice(macAddress_normalized).getNativeDevice();

		if( isNull() )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NULL_SERVER);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		if( !is(macAddress_normalized, CONNECTED ) )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NOT_CONNECTED);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		final BluetoothGattCharacteristic char_native = getNativeCharacteristic(serviceUuid, charUuid);

		if( char_native == null )
		{
			final OutgoingListener.OutgoingEvent e = OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid, futureData, OutgoingListener.Status.NO_MATCHING_TARGET);

			invokeOutgoingListeners(e, listener);

			return e;
		}

		final boolean confirm = isIndication;
		final P_Task_SendNotification task = new P_Task_SendNotification(this, nativeDevice, serviceUuid, charUuid, futureData, confirm, listener);
		taskManager().add(task);

		return OutgoingListener.OutgoingEvent.NULL__NOTIFICATION(this, nativeDevice, serviceUuid, charUuid);
	}

	/**
	 * Checks to see if the device is running an Android OS which supports
	 * advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByAndroidVersion()}.
	 */
	public final boolean isAdvertisingSupportedByAndroidVersion()
	{
		return getManager().isAdvertisingSupportedByAndroidVersion();
	}

	/**
	 * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
	 */
	public final boolean isAdvertisingSupportedByChipset()
	{
		return getManager().isAdvertisingSupportedByChipset();
	}

	/**
	 * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
	 */
	public final boolean isAdvertisingSupported()
	{
		return getManager().isAdvertisingSupported();
	}

	/**
	 * Checks to see if the device is currently advertising.
	 */
	public final boolean isAdvertising()
	{
		return getManager().getTaskManager().isCurrentOrInQueue(P_Task_Advertise.class, getManager());
	}

	/**
	 * Checks to see if the device is currently advertising the given {@link UUID}.
	 */
	public final boolean isAdvertising(UUID serviceUuid)
	{
		if (Utils.isLollipop())
		{
			P_Task_Advertise adtask = getManager().getTaskManager().get(P_Task_Advertise.class, getManager());
			if (adtask != null)
			{
				return adtask.getPacket().hasUuid(serviceUuid);
			}
		}
		return false;
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, serviceData));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, serviceData, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleAdvertisingPacket.Option... options)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuid), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleAdvertisingPacket(serviceUuids), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advPacket)
	{
		return startAdvertising(advPacket, null);
	}

	/**
	 * Overload of {@link #startAdvertising(BleAdvertisingPacket, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advPacket, AdvertisingListener listener)
	{
		return startAdvertising(advPacket, new BleAdvertisingSettings(BleAdvertisingMode.AUTO, BleTransmissionPower.MEDIUM, Interval.ZERO), listener);
	}

	/**
	 * Starts advertising serviceUuids with the information supplied in {@link BleAdvertisingPacket}. Note that this will
	 * only work for devices on Lollipop, or above. Even then, not every device supports advertising. Use
	 * {@link BleManager#isAdvertisingSupported()} to check to see if the phone supports it.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleAdvertisingPacket advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		if (isNull())
		{
			getManager().getLogger().e(BleServer.class.getSimpleName() + " is null!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.NULL_SERVER);
		}

		if (!isAdvertisingSupportedByAndroidVersion())
		{
			getManager().getLogger().e("Advertising NOT supported on android OS's less than Lollipop!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.ANDROID_VERSION_NOT_SUPPORTED);
		}

		if (!isAdvertisingSupportedByChipset())
		{
			getManager().getLogger().e("Advertising NOT supported by current device's chipset!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.CHIPSET_NOT_SUPPORTED);
		}

		if (!getManager().is(BleManagerState.ON))
		{
			getManager().getLogger().e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.BLE_NOT_ON);
		}

		final P_Task_Advertise adTask = getManager().getTaskManager().get(P_Task_Advertise.class, getManager());
		if (adTask != null)
		{
			getManager().getLogger().w(BleServer.class.getSimpleName() + " is already advertising!");

			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.ALREADY_STARTED);
		}
		else
		{
			getManager().ASSERT(!getManager().getTaskManager().isCurrentOrInQueue(P_Task_Advertise.class, getManager()));

			getManager().getTaskManager().add(new P_Task_Advertise(this, advertisePacket, settings, listener));
			return new AdvertisingListener.AdvertisingEvent(this, AdvertisingListener.Status.NULL);
		}
	}

	/**
	 * Stops the server from advertising.
	 */
	public final void stopAdvertising()
	{
		if (Utils.isLollipop())
		{

			final P_Task_Advertise adTask = getManager().getTaskManager().get(P_Task_Advertise.class, getManager());
			if (adTask != null)
			{
				adTask.stopAdvertising();
				adTask.clearFromQueue();
			}
			getManager().ASSERT(!getManager().getTaskManager().isCurrentOrInQueue(P_Task_Advertise.class, getManager()));
		}
	}

	/**
	 * Returns the name this {@link BleServer} is using (and will be advertised as, if applicable).
	 */
	public final String getName()
	{
		return getManager().managerLayer().getName();
	}

	/**
	 * Set the name you wish this {@link BleServer} to be known as. This will affect how other devices see this server, and sets the name
	 * on the lower level {@link BluetoothAdapter}. If you DO change this, please be aware this will affect everything, including apps outside
	 * of your own. It's probably best NOT to use this, but it's here for flexibility.
	 */
	@Advanced
	public final void setName(String name)
	{
		getManager().managerLayer().setName(name);
	}

    /**
     * Provides just-in-case lower-level access to the native server instance.
     * See similar warning for {@link BleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
    {
        return m_nativeWrapper.getNative().getNativeServer();
    }

	/**
	 * Provides just-in-case access to the abstracted server instance.
	 * See similar warning for {@link BleDevice#getNative()}.
	 */
	@Advanced
	public final @Nullable(Nullable.Prevalence.RARE) P_NativeServerLayer getNativeLayer()
	{
		return m_nativeWrapper.getNative();
	}

	/**
	 * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
	 *
	 * @see BleServerState
	 */
	@Advanced
	public final int getStateMask(final String macAddress)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return m_stateTracker.getStateMask(macAddress_normalized);
	}

	/**
	 * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask(String)}.
	 *
	 * @see #isAll(String, int)
	 */
	public final boolean isAny(final String macAddress, final int mask_BleServerState)
	{
		return (getStateMask(macAddress) & mask_BleServerState) != 0x0;
	}

	/**
	 * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask(String)}.
	 *
	 * @see #isAny(String, int)
	 *
	 */
	public final boolean isAll(final String macAddress, final int mask_BleServerState)
	{
		return (getStateMask(macAddress) & mask_BleServerState) == mask_BleServerState;
	}

	/**
	 * Returns true if the given client is in the state provided.
	 */
	public final boolean is(final String macAddress, final BleServerState state)
	{
		return state.overlaps(getStateMask(macAddress));
	}

	/**
	 * Returns true if the given client is in any of the states provided.
	 */
	public final boolean isAny(final String macAddress, final BleServerState ... states )
	{
		final int stateMask = getStateMask(macAddress);

		for( int i = 0; i < states.length; i++ )
		{
			if( states[i].overlaps(stateMask) )  return true;
		}

		return false;
	}

	/**
	 * Overload of {@link #connect(String, ServerStateListener, ServerReconnectFilter)} with no listeners.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress)
	{
		return connect(macAddress, null, null);
	}

	/**
	 * Overload of {@link #connect(String, ServerStateListener, ServerReconnectFilter)} with only one listener.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerStateListener stateListener)
	{
		return connect(macAddress, stateListener, null);
	}

	/**
	 * Overload of {@link #connect(String, ServerStateListener, ServerReconnectFilter)} with only one listener.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerReconnectFilter connectionFailListener)
	{
		return connect(macAddress, null, connectionFailListener);
	}

	/**
	 * Connect to the given client mac address and provided listeners that are shorthand for calling {@link #setListener_State(ServerStateListener)}
	 * {@link #setListener_ReconnectFilter(ServerReconnectFilter)}.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerStateListener stateListener, final ServerReconnectFilter connectionFailListener)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return connect_internal(newNativeDevice(macAddress_normalized).getNativeDevice(), stateListener, connectionFailListener);
	}

	/*package*/ final ServerReconnectFilter.ConnectFailEvent connect_internal(final BluetoothDevice nativeDevice)
	{
		return connect_internal(nativeDevice, null, null);
	}

	/*package*/ final ServerReconnectFilter.ConnectFailEvent connect_internal(final BluetoothDevice nativeDevice, final ServerStateListener stateListener, final ServerReconnectFilter connectionFailListener)
	{
		m_nativeWrapper.clearImplicitDisconnectIgnoring(nativeDevice.getAddress());

		if( stateListener != null )
		{
			setListener_State(stateListener);
		}

		if( connectionFailListener != null )
		{
			setListener_ReconnectFilter(connectionFailListener);
		}

		if( isNull() )
		{
			final ServerReconnectFilter.ConnectFailEvent e = ServerReconnectFilter.ConnectFailEvent.EARLY_OUT(this, nativeDevice, ServerReconnectFilter.Status.NULL_SERVER);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		m_connectionFailMngr.onExplicitConnectionStarted(nativeDevice.getAddress());

		if( isAny(nativeDevice.getAddress(), CONNECTING, CONNECTED) )
		{
			final ServerReconnectFilter.ConnectFailEvent e = ServerReconnectFilter.ConnectFailEvent.EARLY_OUT(this, nativeDevice, ServerReconnectFilter.Status.ALREADY_CONNECTING_OR_CONNECTED);

			m_connectionFailMngr.invokeCallback(e);

			return e;
		}

		m_clientMngr.onConnecting(nativeDevice.getAddress());

		final P_Task_ConnectServer task = new P_Task_ConnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
		taskManager().add(task);

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		return ServerReconnectFilter.ConnectFailEvent.NULL(this, nativeDevice);
	}

	private P_NativeDeviceLayer newNativeDevice(final String macAddress)
	{
		final BleManager mngr = getManager();

		return mngr == null ? P_NativeDeviceLayer.NULL : mngr.newNativeDevice(macAddress);
	}

	public final boolean disconnect(final String macAddress)
	{
		final String macAddress_normalized = getManager().normalizeMacAddress(macAddress);

		return disconnect_private(macAddress_normalized, ServerReconnectFilter.Status.CANCELLED_FROM_DISCONNECT, ChangeIntent.INTENTIONAL);
	}

	private boolean disconnect_private(final String macAddress, final ServerReconnectFilter.Status status_connectionFail, final ChangeIntent intent)
	{
		final boolean addTask = true;

		m_connectionFailMngr.onExplicitDisconnect(macAddress);

		if( is(macAddress, DISCONNECTED) )  return false;

		final BleServerState oldConnectionState = m_stateTracker.getOldConnectionState(macAddress);

		final P_NativeDeviceLayer nativeDevice = newNativeDevice(macAddress);

		if( addTask )
		{
			//--- DRK > Purposely doing explicit=true here without regarding the intent.
			final boolean explicit = true;
			final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice.getNativeDevice(), m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
			taskManager().add(task);
		}

		m_stateTracker.doStateTransition(macAddress, oldConnectionState /* ==> */, BleServerState.DISCONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		if( oldConnectionState == CONNECTING )
		{
			m_connectionFailMngr.onNativeConnectFail(nativeDevice.getNativeDevice(), status_connectionFail, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}

		return true;
	}

	/*package*/ final void disconnect_internal(final AddServiceListener.Status status_serviceAdd, final ServerReconnectFilter.Status status_connectionFail, final ChangeIntent intent)
	{
		stopAdvertising();

		getClients(new ForEach_Void<String>()
		{
			@Override public void next(final String next)
			{
				disconnect_private(next, status_connectionFail, intent);

				m_nativeWrapper.ignoreNextImplicitDisconnect(next);
			}

		}, CONNECTING, CONNECTED);

		m_nativeWrapper.closeServer();

		serviceMngr_server().removeAll(status_serviceAdd);
	}

	/**
	 * Disconnects this server completely, disconnecting all connected clients and shutting things down.
	 * To disconnect individual clients use {@link #disconnect(String)}.
	 */
	public final void disconnect()
	{
		disconnect_internal(AddServiceListener.Status.CANCELLED_FROM_DISCONNECT, ServerReconnectFilter.Status.CANCELLED_FROM_DISCONNECT, ChangeIntent.INTENTIONAL);
	}

	@Override public final boolean isNull()
	{
		return m_isNull;
	}

	/*package*/ final void onNativeConnecting_implicit(final String macAddress)
	{
		m_clientMngr.onConnecting(macAddress);

		m_stateTracker.doStateTransition(macAddress, BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/*package*/ final void onNativeConnect(final String macAddress, final boolean explicit)
	{
		m_clientMngr.onConnected(macAddress);

		final ChangeIntent intent = explicit ? ChangeIntent.INTENTIONAL : ChangeIntent.UNINTENTIONAL;

		//--- DRK > Testing and source code inspection reveals that it's impossible for the native stack to report server->client CONNECTING.
		//---		In other words for both implicit and explicit connects it always jumps from DISCONNECTED to CONNECTED.
		//---		For explicit connects through SweetBlue we can thus fake the CONNECTING state cause we know a task was in the queue, etc.
		//---		For implicit connects the decision is made here to reflect what happens in the native stack, cause as far as SweetBlue
		//---		is concerned we were never in the CONNECTING state either.
		final BleServerState previousState = explicit ? BleServerState.CONNECTING : BleServerState.DISCONNECTED;

		m_stateTracker.doStateTransition(macAddress, previousState /* ==> */, BleServerState.CONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	/*package*/ final void onNativeConnectFail(final BluetoothDevice nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus)
	{
		if( status == ServerReconnectFilter.Status.TIMED_OUT )
		{
			final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice, m_listeners.m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
			taskManager().add(task);
		}

		m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.CONNECTING /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		m_connectionFailMngr.onNativeConnectFail(nativeDevice, status, gattStatus);
	}

	/*package*/ final void onNativeDisconnect( final String macAddress, final boolean explicit, final int gattStatus)
	{
		final boolean ignore = m_nativeWrapper.shouldIgnoreImplicitDisconnect(macAddress);

		if( explicit == false && ignore == false )
		{
			m_stateTracker.doStateTransition(macAddress, BleServerState.CONNECTED /* ==> */, BleServerState.DISCONNECTED, ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// explicit case gets handled immediately by the disconnect method.
		}
	}

	final void invokeAdvertiseListeners(AdvertisingListener.Status result, AdvertisingListener listener)
	{
		final AdvertisingListener.AdvertisingEvent event = new AdvertisingListener.AdvertisingEvent(this, result);
		if (listener != null)
		{
			listener.onEvent(event);
		}
		if (m_advertisingListener != null)
		{
			m_advertisingListener.onEvent(event);
		}
		if (getManager().m_advertisingListener != null)
		{
			getManager().m_advertisingListener.onEvent(event);
		}
	}

	/**
	 * Does a referential equality check on the two servers.
	 */
	public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleServer server_nullable)
	{
		if (server_nullable == null)												return false;
		if (server_nullable == this)												return true;
		if (server_nullable.getNativeLayer().isServerNull() || this.getNativeLayer().isServerNull() )		return false;
		if( this.isNull() && server_nullable.isNull() )								return true;

		return server_nullable == this;
	}

	/**
	 * Returns {@link #equals(BleServer)} if object is an instance of {@link BleServer}. Otherwise calls super.
	 *
	 * @see BleServer#equals(BleServer)
	 */
	@Override public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
	{
		if( object_nullable == null )  return false;

		if (object_nullable instanceof BleServer)
		{
			final BleServer object_cast = (BleServer) object_nullable;

			return this.equals(object_cast);
		}

		return false;
	}

	/*package*/ final void invokeOutgoingListeners(final OutgoingListener.OutgoingEvent e, final OutgoingListener listener_specific_nullable)
	{
		if( listener_specific_nullable != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (listener_specific_nullable != null)
					{
						listener_specific_nullable.onEvent(e);
					}
				}
			});
		}

		if( m_outgoingListener_default != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (m_outgoingListener_default != null)
					{
						m_outgoingListener_default.onEvent(e);
					}
				}
			});
		}

		if( getManager().m_defaultServerOutgoingListener != null )
		{
			getManager().getPostManager().postCallback(new Runnable()
			{
				@Override public void run()
				{
					if (getManager().m_defaultServerOutgoingListener != null)
					{
						getManager().m_defaultServerOutgoingListener.onEvent(e);
					}
				}
			});
		}
	}

	/**
	 * Overload of {@link #addService(BleService, AddServiceListener)} without the listener.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AddServiceListener.ServiceAddEvent addService(final BleService service)
	{
		return this.addService(service, null);
	}

	/**
	 * Starts the process of adding a service to this server. The provided listener will be called when the service is added or there is a problem.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AddServiceListener.ServiceAddEvent addService(final BleService service, final AddServiceListener listener)
	{
		return serviceMngr_server().addService(service, listener);
	}

	// TODO - This should become public for v3. For now, it will stay package private. (We may want to use BleService, etc in the GattDatabase class)
	final AddServiceListener.ServiceAddEvent addService(final BluetoothGattService service, final AddServiceListener listener)
	{
		return serviceMngr_server().addService_native(service, listener);
	}

	/**
	 * Remove any service previously provided to {@link #addService(BleService, AddServiceListener)} or overloads. This can be safely called
	 * even if the call to {@link #addService(BleService, AddServiceListener)} hasn't resulted in a callback to the provided listener yet, in which
	 * case it will be called with {@link AddServiceListener.Status#CANCELLED_FROM_REMOVAL}.
	 */
	public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothGattService removeService(final UUID serviceUuid)
	{
		return serviceMngr_server().remove(serviceUuid);
	}

	/**
	 * Convenience to remove all services previously added with {@link #addService(BleService, AddServiceListener)} (or overloads). This is slightly more performant too.
	 */
	public final void removeAllServices()
	{
		serviceMngr_server().removeAll(AddServiceListener.Status.CANCELLED_FROM_REMOVAL);
	}

	/**
	 * Offers a more "functional" means of iterating through the internal list of clients instead of
	 * using {@link #getClients()} or {@link #getClients_List()}.
	 */
	public final void getClients(final ForEach_Void<String> forEach)
	{
		m_clientMngr.getClients(forEach, 0x0);
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in the given state provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState state)
	{
		m_clientMngr.getClients(forEach, state.bit());
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState ... states)
	{
		m_clientMngr.getClients(forEach, BleServerState.toBits(states));
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach)
	{
		m_clientMngr.getClients(forEach, 0x0);
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void, BleServerState)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState state)
	{
		m_clientMngr.getClients(forEach, state.bit());
	}

	/**
	 * Same as {@link #getClients(ForEach_Breakable)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState ... states)
	{
		m_clientMngr.getClients(forEach, BleServerState.toBits(states));
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients()
	{
		return m_clientMngr.getClients(0x0);
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState state)
	{
		return m_clientMngr.getClients(state.bit());
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState ... states)
	{
		return m_clientMngr.getClients(BleServerState.toBits(states));
	}

	/**
	 * Overload of {@link #getClients()} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List()
	{
		return m_clientMngr.getClients_List(0x0);
	}

	/**
	 * Overload of {@link #getClients(BleServerState)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState state)
	{
		return m_clientMngr.getClients_List(state.bit());
	}

	/**
	 * Overload of {@link #getClients(BleServerState[])} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState ... states)
	{
		return m_clientMngr.getClients_List(BleServerState.toBits(states));
	}

	/**
	 * Returns the total number of clients this server is connecting or connected to (or previously so).
	 */
	public final int getClientCount()
	{
		return m_clientMngr.getClientCount();
	}

	/**
	 * Returns the number of clients that are in the current state.
	 */
	public final int getClientCount(final BleServerState state)
	{
		return m_clientMngr.getClientCount(state.bit());
	}

	/**
	 * Returns the number of clients that are in any of the given states.
	 */
	public final int getClientCount(final BleServerState ... states)
	{
		return m_clientMngr.getClientCount(BleServerState.toBits(states));
	}

	/**
	 * Returns <code>true</code> if this server has any connected or connecting clients (or previously so).
	 */
	public final boolean hasClients()
	{
		return getClientCount() > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in the given state.
	 */
	public final boolean hasClient(final BleServerState state)
	{
		return getClientCount(state) > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in any of the given states.
	 */
	public final boolean hasClient(final BleServerState ... states)
	{
		return getClientCount(states) > 0;
	}

	final P_ServerServiceManager serviceMngr_server()
	{
		return getServiceManager();
	}

	/**
	 * Pretty-prints the list of connecting or connected clients.
	 */
	public final String toString()
	{
		return this.getClass().getSimpleName() + " with " + m_clientMngr.getClientCount(BleServerState.toBits(CONNECTING, CONNECTED)) + " connected/ing clients.";
	}

	/**
	 * Returns the local mac address provided by {@link BluetoothAdapter#getAddress()}.
	 */
	@Override public final @Nullable(Nullable.Prevalence.NEVER) String getMacAddress()
	{
		return getManager().managerLayer().getAddress();
	}
}
