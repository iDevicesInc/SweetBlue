package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Status;
import com.idevicesinc.sweetblue.listeners.DeviceConnectionFailListener.Timing;
import com.idevicesinc.sweetblue.listeners.DeviceStateListener;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Utils_String;

import static com.idevicesinc.sweetblue.BleDeviceState.*;


public class BleDevice extends BleNode
{

    public final static BleDevice NULL = new BleDevice(null, null, null, null, null, true);

    private final boolean mIsNull;
    private int mRssi;
    private final P_DeviceStateTracker mStateTracker;
    private final P_DeviceStateTracker mStateTracker_shortTermReconnect;
    private final BleDeviceOrigin mOrigin;
    private BleDeviceOrigin mOrigin_last;
    private BleDeviceConfig mConfig;
    private BleConnectionPriority mConnectionPriority = BleConnectionPriority.MEDIUM;
    private int mMtu = 0;
    private ReadWriteEvent mNullReadWriteEvent;
    private BleScanInfo mScanInfo;
    private String mName_native;
    private String mName_scanRecord;
    private String mName_device;
    private DeviceConnectionFailListener mConnectionFailListener;
    private final P_NativeDeviceWrapper mNativeWrapper;


    BleDevice(BleManager mgr, BluetoothDevice nativeDevice, BleDeviceOrigin origin, BleDeviceConfig config_nullable, String deviceName, boolean isNull)
    {
        super(mgr);
        mIsNull = isNull;
        mOrigin = origin;
        mOrigin_last = mOrigin;
        mName_device = deviceName;

        if (!mIsNull)
        {
            mStateTracker = new P_DeviceStateTracker(this, false);
            mStateTracker_shortTermReconnect = new P_DeviceStateTracker(this, true);
            stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, DISCONNECTED, true);
            mNativeWrapper = new P_NativeDeviceWrapper(this, nativeDevice);
        }
        else
        {
            mName_device = "NULL";
            mStateTracker = new P_DeviceStateTracker(this, false);
            mNativeWrapper = null;
            stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, true);
            mStateTracker_shortTermReconnect = null;
        }
    }

    public void setConfig(BleDeviceConfig config)
    {
        if (isNull())
        {
            return;
        }

        mConfig = config == null ? null : config.clone();

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

    public int getMtu()
    {
        return mMtu;
    }

    public BleConnectionPriority getConnectionPriority()
    {
        return mConnectionPriority;
    }

    public void setConnectionPriority(BleConnectionPriority priority, ReadWriteListener listener)
    {
        // TODO - Actually implement this.
    }

    public String getName_debug()
    {
        // TODO
        // TODO
        // TODO
        // TODO Actually implement this
        return "";
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
     *
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
        return mNativeWrapper.getNativeDevice();
    }

    public BluetoothGatt getNativeGatt()
    {
        return mNativeWrapper.getGatt();
    }

    @Override public String getMacAddress()
    {
        return mNativeWrapper.getMacAddress();
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
        //mNativeDevice = device_native;
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ADVERTISING, true);
    }

    private void onDiscovered_private(int rssi, BleScanInfo scanInfo)
    {
        if (!scanInfo.isNull())
        {
            if (mScanInfo == null || !mScanInfo.equals(scanInfo))
            {
                mScanInfo = scanInfo.clone();
            }
        }
    }

    private void updateRssi(int rssi)
    {
        mRssi = rssi;
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

    void update(long curTimeMs)
    {
    }

    P_DeviceStateTracker stateTracker()
    {
        return mStateTracker;
    }

    int getStateMask()
    {
        return stateTracker().getState();
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
        return Percent.fromInt(mRssi);
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
        if (device_nullable == null)												return false;
        if (device_nullable == this)												return true;
        if (device_nullable.getNative() == null || this.getNative() == null)		return false;
        if( this.isNull() && device_nullable.isNull() )								return true;

        return device_nullable.getNative().equals(this.getNative());
    }

    public void undiscover()
    {
        // TODO - Implement this
    }

    public void connect()
    {
        if (!isAny(CONNECTING, CONNECTED, CONNECTING_OVERALL))
        {
            stateTracker().update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTING_OVERALL, true);
            getManager().mTaskManager.add(new P_Task_Connect(this, null));
        }
    }

    public void connect(DeviceConnectionFailListener failListener)
    {
        mConnectionFailListener = failListener;
        connect();
    }

    void doNativeConnect()
    {
        mNativeWrapper.connect();
    }

    public void disconnect()
    {
        if (!isAny(DISCONNECTED, DISCONNECTING))
        {
            getManager().mTaskManager.add(new P_Task_Disconnect(this, null));
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

    void onConnected()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTED, true, DISCOVERING_SERVICES, true, CONNECTING, false, CONNECTING_OVERALL, false, DISCONNECTED, false);
        getManager().mTaskManager.succeedTask(P_Task_Connect.class, this);
        getManager().mTaskManager.add(new P_Task_DiscoverServices(this, null));
    }

    void onConnecting()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, CONNECTING, true);
    }

    void onConnectionFailed(int gattStatus)
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, gattStatus, CONNECTED, false, CONNECTING, false, CONNECTING_OVERALL, false, DISCONNECTED, true);
        getManager().mTaskManager.failTask(P_Task_Connect.class, this, false);
    }

    void onDisconnected()
    {
        stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, DISCONNECTED, true, ADVERTISING, true, DISCOVERED, true);
        getManager().mTaskManager.succeedTask(P_Task_Disconnect.class, this);
    }

    void onDisconnected(int gattStatus)
    {
        stateTracker().set(P_StateTracker.E_Intent.UNINTENTIONAL, gattStatus, DISCONNECTED, true, ADVERTISING, true, DISCOVERED, true);
    }

    void onServicesDiscovered()
    {
        stateTracker().update(P_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
        // TODO - Move on to next stage, which should be running any auth/init transactions
        // TODO - For now, succeeding connect task here, until txns are implemented

        getManager().mTaskManager.succeedTask(P_Task_Connect.class, this);
    }

    DeviceConnectionFailListener getConnectionFailListener()
    {
        return mConnectionFailListener;
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

    public void disconnectWithReason(P_TaskPriority priority, Status bleTurningOff, Timing notApplicable, int gattStatusNotApplicable, int bondFailReasonNotApplicable, ReadWriteEvent readWriteEvent)
    {
        // TODO - Implement this
    }

    @Override public String toString()
    {
        if (isNull())
        {
            return NULL_STRING();
        }
        else
        {
            return Utils_String.concatStrings(getName(), " ", stateTracker().toString());
        }
    }
}
