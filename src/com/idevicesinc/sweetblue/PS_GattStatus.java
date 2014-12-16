package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
class PS_GattStatus
{
	public static final int UNKNOWN_STATUS_AFTER_GATT_INSUFFICIENT_AUTHENTICATION = 137;
	public static final String BluetoothDevice_EXTRA_REASON = "android.bluetooth.device.extra.REASON";
	public static final String BluetoothDevice_ACTION_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";
	public static final int BluetoothDevice_BOND_SUCCESS = 0;

	public static final int UNKNOWN_STATUS_FOR_IMMEDIATE_CONNECTION_FAILURE = 133;
	
	//--- DRK > People are referencing this as GATT_ERROR or GATT_INTERNAL_ERROR on stackoverflow but no mentions in the project. 
	public static final int UNKNOWN_STATUS_FOR_SERVICE_DISCOVERY_FAILURE = 129;
	
	
	//--- DRK > List from https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.3_r1.1/stack/include/gatt_api.h
	//---		Copy/pasting here for easier reference or in case that resource disappears or something.
	
//	#define  GATT_SUCCESS                        0x0000
//	#define  GATT_INVALID_HANDLE                 0x0001
//	#define  GATT_READ_NOT_PERMIT                0x0002
//	#define  GATT_WRITE_NOT_PERMIT               0x0003
//	#define  GATT_INVALID_PDU                    0x0004
//	#define  GATT_INSUF_AUTHENTICATION           0x0005
//	#define  GATT_REQ_NOT_SUPPORTED              0x0006
//	#define  GATT_INVALID_OFFSET                 0x0007
//	#define  GATT_INSUF_AUTHORIZATION            0x0008
//	#define  GATT_PREPARE_Q_FULL                 0x0009
//	#define  GATT_NOT_FOUND                      0x000a
//	#define  GATT_NOT_LONG                       0x000b
//	#define  GATT_INSUF_KEY_SIZE                 0x000c
//	#define  GATT_INVALID_ATTR_LEN               0x000d
//	#define  GATT_ERR_UNLIKELY                   0x000e
//	#define  GATT_INSUF_ENCRYPTION               0x000f
//	#define  GATT_UNSUPPORT_GRP_TYPE             0x0010
//	#define  GATT_INSUF_RESOURCE                 0x0011
//	#define  GATT_ILLEGAL_PARAMETER              0x0087
//	#define  GATT_NO_RESOURCES                   0x0080
//	#define  GATT_INTERNAL_ERROR                 0x0081
//	#define  GATT_WRONG_STATE                    0x0082
//	#define  GATT_DB_FULL                        0x0083
//	#define  GATT_BUSY                           0x0084
//	#define  GATT_ERROR                          0x0085
//	#define  GATT_CMD_STARTED                    0x0086
//	#define  GATT_PENDING                        0x0088
//	#define  GATT_AUTH_FAIL                      0x0089
//	#define  GATT_MORE                           0x008a
//	#define  GATT_INVALID_CFG                    0x008b
//	#define  GATT_SERVICE_STARTED                0x008c
//	#define  GATT_ENCRYPED_MITM                  GATT_SUCCESS
//	#define  GATT_ENCRYPED_NO_MITM               0x008d
//	#define  GATT_NOT_ENCRYPTED                  0x008e
//	typedef UINT8 tGATT_STATUS;
//	#define  GATT_RSP_ERROR                      0x01
//	#define  GATT_REQ_MTU                        0x02
//	#define  GATT_RSP_MTU                        0x03
//	#define  GATT_REQ_FIND_INFO                  0x04
//	#define  GATT_RSP_FIND_INFO                  0x05
//	#define  GATT_REQ_FIND_TYPE_VALUE            0x06
//	#define  GATT_RSP_FIND_TYPE_VALUE            0x07
//	#define  GATT_REQ_READ_BY_TYPE               0x08
//	#define  GATT_RSP_READ_BY_TYPE               0x09
//	#define  GATT_REQ_READ                       0x0A
//	#define  GATT_RSP_READ                       0x0B
//	#define  GATT_REQ_READ_BLOB                  0x0C
//	#define  GATT_RSP_READ_BLOB                  0x0D
//	#define  GATT_REQ_READ_MULTI                 0x0E
//	#define  GATT_RSP_READ_MULTI                 0x0F
//	#define  GATT_REQ_READ_BY_GRP_TYPE           0x10
//	#define  GATT_RSP_READ_BY_GRP_TYPE           0x11
//	#define  GATT_REQ_WRITE                      0x12 /*                 0001-0010 (write)*/
//	#define  GATT_RSP_WRITE                      0x13
//	#define  GATT_CMD_WRITE                      0x52 /* changed in V4.0 01001-0010(write cmd)*/
//	#define  GATT_REQ_PREPARE_WRITE              0x16
//	#define  GATT_RSP_PREPARE_WRITE              0x17
//	#define  GATT_REQ_EXEC_WRITE                 0x18
//	#define  GATT_RSP_EXEC_WRITE                 0x19
//	#define  GATT_HANDLE_VALUE_NOTIF             0x1B
//	#define  GATT_HANDLE_VALUE_IND               0x1D
//	#define  GATT_HANDLE_VALUE_CONF              0x1E
//	#define  GATT_SIGN_CMD_WRITE                 0xD2 /* changed in V4.0 1101-0010 (signed write)  see write cmd above*/
//	#define  GATT_OP_CODE_MAX                    GATT_HANDLE_VALUE_CONF + 1 /* 0x1E = 30 + 1 = 31*/
//	#define  GATT_HANDLE_IS_VALID(x) ((x) != 0)
//	#define GATT_CONN_UNKNOWN                   0
//	#define GATT_CONN_NO_RESOURCES              L2CAP_CONN_NO_RESOURCES         /* connection fail for l2cap resource failure */
//	#define GATT_CONN_TIMEOUT                   HCI_ERR_CONNECTION_TOUT         /* 0x08 connection timeout  */
//	#define GATT_CONN_TERMINATE_PEER_USER       HCI_ERR_PEER_USER               /* 0x13 connection terminate by peer user  */
//	#define GATT_CONN_TERMINATE_LOCAL_HOST      HCI_ERR_CONN_CAUSE_LOCAL_HOST   /* 0x16 connectionterminated by local host  */
//	#define GATT_CONN_FAIL_ESTABLISH            HCI_ERR_CONN_FAILED_ESTABLISHMENT/* 0x03E connection fail to establish  */
//	#define GATT_CONN_LMP_TIMEOUT               HCI_ERR_LMP_RESPONSE_TIMEOUT     /* 0x22 connection fail for LMP response tout */
//	#define GATT_CONN_CANCEL                    L2CAP_CONN_CANCEL                /* 0x0100 L2CAP connection cancelled  */
//	typedef UINT16 tGATT_DISCONN_REASON;
}
