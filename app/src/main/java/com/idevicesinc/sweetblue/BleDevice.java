package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.listeners.BondListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Status;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Timing;
import com.idevicesinc.sweetblue.listeners.DeviceStateListener;
import com.idevicesinc.sweetblue.listeners.DiscoveryListener;
import com.idevicesinc.sweetblue.listeners.NotifyListener;
import com.idevicesinc.sweetblue.listeners.P_BaseConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Utils_Rssi;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.Map;
import java.util.UUID;

import static com.idevicesinc.sweetblue.BleDeviceState.*;


public class BleDevice extends BleNode
{

    public final static BleDevice NULL = new BleDevice(null, null, null, null, null, true);
    public final static String NULL_STRING = "NULL";
    static DeviceConnectionFailListener DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultConnectionFailListener();


    private final boolean mIsNull;
    private int mRssi;
    private final P_DeviceStateTracker mStateTracker;
    private final P_DeviceStateTracker mStateTracker_shortTermReconnect;
    private final BleDeviceOrigin mOrigin;
    private BleDeviceOrigin mOrigin_last;
    private BleDeviceConfig mConfig;
    private BleConnectionPriority mConnectionPriority = BleConnectionPriority.MEDIUM;
    private int mMtu = BleDeviceConfig.DEFAULT_MTU_SIZE;
    private ReadWriteEvent mNullReadWriteEvent;
    private BleScanInfo mScanInfo;
    private String mName_native;
    private String mName_scanRecord;
    String mName_device;
    private String mName_debug;
    //private DeviceConnectionFailListener mConnectionFailListener;
    final P_GattManager mGattManager;
    private P_ReconnectManager mReconnectManager;
    private BondListener mBondListener;
    private NotifyListener mNotifyListener;
    final P_TransactionManager mTxnManager;
    private long mLastDiscovery;
    private P_ConnectionFailManager mConnectionFailMgr;


    BleDevice(BleManager mgr, BluetoothDevice nativeDevice, BleDeviceOrigin origin, BleDeviceConfig config_nullable, String deviceName, boolean isNull)
    {
        super(mgr);
        mIsNull = isNull;
        mOrigin = origin;
        mOrigin_last = mOrigin;
        mName_device = deviceName;
        mConnectionFailMgr = new P_ConnectionFailManager(this);
        if (nativeDevice != null)
        {
            String[] address_split = nativeDevice.getAddress().split(":");
            String lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
            if (!mName_device.contains(lastFourOfMac))
            {
                mName_debug = Utils_String.concatStrings(mName_device.toLowerCase(), "_", lastFourOfMac);
            }
            else
            {
                mName_debug = mName_device;
            }
        }
        else
        {
            mName_debug = mName_device;
        }

        if (!mIsNull)
        {
            mStateTracker = new P_DeviceStateTracker(this, false);
            mStateTracker_shortTermReconnect = new P_DeviceStateTracker(this, true);
            stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, DISCONNECTED, true);
            mGattManager = new P_GattManager(this, nativeDevice);
            mGattManager.checkCurrentBondState();
            mReconnectManager = new P_ReconnectManager(this);
            mReconnectManager.setMaxReconnectTries(getConfig().reconnectionTries);
            mTxnManager = new P_TransactionManager(this);
        }
        else
        {
            mName_device = "NULL";
            mStateTracker = new P_DeviceStateTracker(this, false);
            mGattManager = null;
            mReconnectManager = null;
            stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, true);
            mStateTracker_shortTermReconnect = null;
            mTxnManager = null;
        }
    }

    public void setConfig(BleDeviceConfig config)
    {
        if (isNull())
        {
            return;
        }

        mConfig = config == null ? null : config.clone();

        mReconnectManager.setMaxReconnectTries(getConfig().reconnectionTries);
        // TODO - There's more to do here
    }

    @Override public BleDeviceConfig getConfig()
    {
        return mConfig == null ? getManager().mConfig : mConfig;
    }

    public void setStateListener(DeviceStateListener listener)
    {
        mStateTracker.setListener(listener);
    }

    public DeviceStateListener getStateListener()
    {
        return mStateTracker.mStateListener;
    }

    public void setConnectionFailListener(DeviceConnectionFailListener failListener)
    {
        mConnectionFailMgr.setConnectionFailListener(failListener);
    }

    public void setNotifyListener(NotifyListener listener)
    {
        mNotifyListener = listener;
    }

    public int getMtu()
    {
        return mMtu;
    }

    public boolean setMtu(int mtuSize, ReadWriteListener listener)
    {
        if (mtuSize > 22 && mtuSize <= 517 && is(CONNECTED))
        {
            getManager().mTaskManager.add(new P_Task_RequestMtu(this, null, mtuSize, listener));
            return true;
        }
        return false;
    }

    public BleConnectionPriority getConnectionPriority()
    {
        return mConnectionPriority;
    }

    public void setConnectionPriority(BleConnectionPriority priority, ReadWriteListener listener)
    {
        // TODO - Actually implement this.
    }

    /**
     * Returns the name returned from {@link BluetoothDevice#getName()}.
     */
    public String getName_native()
    {
        return mName_native;
    }

    /**
     * Returns the name of this device. This is our best guess for which name to use. First, it pulls the name from
     * {@link BluetoothDevice#getName()}. If that is null, it is then pulled from the scan record. If that is null,
     * then a name will be assigned &lt;No_Name_XX:XX&gt;, where XX:XX are the last 4 of the device's mac address.
     */
    public String getName()
    {
        return mName_device;
    }

    /**
     * Returns the name that was parsed from this device's scan record.
     */
    public String getName_scanRecord()
    {
        return mName_scanRecord;
    }

    public BluetoothDevice getNative()
    {
        return mGattManager.getNativeDevice();
    }

    public BluetoothGatt getNativeGatt()
    {
        return mGattManager.getGatt();
    }

    @Override public String getMacAddress()
    {
        return mGattManager.getMacAddress();
    }

    public int getTxPower()
    {
        return mScanInfo.getTxPower().value;
    }

    public int getManufacturerId()
    {
        return mScanInfo.getManufacturerId();
    }

    public byte[] getManufacturerData()
    {
        return mScanInfo.getManufacturerData();
    }

    public Map<UUID, byte[]> getAdvertisedServiceData()
    {
        return mScanInfo.getServiceData();
    }

    public boolean is(BleDeviceState state)
    {
        return state.overlaps(getStateMask());
    }

    public boolean isAny(BleDeviceState... states)
    {
        if (states != null)
        {
            for (int i = 0; i < states.length; i++)
            {
                if (is(states[i])) return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAll(int)
     */
    public boolean isAny(final int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) != 0x0;
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAny(int)
     */
    public boolean isAll(final int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) == mask_BleDeviceState;
    }

    public int getRssi()
    {
        return mRssi;
    }

    public Percent getRssiPercent()
    {
        if (isNull())
        {
            return Percent.ZERO;
        }
        final double percent = Utils_Rssi.percent(getRssi(), getConfig().rssi_min, getConfig().rssi_max);
        return Percent.fromDouble_clamped(percent);
    }

    public boolean isNull()
    {
        return mIsNull;
    }

    @Override P_ServiceManager newServiceManager()
    {
        return new P_DeviceServiceManager(this);
    }

    public BleDeviceOrigin getOrigin()
    {
        return mOrigin;
    }

    public boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleDevice device_nullable)
    {
        if (device_nullable == null) return false;
        if (device_nullable == this) return true;
        if (device_nullable.getNative() == null || this.getNative() == null) return false;
        if (this.isNull() && device_nullable.isNull()) return true;

        return device_nullable.getNative().equals(this.getNative());
    }

    public void onUndiscovered(P_StateTracker.E_Intent intent)
    {
        stateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, DISCOVERED, false, DISCONNECTED, true, ADVERTISING, false);
        if (mTxnManager != null)
        {
            mTxnManager.cancelAllTxns();
        }
        if (getManager().mDiscoveryListener != null)
        {
            final DiscoveryListener.DiscoveryEvent event = P_EventFactory.newDiscoveryEvent(this, DiscoveryListener.LifeCycle.UNDISCOVERED);
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    if (getManager().mDiscoveryListener != null)
                    {
                        getManager().mDiscoveryListener.onEvent(event);
                    }
                }
            });
        }
    }

    public void undiscover()
    {
        // TODO - Implement this
    }

    public void connect(BleTransaction.Auth authTxn)
    {
        connect(authTxn, getConfig().defaultInitTxn, null, null);
    }

    public void connect(BleTransaction.Init initTxn)
    {
        connect(getConfig().defaultAuthTxn, initTxn, null, null);
    }

    public void connect(BleTransaction.Auth authTxn, BleTransaction.Init initTxn)
    {
        connect(authTxn, initTxn, null, null);
    }

    public void connect(DeviceConnectionFailListener failListener)
    {
        connect(getConfig().defaultAuthTxn, getConfig().defaultInitTxn, null, failListener);
    }

    public void connect(DeviceStateListener stateListener, DeviceConnectionFailListener failListener)
    {
        connect(getConfig().defaultAuthTxn, getConfig().defaultInitTxn, stateListener, failListener);
    }

    public void connect(BleTransaction.Auth authTxn, DeviceStateListener stateListener, DeviceConnectionFailListener failListener)
    {
        connect(authTxn, getConfig().defaultInitTxn, stateListener, failListener);
    }

    public void connect(BleTransaction.Init initTxn, DeviceStateListener stateListener, DeviceConnectionFailListener failListener)
    {
        connect(getConfig().defaultAuthTxn, initTxn, stateListener, failListener);
    }

    public void connect(BleTransaction.Auth authTxn, BleTransaction.Init initTxn, DeviceStateListener stateListener, DeviceConnectionFailListener failListener)
    {
        mTxnManager.setAuthTxn(authTxn);
        mTxnManager.setInitTxn(initTxn);
        if (failListener != null)
        {
            mConnectionFailMgr.setConnectionFailListener(failListener);
        }
        if (stateListener != null)
        {
            mStateTracker.setListener(stateListener);
        }
        connect();
    }

    public void connect()
    {
        connect_private(true);
    }

    private void connect_private(boolean explicit)
    {
        if (!isAny(CONNECTING, CONNECTED, CONNECTING_OVERALL) || isAny(RECONNECTING_SHORT_TERM, RECONNECTING_LONG_TERM))
        {
            mConnectionFailMgr.onExplicitConnectionStarted();
            if (getConfig().bondOnConnectOption != null)
            {
                switch (getConfig().bondOnConnectOption)
                {
                    case BOND:
                        if (!mGattManager.isBonded() || mGattManager.isBonding())
                        {
                            getManager().mTaskManager.add(new P_Task_Bond(this, null));
                        }
                        break;
                    case RE_BOND:
                        if (mGattManager.isBonded())
                        {
                            getManager().mTaskManager.add(new P_Task_Unbond(this, null));
                        }
                        getManager().mTaskManager.add(new P_Task_Bond(this, null));
                        break;
                }
            }
            stateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTING_OVERALL, true);
            getManager().mTaskManager.add(new P_Task_Connect(this, null, explicit));
        }
    }

    void connect_implicitly()
    {
        connect_private(false);
    }

    public void disconnect()
    {
        if (!isAny(DISCONNECTED, DISCONNECTING))
        {
            getManager().mTaskManager.add(new P_Task_Disconnect(this, null));
        }
    }

    public void disconnectWhenReady()
    {
        if (!isAny(DISCONNECTED, DISCONNECTING))
        {
            getManager().mTaskManager.add(new P_Task_Disconnect(this, null, P_TaskPriority.LOW));
        }
    }

    public void disconnect_remote()
    {
        // TODO - Implement this
    }

    public ReadWriteEvent NULL_READWRITE_EVENT()
    {
        if (mNullReadWriteEvent != null)
        {
            return mNullReadWriteEvent;
        }

        mNullReadWriteEvent = ReadWriteEvent.NULL(this);

        return mNullReadWriteEvent;
    }

    public void read(UUID charUuid, ReadWriteListener listener)
    {
        // TODO
        read(null, charUuid, listener);
    }

    public void read(UUID serviceUuid, UUID charUuid, ReadWriteListener listener)
    {
        if (!isNull())
        {
            final P_Task_Read read = new P_Task_Read(this, null, serviceUuid, charUuid, listener);
            addTask(read);
        }
    }

    public void write(UUID charUuid, byte[] data, ReadWriteListener listener)
    {
        write(null, charUuid, data, listener);
    }

    public void write(UUID serviceUuid, UUID charUuid, byte[] data, ReadWriteListener listener)
    {
        if (!isNull())
        {
            final P_Task_Write write = new P_Task_Write(this, null, serviceUuid, charUuid, data, listener);
            addTask(write);
        }
    }

    public void enableNotify(UUID serviceUuid, UUID charUuid, ReadWriteListener listener)
    {
        if (!isNull())
        {
            P_Task_ToggleNotify toggle = new P_Task_ToggleNotify(this, null, serviceUuid, charUuid, true, listener);
            addTask(toggle);
        }
    }

    public void enableNotify(UUID charUuid, ReadWriteListener listener)
    {
        enableNotify(null, charUuid, listener);
    }

    public void disableNotify(UUID serviceUuid, UUID charUuid, ReadWriteListener listener)
    {
        if (!isNull())
        {
            final P_Task_ToggleNotify toggle = new P_Task_ToggleNotify(this, null, serviceUuid, charUuid, false, listener);
            addTask(toggle);
        }
    }

    public void disableNotify(UUID charUuid, ReadWriteListener listener)
    {
        disableNotify(null, charUuid, listener);
    }

    public void disconnectWithReason(P_TaskPriority priority, Status bleTurningOff, Timing notApplicable, int gattStatusNotApplicable, int bondFailReasonNotApplicable, ReadWriteEvent readWriteEvent)
    {
        // TODO - Implement this
    }

    public void performOta(BleTransaction.Ota otaTxn)
    {
        if (!is(INITIALIZED))
        {
            // TODO - Throw error here
            return;
        }
        if (is(PERFORMING_OTA))
        {
            // TODO - Throw error here
            return;
        }
        stateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, PERFORMING_OTA, true);
        mTxnManager.start(otaTxn);
    }

    @Override public String toString()
    {
        if (isNull())
        {
            return NULL_STRING;
        }
        else
        {
            return Utils_String.concatStrings(mName_debug, " ", stateTracker().toString());
        }
    }


    void setNameFromNative(String name)
    {
        mName_native = name;
    }

    void setNameFromScanRecord(String name)
    {
        mName_scanRecord = name;
    }

    void onNewlyDiscovered(int rssi, BleScanInfo scanInfo, BleDeviceOrigin origin)
    {
        mOrigin_last = origin;
        mRssi = rssi;
        onDiscovered_private(rssi, scanInfo);
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, false, DISCOVERED, true, ADVERTISING, origin == BleDeviceOrigin.FROM_DISCOVERY);
    }

    void onRediscovered(BluetoothDevice device_native, BleScanInfo scanInfo, int rssi, BleDeviceOrigin origin)
    {
        mOrigin_last = origin;
        onDiscovered_private(rssi, scanInfo);
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ADVERTISING, true);
    }

    long lastDiscovery()
    {
        return mLastDiscovery;
    }

    void update(long curTimeMs)
    {
    }

    P_DeviceStateTracker stateTracker()
    {
        return mStateTracker;
    }

    public int getStateMask()
    {
        return stateTracker().getState();
    }

    void onNotify(final NotifyListener.NotifyEvent event)
    {
        if (mNotifyListener != null)
        {
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    mNotifyListener.onEvent(event);
                }
            });
        }
    }

    void doNativeConnect()
    {
        mGattManager.connect();
    }

    void onConnected()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTED, true, DISCOVERING_SERVICES, true, CONNECTING, false,
                CONNECTING_OVERALL, false, DISCONNECTED, false, RECONNECTING_SHORT_TERM, false, RECONNECTING_LONG_TERM, false);
        getManager().deviceConnected(this);
        getManager().mTaskManager.succeedTask(P_Task_Connect.class, this);
        getManager().mTaskManager.add(new P_Task_DiscoverServices(this, null));
    }

    void onConnecting()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTING, true);
    }

    void onConnectionFailed(Status status, Timing timing, int gattStatus)
    {
        if (mReconnectManager.shouldFail())
        {
            mConnectionFailMgr.onConnectionFailed(status, timing, gattStatus, BleDeviceState.getTransitoryConnectionState(getStateMask()), P_BaseConnectionFailListener.AutoConnectUsage.UNKNOWN,
                    BleStatuses.BOND_FAIL_REASON_NOT_AVAILABLE, null);
            resetToDisconnected();
            getManager().mTaskManager.failTask(P_Task_Connect.class, this, false);
        }
        else
        {
            mReconnectManager.reconnect(gattStatus);
        }
    }

    void onDisconnectedExplicitly()
    {
        mConnectionFailMgr.onExplicitDisconnect();
        resetToDisconnected();
        getManager().mTaskManager.succeedTask(P_Task_Disconnect.class, this);
    }

    void onDisconnected(int gattStatus)
    {
        if (getManager().mTaskManager.isCurrent(P_Task_Connect.class, this))
        {
            getManager().mTaskManager.failTask(P_Task_Connect.class, this, false);
            onConnectionFailed(Status.NATIVE_CONNECTION_FAILED, Timing.EVENTUALLY, gattStatus);
            return;
        }

        resetToDisconnected();
    }

    void onServicesDiscovered()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true,
                AUTHENTICATING, true);
        // TODO - Move on to next stage, which should be running any auth/init transactions
        // TODO - For now, succeeding connect task here, until txns are implemented

        getManager().mTaskManager.succeedTask(P_Task_Connect.class, this);

        if (mTxnManager.getAuthTxn() != null)
        {
            mTxnManager.start(mTxnManager.getAuthTxn());
        }
        else
        {
            stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, AUTHENTICATED, true, AUTHENTICATING, false,
                    INITIALIZING, true);
            if (mTxnManager.getInitTxn() != null)
            {
                mTxnManager.start(mTxnManager.getInitTxn());
            }
            else
            {
                onInitialized();
            }
        }
    }

    void onInitialized()
    {
        mConnectionFailMgr.onFullyInitialized();
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, INITIALIZED, true, INITIALIZING, false);
    }

    void onBonding(P_StateTracker.E_Intent intent)
    {
        stateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_GattManager.RESET_TO_BONDING);
    }

    void onBond(P_StateTracker.E_Intent intent)
    {
        stateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_GattManager.RESET_TO_BONDED);
        postBondEvent(intent, BleStatuses.BOND_SUCCESS, BondListener.Status.SUCCESS);
    }

    void onBondFailed(P_StateTracker.E_Intent intent, int failReason, BondListener.Status status)
    {
        stateTracker().update(intent, failReason, P_GattManager.RESET_TO_UNBONDED);
        postBondEvent(intent, failReason, status);
    }

    void onUnbond(P_StateTracker.E_Intent intent)
    {
        stateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_GattManager.RESET_TO_UNBONDED);
        postBondEvent(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BondListener.Status.SUCCESS);
    }




    private void resetToDisconnected()
    {
        stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, DISCONNECTED, true, ADVERTISING, true, DISCOVERED, true);
        if (mGattManager == null)
        {
            stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNBONDED, true);
        }
        else if (mGattManager.isBonding())
        {
            stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDING, true);
        }
        else if (mGattManager.isBonded())
        {
            stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, true);
        }
        else
        {
            stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNBONDED, true);
        }
    }

    private void postBondEvent(P_StateTracker.E_Intent intent, int failReason, BondListener.Status status)
    {
        if (mBondListener != null)
        {
            final BondListener.BondEvent event = P_EventFactory.newBondEvent(this, status, failReason, intent.convert());
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    mBondListener.onEvent(event);
                }
            });
        }
        if (getManager().mDefaultBondListener != null)
        {
            final BondListener.BondEvent event = P_EventFactory.newBondEvent(this, status, failReason, intent.convert());
            getManager().mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    getManager().mDefaultBondListener.onEvent(event);
                }
            });
        }
    }

    private void onDiscovered_private(int rssi, BleScanInfo scanInfo)
    {
        mLastDiscovery = System.currentTimeMillis();
        if (!scanInfo.isNull())
        {
            if (mScanInfo == null || !mScanInfo.equals(scanInfo))
            {
                mScanInfo = scanInfo.clone();
            }
        }
        if (rssi < 0)
        {
            updateRssi(rssi);
        }
    }

    private void updateRssi(int rssi)
    {
        mRssi = rssi;
    }

    private void addTask(P_Task task)
    {
        if (mTxnManager.isRunning())
        {
            // If it's an atomic transaction, and a transactionable task, then change the priority so it ends up
            // before any other task with lower priority.
            if (mTxnManager.isAtomic() && task instanceof P_Task_Transactionable)
            {
                ((P_Task_Transactionable) task).mPriority = P_TaskPriority.ATOMIC_TRANSACTION;
            }
        }
        getManager().mTaskManager.add(task);
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

    public static class DefaultConnectionFailListener implements DeviceConnectionFailListener
    {

        /**
         * The default retry count provided to {@link DefaultConnectionFailListener}.
         * So if you were to call {@link BleDevice#connect()} and all connections failed, in total the
         * library would try to connect {@value #DEFAULT_CONNECTION_FAIL_RETRY_COUNT}+1 times.
         *
         * @see DefaultConnectionFailListener
         */
        public static final int DEFAULT_CONNECTION_FAIL_RETRY_COUNT = 2;

        /**
         * The default connection fail limit past which {@link DefaultConnectionFailListener} will start returning {@link DeviceConnectionFailListener.Please#retryWithAutoConnectTrue()}.
         */
        public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;

        private final int m_retryCount;
        private final int m_failCountBeforeUsingAutoConnect;

        public DefaultConnectionFailListener()
        {
            this(DEFAULT_CONNECTION_FAIL_RETRY_COUNT, DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT);
        }

        public DefaultConnectionFailListener(int retryCount, int failCountBeforeUsingAutoConnect)
        {
            m_retryCount = retryCount;
            m_failCountBeforeUsingAutoConnect = failCountBeforeUsingAutoConnect;
        }

        public int getRetryCount()
        {
            return m_retryCount;
        }

        @Override public Please onEvent(ConnectionFailEvent e)
        {
            //--- DRK > Not necessary to check this ourselves, just being explicit.
            if (!e.status().allowsRetry() || e.device().is(RECONNECTING_LONG_TERM))
            {
                return Please.doNotRetry();
            }

            if (e.failureCountSoFar() <= m_retryCount)
            {
                if (e.failureCountSoFar() >= m_failCountBeforeUsingAutoConnect)
                {
                    return Please.retryWithAutoConnectTrue();
                }
                else
                {
                    if (e.status() == Status.NATIVE_CONNECTION_FAILED && e.timing() == Timing.TIMED_OUT)
                    {
                        if (e.autoConnectUsage() == AutoConnectUsage.USED)
                        {
                            return Please.retryWithAutoConnectFalse();
                        }
                        else if (e.autoConnectUsage() == AutoConnectUsage.NOT_USED)
                        {
                            return Please.retryWithAutoConnectTrue();
                        }
                        else
                        {
                            return Please.retry();
                        }
                    }
                    else
                    {
                        return Please.retry();
                    }
                }
            }
            else
            {
                return Please.doNotRetry();
            }
        }
    }

}
