package com.idevicesinc.sweetblue;

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
public class BleStatuses
{
	/**
	 * Status code used for {@link BleDevice.ReadWriteListener.ReadWriteEvent#gattStatus} when the operation failed at a point where a
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
	public static final int GATT_CONN_UNKNOWN                   = 0;
//	public static final int GATT_CONN_NO_RESOURCES              = L2CAP_CONN_NO_RESOURCES;         /* connection fail for l2cap resource failure */
//	public static final int GATT_CONN_TIMEOUT                   = HCI_ERR_CONNECTION_TOUT;         /* 0x08 connection timeout  */
//	public static final int GATT_CONN_TERMINATE_PEER_USER       = HCI_ERR_PEER_USER;               /* 0x13 connection terminate by peer user  */
//	public static final int GATT_CONN_TERMINATE_LOCAL_HOST      = HCI_ERR_CONN_CAUSE_LOCAL_HOST;   /* 0x16 connectionterminated by local host  */
//	public static final int GATT_CONN_FAIL_ESTABLISH            = HCI_ERR_CONN_FAILED_ESTABLISHMENT;/* 0x03E connection fail to establish  */
//	public static final int GATT_CONN_LMP_TIMEOUT               = HCI_ERR_LMP_RESPONSE_TIMEOUT;     /* 0x22 connection fail for LMP response tout */
//	public static final int GATT_CONN_CANCEL                    = L2CAP_CONN_CANCEL;                /* 0x0100 L2CAP connection cancelled  */
//	typedef UINT16 tGATT_DISCONN_REASON;
}
