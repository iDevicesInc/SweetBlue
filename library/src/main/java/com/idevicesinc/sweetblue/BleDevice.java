package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.Status;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.Timing;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent;
import com.idevicesinc.sweetblue.BondFilter.CharacteristicEventType;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.P_Task_Bond.E_TransactionLockBehavior;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.impl.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.Distance;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.TimeEstimator;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Rssi;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_State;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import static com.idevicesinc.sweetblue.BleDeviceState.ADVERTISING;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATED;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATING;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING_OVERALL;
import static com.idevicesinc.sweetblue.BleDeviceState.DISCONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.DISCOVERED;
import static com.idevicesinc.sweetblue.BleDeviceState.DISCOVERING_SERVICES;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZED;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZING;
import static com.idevicesinc.sweetblue.BleDeviceState.PERFORMING_OTA;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_SHORT_TERM;
import static com.idevicesinc.sweetblue.BleDeviceState.RETRYING_BLE_CONNECTION;
import static com.idevicesinc.sweetblue.BleDeviceState.SERVICES_DISCOVERED;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.UNDISCOVERED;


/**
 * This is the one other class you will use the most besides {@link BleManager}.
 * It acts as a BLE-specific abstraction for the {@link BluetoothDevice} and
 * {@link BluetoothGatt} classes. It does everything you would expect, like
 * providing methods for connecting, reading/writing characteristics, enabling
 * notifications, etc.
 * <br><br>
 * Although instances of this class can be created explicitly through
 * {@link BleManager#newDevice(String, String)}, usually they're created
 * implicitly by {@link BleManager} as a result of a scanning operation (e.g.
 * {@link BleManager#startScan()}) and sent to you through
 * {@link DiscoveryListener#onEvent(Event)}.
 */
public final class BleDevice extends BleNode
{
    /**
     * Special value that is used in place of Java's built-in <code>null</code>.
     */
    @Immutable
    public static final BleDevice NULL = new BleDevice(null, P_NativeDeviceLayer.NULL, NULL_STRING(), NULL_STRING(), BleDeviceOrigin.EXPLICIT, null, /*isNull=*/true);

    static DeviceReconnectFilter DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultDeviceReconnectFilter();

    final P_NativeDeviceWrapper m_nativeWrapper;

    private double m_timeSinceLastDiscovery;
    private EpochTime m_lastDiscoveryTime = EpochTime.NULL;

    final P_BleDevice_Listeners m_listeners;
    private final P_DeviceStateTracker m_stateTracker;
    private final P_DeviceStateTracker m_stateTracker_shortTermReconnect;
    private final P_PollManager m_pollMngr;

    final P_TransactionManager m_txnMngr;
    private final P_ReconnectManager m_reconnectMngr_longTerm;
    private final P_ReconnectManager m_reconnectMngr_shortTerm;
    final P_ConnectionFailManager m_connectionFailMngr;
    private final P_RssiPollManager m_rssiPollMngr;
    private final P_RssiPollManager m_rssiPollMngr_auto;
    private final P_Task_Disconnect m_dummyDisconnectTask;
    private final P_HistoricalDataManager m_historicalDataMngr;
    final P_BondManager m_bondMngr;

    private final Stack<ReadWriteListener> m_readWriteListenerStack;
    private final Stack<NotificationListener> m_notificationListenerStack;
    private final Stack<DeviceConnectListener> m_connectListenerStack;
    WeakReference<DeviceConnectListener> m_ephemeralConnectListener;


    private TimeEstimator m_writeTimeEstimator;
    private TimeEstimator m_readTimeEstimator;

    private final PA_Task.I_StateListener m_taskStateListener;

    private final BleDeviceOrigin m_origin;
    private BleDeviceOrigin m_origin_latest;

    private BleConnectionPriority m_connectionPriority = BleConnectionPriority.MEDIUM;
    private int m_mtu = 0;
    private int m_rssi = 0;
    private int m_advertisingFlags = 0x0;
    private Integer m_knownTxPower = null;
    private byte[] m_scanRecord = P_Const.EMPTY_BYTE_ARRAY;

    private BleScanInfo m_scanInfo = new BleScanInfo();

    private boolean m_useAutoConnect = false;
    private boolean m_alwaysUseAutoConnect = false;
    private Boolean m_lastConnectOrDisconnectWasUserExplicit = null;
    private boolean m_lastDisconnectWasBecauseOfBleTurnOff = false;
    private boolean m_underwentPossibleImplicitBondingAttempt = false;

    private BleDeviceConfig m_config = null;
    private P_BleDeviceLayerManager m_layerManager;
    private P_NativeDeviceLayer m_deviceLayer;

    private BondListener.BondEvent m_nullBondEvent = null;
    private ReadWriteListener.ReadWriteEvent m_nullReadWriteEvent = null;
    private ConnectFailEvent m_nullConnectionFailEvent = null;

    private final boolean m_isNull;

    final P_ReliableWriteManager m_reliableWriteMngr;

    BleDevice(BleManager mngr, P_NativeDeviceLayer device_native, String name_normalized, String name_native, BleDeviceOrigin origin, BleDeviceConfig config_nullable, boolean isNull)
    {
        super(mngr);

        m_origin = origin;
        m_origin_latest = m_origin;
        m_isNull = isNull;

        m_readWriteListenerStack = new Stack<>();
        m_notificationListenerStack = new Stack<>();
        m_connectListenerStack = new Stack<>();

        m_deviceLayer = device_native;

        if (isNull)
        {
            m_rssiPollMngr = null;
            m_rssiPollMngr_auto = null;
            // setConfig(config_nullable);
            m_nativeWrapper = new P_NativeDeviceWrapper(this, m_deviceLayer, name_normalized, name_native);
            m_listeners = null;
            m_stateTracker = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/false);
            m_stateTracker_shortTermReconnect = null;
            m_bondMngr = new P_BondManager(this);
            m_pollMngr = new P_PollManager(this);
            m_txnMngr = new P_TransactionManager(this);
            m_taskStateListener = null;
            m_reconnectMngr_longTerm = null;
            m_reconnectMngr_shortTerm = null;
            m_connectionFailMngr = new P_ConnectionFailManager(this);
            m_dummyDisconnectTask = null;
            m_historicalDataMngr = null;
            m_reliableWriteMngr = null;
            stateTracker().set(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, true);
        }
        else
        {
            m_deviceLayer.updateBleDevice(this);
            m_rssiPollMngr = new P_RssiPollManager(this);
            m_rssiPollMngr_auto = new P_RssiPollManager(this);
            setConfig(config_nullable);
            m_nativeWrapper = new P_NativeDeviceWrapper(this, m_deviceLayer, name_normalized, name_native);
            m_listeners = new P_BleDevice_Listeners(this);
            m_stateTracker = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/false);
            m_stateTracker_shortTermReconnect = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/true);
            m_bondMngr = new P_BondManager(this);
            m_pollMngr = new P_PollManager(this);
            m_txnMngr = new P_TransactionManager(this);
            m_taskStateListener = m_listeners.m_taskStateListener;
            m_reconnectMngr_longTerm = new P_ReconnectManager(this, /*isShortTerm=*/false);
            m_reconnectMngr_shortTerm = new P_ReconnectManager(this, /*isShortTerm=*/true);
            m_connectionFailMngr = new P_ConnectionFailManager(this);
            m_dummyDisconnectTask = new P_Task_Disconnect(this, null, /*explicit=*/false, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING, /*cancellable=*/true);
            m_historicalDataMngr = new P_HistoricalDataManager(this, getMacAddress());
            m_reliableWriteMngr = new P_ReliableWriteManager(this);
            final Object[] bondStates = m_bondMngr.getNativeBondingStateOverrides();
            stateTracker().set(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, BleDeviceState.DISCONNECTED, true, bondStates);
        }
    }

    /**
     * Wrapper for {@link BluetoothGatt#beginReliableWrite()} - will return an event such that {@link ReadWriteEvent#isNull()} will
     * return <code>false</code> if there are no problems. After calling this you should do your {@link BleDevice#write(UUID, byte[])}
     * calls then call {@link #reliableWrite_execute()}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteEvent reliableWrite_begin(final ReadWriteListener listener)
    {

        return m_reliableWriteMngr.begin(listener);
    }

    /**
     * Wrapper for {@link BluetoothGatt#abortReliableWrite()} - will return an event such that {@link ReadWriteEvent#isNull()} will
     * return <code>false</code> if there are no problems. This call requires a previous call to {@link #reliableWrite_begin(ReadWriteListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteEvent reliableWrite_abort()
    {

        return m_reliableWriteMngr.abort();
    }

    /**
     * Wrapper for {@link BluetoothGatt#abortReliableWrite()} - will return an event such that {@link ReadWriteEvent#isNull()} will
     * return <code>false</code> if there are no problems. This call requires a previous call to {@link #reliableWrite_begin(ReadWriteListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteEvent reliableWrite_execute()
    {

        return m_reliableWriteMngr.execute();
    }

    /**
     * Returns a string of all the states this {@link BleDevice} is currently in.
     */
    public final String printState()
    {
        return stateTracker_main().toString();
    }

    @Override protected final PA_ServiceManager newServiceManager()
    {
        return new P_DeviceServiceManager(this);
    }

    final void notifyOfPossibleImplicitBondingAttempt()
    {
        m_underwentPossibleImplicitBondingAttempt = true;
    }

    final P_DeviceStateTracker stateTracker_main()
    {
        return m_stateTracker;
    }

    final void stateTracker_updateBoth(E_Intent intent, int status, Object... statesAndValues)
    {
        m_stateTracker_shortTermReconnect.update(intent, status, statesAndValues);
        stateTracker_main().update(intent, status, statesAndValues);
    }

    final P_DeviceStateTracker stateTracker()
    {
        if (stateTracker_main().checkBitMatch(BleDeviceState.RECONNECTING_SHORT_TERM, true))
        {
            return m_stateTracker_shortTermReconnect;
        }
        else
        {
            return stateTracker_main();
        }
    }

    final P_ReconnectManager reconnectMngr()
    {
        if (stateTracker_main().checkBitMatch(BleDeviceState.RECONNECTING_SHORT_TERM, true))
        {
            return m_reconnectMngr_shortTerm;
        }
        else
        {
            return m_reconnectMngr_longTerm;
        }
    }

    private void clear_discovery()
    {
        // clear_common();
        //
        // initEstimators();
    }

    private void clear_common()
    {
        m_connectionFailMngr.setListener(null);
        stateTracker_main().setListener(null);
        m_config = null;
    }

    private void clear_undiscovery()
    {
        // clear_common();

        m_lastDiscoveryTime = EpochTime.NULL;
    }

    /**
     * Optionally sets overrides for any custom options given to {@link BleManager#get(android.content.Context, BleManagerConfig)}
     * for this individual device.
     */
    public final void setConfig(@Nullable(Prevalence.RARE) BleDeviceConfig config_nullable)
    {
        if (isNull()) return;

        m_config = config_nullable == null ? null : config_nullable.clone();

        if (m_layerManager == null)
        {
            m_layerManager = new P_BleDeviceLayerManager(this, conf_mngr().newGattLayer(this), m_deviceLayer, conf_mngr().nativeManagerLayer);
        }

        initEstimators();

        //--- DRK > Not really sure how this config option should be
        // interpreted, but here's a first stab for now.
        //--- Fringe enough use case that I don't think it's really a big deal.
        boolean alwaysUseAutoConnect = BleDeviceConfig.bool(conf_device().alwaysUseAutoConnect, conf_mngr().alwaysUseAutoConnect);
        if (alwaysUseAutoConnect)
        {
            m_alwaysUseAutoConnect = m_useAutoConnect = true;
        }
        else
        {
            m_alwaysUseAutoConnect = false;
        }

        final Interval autoRssiPollRate = BleDeviceConfig.interval(conf_device().rssiAutoPollRate, conf_mngr().rssiAutoPollRate);

        if (!m_rssiPollMngr.isRunning() && !Interval.isDisabled(autoRssiPollRate))
        {
            m_rssiPollMngr_auto.start(autoRssiPollRate.secs(), null);
        }
        else
        {
            m_rssiPollMngr_auto.stop();
        }
    }

    private void initEstimators()
    {
        final Integer nForAverageRunningWriteTime = BleDeviceConfig.integer(conf_device().nForAverageRunningWriteTime, conf_mngr().nForAverageRunningWriteTime);
        m_writeTimeEstimator = nForAverageRunningWriteTime == null ? null : new TimeEstimator(nForAverageRunningWriteTime);

        final Integer nForAverageRunningReadTime = BleDeviceConfig.integer(conf_device().nForAverageRunningReadTime, conf_mngr().nForAverageRunningReadTime);
        m_readTimeEstimator = nForAverageRunningReadTime == null ? null : new TimeEstimator(nForAverageRunningReadTime);
    }

    /**
     * Return the {@link BleDeviceConfig} this device is set to use. If none has been set explicitly, then the instance
     * of {@link BleManagerConfig} is returned.
     */
    @Nullable(Prevalence.NEVER)
    public final BleDeviceConfig getConfig()
    {
        return conf_device();
    }

    final BleDeviceConfig conf_device()
    {
        return m_config != null ? m_config : conf_mngr();
    }

    @Override final BleNodeConfig conf_node()
    {
        return conf_device();
    }

    /**
     * How the device was originally created, either from scanning or explicit creation.
     * <br><br>
     * NOTE: That devices for which this returns {@link BleDeviceOrigin#EXPLICIT} may still be
     * {@link DiscoveryListener.LifeCycle#REDISCOVERED} through {@link BleManager#startScan()}.
     */
    public final BleDeviceOrigin getOrigin()
    {
        return m_origin;
    }

    /**
     * Returns the last time the device was {@link DiscoveryListener.LifeCycle#DISCOVERED}
     * or {@link DiscoveryListener.LifeCycle#REDISCOVERED}. If {@link #getOrigin()} returns
     * {@link BleDeviceOrigin#EXPLICIT} then this will return {@link EpochTime#NULL} unless or until
     * the device is {@link DiscoveryListener.LifeCycle#REDISCOVERED}.
     */
    public final EpochTime getLastDiscoveryTime()
    {
        return m_lastDiscoveryTime;
    }

    /**
     * This enum gives you an indication of the last interaction with a device across app sessions or in-app BLE
     * {@link BleManagerState#OFF}->{@link BleManagerState#ON} cycles or undiscovery->rediscovery, which
     * basically means how it was last {@link BleDeviceState#DISCONNECTED}.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#NULL}, then the last disconnect is unknown because
     * (a) device has never been seen before,
     * (b) reason for disconnect was app being killed and {@link BleDeviceConfig#manageLastDisconnectOnDisk} was <code>false</code>,
     * (c) app user cleared app data between app sessions or reinstalled the app.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}, then from a user experience perspective, the user may not have wanted
     * the disconnect to happen, and thus *probably* would want to be automatically connected again as soon as the device is discovered.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}, then the last reason the device was {@link BleDeviceState#DISCONNECTED} was because
     * {@link BleDevice#disconnect()} was called, which most-likely means the user doesn't want to automatically connect to this device again.
     * <br><br>
     * See further explanation at {@link BleDeviceConfig#manageLastDisconnectOnDisk}.
     */
    @Advanced
    public final State.ChangeIntent getLastDisconnectIntent()
    {
        if (isNull()) return State.ChangeIntent.NULL;

        boolean hitDisk = BleDeviceConfig.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
        State.ChangeIntent lastDisconnect = getManager().m_diskOptionsMngr.loadLastDisconnect(getMacAddress(), hitDisk);

        return lastDisconnect;
    }

    /**
     * Set a listener here to be notified whenever this device's state changes. NOTE: This will clear the stack of {@link DeviceStateListener}s, and set
     * the one provided here to be the only one in the stack.
     *
     * If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_State(@Nullable(Prevalence.NORMAL) DeviceStateListener listener_nullable)
    {
        if (isNull()) return;

        stateTracker_main().clearListenerStack();
        if (listener_nullable != null)
            stateTracker_main().setListener(listener_nullable);
    }

    /**
     * Push a new {@link DeviceStateListener} onto the stack. This new listener will be the one events are dispatched to, until
     * {@link #popListener_State()} is called.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_State(@Nullable(Prevalence.NEVER) DeviceStateListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        stateTracker_main().pushListener(listener);
    }

    /**
     * Pop the current {@link DeviceStateListener} out of the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_State()
    {
        if (isNull()) return false;

        return stateTracker_main().popListener();
    }

    /**
     * Returns the current {@link DeviceStateListener} being used (the top of the stack). This can return <code>null</code> if there
     * are no listeners in the stack.
     */
    public final @Nullable(Prevalence.NORMAL) DeviceStateListener getListener_State()
    {
        return stateTracker_main().getListener();
    }

    /**
     * Set a listener here to be notified whenever this device connects, or gets disconnected. NOTE: This will clear the stack of {@link DeviceStateListener}s, and set
     * the one provided here to be the only one in the stack.
     *
     * If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_Connect(@Nullable(Prevalence.NORMAL) DeviceConnectListener listener)
    {
        if (isNull()) return;

        m_connectListenerStack.clear();
        if (listener != null)
            m_connectListenerStack.push(listener);
    }

    /**
     * Push a new {@link DeviceConnectListener} onto the stack. This new listener will be the one events are dispatched to, until
     * {@link #popListener_State()} is called.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_Connect(@Nullable(Prevalence.NEVER) DeviceConnectListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_connectListenerStack.push(listener);
    }

    /**
     * Pop the current {@link DeviceConnectListener} out of the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_Connect()
    {
        if (isNull()) return false;

        if (m_connectListenerStack.empty()) return false;

        m_connectListenerStack.pop();

        return true;
    }

    /**
     * Returns the current {@link DeviceConnectListener} being used (the top of the stack). This can return <code>null</code> if there
     * are no listeners in the stack.
     */
    public final @Nullable(Prevalence.NORMAL) DeviceConnectListener getListener_Connect()
    {
        if (m_connectListenerStack.empty()) return null;

        return m_connectListenerStack.peek();
    }

    /**
     * Set a listener here to be notified whenever a connection fails and to
     * have control over retry behavior. NOTE: This will clear the stack of {@link DeviceReconnectFilter}s, and set
     * the one provided here to be the only one in the stack. If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_Reconnect(@Nullable(Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
    {
        if (isNull()) return;

        m_connectionFailMngr.clearListenerStack();
        if (listener_nullable != null)
            m_connectionFailMngr.setListener(listener_nullable);
    }

    /**
     * Pushes the provided {@link DeviceReconnectFilter} on to the top of the stack of listeners.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_ConnectionFail(@Nullable(Prevalence.NEVER) DeviceReconnectFilter listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_connectionFailMngr.pushListener(listener);
    }

    /**
     * Pops the current {@link DeviceReconnectFilter} off the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_ConnectionFail()
    {
        if (isNull()) return false;

        return m_connectionFailMngr.popListener();
    }

    /**
     * Returns the current {@link DeviceReconnectFilter} being used (the top of the stack). This can return <code>null</code> if there
     * are no listeners in the stack.
     */
    public final @Nullable(Prevalence.NORMAL) DeviceReconnectFilter getListener_ConnectionFail()
    {
        return m_connectionFailMngr.getListener();
    }

    /**
     * Set a listener here to be notified whenever a bond attempt succeeds. This
     * will catch attempts to bond both through {@link #bond()} and when bonding
     * through the operating system settings or from other apps.
     */
    public final void setListener_Bond(@Nullable(Prevalence.NORMAL) BondListener listener_nullable)
    {
        if (isNull()) return;

        m_bondMngr.setListener(listener_nullable);
    }

    /**
     * Sets a default backup {@link ReadWriteListener} that will be called for all calls to {@link #read(UUID, ReadWriteListener)},
     * {@link #write(UUID, byte[], ReadWriteListener)}, {@link #enableNotify(UUID, ReadWriteListener)}, etc.
     * NOTE: This will clear the stack of {@link ReadWriteListener}s, and set
     * the one provided here to be the only one in the stack. If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_ReadWrite(@Nullable(Prevalence.NORMAL) final ReadWriteListener listener_nullable)
    {
        if (isNull()) return;

        m_readWriteListenerStack.clear();

        if (listener_nullable != null)
            m_readWriteListenerStack.push(listener_nullable);
    }

    /**
     * Pushes the provided {@link ReadWriteListener} on to the top of the stack of listeners.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_ReadWrite(@Nullable(Prevalence.NEVER) ReadWriteListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_readWriteListenerStack.push(listener);
    }

    /**
     * Pops the current {@link ReadWriteListener} off the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_ReadWrite()
    {
        if (isNull()) return false;

        if (m_readWriteListenerStack.empty())
            return false;

        m_readWriteListenerStack.pop();
        return true;
    }

    /**
     * Returns the current {@link ReadWriteListener} being used (the top of the stack). This can return <code>null</code> if there
     * are no listeners in the stack.
     */
    public final @Nullable(Prevalence.NORMAL) ReadWriteListener getListener_ReadWrite()
    {
        if (isNull() || m_readWriteListenerStack.empty())
            return null;
        return m_readWriteListenerStack.peek();
    }


    /**
     * Sets a default {@link NotificationListener} that will be called when receiving notifications, or indications. This listener will also
     * be called when toggling notifications. This does NOT replace {@link com.idevicesinc.sweetblue.ReadWriteListener}, just adds to it. If
     * a default {@link com.idevicesinc.sweetblue.ReadWriteListener} has been set, it will still fire in addition to this listener.
     * NOTE: This will clear the stack of {@link ReadWriteListener}s, and set
     * the one provided here to be the only one in the stack. If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_Notification(@Nullable(Prevalence.NORMAL) NotificationListener listener_nullable)
    {
        if (isNull()) return;

        m_notificationListenerStack.clear();

        if (listener_nullable != null)
            m_notificationListenerStack.push(listener_nullable);
    }

    /**
     * Pushes the provided {@link NotificationListener} on to the top of the stack of listeners.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_Notification(@Nullable(Prevalence.NEVER) NotificationListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_notificationListenerStack.push(listener);
    }

    /**
     * Pops the current {@link NotificationListener} off the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_Notification()
    {
        if (isNull()) return false;

        if (m_notificationListenerStack.empty())
            return false;

        m_notificationListenerStack.pop();
        return true;
    }

    /**
     * Returns the current {@link NotificationListener} being used (the top of the stack). This can return <code>null</code> if there
     * are no listeners in the stack.
     */
    public final @Nullable(Prevalence.NORMAL) NotificationListener getListener_Notification()
    {
        if (isNull() || m_notificationListenerStack.empty()) return null;

        return m_notificationListenerStack.peek();
    }

    /**
     * Sets a default backup {@link BleNode.HistoricalDataLoadListener} that will be invoked
     * for all historical data loads to memory for all uuids.
     */
    public final void setListener_HistoricalDataLoad(@Nullable(Prevalence.NORMAL) final BleNode.HistoricalDataLoadListener listener_nullable)
    {
        if (isNull()) return;

        m_historicalDataMngr.setListener(listener_nullable);
    }

    /**
     * Returns the connection failure retry count during a retry loop. Basic example use case is to provide a callback to
     * {@link #setListener_Reconnect(DeviceReconnectFilter)} and update your application's UI with this method's return value downstream of your
     * {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} override.
     */
    public final int getConnectionRetryCount()
    {
        if (isNull()) return 0;

        return m_connectionFailMngr.getRetryCount();
    }

    /**
     * Returns the bitwise state mask representation of {@link BleDeviceState} for this device.
     *
     * @see BleDeviceState
     */
    @Advanced
    public final int getStateMask()
    {
        return stateTracker_main().getState();
    }

    /**
     * Returns the actual native state mask representation of the {@link BleDeviceState} for this device.
     * The main purpose of this is to reflect what's going on under the hood while {@link BleDevice#is(BleDeviceState)}
     * with {@link BleDeviceState#RECONNECTING_SHORT_TERM} is <code>true</code>.
     */
    @Advanced
    public final int getNativeStateMask()
    {
        return stateTracker().getState();
    }

    /**
     * See similar explanation for {@link #getAverageWriteTime()}.
     *
     * @see #getAverageWriteTime()
     * @see BleManagerConfig#nForAverageRunningReadTime
     */
    @Advanced
    public final Interval getAverageReadTime()
    {
        return m_readTimeEstimator != null ? Interval.secs(m_readTimeEstimator.getRunningAverage()) : Interval.ZERO;
    }

    /**
     * Returns the average round trip time in seconds for all write operations started with {@link #write(UUID, byte[])} or
     * {@link #write(UUID, byte[], ReadWriteListener)}. This is a running average with N being defined by
     * {@link BleManagerConfig#nForAverageRunningWriteTime}. This may be useful for estimating how long a series of
     * reads and/or writes will take. For example for displaying the estimated time remaining for a firmware update.
     */
    @Advanced
    public final Interval getAverageWriteTime()
    {
        return m_writeTimeEstimator != null ? Interval.secs(m_writeTimeEstimator.getRunningAverage()) : Interval.ZERO;
    }

    /**
     * Returns the raw RSSI retrieved from when the device was discovered,
     * rediscovered, or when you call {@link #readRssi()} or {@link #startRssiPoll(Interval)}.
     *
     * @see #getDistance()
     */
    public final int getRssi()
    {
        return m_rssi;
    }

    /**
     * Raw RSSI from {@link #getRssi()} is a little cryptic, so this gives you a friendly 0%-100% value for signal strength.
     */
    public final Percent getRssiPercent()
    {
        if (isNull())
        {
            return Percent.ZERO;
        }
        else
        {
            final int rssi_min = BleDeviceConfig.integer(conf_device().rssi_min, conf_mngr().rssi_min, BleDeviceConfig.DEFAULT_RSSI_MIN);
            final int rssi_max = BleDeviceConfig.integer(conf_device().rssi_max, conf_mngr().rssi_max, BleDeviceConfig.DEFAULT_RSSI_MAX);
            final double percent = Utils_Rssi.percent(getRssi(), rssi_min, rssi_max);

            return Percent.fromDouble_clamped(percent);
        }
    }

    /**
     * Returns the approximate distance in meters based on {@link #getRssi()} and
     * {@link #getTxPower()}. NOTE: the higher the distance, the less the accuracy.
     */
    public final Distance getDistance()
    {
        if (isNull())
        {
            return Distance.INVALID;
        }
        else
        {
            return Distance.meters(Utils_Rssi.distance(getTxPower(), getRssi()));
        }
    }

    /**
     * Returns the calibrated transmission power of the device. If this can't be
     * figured out from the device itself then it backs up to the value provided
     * in {@link BleDeviceConfig#defaultTxPower}.
     *
     * @see BleDeviceConfig#defaultTxPower
     */
    @Advanced
    public final int getTxPower()
    {
        if (isNull())
        {
            return BleNodeConfig.INVALID_TX_POWER;
        }
        else
        {
            if (m_knownTxPower != null)
            {
                return m_knownTxPower;
            }
            else
            {
                final Integer defaultTxPower = BleDeviceConfig.integer(conf_device().defaultTxPower, conf_mngr().defaultTxPower);
                final int toReturn = defaultTxPower == null || defaultTxPower == BleNodeConfig.INVALID_TX_POWER ? BleDeviceConfig.DEFAULT_TX_POWER : defaultTxPower;

                return toReturn;
            }
        }
    }

    /**
     * Returns the scan record from when we discovered the device. May be empty but never <code>null</code>.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) byte[] getScanRecord()
    {
        return m_scanRecord;
    }

    /**
     * Returns the {@link BleScanInfo} instance held by this {@link BleDevice}.
     */
    public final @Nullable(Prevalence.NEVER) BleScanInfo getScanInfo()
    {
        return m_scanInfo;
    }

    /**
     * Returns the advertising flags, if any, parsed from {@link #getScanRecord()}.
     */
    public final int getAdvertisingFlags()
    {
        return m_advertisingFlags;
    }

    /**
     * Returns the advertised services, if any, parsed from {@link #getScanRecord()}. May be empty but never <code>null</code>.
     */
    public final @Nullable(Prevalence.NEVER) UUID[] getAdvertisedServices()
    {
        final UUID[] toReturn = m_scanInfo.getServiceUUIDS().size() > 0 ? new UUID[m_scanInfo.getServiceUUIDS().size()] : P_Const.EMPTY_UUID_ARRAY;
        return m_scanInfo.getServiceUUIDS().toArray(toReturn);
    }

    /**
     * Returns the manufacturer data, if any, parsed from {@link #getScanRecord()}. May be empty but never <code>null</code>.
     */
    public final @Nullable(Prevalence.NEVER) byte[] getManufacturerData()
    {
        final byte[] toReturn = m_scanInfo.getManufacturerData() != null ? m_scanInfo.getManufacturerData().clone() : P_Const.EMPTY_BYTE_ARRAY;

        return toReturn;
    }

    /**
     * Returns the manufacturer id, if any, parsed from {@link #getScanRecord()} }. May be -1 if not set
     */
    public final int getManufacturerId()
    {
        final int toReturn = m_scanInfo.getManufacturerId();

        return toReturn;
    }

    /**
     * Returns the manufacturer data, if any, parsed from {@link #getScanRecord()}. May be empty but never <code>null</code>.
     */
    public final @Nullable(Prevalence.NEVER) Map<UUID, byte[]> getAdvertisedServiceData()
    {
        final Map<UUID, byte[]> toReturn = new HashMap<UUID, byte[]>();

        toReturn.putAll(m_scanInfo.getServiceData());

        return toReturn;
    }

    /**
     * Returns the database table name for the underlying store of historical data for the given {@link UUID}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) String getHistoricalDataTableName(final UUID uuid)
    {
        return getManager().m_historicalDatabase.getTableName(getMacAddress(), uuid);
    }

    /**
     * Returns a cursor capable of random access to the database-persisted historical data for this device.
     * Unlike calls to methods like {@link #getHistoricalData_iterator(UUID)} and other overloads,
     * this call does not force bulk data load into memory.
     * <br><br>
     * NOTE: You must call {@link HistoricalDataCursor#close()} when you are done with the data.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalDataCursor getHistoricalData_cursor(final UUID uuid)
    {
        return getHistoricalData_cursor(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Same as {@link #getHistoricalData_cursor(UUID)} but constrains the results to the given time range.
     * <br><br>
     * NOTE: You must call {@link HistoricalDataCursor#close()} when you are done with the data.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalDataCursor getHistoricalData_cursor(final UUID uuid, final EpochTimeRange range)
    {
        return m_historicalDataMngr.getCursor(uuid, range);
    }

    /**
     * Loads all historical data to memory for this device.
     */
    @Advanced
    public final void loadHistoricalData()
    {
        loadHistoricalData(null, null);
    }

    /**
     * Loads all historical data to memory for this device for the given {@link UUID}.
     */
    @Advanced
    public final void loadHistoricalData(final UUID uuid)
    {
        loadHistoricalData(uuid, null);
    }

    /**
     * Loads all historical data to memory for this device with a callback for when it's complete.
     */
    @Advanced
    public final void loadHistoricalData(final HistoricalDataLoadListener listener)
    {
        loadHistoricalData(null, listener);
    }

    /**
     * Loads all historical data to memory for this device for the given {@link UUID}.
     */
    @Advanced
    public final void loadHistoricalData(final UUID uuid, final HistoricalDataLoadListener listener)
    {
        if (isNull()) return;

        m_historicalDataMngr.load(uuid, listener);
    }

    /**
     * Returns whether the device is currently loading any historical data to memory, either through
     * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData_iterator(UUID)} (or overloads).
     */
    @Advanced
    public final boolean isHistoricalDataLoading()
    {
        return m_historicalDataMngr.isLoading(null);
    }

    /**
     * Returns whether the device is currently loading any historical data to memory for the given uuid, either through
     * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData_iterator(UUID)} (or overloads).
     */
    @Advanced
    public final boolean isHistoricalDataLoading(final UUID uuid)
    {
        return m_historicalDataMngr.isLoading(uuid);
    }

    /**
     * Returns <code>true</code> if the historical data for all historical data for
     * this device is loaded into memory.
     * Use {@link BleNode.HistoricalDataLoadListener}
     * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
     * returns <code>false</code> then this will also always return <code>false</code>.
     */
    @Advanced
    public final boolean isHistoricalDataLoaded()
    {
        return m_historicalDataMngr.isLoaded(null);
    }

    /**
     * Returns <code>true</code> if the historical data for a given uuid is loaded into memory.
     * Use {@link BleNode.HistoricalDataLoadListener}
     * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
     * returns <code>false</code> then this will also always return <code>false</code>.
     */
    @Advanced
    public final boolean isHistoricalDataLoaded(final UUID uuid)
    {
        return m_historicalDataMngr.isLoaded(uuid);
    }

    /**
     * Returns the cached data from the latest successful read or notify received for a given uuid.
     * Basically if you receive a {@link ReadWriteListener.ReadWriteEvent} for which {@link ReadWriteListener.ReadWriteEvent#isRead()}
     * and {@link ReadWriteListener.ReadWriteEvent#wasSuccess()} both return <code>true</code> then {@link ReadWriteListener.ReadWriteEvent#data()},
     * will be cached and is retrievable by this method.
     *
     * @return The cached value from a previous read or notify, or {@link HistoricalData#NULL} otherwise.
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalData getHistoricalData_latest(final UUID uuid)
    {
        return getHistoricalData_atOffset(uuid, getHistoricalDataCount(uuid) - 1);
    }

    /**
     * Returns an iterator that will iterate through all {@link HistoricalData} entries.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<HistoricalData> getHistoricalData_iterator(final UUID uuid)
    {
        return getHistoricalData_iterator(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns an iterator that will iterate through all {@link HistoricalData} entries within the range provided.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<HistoricalData> getHistoricalData_iterator(final UUID uuid, final EpochTimeRange range)
    {
        if (isNull()) return new EmptyIterator<HistoricalData>();

        return m_historicalDataMngr.getIterator(uuid, EpochTimeRange.denull(range));
    }

    /**
     * Provides all historical data through the "for each" provided.
     *
     * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean getHistoricalData_forEach(final UUID uuid, final ForEach_Void<HistoricalData> forEach)
    {
        return getHistoricalData_forEach(uuid, EpochTimeRange.FROM_MIN_TO_MAX, forEach);
    }

    /**
     * Provides all historical data through the "for each" provided within the range provided.
     *
     * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Void<HistoricalData> forEach)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
    }

    /**
     * Provides all historical data through the "for each" provided.
     *
     * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean getHistoricalData_forEach(final UUID uuid, final ForEach_Breakable<HistoricalData> forEach)
    {
        return getHistoricalData_forEach(uuid, EpochTimeRange.FROM_MIN_TO_MAX, forEach);
    }

    /**
     * Provides all historical data through the "for each" provided within the range provided.
     *
     * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Breakable<HistoricalData> forEach)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but returns the data from the chronological offset, i.e. <code>offsetFromStart=0</code>
     * will return the earliest {@link HistoricalData}. Use in combination with {@link #getHistoricalDataCount(java.util.UUID)} to iterate
     * "manually" through this device's historical data for the given characteristic.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalData getHistoricalData_atOffset(final UUID uuid, final int offsetFromStart)
    {
        return getHistoricalData_atOffset(uuid, EpochTimeRange.FROM_MIN_TO_MAX, offsetFromStart);
    }

    /**
     * Same as {@link #getHistoricalData_atOffset(java.util.UUID, int)} but offset is relative to the time range provided.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalData getHistoricalData_atOffset(final UUID uuid, final EpochTimeRange range, final int offsetFromStart)
    {
        if (isNull()) return HistoricalData.NULL;

        return m_historicalDataMngr.getWithOffset(uuid, EpochTimeRange.denull(range), offsetFromStart);
    }

    /**
     * Returns the number of historical data entries that have been logged for the device's given characteristic.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final int getHistoricalDataCount(final UUID uuid)
    {
        return getHistoricalDataCount(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns the number of historical data entries that have been logged
     * for the device's given characteristic within the range provided.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final int getHistoricalDataCount(final UUID uuid, final EpochTimeRange range)
    {
        if (isNull()) return 0;

        return m_historicalDataMngr.getCount(uuid, EpochTimeRange.denull(range));
    }

    /**
     * Returns <code>true</code> if there is any historical data at all for this device.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData()
    {
        return hasHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns <code>true</code> if there is any historical data at all for this device within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final EpochTimeRange range)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.hasHistoricalData(range);
    }

    /**
     * Returns <code>true</code> if there is any historical data for the given uuid.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID uuid)
    {
        return hasHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns <code>true</code> if there is any historical data for any of the given uuids.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID[] uuids)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            if (hasHistoricalData(uuids[i], EpochTimeRange.FROM_MIN_TO_MAX))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if there is any historical data for the given uuid within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.hasHistoricalData(uuid, range);
    }

    /**
     * Manual way to add data to the historical data list managed by this device. You may want to use this if,
     * for example, your remote BLE device is capable of taking and caching independent readings while not connected.
     * After you connect with this device and download the log you can add it manually here.
     * Really you can use this for any arbitrary historical data though, even if it's not associated with a characteristic.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final byte[] data, final EpochTime epochTime)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_single(uuid, data, epochTime, BleNodeConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
    }

    /**
     * Just an overload of {@link #addHistoricalData(UUID, byte[], EpochTime)} with the data and epochTime parameters switched around.
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final EpochTime epochTime, final byte[] data)
    {
        this.addHistoricalData(uuid, data, epochTime);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but uses {@link System#currentTimeMillis()} for the timestamp.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final byte[] data)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_single(uuid, data, new EpochTime(), BleNodeConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)}.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final HistoricalData historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_single(uuid, historicalData, BleNodeConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final Iterator<HistoricalData> historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_multiple(uuid, historicalData);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final List<HistoricalData> historicalData)
    {
        addHistoricalData(uuid, historicalData.iterator());
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final ForEach_Returning<HistoricalData> historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_multiple(uuid, historicalData);
    }

    /**
     * Returns whether the device is in any of the provided states.
     *
     * @see #is(BleDeviceState)
     */
    public final boolean isAny(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (is(states[i])) return true;
        }

        return false;
    }

    /**
     * Returns whether the device is in all of the provided states.
     *
     * @see #isAny(BleDeviceState...)
     */
    public final boolean isAll(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (!is(states[i])) return false;
        }
        return true;
    }

    /**
     * Convenience method to tell you whether a call to {@link #connect()} (or overloads) has a chance of succeeding.
     * For example if the device is {@link BleDeviceState#CONNECTING_OVERALL} or {@link BleDeviceState#INITIALIZED}
     * then this will return <code>false</code>.
     */
    public final boolean isConnectable()
    {
        if (isAny(BleDeviceState.INITIALIZED, BleDeviceState.CONNECTING_OVERALL))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Returns whether the device is in the provided state.
     *
     * @see #isAny(BleDeviceState...)
     */
    public final boolean is(final BleDeviceState state)
    {
        return state.overlaps(getStateMask());
    }

    /**
     * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAll(int)
     */
    public final boolean isAny(final int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) != 0x0;
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAny(int)
     */
    public final boolean isAll(final int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) == mask_BleDeviceState;
    }

    /**
     * Similar to {@link #is(BleDeviceState)} and {@link #isAny(BleDeviceState...)} but allows you to give a simple query
     * made up of {@link BleDeviceState} and {@link Boolean} pairs. So an example would be
     * <code>myDevice.is({@link BleDeviceState#CONNECTING}, true, {@link BleDeviceState#RECONNECTING_LONG_TERM}, false)</code>.
     */
    public final boolean is(Object... query)
    {
        return Utils_State.query(getStateMask(), query);
    }

    final boolean isAny_internal(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (is_internal(states[i]))
            {
                return true;
            }
        }

        return false;
    }

    final boolean is_internal(BleDeviceState state)
    {
        return state.overlaps(stateTracker().getState());
    }

    final P_BleDeviceLayerManager layerManager()
    {
        return m_layerManager;
    }

    /**
     * If {@link #is(BleDeviceState)} returns true for the given state (i.e. if
     * the device is in the given state) then this method will (a) return the
     * amount of time that the device has been in the state. Otherwise, this
     * will (b) return the amount of time that the device was *previously* in
     * that state. Otherwise, if the device has never been in the state, it will
     * (c) return 0.0 seconds. Case (b) might be useful for example for checking
     * how long you <i>were</i> connected for after becoming
     * {@link BleDeviceState#DISCONNECTED}, for analytics purposes or whatever.
     */
    public final Interval getTimeInState(BleDeviceState state)
    {
        return Interval.millis(stateTracker_main().getTimeInState(state.ordinal()));
    }

    /**
     * Overload of {@link #refreshGattDatabase(Interval)} which uses the gatt refresh delay set in {@link BleDeviceConfig}.
     */
    public final void refreshGattDatabase()
    {
        refreshGattDatabase(BleDeviceConfig.interval(conf_device().gattRefreshDelay, conf_mngr().gattRefreshDelay));
    }

    /**
     * This only applies to a device which is {@link BleDeviceState#CONNECTED}. This is meant to be used mainly after performing a
     * firmware update, and the Gatt database has changed. This will clear the device's gatt cache, and perform discover services again.
     * The device will drop out of {@link BleDeviceState#SERVICES_DISCOVERED}, and enter {@link BleDeviceState#DISCOVERING_SERVICES}. So,
     * you can listen in your device's {@link DeviceStateListener} for when it enters {@link BleDeviceState#SERVICES_DISCOVERED} to know
     * when the operation is complete.
     */
    public final void refreshGattDatabase(Interval gattPause)
    {
        if (is(CONNECTED))
        {
            stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SERVICES_DISCOVERED, false, DISCOVERING_SERVICES, true);
            P_Task_DiscoverServices discTask = new P_Task_DiscoverServices(this, new PA_Task.I_StateListener()
            {
                @Override public void onStateChange(PA_Task task, PE_TaskState state)
                {
                    if (task.getClass() == P_Task_DiscoverServices.class)
                    {
                        if (state == PE_TaskState.SUCCEEDED)
                        {
                            stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_SUCCESS, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
                        }
                    }
                }
            }, true, gattPause);
            taskManager().add(discTask);
        }
    }

    /**
     * Same as {@link #setName(String, UUID, ReadWriteListener)} but will not attempt to propagate the
     * name change to the remote device. Only {@link #getName_override()} will be affected by this.
     */
    public final void setName(final String name)
    {
        setName(name, null, null);
    }

    /**
     * Same as {@link #setName(String, UUID, ReadWriteListener)} but you can use this
     * if you don't care much whether the device name change actually successfully reaches
     * the remote device. The write will be attempted regardless.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setName(final String name, final UUID characteristicUuid)
    {
        return setName(name, characteristicUuid, null);
    }

    /**
     * Sets the local name of the device and also attempts a {@link #write(java.util.UUID, byte[], ReadWriteListener)}
     * using the given {@link UUID}. Further calls to {@link #getName_override()} will immediately reflect the name given here.
     * Further calls to {@link #getName_native()}, {@link #getName_debug()} and {@link #getName_normalized()} will only reflect
     * the name given here if the write is successful. It is somewhat assumed that doing this write will cause the remote device
     * to use the new name given here for its device information service {@link Uuids#DEVICE_NAME}.
     * If {@link BleDeviceConfig#saveNameChangesToDisk} is <code>true</code> then this name
     * will always be returned for {@link #getName_override()}, even if you kill/restart the app.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setName(final String name, final UUID characteristicUuid, final ReadWriteListener listener)
    {
        if (!isNull())
        {
            m_nativeWrapper.setName_override(name);

            final boolean saveToDisk = BleDeviceConfig.bool(conf_device().saveNameChangesToDisk, conf_mngr().saveNameChangesToDisk);

            getManager().m_diskOptionsMngr.saveName(getMacAddress(), name, saveToDisk);
        }

        if (characteristicUuid != null)
        {
            final ReadWriteListener listener_wrapper = new ReadWriteListener()
            {
                @Override public void onEvent(ReadWriteEvent e)
                {
                    if (e.wasSuccess())
                    {
                        m_nativeWrapper.updateNativeName(name);
                    }

                    invokeReadWriteCallback(listener, e);
                }
            };

            return this.write(characteristicUuid, name.getBytes(), listener_wrapper);
        }
        else
        {
            return NULL_READWRITE_EVENT();
        }
    }

    /**
     * Clears any name previously provided through {@link #setName(String)} or overloads.
     */
    public final void clearName()
    {
        m_nativeWrapper.clearName_override();
        getManager().m_diskOptionsMngr.clearName(getMacAddress());
    }

    /**
     * By default returns the same value as {@link #getName_native()}.
     * If you call {@link #setName(String)} (or overloads)
     * then calling this will return the same string provided in that setter.
     */
    public final @Nullable(Prevalence.NEVER) String getName_override()
    {
        return m_nativeWrapper.getName_override();
    }

    /**
     * Returns the raw, unmodified device name retrieved from the stack.
     * Equivalent to {@link BluetoothDevice#getName()}. It's suggested to use
     * {@link #getName_normalized()} if you're using the name to match/filter
     * against something, e.g. an entry in a config file or for advertising
     * filtering.
     */
    public final @Nullable(Prevalence.NEVER) String getName_native()
    {
        return m_nativeWrapper.getNativeName();
    }

    /**
     * The name retrieved from {@link #getName_native()} can change arbitrarily,
     * like the last 4 of the MAC address can get appended sometimes, and spaces
     * might get changed to underscores or vice-versa, caps to lowercase, etc.
     * This may somehow be standard, to-the-spec behavior but to the newcomer
     * it's confusing and potentially time-bomb-bug-inducing, like if you're
     * using device name as a filter for something and everything's working
     * until one day your app is suddenly broken and you don't know why. This
     * method is an attempt to normalize name behavior and always return the
     * same name regardless of the underlying stack's whimsy. The target format
     * is all lowercase and underscore-delimited with no trailing MAC address.
     */
    public final @Nullable(Prevalence.NEVER) String getName_normalized()
    {
        return m_nativeWrapper.getNormalizedName();
    }

    /**
     * Returns a name useful for logging and debugging. As of this writing it is
     * {@link #getName_normalized()} plus the last four digits of the device's
     * MAC address from {@link #getMacAddress()}. {@link BleDevice#toString()}
     * uses this.
     */
    public final @Nullable(Prevalence.NEVER) String getName_debug()
    {
        return m_nativeWrapper.getDebugName();
    }

    /**
     * Provides just-in-case lower-level access to the native device instance.
     * <br><br>
     * WARNING: Be careful with this. It generally should not be needed. Only
     * invoke "mutators" of this object in times of extreme need.
     * <br><br>
     * NOTE: If you are forced to use this please contact library developers to
     * discuss possible feature addition or report bugs.
     */
    @Advanced
    public final @Nullable(Prevalence.RARE) BluetoothDevice getNative()
    {
        return m_nativeWrapper.getDevice();
    }

    /**
     * See pertinent warning for {@link #getNative()}. Generally speaking, this
     * will return <code>null</code> if the BleDevice is {@link BleDeviceState#DISCONNECTED}.
     * <br><br>
     * NOTE: If you are forced to use this please contact library developers to
     * discuss possible feature addition or report bugs.
     */
    @Advanced
    public final @Nullable(Prevalence.NORMAL) BluetoothGatt getNativeGatt()
    {
        return m_nativeWrapper.getGatt();
    }

    /**
     * Returns the MAC address of this device, as retrieved from the native stack or provided through {@link BleManager#newDevice(String)} (or overloads thereof).
     * You may treat this as the unique ID of the device, suitable as a key in a {@link java.util.HashMap}, {@link android.content.SharedPreferences}, etc.
     */
    @Override public final @Nullable(Prevalence.NEVER) String getMacAddress()
    {
        return m_nativeWrapper.getAddress();
    }

    /**
     * Same as {@link #bond()} but you can pass a listener to be notified of the details behind success or failure.
     *
     * @return same as {@link #bond()}.
     */
    public final @Nullable(Prevalence.NEVER) BondListener.BondEvent bond(BondListener listener)
    {
        return bond_private(/*isDirect=*/true, true, listener);
    }

    final BondListener.BondEvent bond_private(boolean isDirect, boolean userCalled, BondListener listener)
    {
        if (listener != null)
        {
            setListener_Bond(listener);
        }

        if (isNull())
        {
            final BondListener.BondEvent event = m_bondMngr.invokeCallback(BondListener.Status.NULL_DEVICE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);

            return event;
        }

        if (isAny(BONDING, BONDED))
        {
            final BondListener.BondEvent event = m_bondMngr.invokeCallback(BondListener.Status.ALREADY_BONDING_OR_BONDED, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);

            return event;
        }

        if (userCalled)
        {
            m_bondMngr.resetBondRetryCount();
        }

        bond_justAddTheTask(E_TransactionLockBehavior.PASSES, isDirect);

        stateTracker_updateBoth(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDING, true, UNBONDED, false);

        return NULL_BOND_EVENT();
    }

    /**
     * Attempts to create a bond. Analogous to {@link BluetoothDevice#createBond()} This is also sometimes called
     * pairing, but while pairing and bonding are closely related, they are technically different from each other.
     * <br><br>
     * Bonding is required for reading/writing encrypted characteristics and,
     * anecdotally, may improve connection stability in some cases. This is
     * mentioned here and there on Internet threads complaining about Android
     * BLE so take it with a grain of salt because it has been directly observed
     * by us to degrade stability in some cases as well.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     * @see #unbond()
     */
    public final @Nullable(Prevalence.NEVER) BondListener.BondEvent bond()
    {
        return this.bond(null);
    }

    /**
     * Opposite of {@link #bond()}.
     *
     * @return <code>true</code> if successfully {@link BleDeviceState#UNBONDED}, <code>false</code> if already {@link BleDeviceState#UNBONDED}.
     * @see #bond()
     */
    public final boolean unbond()
    {
        final boolean alreadyUnbonded = is(UNBONDED);

        unbond_internal(null, BondListener.Status.CANCELLED_FROM_UNBOND);

        return !alreadyUnbonded;
    }

    /**
     * Starts a connection process, or does nothing if already {@link BleDeviceState#CONNECTED} or {@link BleDeviceState#CONNECTING}.
     * Use {@link #setListener_Reconnect(DeviceReconnectFilter)} and {@link #setListener_State(DeviceStateListener)} to receive callbacks for
     * progress and errors.
     *
     * @return same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect()
    {
        return connect(null, null);
    }

    /**
     * Starts a connection process, or does nothing if already {@link BleDeviceState#CONNECTED} or {@link BleDeviceState#CONNECTING}.
     * Use {@link #setListener_Reconnect(DeviceReconnectFilter)} and {@link #setListener_State(DeviceStateListener)} to receive callbacks for more
     * thorough progress and errors.
     *
     * @return same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect(DeviceConnectListener connectListener)
    {
        return connect(null, null, connectListener);
    }

    /**
     * Same as {@link #connect()} but provides a hook for the app to do some kind of authentication handshake if it wishes. This is popular with
     * commercial BLE devices where you don't want hobbyists or competitors using your devices for nefarious purposes - like releasing a better application
     * for your device than you ;-). Usually the characteristics read/written inside this transaction are encrypted and so one way or another will require
     * the device to become {@link BleDeviceState#BONDED}. This should happen automatically for you, i.e you shouldn't need to call {@link #bond()} yourself.
     *
     * @return same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     * @see #connect()
     * @see BleDeviceState#AUTHENTICATING
     * @see BleDeviceState#AUTHENTICATED
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect(BleTransaction.Auth authenticationTxn)
    {
        return connect(authenticationTxn, null);
    }

    /**
     * Same as {@link #connect()} but provides a hook for the app to do some kind of initialization before it's considered fully
     * {@link BleDeviceState#INITIALIZED}. For example if you had a BLE-enabled thermometer you could use this transaction to attempt an initial
     * temperature read before updating your UI to indicate "full" connection success, even though BLE connection itself already succeeded.
     *
     * @return same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     * @see #connect()
     * @see BleDeviceState#INITIALIZING
     * @see BleDeviceState#INITIALIZED
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect(BleTransaction.Init initTxn)
    {
        return connect(null, initTxn);
    }

    /**
     * Combination of {@link #connect(BleTransaction.Auth)} and {@link #connect(BleTransaction.Init)}. See those two methods for explanation.
     *
     * @return same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     * @see #connect()
     * @see #connect(BleTransaction.Auth)
     * @see #connect(BleTransaction.Init)
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn)
    {
        return connect(authenticationTxn, initTxn, null);
    }

    /**
     * Same as {@link #connect(BleTransaction.Auth, BleTransaction.Init)} but calls {@link #setListener_State(DeviceStateListener)} and
     * {@link #setListener_Reconnect(DeviceReconnectFilter)} for you.
     *
     * @return If the attempt could not even "leave the gate" for some reason, a valid {@link ConnectFailEvent} is returned telling you why. Otherwise
     * this method will still return a non-null instance but {@link ConnectFailEvent#isNull()} will be <code>true</code>.
     * <br><br>
     * NOTE: your {@link DeviceReconnectFilter} will still be called even if this method early-outs.
     * <br><br>
     * TIP:	You can use the return value as an optimization. Many apps will call this method (or its overloads) and throw up a spinner until receiving a
     * callback to {@link DeviceReconnectFilter}. However if {@link ConnectFailEvent#isNull()} for the return value is <code>false</code>, meaning
     * the connection attempt couldn't even start for some reason, then you don't have to throw up the spinner in the first place.
     */
    public final @Nullable(Prevalence.NEVER) ConnectFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, DeviceConnectListener connectionListener)
    {

        if (connectionListener != null)
            m_ephemeralConnectListener = new WeakReference<>(connectionListener);

        m_connectionFailMngr.onExplicitConnectionStarted();

        final ConnectFailEvent info_earlyOut = connect_earlyOut();

        if (info_earlyOut != null) return info_earlyOut;

        m_lastConnectOrDisconnectWasUserExplicit = true;

        if (isAny(CONNECTED, CONNECTING, CONNECTING_OVERALL))
        {
            //--- DRK > Making a judgement call that an explicit connect call here means we bail out of the long term reconnect state.
            stateTracker_main().remove(RECONNECTING_LONG_TERM, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

            final ConnectFailEvent info_alreadyConnected = ConnectFailEvent.EARLY_OUT(this, Status.ALREADY_CONNECTING_OR_CONNECTED);

            m_connectionFailMngr.invokeCallback(info_alreadyConnected);

            return info_alreadyConnected;
        }

        connect_private(authenticationTxn, initTxn, /* isReconnect= */false);

        return NULL_CONNECTIONFAIL_INFO();
    }

    /**
     * Disconnects from a connected device or does nothing if already {@link BleDeviceState#DISCONNECTED}. You can call this at any point
     * during the connection process as a whole, during reads and writes, during transactions, whenever, and the device will cleanly cancel all ongoing
     * operations. This method will also bring the device out of the {@link BleDeviceState#RECONNECTING_LONG_TERM} state.
     *
     * @return <code>true</code> if this call "had an effect", such as if the device was previously {@link BleDeviceState#RECONNECTING_LONG_TERM},
     * {@link BleDeviceState#CONNECTING_OVERALL}, or {@link BleDeviceState#INITIALIZED}
     * @see DeviceReconnectFilter.Status#EXPLICIT_DISCONNECT
     */
    public final boolean disconnect()
    {
        return disconnect_private(null, Status.EXPLICIT_DISCONNECT, false);
    }

    final boolean disconnectAndUndiscover()
    {
        return disconnect_private(null, Status.EXPLICIT_DISCONNECT, true);
    }

    /**
     * Similar to {@link #disconnect()} with the difference being the disconnect task is set to a low priority. This allows all current calls to finish
     * executing before finally disconnecting. Note that this can cause issues if you keep executing reads/writes, as they have a higher priority.
     *
     * @return <code>true</code> if this call "had an effect", such as if the device was previously {@link BleDeviceState#RECONNECTING_LONG_TERM},
     * {@link BleDeviceState#CONNECTING_OVERALL}, or {@link BleDeviceState#INITIALIZED}
     * @see DeviceReconnectFilter.Status#EXPLICIT_DISCONNECT
     */
    public final boolean disconnectWhenReady()
    {
        return disconnect_private(PE_TaskPriority.LOW, Status.EXPLICIT_DISCONNECT, false);
    }

    /**
     * Same as {@link #disconnect()} but this call roughly simulates the disconnect as if it's because of the remote device going down, going out of range, etc.
     * For example {@link #getLastDisconnectIntent()} will be {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL} instead of
     * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}.
     * <br><br>
     * If the device is currently {@link BleDeviceState#CONNECTING_OVERALL} then your
     * {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)}
     * implementation will be called with {@link DeviceReconnectFilter.Status#ROGUE_DISCONNECT}.
     * <br><br>
     * NOTE: One major difference between this and an actual remote disconnect is that this will not cause the device to enter
     * {@link BleDeviceState#RECONNECTING_SHORT_TERM} or {@link BleDeviceState#RECONNECTING_LONG_TERM}.
     */
    public final boolean disconnect_remote()
    {
        return disconnect_private(null, Status.ROGUE_DISCONNECT, false);
    }

    private boolean disconnect_private(final PE_TaskPriority priority, final Status status, final boolean undiscoverAfter)
    {
        if (isNull()) return false;

        final boolean alreadyDisconnected = is(DISCONNECTED);
        final boolean reconnecting_longTerm = is(RECONNECTING_LONG_TERM);
        final boolean alreadyQueuedToDisconnect = taskManager().isInQueue(P_Task_Disconnect.class, this);

        if (!alreadyQueuedToDisconnect)
        {
            if (status == Status.EXPLICIT_DISCONNECT)
            {
                clearForExplicitDisconnect();
            }

            disconnectWithReason(priority, status, Timing.NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, undiscoverAfter, NULL_READWRITE_EVENT());
        }

        return !alreadyDisconnected || reconnecting_longTerm || !alreadyQueuedToDisconnect;
    }

    private void clearForExplicitDisconnect()
    {
        m_pollMngr.clear();
        clearMtu();
    }

    /**
     * Convenience method that calls {@link BleManager#undiscover(BleDevice)}.
     *
     * @return <code>true</code> if the device was successfully {@link BleDeviceState#UNDISCOVERED}, <code>false</code> if BleDevice isn't known to the {@link BleManager}.
     * @see BleManager#undiscover(BleDevice)
     */
    public final boolean undiscover()
    {
        if (isNull()) return false;

        return getManager().undiscover(this);
    }

    /**
     * Convenience forwarding of {@link BleManager#clearSharedPreferences(String)}.
     *
     * @see BleManager#clearSharedPreferences(BleDevice)
     */
    public final void clearSharedPreferences()
    {
        getManager().clearSharedPreferences(this);
    }

    /**
     * First checks referential equality and if <code>false</code> checks
     * equality of {@link #getMacAddress()}. Note that ideally this method isn't
     * useful to you and never returns true (besides the identity case, which
     * isn't useful to you). Otherwise it probably means your app is holding on
     * to old references that have been undiscovered, and this may be a bug or
     * bad design decision in your code. This library will (well, should) never
     * hold references to two devices such that this method returns true for them.
     */
    public final boolean equals(@Nullable(Prevalence.NORMAL) final BleDevice device_nullable)
    {
        if (device_nullable == null) return false;
        if (device_nullable == this) return true;
        if (device_nullable.layerManager().getDeviceLayer().isDeviceNull() || this.layerManager().getDeviceLayer().isDeviceNull()) return false;
        if (this.isNull() && device_nullable.isNull()) return true;

        return device_nullable.layerManager().getDeviceLayer().equals(getNative());
    }

    /**
     * Returns {@link #equals(BleDevice)} if object is an instance of {@link BleDevice}. Otherwise calls super.
     *
     * @see BleDevice#equals(BleDevice)
     */
    @Override public final boolean equals(@Nullable(Prevalence.NORMAL) final Object object_nullable)
    {
        if (object_nullable == null) return false;

        if (object_nullable instanceof BleDevice)
        {
            BleDevice object_cast = (BleDevice) object_nullable;

            return this.equals(object_cast);
        }

        return false;
    }

    /**
     * Starts a periodic read of a particular characteristic. Use this wherever you can in place of {@link #enableNotify(UUID, ReadWriteListener)}. One
     * use case would be to periodically read wind speed from a weather device. You *could* develop your device firmware to send notifications to the app
     * only when the wind speed changes, but Android has observed stability issues with notifications, so use them only when needed.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
     *
     * @see #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)
     * @see #enableNotify(UUID, ReadWriteListener)
     * @see #stopPoll(UUID, ReadWriteListener)
     */
    public final void startPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        m_pollMngr.startPoll(serviceUuid, characteristicUuid, null, Interval.secs(interval), listener, /*trackChanges=*/false, /*usingNotify=*/false);
    }

    /**
     * Same as {@link #startPoll(java.util.UUID, Interval, ReadWriteListener)} but without a listener.
     * <br><br>
     * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
     */
    public final void startPoll(final UUID characteristicUuid, final Interval interval)
    {
        startPoll(characteristicUuid, interval, null);
    }

    /**
     * Overload of {@link #startPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     */
    public final void startPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        m_pollMngr.startPoll(serviceUuid, characteristicUuid, null, Interval.secs(interval), listener, /*trackChanges=*/false, /*usingNotify=*/false);
    }

    /**
     * Overload of {@link #startPoll(UUID, UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under the same service.
     */
    public final void startPoll(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval interval, final ReadWriteListener listener)
    {
        m_pollMngr.startPoll(serviceUuid, characteristicUuid, descriptorFilter, Interval.secs(interval), listener, false, false);
    }

    /**
     * Overload of {@link #startPoll(UUID, Interval)} for when you have characteristics with identical uuids under different services.
     */
    public final void startPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval)
    {
        startPoll(serviceUuid, characteristicUuid, interval, null);
    }

    /**
     * Convenience to call {@link #startPoll(java.util.UUID, Interval, ReadWriteListener)} for multiple
     * characteristic uuids all at once.
     */
    public final void startPoll(final UUID[] charUuids, final Interval interval, final ReadWriteListener listener)
    {
        for (int i = 0; i < charUuids.length; i++)
        {
            startPoll(charUuids[i], interval, listener);
        }
    }

    /**
     * Same as {@link #startPoll(java.util.UUID[], Interval, ReadWriteListener)} but without a listener.
     * <br><br>
     * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
     */
    public final void startPoll(final UUID[] charUuids, final Interval interval)
    {
        startPoll(charUuids, interval, null);
    }

    /**
     * Convenience to call {@link #startPoll(java.util.UUID, Interval, ReadWriteListener)} for multiple
     * characteristic uuids all at once.
     */
    public final void startPoll(final Iterable<UUID> charUuids, final Interval interval, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = charUuids.iterator();

        while (iterator.hasNext())
        {
            final UUID ith = iterator.next();

            startPoll(ith, interval, listener);
        }
    }

    /**
     * Same as {@link #startPoll(java.util.UUID[], Interval, ReadWriteListener)} but without a listener.
     * <br><br>
     * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
     */
    public final void startPoll(final Iterable<UUID> charUuids, final Interval interval)
    {
        startPoll(charUuids, interval, null);
    }

    /**
     * Convenience to call {@link #startChangeTrackingPoll(java.util.UUID, Interval, ReadWriteListener)} for multiple
     * characteristic uuids all at once.
     */
    public final void startChangeTrackingPoll(final UUID[] charUuids, final Interval interval, final ReadWriteListener listener)
    {
        for (int i = 0; i < charUuids.length; i++)
        {
            startChangeTrackingPoll(charUuids[i], interval, listener);
        }
    }

    /**
     * Same as {@link #startChangeTrackingPoll(java.util.UUID[], Interval, ReadWriteListener)} but without a listener.
     * <br><br>
     * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
     */
    public final void startChangeTrackingPoll(final UUID[] charUuids, final Interval interval)
    {
        startChangeTrackingPoll(charUuids, interval, null);
    }

    /**
     * Convenience to call {@link #startChangeTrackingPoll(java.util.UUID, Interval, ReadWriteListener)} for multiple
     * characteristic uuids all at once.
     */
    public final void startChangeTrackingPoll(final Iterable<UUID> charUuids, final Interval interval, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = charUuids.iterator();

        while (iterator.hasNext())
        {
            final UUID ith = iterator.next();

            startChangeTrackingPoll(ith, interval, listener);
        }
    }

    /**
     * Same as {@link #startChangeTrackingPoll(java.util.UUID[], Interval, ReadWriteListener)} but without a listener.
     * <br><br>
     * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
     */
    public final void startChangeTrackingPoll(final Iterable<UUID> charUuids, final Interval interval)
    {
        startChangeTrackingPoll(charUuids, interval, null);
    }

    /**
     * Similar to {@link #startPoll(UUID, Interval, ReadWriteListener)} but only
     * invokes a callback when a change in the characteristic value is detected.
     * Use this in preference to {@link #enableNotify(UUID, ReadWriteListener)} if possible,
     * due to instability issues (rare, but still) with notifications on Android.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
     */
    public final void startChangeTrackingPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        m_pollMngr.startPoll(serviceUuid, characteristicUuid, null, Interval.secs(interval), listener, /*trackChanges=*/true, /*usingNotify=*/false);
    }

    /**
     * Overload of {@link #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     */
    public final void startChangeTrackingPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        m_pollMngr.startPoll(serviceUuid, characteristicUuid, null, Interval.secs(interval), listener, /*trackChanges=*/true, /*usingNotify=*/false);
    }

    /**
     * Overload of {@link #startChangeTrackingPoll(UUID, UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under the same service.
     */
    public final void startChangeTrackingPoll(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval interval, final ReadWriteListener listener)
    {
        m_pollMngr.startPoll(serviceUuid, characteristicUuid, descriptorFilter, Interval.secs(interval), listener, /*trackChanges=*/true, /*usingNotify=*/false);
    }

    /**
     * Stops a poll(s) started by either {@link #startPoll(UUID, Interval, ReadWriteListener)} or
     * {@link #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)}. This will stop all polls matching the provided parameters.
     *
     * @see #startPoll(UUID, Interval, ReadWriteListener)
     * @see #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)
     */
    public final void stopPoll(final UUID characteristicUuid, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        stopPoll_private(serviceUuid, characteristicUuid, null, null, listener);
    }

    /**
     * Same as {@link #stopPoll(java.util.UUID, ReadWriteListener)} but without the listener.
     */
    public final void stopPoll(final UUID characteristicUuid)
    {
        stopPoll(characteristicUuid, (ReadWriteListener) null);
    }

    /**
     * Same as {@link #stopPoll(UUID, ReadWriteListener)} but with added filtering for the poll {@link Interval}.
     */
    public final void stopPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        stopPoll_private(serviceUuid, characteristicUuid, null, interval != null ? interval.secs() : null, listener);
    }

    /**
     * Same as {@link #stopPoll(java.util.UUID, Interval, ReadWriteListener)} but without the listener.
     */
    public final void stopPoll(final UUID characteristicUuid, final Interval interval)
    {
        final UUID serviceUuid = null;

        stopPoll_private(serviceUuid, characteristicUuid, null, interval != null ? interval.secs() : null, null);
    }

    /**
     * Overload of {@link #stopPoll(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     */
    public final void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
    {
        stopPoll_private(serviceUuid, characteristicUuid, null, null, listener);
    }

    /**
     * Overload of {@link #stopPoll(UUID)} for when you have characteristics with identical uuids under different services.
     */
    public final void stopPoll(final UUID serviceUuid, final UUID characteristicUuid)
    {
        stopPoll(serviceUuid, characteristicUuid, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #stopPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     */
    public final void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
    {
        stopPoll_private(serviceUuid, characteristicUuid, null, interval != null ? interval.secs() : null, listener);
    }

    /**
     * Overload of {@link #stopPoll(UUID, UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under the same service.
     */
    public final void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval interval, final ReadWriteListener listener)
    {
        stopPoll_private(serviceUuid, characteristicUuid, descriptorFilter, interval != null ? interval.secs() : null, listener);
    }

    /**
     * Overload of {@link #stopPoll(UUID, Interval)} for when you have characteristics with identical uuids under different services.
     */
    public final void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval)
    {
        stopPoll_private(serviceUuid, characteristicUuid, null, interval != null ? interval.secs() : null, null);
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID, Interval, ReadWriteListener)} multiple times for you.
     */
    public final void stopPoll(final UUID[] uuids, final Interval interval, final ReadWriteListener listener)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            stopPoll(uuids[i], interval, listener);
        }
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID, Interval)} multiple times for you.
     */
    public final void stopPoll(final UUID[] uuids, final Interval interval)
    {
        stopPoll(uuids, interval, null);
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID)} multiple times for you.
     */
    public final void stopPoll(final UUID[] uuids)
    {
        stopPoll(uuids, null, null);
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID, Interval, ReadWriteListener)} multiple times for you.
     */
    public final void stopPoll(final Iterable<UUID> uuids, final Interval interval, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = uuids.iterator();

        while (iterator.hasNext())
        {
            final UUID ith = iterator.next();

            stopPoll(ith, interval, listener);
        }
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID, Interval)} multiple times for you.
     */
    public final void stopPoll(final Iterable<UUID> uuids, final Interval interval)
    {
        stopPoll(uuids, interval, null);
    }

    /**
     * Calls {@link #stopPoll(java.util.UUID)} multiple times for you.
     */
    public final void stopPoll(final Iterable<UUID> uuids)
    {
        stopPoll(uuids, null, null);
    }


    /**
     * Overload for {@link #write(BleWrite)}.
     */
    public final void writeMany(BleWrite[] writes)
    {
        for (int i = 0; i < writes.length; i++)
        {
            final BleWrite write = writes[i];

            write(write);
        }
    }

    /**
     * Overload for {@link #write(BleWrite)}.
     */
    public final void writeMany(Iterable<BleWrite> writes)
    {
        final Iterator<BleWrite> iterator = writes.iterator();

        while (iterator.hasNext())
        {
            final BleWrite write = iterator.next();

            write(write);
        }
    }

    /**
     * Writes to the device without a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(BleWrite bleWrite)
    {
        // Set the descriptor Uuid to INVALID, to ensure that SweetBlue treats this as a characteristic write, as opposed to a descriptor write.
        bleWrite.descriptorUuid = Uuids.INVALID;
        return write_internal(bleWrite);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(BleWrite bleWrite, ReadWriteListener listener)
    {
        return write(bleWrite.setReadWriteListener(listener));
    }

    /**
     * Writes to the device without a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final byte[] data)
    {
        return this.write(characteristicUuid, new PresentData(data), (ReadWriteListener) null);
    }

    /**
     * Writes to the device without a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final byte[] data)
    {
        return this.write(characteristicUuid, new PresentData(data), descriptorFilter, (ReadWriteListener) null);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final byte[] data, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return write(serviceUuid, characteristicUuid, new PresentData(data), listener);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final byte[] data, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return write(serviceUuid, characteristicUuid, new PresentData(data), descriptorFilter, listener);
    }

    /**
     * Overload of {@link #write(UUID, byte[])} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final byte[] data)
    {
        return this.write(serviceUuid, characteristicUuid, new PresentData(data), (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #write(UUID, DescriptorFilter, byte[])} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter filter, final byte[] data)
    {
        return this.write(serviceUuid, characteristicUuid, new PresentData(data), filter, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #write(UUID, byte[], ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final byte[] data, final ReadWriteListener listener)
    {
        return write(serviceUuid, characteristicUuid, new PresentData(data), listener);
    }

    /**
     * Overload of {@link #write(UUID, byte[], ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final byte[] data, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        return write(serviceUuid, characteristicUuid, new PresentData(data), descriptorFilter, listener);
    }

    /**
     * Writes to the device without a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final FutureData futureData)
    {
        return this.write(characteristicUuid, futureData, (ReadWriteListener) null);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final FutureData futureData, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return write(serviceUuid, characteristicUuid, futureData, listener);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return same as {@link #write(BleWrite, ReadWriteListener)}.
     * @see #write(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final FutureData futureData, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return write(serviceUuid, characteristicUuid, futureData, descriptorFilter, listener);
    }

    /**
     * Overload of {@link #write(UUID, FutureData)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData)
    {
        return this.write(serviceUuid, characteristicUuid, futureData, (ReadWriteListener) null);
    }


    /**
     * Overload of {@link #write(UUID, FutureData)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData, final DescriptorFilter descriptorFilter)
    {
        return this.write(serviceUuid, characteristicUuid, futureData, descriptorFilter, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #write(UUID, FutureData, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData, final ReadWriteListener listener)
    {
        final BleWrite write = new BleWrite(serviceUuid, characteristicUuid).setData(futureData).setReadWriteListener(listener);
        return write_internal(write);
    }

    /**
     * Overload of {@link #write(UUID, FutureData, ReadWriteListener)} for when you have characteristics with identical uuids under the same service.
     *
     * @deprecated to be removed in 3.1, use {@link #write(BleWrite)}, or {@link #write(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        final BleWrite builder = new BleWrite(serviceUuid, characteristicUuid).setDescriptorFilter(descriptorFilter).
                setReadWriteListener(listener).setData(futureData);
        return write_internal(builder);
    }

    /**
     * Writes to the device descriptor without a callback.
     *
     * @return same as {@link #writeDescriptor(BleWrite, ReadWriteListener)}.
     * @see #writeDescriptor(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID descriptorUuid, final byte[] data)
    {
        return writeDescriptor(descriptorUuid, data, (ReadWriteListener) null);
    }

    /**
     * Writes to the device descriptor with a callback.
     *
     * @return same as {@link #writeDescriptor(BleWrite, ReadWriteListener)}.
     * @see #writeDescriptor(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID descriptorUuid, final byte[] data, final ReadWriteListener listener)
    {
        final UUID characteristicUuid = null;

        return writeDescriptor(characteristicUuid, descriptorUuid, data, listener);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[])} for when you have descriptors with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID characteristicUuid, final UUID descriptorUuid, final byte[] data)
    {
        return writeDescriptor(characteristicUuid, descriptorUuid, data, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[], ReadWriteListener)} for when you have descriptors with identical uuids under different characteristics.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID characteristicUuid, final UUID descriptorUuid, final byte[] data, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return writeDescriptor(serviceUuid, characteristicUuid, descriptorUuid, data, listener);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[], ReadWriteListener)} for when you have descriptors with identical uuids under different characteristics and/or services.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid, final byte[] data, final ReadWriteListener listener)
    {
        return writeDescriptor(serviceUuid, characteristicUuid, descriptorUuid, new PresentData(data), listener);
    }

    /**
     * Writes to the device descriptor without a callback.
     *
     * @return same as {@link #writeDescriptor(BleWrite, ReadWriteListener)}.
     * @see #writeDescriptor(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID descriptorUuid, final FutureData futureData)
    {
        return writeDescriptor(descriptorUuid, futureData, (ReadWriteListener) null);
    }

    /**
     * Writes to the device with a callback.
     *
     * @return same as {@link #writeDescriptor(BleWrite, ReadWriteListener)}.
     * @see #writeDescriptor(BleWrite, ReadWriteListener)
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID descriptorUuid, final FutureData futureData, final ReadWriteListener listener)
    {
        final UUID characteristicUuid = null;

        return writeDescriptor(characteristicUuid, descriptorUuid, futureData, listener);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[])} for when you have descriptors with identical uuids under different services.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID characteristicUuid, final UUID descriptorUuid, final FutureData futureData)
    {
        return writeDescriptor(characteristicUuid, descriptorUuid, futureData, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[], ReadWriteListener)} for when you have descriptors with identical uuids under different characteristics.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID characteristicUuid, final UUID descriptorUuid, final FutureData futureData, final ReadWriteListener listener)
    {
        return writeDescriptor(null, characteristicUuid, descriptorUuid, futureData, listener);
    }

    /**
     * Overload of {@link #writeDescriptor(UUID, byte[], ReadWriteListener)} for when you have descriptors with identical uuids under different characteristics and/or services.
     *
     * @deprecated to be removed in 3.1, use {@link #writeDescriptor(BleWrite)}, or {@link #writeDescriptor(BleWrite, ReadWriteListener)} instead
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid, final FutureData futureData, final ReadWriteListener listener)
    {
        final BleWrite builder = new BleWrite(serviceUuid, characteristicUuid).setDescriptorUUID(descriptorUuid).setData(futureData).
                setReadWriteListener(listener);
        return writeDescriptor(builder);
    }

    /**
     * Writes to the device descriptor without a callback.
     *
     * @return same as {@link #writeDescriptor(BleWrite, ReadWriteListener)}.
     * @see #writeDescriptor(BleWrite, ReadWriteListener)
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(BleWrite write)
    {
        // If no descriptor Uuid is set, or it's invalid, then early out here so we don't try to write to a characteristic.
        if (write.descriptorUuid == null || write.descriptorUuid == Uuids.INVALID)
            return new ReadWriteEvent(this, write, write.writeType, ReadWriteListener.Target.DESCRIPTOR, ReadWriteListener.Status.NO_DESCRIPTOR_UUID, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);
        return write_internal(write);
    }

    /**
     * Writes to the device descriptor with a callback.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent writeDescriptor(BleWrite write, ReadWriteListener listener)
    {
        write.setReadWriteListener(listener);
        return writeDescriptor(write);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID descriptorUuid)
    {
        return readDescriptor(descriptorUuid, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID descriptorUuid, final ReadWriteListener listener)
    {
        final UUID characteristicUuid = null;

        return readDescriptor(characteristicUuid, descriptorUuid, listener);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID characteristicUuid, final UUID descriptorUuid)
    {
        return readDescriptor(characteristicUuid, descriptorUuid, (ReadWriteListener) null);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID characteristicUuid, final UUID descriptorUuid, final ReadWriteListener listener)
    {
        return readDescriptor(null, characteristicUuid, descriptorUuid, listener);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid)
    {
        return readDescriptor(serviceUuid, characteristicUuid, descriptorUuid, null);
    }

    /**
     * Overload of {@link #readDescriptor(BleRead)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readDescriptor(BleRead)} instead.
     */
    @Deprecated
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid, final ReadWriteListener listener)
    {
        final BleRead read = new BleRead(serviceUuid, characteristicUuid).setDescriptorUUID(descriptorUuid).setReadWriteListener(listener);
        return readDescriptor(read);
    }

    /**
     * Reads a descriptor from the device with a callback, if one is set in the provided {@link BleRead}.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent readDescriptor(final BleRead read)
    {
        // If there's no descriptor UUID set, or it's invalid, then we early out here to avoid trying to read a characteristic.
        if (read.descriptorUuid == null || read.descriptorUuid == Uuids.INVALID)
            return new ReadWriteEvent(this, read, Type.READ, ReadWriteListener.Target.DESCRIPTOR, ReadWriteListener.Status.NO_DESCRIPTOR_UUID, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);
        return read_internal(Type.READ, read);
    }

    /**
     * Same as {@link #readRssi(ReadWriteListener)} but use this method when you don't much care when/if the RSSI is actually updated.
     *
     * @return same as {@link #readRssi(ReadWriteListener)}.
     */
    public final ReadWriteListener.ReadWriteEvent readRssi()
    {
        return readRssi(null);
    }

    /**
     * Wrapper for {@link BluetoothGatt#readRemoteRssi()}. This will eventually update the value returned by {@link #getRssi()} but it is not
     * instantaneous. When a new RSSI is actually received the given listener will be called. The device must be {@link BleDeviceState#CONNECTED} for
     * this call to succeed. When the device is not {@link BleDeviceState#CONNECTED} then the value returned by
     * {@link #getRssi()} will be automatically updated every time this device is discovered (or rediscovered) by a scan operation.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final ReadWriteListener.ReadWriteEvent readRssi(final ReadWriteListener listener)
    {
        final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(BleRead.INVALID, Type.READ, ReadWriteListener.Target.RSSI);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(listener, earlyOutResult);

            return earlyOutResult;
        }

        readRssi_internal(Type.READ, listener);

        return NULL_READWRITE_EVENT();
    }

    /**
     * Same as {@link #setConnectionPriority(BleConnectionPriority, ReadWriteListener)} but use this method when you don't much care when/if the connection priority is updated.
     *
     * @return same as {@link #setConnectionPriority(BleConnectionPriority, ReadWriteListener)}.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setConnectionPriority(final BleConnectionPriority connectionPriority)
    {
        return setConnectionPriority(connectionPriority, null);
    }

    /**
     * Wrapper for {@link BluetoothGatt#requestConnectionPriority(int)} which attempts to change the connection priority for a given connection.
     * This will eventually update the value returned by {@link #getConnectionPriority()} but it is not
     * instantaneous. When we receive confirmation from the native stack then this value will be updated. The device must be {@link BleDeviceState#CONNECTED} for
     * this call to succeed.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     * @see #setConnectionPriority(BleConnectionPriority, ReadWriteListener)
     * @see #getConnectionPriority()
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setConnectionPriority(final BleConnectionPriority connectionPriority, final ReadWriteListener listener)
    {
        return setConnectionPriority_private(connectionPriority, listener, getOverrideReadWritePriority());
    }

    private @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setConnectionPriority_private(final BleConnectionPriority connectionPriority, final ReadWriteListener listener, final PE_TaskPriority taskPriority)
    {
        if (false == Utils.isLollipop())
        {
            final ReadWriteEvent e = new ReadWriteEvent(this, connectionPriority, ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
        else
        {
            final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(BleWrite.INVALID, Type.WRITE, ReadWriteListener.Target.CONNECTION_PRIORITY);

            if (earlyOutResult != null)
            {
                invokeReadWriteCallback(listener, earlyOutResult);

                return earlyOutResult;
            }
            else
            {
                getTaskManager().add(new P_Task_RequestConnectionPriority(this, listener, m_txnMngr.getCurrent(), taskPriority, connectionPriority));

                return NULL_READWRITE_EVENT();
            }
        }
    }

    /**
     * Returns the connection priority value set by {@link #setConnectionPriority(BleConnectionPriority, ReadWriteListener)}, or {@link BleDeviceConfig#DEFAULT_MTU_SIZE} if
     * it was never set explicitly.
     */
    @Advanced
    public final BleConnectionPriority getConnectionPriority()
    {
        return m_connectionPriority;
    }

    /**
     * Returns the "maximum transmission unit" value set by {@link #setMtu(int, ReadWriteListener)}, or {@link BleDeviceConfig#DEFAULT_MTU_SIZE} if
     * it was never set explicitly.
     */
    @Advanced
    public final int getMtu()
    {
        return m_mtu == 0 ? BleDeviceConfig.DEFAULT_MTU_SIZE : m_mtu;
    }

    /**
     * Same as {@link #setMtuToDefault(ReadWriteListener)} but use this method when you don't much care when/if the "maximum transmission unit" is actually updated.
     *
     * @return same as {@link #setMtu(int, ReadWriteListener)}.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setMtuToDefault()
    {
        return setMtuToDefault(null);
    }

    /**
     * Overload of {@link #setMtu(int, ReadWriteListener)} that returns the "maximum transmission unit" to the default.
     * Unlike {@link #setMtu(int)}, this can be called when the device is {@link BleDeviceState#DISCONNECTED} in the event that you don't want the
     * MTU to be auto-set upon next reconnection.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setMtuToDefault(final ReadWriteListener listener)
    {
        if (is(CONNECTED))
        {
            return setMtu(BleNodeConfig.DEFAULT_MTU_SIZE, listener);
        }
        else
        {
            clearMtu();

            final ReadWriteEvent e = new ReadWriteEvent(this, getMtu(), ReadWriteListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
    }

    /**
     * Same as {@link #setMtu(int, ReadWriteListener)} but use this method when you don't much care when/if the "maximum transmission unit" is actually updated.
     *
     * @return same as {@link #setMtu(int, ReadWriteListener)}.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setMtu(final int mtu)
    {
        return setMtu(mtu, null);
    }

    /**
     * Wrapper for {@link BluetoothGatt#requestMtu(int)} which attempts to change the "maximum transmission unit" for a given connection.
     * This will eventually update the value returned by {@link #getMtu()} but it is not
     * instantaneous. When we receive confirmation from the native stack then this value will be updated. The device must be {@link BleDeviceState#CONNECTED} for
     * this call to succeed.
     *
     * <b>NOTE 1:</b> This will only work on devices running Android Lollipop (5.0) or higher. Otherwise it will be ignored.
     * <b>NOTE 2:</b> Some phones will request an MTU, and accept a higher number, but will fail (time out) when writing a characteristic with a large
     * payload. Namely, we've found the Moto Pure X, and the OnePlus OnePlus2 to have this behavior. For those phones any MTU above
     * 50 failed.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    @Advanced
    public final @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setMtu(final int mtu, final ReadWriteListener listener)
    {
        return setMtu_private(mtu, listener, getOverrideReadWritePriority());
    }

    private ReadWriteListener.ReadWriteEvent setMtu_private(final int mtu, final ReadWriteListener listener, PE_TaskPriority priority)
    {
        if (false == Utils.isLollipop())
        {
            final ReadWriteEvent e = new ReadWriteEvent(this, getMtu(), ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
        else
        {
            if (mtu <= 0)
            {
                final ReadWriteEvent e = new ReadWriteEvent(this, getMtu(), ReadWriteListener.Status.INVALID_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

                invokeReadWriteCallback(listener, e);

                return e;
            }
            else
            {
                final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(BleWrite.INVALID, Type.WRITE, ReadWriteListener.Target.MTU);

                if (earlyOutResult != null)
                {
                    invokeReadWriteCallback(listener, earlyOutResult);

                    return earlyOutResult;
                }
                else
                {
                    getTaskManager().add(new P_Task_RequestMtu(this, listener, m_txnMngr.getCurrent(), priority, mtu));

                    return NULL_READWRITE_EVENT();
                }
            }
        }
    }

    /**
     * Same as {@link #startPoll(UUID, Interval, ReadWriteListener)} but for when you don't care when/if the RSSI is actually updated.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
     */
    public final void startRssiPoll(final Interval interval)
    {
        startRssiPoll(interval, null);
    }

    /**
     * Kicks off a poll that automatically calls {@link #readRssi(ReadWriteListener)} at the {@link Interval} frequency
     * specified. This can be called before the device is actually {@link BleDeviceState#CONNECTED}. If you call this more than once in a
     * row then the most recent call's parameters will be respected.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
     */
    public final void startRssiPoll(final Interval interval, final ReadWriteListener listener)
    {
        if (isNull()) return;

        m_rssiPollMngr.start(interval.secs(), listener);

        m_rssiPollMngr_auto.stop();
    }

    /**
     * Stops an RSSI poll previously started either by {@link #startRssiPoll(Interval)} or {@link #startRssiPoll(Interval, ReadWriteListener)}.
     */
    public final void stopRssiPoll()
    {
        if (isNull()) return;

        m_rssiPollMngr.stop();

        final Interval autoPollRate = BleDeviceConfig.interval(conf_device().rssiAutoPollRate, conf_mngr().rssiAutoPollRate);

        if (!Interval.isDisabled(autoPollRate))
        {
            m_rssiPollMngr_auto.start(autoPollRate.secs(), null);
        }
    }

    final void readRssi_internal(Type type, ReadWriteListener listener)
    {
        taskManager().add(new P_Task_ReadRssi(this, listener, m_txnMngr.getCurrent(), getOverrideReadWritePriority(), type));
    }

    /**
     * One method to remove absolutely all "metadata" related to this device that is stored on disk and/or cached in memory in any way.
     * This method is useful if for example you have a "forget device" feature in your app.
     */
    public final void clearAllData()
    {
        clearName();
        clearHistoricalData();
        clearSharedPreferences();
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData()
    {
        if (isNull()) return;

        m_historicalDataMngr.clearEverything();
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final long count)
    {
        clearHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final EpochTimeRange range)
    {
        clearHistoricalData(range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final EpochTimeRange range, final long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/false);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid)
    {
        clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Overload of {@link #clearHistoricalData(UUID)} that just calls that method multiple times.
     */
    public final void clearHistoricalData(final UUID... uuids)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            final UUID ith = uuids[i];

            clearHistoricalData(ith);
        }
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final long count)
    {
        clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final EpochTimeRange range)
    {
        clearHistoricalData(uuid, range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final EpochTimeRange range, final long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete(uuid, range, count, /*memoryOnly=*/false);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly()
    {
        clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final long count)
    {
        clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final EpochTimeRange range)
    {
        clearHistoricalData_memoryOnly(range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final EpochTimeRange range, final long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/true);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID uuid)
    {
        clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID uuid, final long count)
    {
        clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range)
    {
        clearHistoricalData_memoryOnly(characteristicUuid, range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see BleNodeConfig.HistoricalDataLogFilter
     * @see BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range, final long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete(characteristicUuid, range, count, /*memoryOnly=*/true);
    }

    /**
     * Overload of {@link #read(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readMany(BleRead[])} instead.
     */
    @Deprecated
    public final void read(final UUID[] charUuids)
    {
        read(charUuids, null);
    }

    /**
     * Overload of {@link #read(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readMany(BleRead[])} instead.
     */
    @Deprecated
    public final void read(final UUID[] charUuids, final ReadWriteListener listener)
    {
        for (int i = 0; i < charUuids.length; i++)
        {
            read(charUuids[i], listener);
        }
    }

    /**
     * Overload of {@link #read(BleRead)}.
     */
    public final void readMany(final BleRead[] bleReads)
    {
        for (int i = 0; i < bleReads.length; i++)
        {
            read(bleReads[i]);
        }
    }

    /**
     * Overload of {@link #read(BleRead)}.
     */
    public final void readMany(final Iterable<BleRead> bleReads)
    {
        final Iterator<BleRead> it = bleReads.iterator();

        while (it.hasNext())
        {
            final BleRead read = it.next();

            read(read);
        }
    }

    /**
     * Overload of {@link #read(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readMany(Iterable<BleRead)} instead.
     */
    @Deprecated
    public final void read(final Iterable<UUID> charUuids)
    {
        read(charUuids, null);
    }

    /**
     * Overload of {@link #read(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #readMany(Iterable<BleRead)} instead.
     */
    @Deprecated
    public final void read(final Iterable<UUID> charUuids, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = charUuids.iterator();

        while (iterator.hasNext())
        {
            final UUID charUuid = iterator.next();

            read(charUuid, listener);
        }
    }

    /**
     * Same as {@link #read(java.util.UUID, ReadWriteListener)} but you can use this
     * if you don't immediately care about the result. The callback will still be posted to {@link ReadWriteListener}
     * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(ReadWriteListener)} and
     * {@link BleManager#setListener_Read_Write(ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid)
    {
        final UUID serviceUuid = null;

        return read(serviceUuid, characteristicUuid, null, null);
    }

    /**
     * Same as {@link #read(java.util.UUID, DescriptorFilter, ReadWriteListener)} but you can use this
     * if you don't immediately care about the result. The callback will still be posted to {@link ReadWriteListener}
     * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(ReadWriteListener)} and
     * {@link BleManager#setListener_Read_Write(ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid, final DescriptorFilter descriptorFilter)
    {
        final UUID serviceUuid = null;

        return read(serviceUuid, characteristicUuid, descriptorFilter, null);
    }

    /**
     * Reads a characteristic from the device.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return read(serviceUuid, characteristicUuid, null, listener);
    }

    /**
     * Reads a characteristic from the device. The provided {@link DescriptorFilter} will grab the correct {@link BluetoothGattCharacteristic} in the case there are
     * more than one with the same {@link UUID} in the same {@link BluetoothGattService}.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return read(serviceUuid, characteristicUuid, descriptorFilter, listener);
    }

    /**
     * Overload of {@link #read(UUID)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid)
    {
        return read(serviceUuid, characteristicUuid, null, null);
    }

    /**
     * Overload of {@link #read(UUID, DescriptorFilter)} for when you have characteristics with identical uuids under the same service.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid, DescriptorFilter descriptorFilter)
    {
        return read(serviceUuid, characteristicUuid, descriptorFilter, null);
    }

    /**
     * Overload of {@link #read(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
    {
        return read(serviceUuid, characteristicUuid, null, listener);
    }

    /**
     * Overload of {@link #read(UUID, DescriptorFilter, ReadWriteListener)} for when you have characteristics with identical uuids under the same service.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #read(BleRead)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        BleRead read = new BleRead(serviceUuid, characteristicUuid).setDescriptorFilter(descriptorFilter).setReadWriteListener(listener);
        return read(read);
    }

    /**
     * Reads a characteristic from the device. The provided {@link DescriptorFilter} (if set in the {@link BleRead} will grab the correct {@link BluetoothGattCharacteristic} in the case there are
     * more than one with the same {@link UUID} in the same {@link BluetoothGattService}.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final ReadWriteListener.ReadWriteEvent read(final BleRead read)
    {
        // Set the descriptor Uuid to INVALID, to ensure that SweetBlue treats this as a characteristic read, as opposed to a descriptor read.
        read.descriptorUuid = Uuids.INVALID;
        return read_internal(Type.READ, read);
    }

    /**
     * Read the battery level of this device. This just calls {@link #read(UUID, UUID, ReadWriteListener)} using {@link Uuids#BATTERY_SERVICE_UUID},
     * and {@link Uuids#BATTERY_LEVEL}. If your battery service/characteristic uses a custom UUID, then use {@link #read(UUID, UUID, ReadWriteListener)} with
     * your custom UUIDs.
     */
    public final ReadWriteEvent readBatteryLevel(ReadWriteListener listener)
    {
        final BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(listener);
        return read_internal(Type.READ, read);
    }

    /**
     * Returns <code>true</code> if notifications are enabled for the given uuid.
     * NOTE: {@link #isNotifyEnabling(UUID)} may return true even if this method returns false.
     *
     * @see #isNotifyEnabling(UUID)
     */
    public final boolean isNotifyEnabled(final UUID uuid)
    {
        if (isNull()) return false;

        final UUID serviceUuid = null;

        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

        return notifyState == P_PollManager.E_NotifyState__ENABLED;
    }

    /**
     * Returns <code>true</code> if SweetBlue is in the process of enabling notifications for the given uuid.
     *
     * @see #isNotifyEnabled(UUID)
     */
    public final boolean isNotifyEnabling(final UUID uuid)
    {
        if (isNull()) return false;

        final UUID serviceUuid = null;

        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

        return notifyState == P_PollManager.E_NotifyState__ENABLING;
    }

    /**
     * Overload for {@link #enableNotify(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void enableNotify(final UUID[] charUuids)
    {
        this.enableNotify(charUuids, Interval.INFINITE, null);
    }

    /**
     * Overload for {@link #enableNotify(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void enableNotify(final UUID[] charUuids, ReadWriteListener listener)
    {
        this.enableNotify(charUuids, Interval.INFINITE, listener);
    }

    /**
     * Overload for {@link #enableNotify(UUID, Interval)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void enableNotify(final UUID[] charUuids, final Interval forceReadTimeout)
    {
        this.enableNotify(charUuids, forceReadTimeout, null);
    }

    /**
     * Overload for {@link #enableNotify(UUID, Interval, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void enableNotify(final UUID[] charUuids, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        for (int i = 0; i < charUuids.length; i++)
        {
            final UUID ith = charUuids[i];

            enableNotify(ith, forceReadTimeout, listener);
        }
    }

    /**
     * Overload for {@link #enableNotify(BleNotify)}.
     */
    public final void enableNotifies(BleNotify[] notifies)
    {
        for (int i = 0; i < notifies.length; i++)
        {
            final BleNotify notify = notifies[i];

            enableNotify(notify);
        }
    }

    /**
     * Overload for {@link #enableNotify(BleNotify)}.
     */
    public final void enableNotifies(final Iterable<BleNotify> notifies)
    {
        final Iterator<BleNotify> iterator = notifies.iterator();

        while (iterator.hasNext())
        {
            final BleNotify notify = iterator.next();
            enableNotify(notify);
        }
    }

    /**
     * Overload for {@link #enableNotify(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(Iterable)} instead.
     */
    @Deprecated
    public final void enableNotify(final Iterable<UUID> charUuids)
    {
        this.enableNotify(charUuids, Interval.INFINITE, null);
    }

    /**
     * Overload for {@link #enableNotify(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(Iterable)} instead.
     */
    @Deprecated
    public final void enableNotify(final Iterable<UUID> charUuids, ReadWriteListener listener)
    {
        this.enableNotify(charUuids, Interval.INFINITE, listener);
    }

    /**
     * Overload for {@link #enableNotify(UUID, Interval)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(Iterable)} instead.
     */
    @Deprecated
    public final void enableNotify(final Iterable<UUID> charUuids, final Interval forceReadTimeout)
    {
        this.enableNotify(charUuids, forceReadTimeout, null);
    }

    /**
     * Overload for {@link #enableNotify(UUID, Interval, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotifies(Iterable)} instead.
     */
    @Deprecated
    public final void enableNotify(final Iterable<UUID> charUuids, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = charUuids.iterator();

        while (iterator.hasNext())
        {
            final UUID ith = iterator.next();

            enableNotify(ith, forceReadTimeout, listener);
        }
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid)
    {
        return this.enableNotify(null, characteristicUuid, Interval.INFINITE, null, null);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, ReadWriteListener listener)
    {
        return this.enableNotify(null, characteristicUuid, Interval.INFINITE, null, listener);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, final Interval forceReadTimeout)
    {
        return this.enableNotify(null, characteristicUuid, forceReadTimeout, null, null);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        return this.enableNotify(null, characteristicUuid, forceReadTimeout, null, listener);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid)
    {
        return this.enableNotify(serviceUuid, characteristicUuid, Interval.INFINITE, null, null);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, ReadWriteListener listener)
    {
        return enableNotify(serviceUuid, characteristicUuid, Interval.INFINITE, null, listener);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout)
    {
        return enableNotify(serviceUuid, characteristicUuid, forceReadTimeout, null, null);
    }

    /**
     * Overload of {@link #enableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #enableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout, final DescriptorFilter descriptorFilter, final ReadWriteListener listener)
    {
        final BleNotify notify = new BleNotify(serviceUuid, characteristicUuid).setForceReadTimeout(forceReadTimeout).setReadWriteListener(listener).setDescriptorFilter(descriptorFilter);
        return enableNotify(notify);
    }

    /**
     * Enables notification on the given characteristic. The listener will be called both for the notifications themselves and for the actual
     * registration for the notification. <code>switch</code> on {@link Type#ENABLING_NOTIFICATION}
     * and {@link Type#NOTIFICATION} (or {@link Type#INDICATION}) in your listener to distinguish between these.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final ReadWriteListener.ReadWriteEvent enableNotify(BleNotify notify)
    {
        return enableNotify_private(notify);
    }

    private ReadWriteListener.ReadWriteEvent enableNotify_private(BleNotify notify)
    {
        final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(notify, Type.ENABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(notify.readWriteListener, earlyOutResult);

            if (earlyOutResult.status() == ReadWriteListener.Status.NO_MATCHING_TARGET || (Interval.INFINITE.equals(notify.m_forceReadTimeout) || Interval.DISABLED.equals(notify.m_forceReadTimeout)))
            {
                //--- DRK > No need to put this notify in the poll manager because either the characteristic wasn't found
                //--- or the notify (or indicate) property isn't supported and we're not doing a backing read poll.
                return earlyOutResult;
            }
        }

        final BleCharacteristicWrapper characteristic = getServiceManager().getCharacteristic(notify.serviceUuid, notify.charUuid);
        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(notify.serviceUuid, notify.charUuid);

        final boolean shouldSendOutNotifyEnable = notifyState == P_PollManager.E_NotifyState__NOT_ENABLED && (earlyOutResult == null || earlyOutResult.status() != ReadWriteListener.Status.OPERATION_NOT_SUPPORTED);

        final ReadWriteEvent result;
        final boolean isConnected = is(CONNECTED);

        if (shouldSendOutNotifyEnable && characteristic != null && isConnected)
        {
            m_bondMngr.bondIfNeeded(notify.charUuid, CharacteristicEventType.ENABLE_NOTIFY);

            final P_Task_ToggleNotify task = new P_Task_ToggleNotify(this, notify, true, m_txnMngr.getCurrent(), getOverrideReadWritePriority());

            taskManager().add(task);

            m_pollMngr.onNotifyStateChange(notify.serviceUuid, notify.charUuid, P_PollManager.E_NotifyState__ENABLING);

            result = NULL_READWRITE_EVENT();
        }
        else if (notifyState == P_PollManager.E_NotifyState__ENABLED)
        {
            if (notify.readWriteListener != null && isConnected)
            {
                result = m_pollMngr.newAlreadyEnabledEvent(characteristic.getCharacteristic(), notify.serviceUuid, notify.charUuid, notify.descriptorFilter);

                invokeReadWriteCallback(notify.readWriteListener, result);
            }
            else
            {
                result = NULL_READWRITE_EVENT();
            }

            if (!isConnected)
            {
                getManager().ASSERT(false, "Notification is enabled but we're not connected!");
            }
        }
        else
        {
            result = NULL_READWRITE_EVENT();
        }

        m_pollMngr.startPoll(notify.serviceUuid, notify.charUuid, notify.descriptorFilter, notify.m_forceReadTimeout.secs(), notify.readWriteListener, /*trackChanges=*/true, /*usingNotify=*/true);

        return result;
    }



    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return disableNotify(serviceUuid, characteristicUuid, null, listener);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        final UUID serviceUuid = null;

        return disableNotify(serviceUuid, characteristicUuid, forceReadTimeout, listener);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid)
    {
        final UUID serviceUuid = null;

        return disableNotify(serviceUuid, characteristicUuid, (DescriptorFilter) null, null);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final Interval forceReadTimeout)
    {
        final UUID serviceUuid = null;

        return disableNotify(serviceUuid, characteristicUuid, forceReadTimeout, null);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
    {
        return disableNotify(serviceUuid, characteristicUuid, null, listener);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        BleNotify notify = new BleNotify(serviceUuid, characteristicUuid).setForceReadTimeout(forceReadTimeout).setReadWriteListener(listener);
        return disableNotify(notify);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid)
    {
        return disableNotify(serviceUuid, characteristicUuid, (DescriptorFilter) null, null);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout)
    {
        return disableNotify(serviceUuid, characteristicUuid, null, forceReadTimeout);
    }

    /**
     * Overload of {@link #disableNotify(BleNotify)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotify(BleNotify)} instead.
     */
    @Deprecated
    public final ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval forceReadTimeout)
    {
        final BleNotify notify = new BleNotify(serviceUuid, characteristicUuid).setDescriptorFilter(descriptorFilter).setForceReadTimeout(forceReadTimeout);
        return disableNotify(notify);
    }

    /**
     * Disables all notifications enabled by {@link #enableNotify(BleNotify)} or
     * any of it's overloads. The listener provided should be the same one that you passed to {@link #enableNotify(BleNotify)}. Listen for
     * {@link Type#DISABLING_NOTIFICATION} in your listener to know when the remote device actually confirmed.
     *
     * @return see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, DeviceConnectListener)}.
     */
    public final ReadWriteListener.ReadWriteEvent disableNotify(BleNotify notify)
    {
        return disableNotify_private(notify);
    }

    /**
     * Overload for {@link #disableNotify(BleNotify)}.
     */
    public final void disableNotifies(final BleNotify[] notifies)
    {
        for (int i = 0; i < notifies.length; i++)
        {
            final BleNotify notify = notifies[i];

            disableNotify(notify);
        }
    }

    /**
     * Overload for {@link #disableNotify(BleNotify)}.
     */
    public final void disableNotifies(final Iterable<BleNotify> notifies)
    {
        final Iterator<BleNotify> iterator = notifies.iterator();

        while (iterator.hasNext())
        {
            final BleNotify notify = iterator.next();

            disableNotify(notify);
        }
    }

    /**
     * Overload for {@link #disableNotify(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void disableNotify(final UUID[] uuids, final ReadWriteListener listener)
    {
        this.disableNotify(uuids, null, listener);
    }

    /**
     * Overload for {@link #disableNotify(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void disableNotify(final UUID[] uuids)
    {
        this.disableNotify(uuids, null, null);
    }

    /**
     * Overload for {@link #disableNotify(UUID, Interval)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void disableNotify(final UUID[] uuids, final Interval forceReadTimeout)
    {
        this.disableNotify(uuids, forceReadTimeout, null);
    }

    /**
     * Overload for {@link #disableNotify(UUID, Interval, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(BleNotify[])} instead.
     */
    @Deprecated
    public final void disableNotify(final UUID[] uuids, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            final UUID ith = uuids[i];

            disableNotify(ith, forceReadTimeout, listener);
        }
    }

    /**
     * Overload for {@link #disableNotify(UUID)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(Iterable<BleNotify>)} instead.
     */
    @Deprecated
    public final void disableNotify(final Iterable<UUID> charUuids)
    {
        this.disableNotify(charUuids, Interval.INFINITE, null);
    }

    /**
     * Overload for {@link #disableNotify(UUID, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(Iterable<BleNotify>)} instead.
     */
    @Deprecated
    public final void disableNotify(final Iterable<UUID> charUuids, ReadWriteListener listener)
    {
        this.disableNotify(charUuids, Interval.INFINITE, listener);
    }

    /**
     * Overload for {@link #disableNotify(UUID, Interval)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(Iterable<BleNotify>)} instead.
     */
    @Deprecated
    public final void disableNotify(final Iterable<UUID> charUuids, final Interval forceReadTimeout)
    {
        this.disableNotify(charUuids, forceReadTimeout, null);
    }

    /**
     * Overload for {@link #disableNotify(UUID, Interval, ReadWriteListener)}.
     *
     * @deprecated - This will be removed in v3.1. Please use {@link #disableNotifies(Iterable<BleNotify>)} instead.
     */
    @Deprecated
    public final void disableNotify(final Iterable<UUID> charUuids, final Interval forceReadTimeout, final ReadWriteListener listener)
    {
        final Iterator<UUID> iterator = charUuids.iterator();

        while (iterator.hasNext())
        {
            final UUID ith = iterator.next();

            disableNotify(ith, forceReadTimeout, listener);
        }
    }

    /**
     * Kicks off an "over the air" long-term transaction if it's not already
     * taking place and the device is {@link BleDeviceState#INITIALIZED}. This
     * will put the device into the {@link BleDeviceState#PERFORMING_OTA} state
     * if <code>true</code> is returned. You can use this to do firmware
     * updates, file transfers, etc.
     * <br><br>
     * TIP: Use the {@link TimeEstimator} class to let your users know roughly
     * how much time it will take for the ota to complete.
     * <br><br>
     * TIP: For shorter-running transactions consider using {@link #performTransaction(BleTransaction)}.
     *
     * @return <code>true</code> if OTA has started, otherwise <code>false</code> if device is either already
     * {@link BleDeviceState#PERFORMING_OTA} or is not {@link BleDeviceState#INITIALIZED}.
     * @see BleManagerConfig#includeOtaReadWriteTimesInAverage
     * @see BleManagerConfig#autoScanDuringOta
     * @see #performTransaction(BleTransaction)
     */
    public final boolean performOta(final BleTransaction.Ota txn)
    {
        if (performTransaction_earlyOut(txn)) return false;

        if (is(PERFORMING_OTA))
        {
            //--- DRK > The strictest and maybe best way to early out here, but as far as expected behavior this may be better.
            //---		In the end it's a judgement call, what's best API-wise with user expectations.
            m_txnMngr.cancelOtaTransaction();
        }

        m_txnMngr.startOta(txn);

        return true;
    }

    /**
     * Allows you to perform an arbitrary transaction that is not associated with any {@link BleDeviceState} like
     * {@link BleDeviceState#PERFORMING_OTA}, {@link BleDeviceState#AUTHENTICATING} or {@link BleDeviceState#INITIALIZING}.
     * Generally this transaction should be short, several reads and writes. For longer-term transaction consider using
     * {@link #performOta(BleTransaction.Ota)}.
     * <br><br>
     * The device must be {@link BleDeviceState#INITIALIZED}.
     * <br><br>
     * TIP: For long-term transactions consider using {@link #performOta(BleTransaction.Ota)}.
     *
     * @return <code>true</code> if the transaction successfully started, <code>false</code> otherwise if device is not {@link BleDeviceState#INITIALIZED}.
     */
    public final boolean performTransaction(final BleTransaction txn)
    {
        if (performTransaction_earlyOut(txn)) return false;

        m_txnMngr.performAnonTransaction(txn);

        return true;
    }

    /**
     * Returns the effective MTU size for a write. BLE has an overhead when reading and writing, so that eats out of the MTU size.
     * The write overhead is defined via {@link BleManagerConfig#GATT_WRITE_MTU_OVERHEAD}. The method simply returns the MTU size minus
     * the overhead. This is just used internally, but is exposed in case it's needed for some other use app-side.
     */
    public final int getEffectiveWriteMtuSize()
    {
        return getMtu() - BleManagerConfig.GATT_WRITE_MTU_OVERHEAD;
    }

    private boolean performTransaction_earlyOut(final BleTransaction txn)
    {
        if (txn == null) return true;
        if (isNull()) return true;
        if (!is_internal(INITIALIZED)) return true;
        if (m_txnMngr.getCurrent() != null) return true;

        return false;
    }

    /**
     * Returns the device's name and current state for logging and debugging purposes.
     */
    @Override public final String toString()
    {
        if (isNull())
        {
            return NULL_STRING();
        }
        else
        {
            return m_nativeWrapper.getDebugName() + " " + stateTracker_main().toString();
        }
    }

    private boolean shouldAddOperationTime()
    {
        boolean includeFirmwareUpdateReadWriteTimesInAverage = BleDeviceConfig.bool(conf_device().includeOtaReadWriteTimesInAverage, conf_mngr().includeOtaReadWriteTimesInAverage);

        return includeFirmwareUpdateReadWriteTimesInAverage || !is(PERFORMING_OTA);
    }

    final void addReadTime(double timeStep)
    {
        if (!shouldAddOperationTime())
            return;

        if (m_readTimeEstimator != null)
        {
            m_readTimeEstimator.addTime(timeStep);
        }
    }

    final void addWriteTime(double timeStep)
    {
        if (!shouldAddOperationTime()) return;

        if (m_writeTimeEstimator != null)
        {
            m_writeTimeEstimator.addTime(timeStep);
        }
    }

    final void setToAlwaysUseAutoConnectIfItWorked()
    {
        m_alwaysUseAutoConnect = m_useAutoConnect;
    }

    final boolean shouldUseAutoConnect()
    {
        return m_useAutoConnect;
    }

    final P_BleDevice_Listeners getListeners()
    {
        return m_listeners;
    }

    final P_TaskManager getTaskManager()
    {
        return taskManager();
    }

    // PA_StateTracker getStateTracker(){ return m_stateTracker; }
    final BleTransaction getOtaTxn()
    {
        return m_txnMngr.m_otaTxn;
    }

    final P_PollManager getPollManager()
    {
        return m_pollMngr;
    }

    final void onLongTermReconnectTimeOut()
    {
        m_connectionFailMngr.onLongTermTimedOut();
    }

    final void onNewlyDiscovered(final P_NativeDeviceLayer device_native, final ScanFilter.ScanEvent scanEvent_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
    {
        m_origin_latest = origin;

        clear_discovery();

//        m_nativeWrapper.updateNativeDevice(device_native, scanRecord_nullable, false);

        m_nativeWrapper.updateNativeDeviceOnly(device_native);

        onDiscovered_private(scanEvent_nullable, rssi, scanRecord_nullable);

        stateTracker_main().update(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), UNDISCOVERED, false, DISCOVERED, true, ADVERTISING, origin == BleDeviceOrigin.FROM_DISCOVERY, DISCONNECTED, true);
    }

    final void onRediscovered(final P_NativeDeviceLayer device_native, final ScanFilter.ScanEvent scanEvent_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
    {
        m_origin_latest = origin;

        m_nativeWrapper.updateNativeDevice(device_native, scanRecord_nullable, Arrays.equals(m_scanRecord, scanRecord_nullable));

        onDiscovered_private(scanEvent_nullable, rssi, scanRecord_nullable);

        stateTracker_main().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), ADVERTISING, true);
    }

    final void onUndiscovered(E_Intent intent)
    {
        clear_undiscovery();

        if (m_reconnectMngr_longTerm != null) m_reconnectMngr_longTerm.stop();
        if (m_reconnectMngr_shortTerm != null) m_reconnectMngr_shortTerm.stop();
        if (m_rssiPollMngr != null) m_rssiPollMngr.stop();
        if (m_rssiPollMngr_auto != null) m_rssiPollMngr_auto.stop();
        if (m_pollMngr != null) m_pollMngr.clear();

        stateTracker_main().set(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, DISCOVERED, false, ADVERTISING, false, m_bondMngr.getNativeBondingStateOverrides(), DISCONNECTED, true);

        if (m_txnMngr != null)
        {
            m_txnMngr.cancelAllTransactions();
        }
    }

    final double getTimeSinceLastDiscovery()
    {
        return m_timeSinceLastDiscovery;
    }

    private void onDiscovered_private(final ScanFilter.ScanEvent scanEvent_nullable, final int rssi, byte[] scanRecord_nullable)
    {
        m_lastDiscoveryTime = EpochTime.now();
        m_timeSinceLastDiscovery = 0.0;
        updateRssi(rssi);

        if (scanEvent_nullable != null)
        {
            m_scanRecord = scanEvent_nullable.scanRecord();

            updateKnownTxPower(scanEvent_nullable.txPower());

            m_advertisingFlags = scanEvent_nullable.advertisingFlags();

            m_scanInfo.clearServiceUUIDs();
            m_scanInfo.addServiceUUIDs(scanEvent_nullable.advertisedServices());

            m_scanInfo.setManufacturerId((short) scanEvent_nullable.manufacturerId());
            m_scanInfo.setManufacturerData(scanEvent_nullable.manufacturerData());

            m_scanInfo.clearServiceData();
            m_scanInfo.addServiceData(scanEvent_nullable.serviceData());
        }
        else if (scanRecord_nullable != null)
        {
            m_scanRecord = scanRecord_nullable;

            m_scanInfo = Utils_ScanRecord.parseScanRecord(scanRecord_nullable);

            updateKnownTxPower(m_scanInfo.getTxPower().value);
        }
    }

    private void updateKnownTxPower(final int txPower)
    {
        if (txPower != BleNodeConfig.INVALID_TX_POWER)
        {
            m_knownTxPower = txPower;
        }
    }

    final void updateRssi(final int rssi)
    {
        m_rssi = rssi;
    }

    final void updateMtu(final int mtu)
    {
        m_mtu = mtu;
    }

    final void updateConnectionPriority(final BleConnectionPriority connectionPriority)
    {
        m_connectionPriority = connectionPriority;
    }

    private void clearMtu()
    {
        updateMtu(0);
    }

    final void update(double timeStep)
    {
        m_timeSinceLastDiscovery += timeStep;

        m_pollMngr.update(timeStep);
        m_txnMngr.update(timeStep);
        m_reconnectMngr_longTerm.update(timeStep);
        m_reconnectMngr_shortTerm.update(timeStep);
        m_rssiPollMngr.update(timeStep);
    }

    final void bond_justAddTheTask(E_TransactionLockBehavior lockBehavior, boolean isDirect)
    {
        if (conf_device().forceBondDialog)
        {
            taskManager().add(new P_Task_BondPopupHack(this, null));
        }
        taskManager().add(new P_Task_Bond(this, /*isExplicit=*/true, isDirect, /*partOfConnection=*/false, m_taskStateListener, lockBehavior));
    }

    final void unbond_justAddTheTask()
    {
        unbond_justAddTheTask(null);
    }

    final void unbond_justAddTheTask(final PE_TaskPriority priority_nullable)
    {
        taskManager().add(new P_Task_Unbond(this, m_taskStateListener, priority_nullable));
    }

    final void unbond_internal(final PE_TaskPriority priority_nullable, final BondListener.Status status)
    {
        // If the unbond task is already in the queue, then do nothing
        if (!taskManager().isInQueue(P_Task_Unbond.class, this))
        {
            unbond_justAddTheTask(priority_nullable);

            final boolean wasBonding = is(BONDING);

            stateTracker_updateBoth(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_BondManager.OVERRIDE_UNBONDED_STATES);

            if (wasBonding)
            {
                m_bondMngr.invokeCallback(status, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);
            }
        }
    }

    final P_DeviceServiceManager serviceMngr_device()
    {
        return getServiceManager();
    }

    private ConnectFailEvent connect_earlyOut()
    {
        if (isNull())
        {
            final ConnectFailEvent e = ConnectFailEvent.EARLY_OUT(this, Status.NULL_DEVICE);

            m_connectionFailMngr.invokeCallback(e);

            return e;
        }

        return null;
    }

    final void attemptReconnect()
    {
        if (connect_earlyOut() != null) return;

        m_lastConnectOrDisconnectWasUserExplicit = true;

        if (isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
        {
            final ConnectFailEvent info = ConnectFailEvent.EARLY_OUT(this, Status.ALREADY_CONNECTING_OR_CONNECTED);

            m_connectionFailMngr.invokeCallback(info);

            return;
        }

        connect_private(m_txnMngr.m_authTxn, m_txnMngr.m_initTxn, /*isReconnect=*/true);
    }

    private BleTransaction.Auth getAuthTxn(BleTransaction.Auth txn)
    {
        if (txn != null)
        {
            return txn;
        }
        if (conf_device().defaultAuthFactory != null)
        {
            return conf_device().defaultAuthFactory.newAuthTxn();
        }
        return conf_device().defaultAuthTransaction;
    }

    private BleTransaction.Init getInitTxn(BleTransaction.Init txn)
    {
        if (txn != null)
        {
            return txn;
        }
        if (conf_device().defaultInitFactory != null)
        {
            return conf_device().defaultInitFactory.newInitTxn();
        }
        return conf_device().defaultInitTransaction;
    }

    private void connect_private(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, final boolean isReconnect)
    {
        if (is_internal(INITIALIZED))
        {
            getManager().ASSERT(false, "Device is initialized but not connected!");

            return;
        }

        BleTransaction.Auth auth = getAuthTxn(authenticationTxn);
        BleTransaction.Init init = getInitTxn(initTxn);

        m_txnMngr.onConnect(auth, init);

        final Object[] extraBondingStates;

        boolean needsBond = false;

        if (is(UNBONDED) && Utils.isKitKat())
        {
            final boolean tryBondingWhileDisconnected = BleDeviceConfig.bool(conf_device().tryBondingWhileDisconnected, conf_mngr().tryBondingWhileDisconnected);
            final boolean tryBondingWhileDisconnected_manageOnDisk = BleDeviceConfig.bool(conf_device().tryBondingWhileDisconnected_manageOnDisk, conf_mngr().tryBondingWhileDisconnected_manageOnDisk);
            final boolean autoBondFix = BleDeviceConfig.bool(conf_device().autoBondFixes, conf_mngr().autoBondFixes);
            needsBond = autoBondFix && (Utils.phoneHasBondingIssues() || BleDeviceConfig.bool(conf_device().alwaysBondOnConnect, conf_mngr().alwaysBondOnConnect));
            final boolean doPreBond = getManager().m_diskOptionsMngr.loadNeedsBonding(getMacAddress(), tryBondingWhileDisconnected_manageOnDisk) || needsBond;

            if (doPreBond && tryBondingWhileDisconnected)
            {
                needsBond = false;
                bond_justAddTheTask(E_TransactionLockBehavior.PASSES, /*isDirect=*/false);

                extraBondingStates = P_BondManager.OVERRIDE_BONDING_STATES;
            }
            else
            {
                extraBondingStates = P_BondManager.OVERRIDE_EMPTY_STATES;
            }
        }
        else
        {
            extraBondingStates = P_BondManager.OVERRIDE_EMPTY_STATES;
        }

        onConnecting(/* definitelyExplicit= */true, isReconnect, extraBondingStates, /*bleConnect=*/false);

        //--- DRK > Just accounting for technical possibility that user calls #disconnect() or something in the state change callback for connecting overall.
        if (!/*still*/is_internal(CONNECTING_OVERALL))
        {
            return;
        }

        taskManager().add(new P_Task_Connect(this, m_taskStateListener));

        if (needsBond)
        {
            bond_justAddTheTask(E_TransactionLockBehavior.PASSES, /*isDirect=*/false);
        }


        onConnecting(/* definitelyExplicit= */true, isReconnect, extraBondingStates, /*bleConnect=*/true);
    }

    final void onConnecting(boolean definitelyExplicit, boolean isReconnect, final Object[] extraBondingStates, final boolean bleConnect)
    {
        m_lastConnectOrDisconnectWasUserExplicit = definitelyExplicit;

        if (bleConnect && is_internal(/* already */CONNECTING))
        {
            P_Task_Connect task = getTaskManager().getCurrent(P_Task_Connect.class, this);
            boolean mostDefinitelyExplicit = task != null && task.isExplicit();

            //--- DRK > Not positive about this assert...we'll see if it trips.
            getManager().ASSERT(definitelyExplicit || mostDefinitelyExplicit);

            stateTracker_main().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, extraBondingStates);
        }
        else
        {
            final E_Intent intent;

            if (definitelyExplicit && !isReconnect)
            {
                //--- DRK > We're stopping the reconnect process (if it's running) because the user has decided to explicitly connect
                //--- for whatever reason. Making a judgement call that the user would then expect reconnect to stop.
                //--- In other words it's not stopped for any hard technical reasons...it could go on.
                m_reconnectMngr_longTerm.stop();
                intent = E_Intent.INTENTIONAL;
                stateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, RECONNECTING_LONG_TERM, false, CONNECTING, bleConnect, CONNECTING_OVERALL, true, DISCONNECTED, false, ADVERTISING, false, extraBondingStates);
            }
            else
            {
                intent = lastConnectDisconnectIntent();
                stateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, CONNECTING, bleConnect, CONNECTING_OVERALL, true, DISCONNECTED, false, ADVERTISING, false, extraBondingStates);
            }

            if (stateTracker() != stateTracker_main())
            {
                stateTracker_main().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNBONDED, stateTracker().is(UNBONDED), BONDING, stateTracker().is(BONDING), BONDED, stateTracker().is(BONDED));
            }
        }
    }

    final void onNativeConnect(boolean explicit)
    {
        m_lastDisconnectWasBecauseOfBleTurnOff = false; // DRK > Just being anal.

        E_Intent intent = explicit && !is(RECONNECTING_LONG_TERM) ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
        m_lastConnectOrDisconnectWasUserExplicit = intent == E_Intent.INTENTIONAL;

        if (is_internal(/*already*/CONNECTED))
        {
            //--- DRK > Possible to get here when implicit tasks are involved I think. Not sure if assertion should be here,
            //--- and if it should it perhaps should be keyed off whether the task is implicit or something.
            //--- Also possible to get here for example on connection fail retries, where we queue a disconnect
            //--- but that gets immediately soft-cancelled by what will be a redundant connect task.
            //--- OVERALL, This assert is here because I'm just curious how it hits (it does).
            String message = "nativelyConnected=" + logger().gattConn(m_nativeWrapper.getConnectionState()) + " gatt==" + m_nativeWrapper.getGatt();
            // getManager().ASSERT(false, message);
            getManager().ASSERT(m_nativeWrapper.isNativelyConnected(), message);

            return;
        }

        getManager().ASSERT(!layerManager().getGattLayer().isGattNull());

        //--- DRK > There exists a fringe case like this: You try to connect with autoConnect==true in the gatt object.
        //--- The connection fails, so you stop trying. Then you turn off the remote device. Device gets "undiscovered".
        //--- You turn the device back on, and apparently underneath the hood, this whole time, the stack has been trying
        //--- to reconnect, and now it does, *without* (re)discovering the device first, or even discovering it at all.
        //--- So as usual, here's another gnarly workaround to ensure a consistent API experience through SweetBlue.
        //
        //--- NOTE: We do explicitly disconnect after a connection failure if we're using autoConnect, so this
        //--- case shouldn't really come up much or at all with that in place.
        if (!getManager().hasDevice(getMacAddress()))
        {
            getManager().onDiscovered_fromRogueAutoConnect(this, /*newlyDiscovered=*/true, m_scanInfo.getServiceUUIDS(), getScanRecord(), getRssi());
        }

        //--- DRK > Some trapdoor logic for bad android ble bug.
        int nativeBondState = m_nativeWrapper.getNativeBondState();
        if (nativeBondState == BluetoothDevice.BOND_BONDED)
        {
            //--- DRK > Trying to catch fringe condition here of stack lying to
            // us about bonded state.
            //--- This is not about finding a logic error in my code.
            getManager().ASSERT(getManager().managerLayer().getBondedDevices().contains(m_nativeWrapper.getDevice()));
        }

        logger().d(logger().gattBondState(m_nativeWrapper.getNativeBondState()));

        boolean autoGetServices = BleDeviceConfig.bool(conf_device().autoGetServices, conf_mngr().autoGetServices);
        if (autoGetServices)
        {
            getServices(DISCONNECTED, false, CONNECTING_OVERALL, true, CONNECTING, false, CONNECTED, true, ADVERTISING, false);
        }
        else
        {
            m_txnMngr.runAuthOrInitTxnIfNeeded(BluetoothGatt.GATT_SUCCESS, DISCONNECTED, false, CONNECTING_OVERALL, true, CONNECTING, false, CONNECTED, true, ADVERTISING, false);
        }
    }

    private void getServices(Object... extraFlags)
    {
        if (!m_nativeWrapper.isNativelyConnected())
        {
            return;
        }

        boolean gattRefresh = BleDeviceConfig.bool(conf_device().useGattRefresh, conf_mngr().useGattRefresh);
        BleDeviceConfig.RefreshOption option = conf_device().gattRefreshOption != null ? conf_device().gattRefreshOption : conf_mngr().gattRefreshOption;
        gattRefresh = gattRefresh && option == BleDeviceConfig.RefreshOption.BEFORE_SERVICE_DISCOVERY;
        Interval refreshDelay = BleDeviceConfig.interval(conf_device().gattRefreshDelay, conf_mngr().gattRefreshDelay);
        taskManager().add(new P_Task_DiscoverServices(this, m_taskStateListener, gattRefresh, refreshDelay));

        //--- DRK > We check up top, but check again here cause we might have been disconnected on another thread in the mean time.
        //--- Even without this check the library should still be in a goodish state. Might send some weird state
        //--- callbacks to the app but eventually things settle down and we're good again.
        if (m_nativeWrapper.isNativelyConnected())
        {
            stateTracker().update(lastConnectDisconnectIntent(), BluetoothGatt.GATT_SUCCESS, extraFlags, DISCOVERING_SERVICES, true);
        }
    }

    final void onNativeConnectFail(PE_TaskState state, int gattStatus, ReconnectFilter.AutoConnectUsage autoConnectUsage)
    {
        m_nativeWrapper.closeGattIfNeeded(/* disconnectAlso= */true);

        if (state == PE_TaskState.SOFTLY_CANCELLED) return;

        boolean attemptingReconnect = is(RECONNECTING_LONG_TERM);
        BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

        // if( !m_nativeWrapper.isNativelyConnected() )
        // {
        // if( !attemptingReconnect )
        // {
        //--- DRK > Now doing this at top of method...no harm really and
        // catches fringe case logic erros upstream.
        // m_nativeWrapper.closeGattIfNeeded(/*disconnectAlso=*/true);
        // }
        // }

        final boolean wasConnecting = is_internal(CONNECTING_OVERALL);
        final DeviceReconnectFilter.Status connectionFailStatus = DeviceReconnectFilter.Status.NATIVE_CONNECTION_FAILED;

        m_txnMngr.cancelAllTransactions();

        if (Utils.phoneHasBondingIssues())
        {
            getTaskManager().clearQueueOf(P_Task_Bond.class, this, getTaskManager().getSize());
        }

        boolean retryingConnection = false;

        if (wasConnecting)
        {
            DeviceReconnectFilter.Timing timing = state == PE_TaskState.FAILED_IMMEDIATELY ? DeviceReconnectFilter.Timing.IMMEDIATELY : DeviceReconnectFilter.Timing.EVENTUALLY;

            if (state == PE_TaskState.TIMED_OUT)
            {
                timing = DeviceReconnectFilter.Timing.TIMED_OUT;
            }

            final int retry__PE_Please = m_connectionFailMngr.onConnectionFailed(connectionFailStatus, timing, attemptingReconnect, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, highestState, autoConnectUsage, NULL_READWRITE_EVENT());

            if (!attemptingReconnect && retry__PE_Please == DeviceReconnectFilter.ConnectFailPlease.PE_Please_RETRY_WITH_AUTOCONNECT_TRUE)
            {
                m_useAutoConnect = true;
            }
            else if (!attemptingReconnect && retry__PE_Please == DeviceReconnectFilter.ConnectFailPlease.PE_Please_RETRY_WITH_AUTOCONNECT_FALSE)
            {
                m_useAutoConnect = false;
            }
            else
            {
                m_useAutoConnect = m_alwaysUseAutoConnect;
            }
        }
        else
        {
            // This was moved into the onConnectionFailed method of the connectionfaillistener, so we can add the RETRYING_BLE_CONNECTION state while
            // setting the state to disconnected. This way, both states are set at the same time, eliminating any race conditions between those 2
            // states. This is here now in the case the connectionfaillistener doesn't get called, but the device is still in any connected/ing state
            if (isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
            {
                setStateToDisconnected(attemptingReconnect, retryingConnection, E_Intent.UNINTENTIONAL, gattStatus, /*forceMainStateTracker=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
            }
        }
    }

    final void onServicesDiscovered()
    {
        boolean autoNegotiateMtu = BleDeviceConfig.bool(conf_device().autoNegotiateMtuOnReconnect, conf_mngr().autoNegotiateMtuOnReconnect);
        if (autoNegotiateMtu && m_mtu > BleNodeConfig.DEFAULT_MTU_SIZE)
        {
            if (isAny(RECONNECTING_SHORT_TERM, RECONNECTING_LONG_TERM))
            {
                setMtu_private(m_mtu, null, PE_TaskPriority.FOR_PRIORITY_READS_WRITES);
            }
        }

        if (m_connectionPriority != BleConnectionPriority.MEDIUM)
        {
            if (isAny(RECONNECTING_SHORT_TERM, RECONNECTING_LONG_TERM))
            {
                setConnectionPriority_private(m_connectionPriority, null, PE_TaskPriority.FOR_PRIORITY_READS_WRITES);
            }
        }

        m_txnMngr.runAuthOrInitTxnIfNeeded(BluetoothGatt.GATT_SUCCESS, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
    }

    final void onFullyInitialized(final int gattStatus, Object... extraFlags)
    {
        m_reconnectMngr_longTerm.stop();
        m_reconnectMngr_shortTerm.stop();
        m_connectionFailMngr.onFullyInitialized();

        //--- DRK > Saving last disconnect as unintentional here in case for some
        //--- reason app is hard killed or something and we never get a disconnect callback.
        final boolean hitDisk = BleDeviceConfig.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
        getManager().m_diskOptionsMngr.saveLastDisconnect(getMacAddress(), State.ChangeIntent.UNINTENTIONAL, hitDisk);

        stateTracker().update(lastConnectDisconnectIntent(), gattStatus, extraFlags,
                RECONNECTING_LONG_TERM, false, CONNECTING_OVERALL, false,
                AUTHENTICATING, false, AUTHENTICATED, true, INITIALIZING, false,
                INITIALIZED, true, RETRYING_BLE_CONNECTION, false);

        stateTracker_main().remove(BleDeviceState.RECONNECTING_SHORT_TERM, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        final DeviceConnectListener.ConnectEvent event = new DeviceConnectListener.ConnectEvent(this, null);

        invokeConnectCallbacks(event);

    }

    final void invokeConnectCallbacks(DeviceConnectListener.ConnectEvent event)
    {
        // Post to the ephemeral listener first, if it's not null
        if (m_ephemeralConnectListener != null && m_ephemeralConnectListener.get() != null)
        {
            m_ephemeralConnectListener.get().onEvent(event);
        }

        // Now post to the default listener, if there is one
        DeviceConnectListener listener = getListener_Connect();

        if (listener != null)
            listener.onEvent(event);

        // Now post to the manager's listener, if there is one
        listener = getManager().m_defaultDeviceConnectListener;
        if (listener != null)
            listener.onEvent(event);
    }

    final void setStateToDisconnected(final boolean attemptingReconnect_longTerm, final boolean retryingConnection, final E_Intent intent, final int gattStatus, final boolean forceMainStateTracker, final Object[] overrideBondingStates)
    {
        //--- DRK > Device probably wasn't advertising while connected so here we reset the timer to keep
        //--- it from being immediately undiscovered after disconnection.
        m_timeSinceLastDiscovery = 0.0;

        m_txnMngr.clearQueueLock();

        final P_DeviceStateTracker tracker = forceMainStateTracker ? stateTracker_main() : stateTracker();

        final int bondState = m_nativeWrapper.getNativeBondState();

        tracker.set
                (
                        intent,
                        gattStatus,
                        DISCOVERED, true,
                        DISCONNECTED, true,
                        // Commenting these out because of un-thought-of case where you unbond then immediately disconnect...native bond state is still BONDED but abstracted state is UNBONDED so a state transition occurs where it shouldn't.
                        // Uncommenting these out to reflect actual bond state when the device gets disconnected
			            BONDING, m_nativeWrapper.isNativelyBonding(bondState),
			            BONDED, m_nativeWrapper.isNativelyBonded(bondState),
			            UNBONDED, m_nativeWrapper.isNativelyUnbonded(bondState),
                        RETRYING_BLE_CONNECTION, retryingConnection,
                        RECONNECTING_LONG_TERM, attemptingReconnect_longTerm,
                        ADVERTISING, !attemptingReconnect_longTerm && m_origin_latest == BleDeviceOrigin.FROM_DISCOVERY

//                        overrideBondingStates
                );

        if (tracker != stateTracker_main())
        {
            stateTracker_main().update
                    (
                            intent,
                            gattStatus,
                            BONDING, tracker.is(BONDING),
                            BONDED, tracker.is(BONDED),
                            UNBONDED, tracker.is(UNBONDED)
                    );
        }
    }

    final void disconnectWithReason(DeviceReconnectFilter.Status connectionFailReasonIfConnecting, Timing timing, int gattStatus, int bondFailReason, ReadWriteListener.ReadWriteEvent txnFailReason)
    {
        disconnectWithReason(null, connectionFailReasonIfConnecting, timing, gattStatus, bondFailReason, false, txnFailReason);
    }

    final void disconnectWithReason(final PE_TaskPriority disconnectPriority_nullable, final DeviceReconnectFilter.Status connectionFailReasonIfConnecting, final Timing timing, final int gattStatus, final int bondFailReason, final boolean undiscoverAfter, final ReadWriteListener.ReadWriteEvent txnFailReason)
    {
        getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
        {
            @Override public void run()
            {

                // If there is already a disconnect task in the queue, then simply ignore this (in the case an explicit disconnect comes in while
                // a transaction is processing...the transaction will call this method as well)
                boolean inQueue = getTaskManager().isInQueue(P_Task_Disconnect.class, BleDevice.this);

                if (!inQueue)
                {

                    if (isNull()) return;

                    final boolean cancelled = connectionFailReasonIfConnecting != null && connectionFailReasonIfConnecting.wasCancelled();
                    final boolean explicit = connectionFailReasonIfConnecting != null && connectionFailReasonIfConnecting.wasExplicit();
                    final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

                    if (explicit)
                    {
                        m_reconnectMngr_shortTerm.stop();
                    }

                    if (cancelled)
                    {
                        m_useAutoConnect = m_alwaysUseAutoConnect;

                        m_connectionFailMngr.onExplicitDisconnect();
                    }

                    final boolean wasConnecting = is_internal(CONNECTING_OVERALL);
                    final boolean attemptingReconnect_shortTerm = is(RECONNECTING_SHORT_TERM);
                    final boolean attemptingReconnect_longTerm = cancelled ? false : is(RECONNECTING_LONG_TERM);

                    E_Intent intent = cancelled ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
                    m_lastConnectOrDisconnectWasUserExplicit = intent == E_Intent.INTENTIONAL;

                    final boolean cancellableFromConnect = BleDeviceConfig.bool(conf_device().disconnectIsCancellable, conf_mngr().disconnectIsCancellable);
                    final boolean tryBondingWhileDisconnected = connectionFailReasonIfConnecting == Status.BONDING_FAILED && BleDeviceConfig.bool(conf_device().tryBondingWhileDisconnected, conf_mngr().tryBondingWhileDisconnected);
                    final boolean underwentPossibleImplicitBondingAttempt = m_nativeWrapper.isNativelyUnbonded() && m_underwentPossibleImplicitBondingAttempt == true;
                    final boolean taskIsCancellable = cancellableFromConnect == true && tryBondingWhileDisconnected == false && underwentPossibleImplicitBondingAttempt == false;

                    //--- DRK > Had this here but for a final connection failure it would add one more bond attempt after disconnected, which didn't make sense.
                    //---		Now all handled before connection.
//		if( tryBondingWhileDisconnected )
//		{
//			bond_justAddTheTask(E_TransactionLockBehavior.DOES_NOT_PASS);
//		}

//		if (isAny_internal(CONNECTED, CONNECTING_OVERALL, INITIALIZED))
                    {
                        saveLastDisconnect(explicit);

                        final boolean saveLastDisconnectAfterTaskCompletes = connectionFailReasonIfConnecting != Status.ROGUE_DISCONNECT;

                        final int taskOrdinal;
                        final boolean clearQueue;

                        if (isAny_internal(CONNECTED, CONNECTING, INITIALIZED))
                        {
                            final P_Task_Disconnect disconnectTask = new P_Task_Disconnect(BleDevice.this, m_taskStateListener, /*explicit=*/explicit, disconnectPriority_nullable, taskIsCancellable, saveLastDisconnectAfterTaskCompletes);
                            taskManager().add(disconnectTask);

                            taskOrdinal = disconnectTask.getOrdinal();
                            clearQueue = true;

                            //--- DRK > Taking this out because the problem is this invokes
                            //---		callbacks to appland for e.g. a read failing because of EXPLICIT_DISCONNECT before the BleDeviceState change below.
                            //---		This is now moved to the clearQueue if block below so that callbacks to appland get sent *after* disconnect state change.
//				m_queue.clearQueueOf(PA_Task_RequiresConnection.class, this);
                        }
                        else
                        {
                            taskOrdinal = -1;
                            clearQueue = false;
                        }

                        final Object[] overrideBondingStates = m_bondMngr.getOverrideBondStatesForDisconnect(connectionFailReasonIfConnecting);
                        final boolean forceMainStateTracker = explicit;

                        // Commenting this out now. We should wait for the native callback to say if we're disconnected or not.
//                    setStateToDisconnected(attemptingReconnect_longTerm, intent, gattStatus, forceMainStateTracker, overrideBondingStates);

                        m_txnMngr.cancelAllTransactions();

                        if (clearQueue)
                        {
                            taskManager().clearQueueOf(P_Task_Connect.class, BleDevice.this, -1);
                            taskManager().clearQueueOf(PA_Task_RequiresConnection.class, BleDevice.this, taskOrdinal);
                        }

                        if (!attemptingReconnect_longTerm)
                        {
                            m_reconnectMngr_longTerm.stop();
                        }
                    }
//		else
//		{
//			if (!attemptingReconnect_longTerm)
//			{
//				stateTracker().update(intent, gattStatus, RECONNECTING_LONG_TERM, false);
//
//				m_reconnectMngr_longTerm.stop();
//			}
//		}

                    if (wasConnecting || attemptingReconnect_shortTerm)
                    {
                        if (getManager().ASSERT(connectionFailReasonIfConnecting != null))
                        {
                            m_connectionFailMngr.onConnectionFailed(connectionFailReasonIfConnecting, timing, attemptingReconnect_longTerm, gattStatus, bondFailReason, highestState, ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE, txnFailReason);
                        }
                    }
                }

                if (undiscoverAfter)
                    getManager().m_deviceMngr.undiscoverAndRemove(BleDevice.this, getManager().m_discoveryListener, getManager().m_deviceMngr_cache, E_Intent.INTENTIONAL);
            }
        });
    }

    final boolean lastDisconnectWasBecauseOfBleTurnOff()
    {
        return m_lastDisconnectWasBecauseOfBleTurnOff;
    }

    private void saveLastDisconnect(final boolean explicit)
    {
        if (!is(INITIALIZED)) return;

        final boolean hitDisk = BleDeviceConfig.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);

        if (explicit)
        {
            getManager().m_diskOptionsMngr.saveLastDisconnect(getMacAddress(), State.ChangeIntent.INTENTIONAL, hitDisk);
        }
        else
        {
            getManager().m_diskOptionsMngr.saveLastDisconnect(getMacAddress(), State.ChangeIntent.UNINTENTIONAL, hitDisk);
        }
    }

    final void onNativeDisconnect(final boolean wasExplicit, final int gattStatus, final boolean attemptShortTermReconnect, final boolean saveLastDisconnect)
    {
        if (!wasExplicit && !attemptShortTermReconnect)
        {
            //--- DRK > Just here so it's easy to filter out in logs.
            logger().w("Disconnected Implicitly and attemptShortTermReconnect=" + attemptShortTermReconnect);
        }

        m_lastDisconnectWasBecauseOfBleTurnOff = getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF);
        m_lastConnectOrDisconnectWasUserExplicit = wasExplicit;

        final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

        if (saveLastDisconnect)
        {
            saveLastDisconnect(wasExplicit);
        }

        m_pollMngr.resetNotifyStates();

//		if( attemptShortTermReconnect )
        {
            m_nativeWrapper.closeGattIfNeeded(/* disconnectAlso= */false);
        }

        final int overrideOrdinal = getManager().getTaskManager().getCurrentOrdinal();

        final boolean wasInitialized = is(INITIALIZED);

        if (attemptShortTermReconnect)
        {
            if (!wasExplicit && wasInitialized && !m_reconnectMngr_shortTerm.isRunning())
            {
                m_stateTracker_shortTermReconnect.sync(stateTracker_main());
                m_reconnectMngr_shortTerm.attemptStart(gattStatus);

                if (m_reconnectMngr_shortTerm.isRunning())
                {
                    stateTracker_main().append(BleDeviceState.RECONNECTING_SHORT_TERM, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
                }
            }
        }

        final boolean isDisconnectedAfterReconnectingShortTermStateCallback = is(DISCONNECTED) && is(RECONNECTING_SHORT_TERM);
        final boolean isConnectingBle = is(CONNECTING);
        final boolean ignoreKindOf = isConnectingBle && wasExplicit;
        final boolean cancelTasks;

        if (!ignoreKindOf)
        {
            if (isDisconnectedAfterReconnectingShortTermStateCallback/* || wasExplicit*/)
            {
                m_connectionFailMngr.onExplicitDisconnect();

                cancelTasks = false;
            }
            else
            {
                cancelTasks = true;
            }
        }
        else
        {
            cancelTasks = false;
        }

        //--- DRK > Fringe case bail out in case user calls disconnect() in state change for short term reconnect.
        if (isDisconnectedAfterReconnectingShortTermStateCallback)
        {
            m_txnMngr.cancelAllTransactions();

            return;
        }

        final boolean isAttemptingReconnect_longTerm = is_internal(RECONNECTING_LONG_TERM);
        final boolean wasConnectingOverall = is(CONNECTING_OVERALL);

        if (ignoreKindOf)
        {
            //--- DRK > Shouldn't be possible to get here for now, just being future-proof.
            if (cancelTasks)
            {
                softlyCancelTasks(overrideOrdinal);
            }

            m_txnMngr.cancelAllTransactions();

            // We want to make sure that we update the state here. If you call disconnect() when currently connecting, the state won't get update, unless this is here
            final E_Intent intent = wasExplicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
            setStateToDisconnected(isAttemptingReconnect_longTerm, false, intent, gattStatus, /*forceMainStateTracker=*/attemptShortTermReconnect == false, P_BondManager.OVERRIDE_EMPTY_STATES);

            return;
        }

        // BEGIN CALLBACKS TO USER

        final E_Intent intent = wasExplicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
        setStateToDisconnected(isAttemptingReconnect_longTerm, false, intent, gattStatus, /*forceMainStateTracker=*/attemptShortTermReconnect == false, P_BondManager.OVERRIDE_EMPTY_STATES);

        //--- DRK > Technically user could have called connect() in callbacks above....bad form but we need to account for it.
        final boolean isConnectingOverall_1 = is_internal(CONNECTING_OVERALL);
        final boolean isStillAttemptingReconnect_longTerm = is_internal(RECONNECTING_LONG_TERM);
        final DeviceReconnectFilter.Status connectionFailReason_nullable;
        if (!m_reconnectMngr_shortTerm.isRunning() && wasConnectingOverall && !wasExplicit)
        {
            if (getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF))
            {
                connectionFailReason_nullable = DeviceReconnectFilter.Status.BLE_TURNING_OFF;
            }
            else
            {
                connectionFailReason_nullable = DeviceReconnectFilter.Status.ROGUE_DISCONNECT;
            }
        }
        else
        {
            connectionFailReason_nullable = null;
        }

        //--- DRK > Originally had is(DISCONNECTED) here, changed to is_internal, but then realized
        //---		you probably want to (and it's safe to ) cancel all transactions all the time.
        //---		I think the original intent was to account for the faulty assumption that someone
        //---		would call connect again themselves in the state callback and somehow cancel the
        //---		new transaction passed to connect()...BUT this can't happen cause the actual connect
        //---		task has to run (even if it's redundant), and services have to be discovered.
//						if (is_internal(DISCONNECTED))
        {
            m_txnMngr.cancelAllTransactions();
        }

        //--- DRK > This was originally where cancelTasks = true; is now placed, before disconnected state change. Putting it after because of the following scenario:
        //---		(1) Write task takes a long time (timeout scenario). Specifically, tab 4 onCharacteristicWrite gets called only internally (doesn't make it to callback) then keeps spinning.
        //---		(2) An unsolicited disconnect comes in but we don't get a callback for the write.
        //---		(3) Before, the soft cancellation was done before the state change, which made the connection failure reason due to authentication failing, not an unsolicited disconnect like it should be.
        if (cancelTasks)
        {
            softlyCancelTasks(overrideOrdinal);
        }

        final int retrying__PE_Please;

        if (!isConnectingOverall_1 && !m_reconnectMngr_shortTerm.isRunning())
        {
            if (connectionFailReason_nullable != null && wasExplicit)
            {
                // If we're already disconnected, then this is the native callback coming back, and we've already sent the connectionfail event back to the user
                // So we don't need to post the event again here.
                if (!isDisconnectedAfterReconnectingShortTermStateCallback)
                {
                    retrying__PE_Please = m_connectionFailMngr.onConnectionFailed(connectionFailReason_nullable, Timing.NOT_APPLICABLE, isStillAttemptingReconnect_longTerm, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, highestState, ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE, NULL_READWRITE_EVENT());
                }
                else
                {
                    retrying__PE_Please = DeviceReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY;
                }
            }
            else
            {
                if (m_connectionFailMngr.hasPendingConnectionFailEvent())
                {
                    retrying__PE_Please = m_connectionFailMngr.getPendingConnectionFailRetry();
                    m_connectionFailMngr.clearPendingRetry();
                }
                else
                {
                    retrying__PE_Please = m_connectionFailMngr.onConnectionFailed(connectionFailReason_nullable, Timing.NOT_APPLICABLE, isStillAttemptingReconnect_longTerm, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, highestState, ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE, NULL_READWRITE_EVENT());
//                    retrying__PE_Please = ConnectionFailListener.Please.PE_Please_DO_NOT_RETRY;PE_Please_DO_NOT_RETRY
                }
            }
        }
        else
        {
            retrying__PE_Please = DeviceReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY;
        }

        //--- DRK > Again, technically user could have called connect() in callbacks above....bad form but we need to account for it.
        final boolean isConnectingOverall_2 = is_internal(CONNECTING_OVERALL);

        if (!m_reconnectMngr_shortTerm.isRunning() && !m_reconnectMngr_longTerm.isRunning() && !wasExplicit && wasInitialized && !isConnectingOverall_2)
        {
            m_reconnectMngr_longTerm.attemptStart(gattStatus);

            if (m_reconnectMngr_longTerm.isRunning())
            {
                stateTracker_main().append(RECONNECTING_LONG_TERM, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
            }
        }

        //--- DRK > Throwing in one last disconnect if it looks like we just came out of a short term reconnect
        //---		that was connected and (e.g.) getting services and so this method was called but no long term reconnect was started
        //---		so we have to bail out.
        if (is(DISCONNECTED) && !is(RECONNECTING_LONG_TERM) && m_reconnectMngr_longTerm.isRunning() == false && m_reconnectMngr_shortTerm.isRunning() == false)
        {
            if (m_nativeWrapper.isNativelyConnectingOrConnected())
            {
                taskManager().add(new P_Task_Disconnect(this, m_taskStateListener, /*explicit=*/false, null, /*cancellable=*/true));
            }
        }

        //--- DRK > Not actually entirely sure how, it may be legitimate, but a connect task can still be
        //--- hanging out in the queue at this point, so we just make sure to clear the queue as a failsafe.
        //--- TODO: Understand the conditions under which a connect task can still be queued...might be a bug upstream.
        if (!isConnectingOverall_2 && retrying__PE_Please == DeviceReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY)
        {
            taskManager().clearQueueOf(P_Task_Connect.class, this, -1);
        }

        boolean doReconnectForConnectingOverall = BleDeviceConfig.bool(conf_device().connectFailRetryConnectingOverall, conf_mngr().connectFailRetryConnectingOverall);

        if (doReconnectForConnectingOverall && !wasExplicit && !wasInitialized && retrying__PE_Please != DeviceReconnectFilter.ConnectFailPlease.PE_Please_DO_NOT_RETRY)
        {
            attemptReconnect();
        }
    }

    private void softlyCancelTasks(final int overrideOrdinal)
    {
        m_dummyDisconnectTask.setOverrideOrdinal(overrideOrdinal);
        taskManager().softlyCancelTasks(m_dummyDisconnectTask);
        taskManager().clearQueueOf(PA_Task_RequiresConnection.class, this, overrideOrdinal);
    }

    private void stopPoll_private(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Double interval, final ReadWriteListener listener)
    {
        m_pollMngr.stopPoll(serviceUuid, characteristicUuid, descriptorFilter, interval, listener, /* usingNotify= */false);
    }

    final ReadWriteListener.ReadWriteEvent read_internal(final Type type, final BleRead read)
    {

        final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(read, type, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(read.readWriteListener, earlyOutResult);

            return earlyOutResult;
        }

        if (read.descriptorUuid == null || read.descriptorUuid.equals(Uuids.INVALID))
        {
            final boolean requiresBonding = m_bondMngr.bondIfNeeded(read.charUuid, BondFilter.CharacteristicEventType.READ);
            final P_Task_Read task = new P_Task_Read(this, read, type, requiresBonding, m_txnMngr.getCurrent(), getOverrideReadWritePriority());
            taskManager().add(task);
        }
        else
        {
            final boolean requiresBonding = false;

            taskManager().add(new P_Task_ReadDescriptor(this, read, type, requiresBonding, m_txnMngr.getCurrent(), getOverrideReadWritePriority()));
        }

        return NULL_READWRITE_EVENT();
    }

    final ReadWriteListener.ReadWriteEvent write_internal(final BleWrite wb)
    {
        final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(wb, Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(wb.readWriteListener, earlyOutResult);

            return earlyOutResult;
        }

        if (wb.descriptorUuid == null || wb.descriptorUuid.equals(Uuids.INVALID))
        {
            final BleCharacteristicWrapper characteristic = getServiceManager().getCharacteristic(wb.serviceUuid, wb.charUuid);

            final boolean requiresBonding = m_bondMngr.bondIfNeeded(characteristic.getCharacteristic().getUuid(), BondFilter.CharacteristicEventType.WRITE);

            addWriteTasks(wb, requiresBonding);
        }
        else
        {
            final boolean requiresBonding = false;

            addWriteDescriptorTasks(wb, requiresBonding);
        }

        return NULL_READWRITE_EVENT();
    }

    private void addWriteDescriptorTasks(BleWrite write, boolean requiresBonding)
    {
        int mtuSize = getEffectiveWriteMtuSize();
        if (!conf_device().autoStripeWrites || write.m_data.getData().length < mtuSize)
        {
            taskManager().add(new P_Task_WriteDescriptor(this, write, requiresBonding, m_txnMngr.getCurrent(), getOverrideReadWritePriority()));
        }
        else
        {
            P_StripedWriteDescriptorTransaction descTxn = new P_StripedWriteDescriptorTransaction(write, requiresBonding);
            performTransaction(descTxn);
        }
    }

    private void addWriteTasks(BleWrite write, boolean requiresBonding)
    {
        int mtuSize = getEffectiveWriteMtuSize();
        if (!conf_device().autoStripeWrites || write.m_data.getData().length <= mtuSize)
        {
            final P_Task_Write task_write = new P_Task_Write(this, write, requiresBonding, m_txnMngr.getCurrent(), getOverrideReadWritePriority());
            taskManager().add(task_write);
        }
        else
        {
            P_StripedWriteTransaction stripedTxn = new P_StripedWriteTransaction(write, requiresBonding, write.writeType);
            performTransaction(stripedTxn);
        }
    }

    private ReadWriteListener.ReadWriteEvent disableNotify_private(BleNotify notify)
    {

        final ReadWriteEvent earlyOutResult = serviceMngr_device().getEarlyOutEvent(notify, Type.DISABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(notify.readWriteListener, earlyOutResult);

            return earlyOutResult;
        }

        final BleCharacteristicWrapper characteristic = getServiceManager().getCharacteristic(notify.serviceUuid, notify.charUuid);

        if (!characteristic.isNull() && is(CONNECTED))
        {
            final P_Task_ToggleNotify task = new P_Task_ToggleNotify(this, notify, false, m_txnMngr.getCurrent(), getOverrideReadWritePriority());
            taskManager().add(task);
        }

        m_pollMngr.stopPoll(notify.serviceUuid, notify.charUuid, notify.descriptorFilter, notify.m_forceReadTimeout.secs(), notify.readWriteListener, /* usingNotify= */true);

        return NULL_READWRITE_EVENT();
    }

    final E_Intent lastConnectDisconnectIntent()
    {
        if (m_lastConnectOrDisconnectWasUserExplicit == null)
        {
            return E_Intent.UNINTENTIONAL;
        }
        else
        {
            return m_lastConnectOrDisconnectWasUserExplicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
        }
    }

    final PE_TaskPriority getOverrideReadWritePriority()
    {
        if (isAny(AUTHENTICATING, INITIALIZING))
        {
            getManager().ASSERT(m_txnMngr.getCurrent() != null);

            return PE_TaskPriority.FOR_PRIORITY_READS_WRITES;
        }
        else
        {
            return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
        }
    }

    final void invokeReadWriteCallback(final ReadWriteListener listener_nullable, final ReadWriteListener.ReadWriteEvent event)
    {
        if (event.wasSuccess() && event.isRead() && event.target() == ReadWriteListener.Target.CHARACTERISTIC)
        {
            final EpochTime timestamp = new EpochTime();
            final BleNodeConfig.HistoricalDataLogFilter.Source source = event.type().toHistoricalDataSource();

            m_historicalDataMngr.add_single(event.charUuid(), event.data(), timestamp, source);
        }

        m_txnMngr.onReadWriteResult(event);

        if (listener_nullable != null)
        {
            postEventAsCallback(listener_nullable, event);
        }

        if (getListener_ReadWrite() != null)
        {
            postEventAsCallback(getListener_ReadWrite(), event);
        }

        if (getManager() != null && getManager().m_defaultReadWriteListener != null)
        {
            postEventAsCallback(getManager().m_defaultReadWriteListener, event);
        }

        final boolean isNotificationType = (event.type().isNotification() || event.type() == Type.DISABLING_NOTIFICATION || event.type() == Type.ENABLING_NOTIFICATION);

        if (getListener_Notification() != null && isNotificationType)
        {
            postEventAsCallback(getListener_Notification(), fromReadWriteEvent(event));
        }

        if (getManager() != null && getManager().m_defaultNotificationListener != null && isNotificationType)
        {
            postEventAsCallback(getManager().m_defaultNotificationListener, fromReadWriteEvent(event));
        }

        m_txnMngr.onReadWriteResultCallbacksCalled();
    }

    private NotificationListener.NotificationEvent fromReadWriteEvent(ReadWriteEvent event)
    {
        NotificationListener.Type type;
        switch (event.type())
        {
            case INDICATION:
                type = NotificationListener.Type.INDICATION;
                break;
            case PSUEDO_NOTIFICATION:
                type = NotificationListener.Type.PSEUDO_NOTIFICATION;
                break;
            case DISABLING_NOTIFICATION:
                type = NotificationListener.Type.DISABLING_NOTIFICATION;
                break;
            case ENABLING_NOTIFICATION:
                type = NotificationListener.Type.ENABLING_NOTIFICATION;
                break;
            default:
                type = NotificationListener.Type.NOTIFICATION;
                break;
        }
        NotificationListener.Status status;
        switch (event.status())
        {
            case SUCCESS:
                status = NotificationListener.Status.SUCCESS;
                break;
            case NULL:
                status = NotificationListener.Status.NULL;
                break;
            case ANDROID_VERSION_NOT_SUPPORTED:
                status = NotificationListener.Status.ANDROID_VERSION_NOT_SUPPORTED;
                break;
            case CANCELLED_FROM_BLE_TURNING_OFF:
                status = NotificationListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
                break;
            case CANCELLED_FROM_DISCONNECT:
                status = NotificationListener.Status.CANCELLED_FROM_DISCONNECT;
                break;
            case EMPTY_DATA:
                status = NotificationListener.Status.EMPTY_DATA;
                break;
            case INVALID_DATA:
                status = NotificationListener.Status.INVALID_DATA;
                break;
            case NULL_DATA:
                status = NotificationListener.Status.NULL_DATA;
                break;
            case NO_MATCHING_TARGET:
                status = NotificationListener.Status.NO_MATCHING_TARGET;
                break;
            case NOT_CONNECTED:
                status = NotificationListener.Status.NOT_CONNECTED;
                break;
            case FAILED_TO_TOGGLE_NOTIFICATION:
                status = NotificationListener.Status.FAILED_TO_TOGGLE_NOTIFICATION;
                break;
            case REMOTE_GATT_FAILURE:
                status = NotificationListener.Status.REMOTE_GATT_FAILURE;
                break;
            default:
                status = NotificationListener.Status.UNKNOWN_ERROR;
                break;
        }
        return new NotificationListener.NotificationEvent(this, event.serviceUuid(), event.charUuid(), type, event.data(), status, event.gattStatus(), event.time_total().secs(), event.time_ota().secs(), event.solicited());
    }

    final ReadWriteListener.ReadWriteEvent NULL_READWRITE_EVENT()
    {
        if (m_nullReadWriteEvent != null)
        {
            return m_nullReadWriteEvent;
        }

        m_nullReadWriteEvent = ReadWriteListener.ReadWriteEvent.NULL(this);

        return m_nullReadWriteEvent;
    }

    final ConnectFailEvent NULL_CONNECTIONFAIL_INFO()
    {
        if (m_nullConnectionFailEvent != null)
        {
            return m_nullConnectionFailEvent;
        }

        m_nullConnectionFailEvent = ConnectFailEvent.NULL(this);

        return m_nullConnectionFailEvent;
    }

    final BondListener.BondEvent NULL_BOND_EVENT()
    {
        if (m_nullBondEvent != null)
        {
            return m_nullBondEvent;
        }

        m_nullBondEvent = BondListener.BondEvent.NULL(this);

        return m_nullBondEvent;
    }

    /**
     * Returns <code>true</code> if <code>this</code> is referentially equal to {@link #NULL}.
     */
    @Override public final boolean isNull()
    {
        return m_isNull;
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getNext()
    {
        return getManager().getDevice_next(this);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice, BleDeviceState)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getNext(final BleDeviceState state)
    {
        return getManager().getDevice_next(this, state);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice, Object...)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getNext(final Object... query)
    {
        return getManager().getDevice_next(this, query);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getPrevious()
    {
        return getManager().getDevice_previous(this);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice, BleDeviceState)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getPrevious(final BleDeviceState state)
    {
        return getManager().getDevice_previous(this, state);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice, Object...)}.
     */
    public final @Nullable(Prevalence.NEVER) BleDevice getPrevious(final Object... query)
    {
        return getManager().getDevice_previous(this, query);
    }

    /**
     * Convenience forwarding of {@link BleManager#getDeviceIndex(BleDevice)}.
     */
    public final int getIndex()
    {
        return getManager().getDeviceIndex(this);
    }

    /**
     * Spells out "Decaff Coffee"...clever, right? I figure all zeros or
     * something would actually have a higher chance of collision in a dev
     * environment.
     */
    static String NULL_MAC()
    {
        return "DE:CA:FF:C0:FF:EE";
    }

    static String NULL_STRING()
    {
        return "NULL";
    }
    // static String NULL_MAC = "DE:AD:BE:EF:BA:BE";

    void postEventAsCallback(final GenericListener_Void listener, final Event event)
    {
        if (listener != null)
        {
            if (listener instanceof PA_CallbackWrapper)
            {
                getManager().getPostManager().runOrPostToUpdateThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (listener != null)
                        {
                            listener.onEvent(event);
                        }
                    }
                });
            }
            else
            {
                getManager().getPostManager().postCallback(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (listener != null)
                        {
                            listener.onEvent(event);
                        }
                    }
                });
            }
        }
    }


}
