package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * A collection of various BLE status codes that for whatever reason are not exposed through Android's 
 * public BLE layer - this can be because they are <code>public</code> but use the @hide annotation,
 * or they are not <code>public</code> in the first place, or they can only be found by Googling
 * for native C/C++ library code.
 * <br><br>
 * See the static members of {@link android.bluetooth.BluetoothDevice} and {@link BluetoothGatt} for more information.
 * <br><br>
 * NOTE: Most <code>GATT_</code> members here are copy/pasted from
 * https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.3_r1.1/stack/include/gatt_api.h
 */
public final class BleStatuses
{
	/**
	 * Status code used for {@link BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus()} when the operation failed at a point where a
	 * gatt status from the underlying stack isn't provided or applicable.
	 * <br><br>
	 * Also used for {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#gattStatus()} for when the failure didn't involve the gatt layer.
	 */
	public static final int GATT_STATUS_NOT_APPLICABLE 					= -1;
	
	/**
	 * Used on {@link BleDevice.BondListener.BondEvent#failReason()} when {@link BleDevice.BondListener.BondEvent#status()}
	 * isn't applicable, for example {@link BleDevice.BondListener.Status#SUCCESS}.
	 */
	public static final int BOND_FAIL_REASON_NOT_APPLICABLE				= GATT_STATUS_NOT_APPLICABLE;

	/**
	 * Used when attempting to bond on a device whose API level is less than 19, or does not have access to android's
	 * {@link BluetoothDevice#createBond()} method.
	 */
	public static final int BOND_FAIL_REASON_NOT_AVAILABLE						= 42;


	/**
	 * Status code used for {@link BleServer.AdvertisingListener.AdvertisingEvent#status()} when advertising has been
	 * successfully started.
	 */
	public static final int ADVERTISE_SUCCESS = 0;

	/**
	 * Status code for (@link BleServer.AdvertiseListener.AdvertisingEvent#status} when trying to advertise on
	 * a device which isn't running an android OS of Lollipop or higher.
	 */
	public static final int ADVERTISE_ANDROID_VERSION_NOT_SUPPORTED = 20;
	
	/**
     * A bond attempt succeeded.
     */
    public static final int BOND_SUCCESS = 0;

    /**
     * A bond attempt failed because pins did not match, or remote device did not respond to pin request in time.
     */
    public static final int UNBOND_REASON_AUTH_FAILED = 1;

    /**
     * A bond attempt failed because the other side explicitly rejected bonding.
     */
    public static final int UNBOND_REASON_AUTH_REJECTED = 2;

    /**
     * A bond attempt failed because we canceled the bonding process.
     */
    public static final int UNBOND_REASON_AUTH_CANCELED = 3;

    /**
     * A bond attempt failed because we could not contact the remote device.
     */
    public static final int UNBOND_REASON_REMOTE_DEVICE_DOWN = 4;

    /**
     * A bond attempt failed because a discovery is in progress.
     */
    public static final int UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5;

    /**
     * A bond attempt failed because of authentication timeout.
     */
    public static final int UNBOND_REASON_AUTH_TIMEOUT = 6;

    /**
     * A bond attempt failed because of repeated attempts.
     */
    public static final int UNBOND_REASON_REPEATED_ATTEMPTS = 7;

    /**
     * A bond attempt failed because we received an Authentication Cancel by remote end.
     */
    public static final int UNBOND_REASON_REMOTE_AUTH_CANCELED = 8;

	/**
	 * An existing bond was explicitly revoked.
	 */
	public static final int UNBOND_REASON_REMOVED = 9;

	// In Nougat, mAuthRetry changed from a boolean, to a state. These are the values for the states.

	public static final int AUTH_RETRY_STATE_IDLE = 0;

	public static final int AUTH_RETRY_STATE_NO_MITM = 1;

	public static final int AUTH_RETRY_STATE_MITM = 2;


	/**
	 * Indicates the local Bluetooth adapter is off.
	 */
	static final int STATE_OFF = BluetoothAdapter.STATE_OFF;

	/**
	 * Indicates the local Bluetooth adapter is turning on. However local
	 * clients should wait for {@link #STATE_ON} before attempting to
	 * use the adapter.
	 */
	static final int STATE_TURNING_ON = BluetoothAdapter.STATE_TURNING_ON;

	/**
	 * Indicates the local Bluetooth adapter is on, and ready for use.
	 */
	static final int STATE_ON = BluetoothAdapter.STATE_ON;

	/**
	 * Indicates the local Bluetooth adapter is turning off. Local clients
	 * should immediately attempt graceful disconnection of any remote links.
	 */
	static final int STATE_TURNING_OFF = BluetoothAdapter.STATE_TURNING_OFF;

	/**
	 * Indicates the local Bluetooth adapter is turning Bluetooth LE mode on.
	 */
	/*package*/ static final int STATE_BLE_TURNING_ON = 14;

	/**
	 * Indicates the local Bluetooth adapter is in LE only mode.
	 */
	/*package*/ static final int STATE_BLE_ON = 15;

	/**
	 * Indicates the local Bluetooth adapter is turning off LE only mode.
	 */
	/*package*/ static final int STATE_BLE_TURNING_OFF = 16;
	
	//--- DRK > List from https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.3_r1.1/stack/include/gatt_api.h
	//---		Copy/pasting here for easier reference or in case that resource disappears or something.
	
	public static final int  GATT_SUCCESS                        = 0x0000;
	public static final int  GATT_INVALID_HANDLE                 = 0x0001;
	public static final int  GATT_READ_NOT_PERMIT                = 0x0002;
	public static final int  GATT_WRITE_NOT_PERMIT               = 0x0003;
	public static final int  GATT_INVALID_PDU                    = 0x0004;
	public static final int  GATT_INSUF_AUTHENTICATION           = 0x0005;
	public static final int  GATT_REQ_NOT_SUPPORTED              = 0x0006;
	public static final int  GATT_INVALID_OFFSET                 = 0x0007;
	public static final int  GATT_INSUF_AUTHORIZATION            = 0x0008;
	public static final int  GATT_PREPARE_Q_FULL                 = 0x0009;
	public static final int  GATT_NOT_FOUND                      = 0x000a;
	public static final int  GATT_NOT_LONG                       = 0x000b;
	public static final int  GATT_INSUF_KEY_SIZE                 = 0x000c;
	public static final int  GATT_INVALID_ATTR_LEN               = 0x000d;
	public static final int  GATT_ERR_UNLIKELY                   = 0x000e;
	public static final int  GATT_INSUF_ENCRYPTION               = 0x000f;
	public static final int  GATT_UNSUPPORT_GRP_TYPE             = 0x0010;
	public static final int  GATT_INSUF_RESOURCE                 = 0x0011;
	public static final int  GATT_ILLEGAL_PARAMETER              = 0x0087;
	public static final int  GATT_NO_RESOURCES                   = 0x0080;
	public static final int  GATT_INTERNAL_ERROR                 = 0x0081;
	public static final int  GATT_WRONG_STATE                    = 0x0082;
	public static final int  GATT_DB_FULL                        = 0x0083;
	public static final int  GATT_BUSY                           = 0x0084;
	public static final int  GATT_ERROR                          = 0x0085;
	public static final int  GATT_CMD_STARTED                    = 0x0086;
	public static final int  GATT_PENDING                        = 0x0088;
	public static final int  GATT_AUTH_FAIL                      = 0x0089;
	public static final int  GATT_MORE                           = 0x008a;
	public static final int  GATT_INVALID_CFG                    = 0x008b;
	public static final int  GATT_SERVICE_STARTED                = 0x008c;
	public static final int  GATT_ENCRYPED_MITM                  = GATT_SUCCESS;
	public static final int  GATT_ENCRYPED_NO_MITM               = 0x008d;
	public static final int  GATT_NOT_ENCRYPTED                  = 0x008e;
//	typedef UINT8 tGATT_STATUS;
	public static final int  GATT_RSP_ERROR                      = 0x01;
	public static final int  GATT_REQ_MTU                        = 0x02;
	public static final int  GATT_RSP_MTU                        = 0x03;
	public static final int  GATT_REQ_FIND_INFO                  = 0x04;
	public static final int  GATT_RSP_FIND_INFO                  = 0x05;
	public static final int  GATT_REQ_FIND_TYPE_VALUE            = 0x06;
	public static final int  GATT_RSP_FIND_TYPE_VALUE            = 0x07;
	public static final int  GATT_REQ_READ_BY_TYPE               = 0x08;
	public static final int  GATT_RSP_READ_BY_TYPE               = 0x09;
	public static final int  GATT_REQ_READ                       = 0x0A;
	public static final int  GATT_RSP_READ                       = 0x0B;
	public static final int  GATT_REQ_READ_BLOB                  = 0x0C;
	public static final int  GATT_RSP_READ_BLOB                  = 0x0D;
	public static final int  GATT_REQ_READ_MULTI                 = 0x0E;
	public static final int  GATT_RSP_READ_MULTI                 = 0x0F;
	public static final int  GATT_REQ_READ_BY_GRP_TYPE           = 0x10;
	public static final int  GATT_RSP_READ_BY_GRP_TYPE           = 0x11;
	public static final int  GATT_REQ_WRITE                      = 0x12; /*                 0001-0010 (write)*/
	public static final int  GATT_RSP_WRITE                      = 0x13;
	public static final int  GATT_CMD_WRITE                      = 0x52; /* changed in V4.0 01001-0010(write cmd)*/
	public static final int  GATT_REQ_PREPARE_WRITE              = 0x16;
	public static final int  GATT_RSP_PREPARE_WRITE              = 0x17;
	public static final int  GATT_REQ_EXEC_WRITE                 = 0x18;
	public static final int  GATT_RSP_EXEC_WRITE                 = 0x19;
	public static final int  GATT_HANDLE_VALUE_NOTIF             = 0x1B;
	public static final int  GATT_HANDLE_VALUE_IND               = 0x1D;
	public static final int  GATT_HANDLE_VALUE_CONF              = 0x1E;
	public static final int  GATT_SIGN_CMD_WRITE                 = 0xD2; /* changed in V4.0 1101-0010 (signed write)  see write cmd above*/
	public static final int  GATT_OP_CODE_MAX                    = GATT_HANDLE_VALUE_CONF + 1; /* 0x1E = 30 + 1 = 31*/
//	public static final int  GATT_HANDLE_IS_VALID(x) ((x) != 0)

	public static final int CONN_SUCCESS                   = 0;			// This is actually GATT_CONN_UNKNOWN, but 0 is always SUCCESS, so using SUCCESS here instead
	public static final int CONN_NO_RESOURCES              = 4;         // L2CAP_CONN_NO_RESOURCES				/* connection fail for l2cap resource failure */
	public static final int CONN_TIMEOUT                   = 0x08;      // HCI_ERR_CONNECTION_TOUT				/* 0x08 connection timeout  */
	public static final int CONN_TERMINATE_PEER_USER       = 0x13;      // HCI_ERR_PEER_USER					/* 0x13 connection terminate by peer user  */
	public static final int CONN_TERMINATE_LOCAL_HOST      = 0x16;   	// HCI_ERR_CONN_CAUSE_LOCAL_HOST		/* 0x16 connectionterminated by local host  */
	public static final int CONN_FAIL_ESTABLISH            = 0x03E;		// HCI_ERR_CONN_FAILED_ESTABLISHMENT	/* 0x03E connection fail to establish  */
	public static final int CONN_LMP_TIMEOUT               = 0x22;     	// HCI_ERR_LMP_RESPONSE_TIMEOUT			/* 0x22 connection fail for LMP response tout */
	public static final int CONN_CANCEL                    = 0x0100;    // L2CAP_CONN_CANCEL					/* 0x0100 L2CAP connection cancelled  */
//	typedef UINT16 tGATT_DISCONN_REASON;




	//--- DRK > Just for reference, copy/pasting the logcat logs of polling for ble state changes in recently released Android M.

//	10-15 11:08:13.966 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:13.999 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.032 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.069 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.099 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.131 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.160 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.189 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.222 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.251 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:14.305 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_TURNING_OFF(13)
//	10-15 11:08:14.332 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_ON(15)
//	10-15 11:08:14.363 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.389 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.416 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.441 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.467 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.493 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.519 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.546 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.571 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.598 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.624 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.650 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.676 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.703 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.731 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_OFF(16)
//	10-15 11:08:14.757 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.783 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.809 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.835 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.861 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.887 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.913 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.939 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.965 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:14.992 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.018 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.049 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.076 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.104 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.130 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.156 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.186 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.214 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.241 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.271 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.299 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.325 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.351 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.377 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.405 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.431 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.457 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.484 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.512 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.539 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.565 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.594 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.622 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.649 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.683 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.713 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.740 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.767 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.794 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.821 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.849 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.876 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.904 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.933 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.959 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:15.985 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.012 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.038 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.065 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.093 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.126 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.155 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.182 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.209 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.246 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.276 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.303 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.330 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.360 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.389 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.418 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.446 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.473 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.501 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.528 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.554 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.579 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.605 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_OFF(10)
//	10-15 11:08:16.632 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.659 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.686 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.715 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.744 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.784 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.813 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.843 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.872 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.902 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.932 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.963 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:16.991 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.020 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.057 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.086 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.115 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.148 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.181 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.211 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.240 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.268 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.297 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.326 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.356 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.386 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.415 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.444 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.472 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.503 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.532 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.562 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.591 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.622 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.651 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.679 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.706 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.735 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.763 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)
//	10-15 11:08:17.791 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_TURNING_ON(14)

//	10-15 11:08:17.791 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_BLE_ON(15) // added manually from another test run, not detected in original run

//	10-15 11:08:17.820 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_TURNING_ON(11)
//	10-15 11:08:17.847 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_TURNING_ON(11)
//	10-15 11:08:17.873 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_TURNING_ON(11)
//	10-15 11:08:17.900 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_TURNING_ON(11)
//	10-15 11:08:17.926 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:17.960 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:17.986 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:18.013 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:18.038 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:18.068 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
//	10-15 11:08:18.094 6089-6089/com.idevicesllc.connected E/P_BleManager_Listeners: AMY(6089) update() - *********************STATE_ON(12)
}
