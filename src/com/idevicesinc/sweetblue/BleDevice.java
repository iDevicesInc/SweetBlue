package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.Cursor;

import com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.AutoConnectUsage;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Timing;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter;
import com.idevicesinc.sweetblue.BleDeviceConfig.BondFilter.CharacteristicEventType;
import com.idevicesinc.sweetblue.P_PollManager.E_NotifyState;
import com.idevicesinc.sweetblue.P_Task_Bond.E_TransactionLockBehavior;
import com.idevicesinc.sweetblue.utils.*;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;

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
 * {@link BleManager.DiscoveryListener#onEvent(BleManager.DiscoveryListener.DiscoveryEvent)}.
 */
public class BleDevice implements UsesCustomNull
{
	/**
	 * Special value that is used in place of Java's built-in <code>null</code>.
	 */
	@Immutable
	public static final BleDevice NULL = new BleDevice(null, null, NULL_STRING(), NULL_STRING(), BleDeviceOrigin.EXPLICIT, null, /*isNull=*/true);

	/**
	 * Provide an implementation of this callback to various methods like {@link BleDevice#read(UUID, ReadWriteListener)},
	 * {@link BleDevice#write(UUID, byte[], ReadWriteListener)}, {@link BleDevice#startPoll(UUID, Interval, ReadWriteListener)},
	 * {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, {@link BleDevice#readRssi(ReadWriteListener)}, etc.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ReadWriteListener
	{
		/**
		 * A value returned to {@link ReadWriteListener#onEvent(ReadWriteEvent)}
		 * by way of {@link ReadWriteEvent#status} that indicates success of the
		 * operation or the reason for its failure. This enum is <i>not</i>
		 * meant to match up with {@link BluetoothGatt}.GATT_* values in any way.
		 *
		 * @see ReadWriteEvent#status()
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * As of now, only used for {@link ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
			 */
			NULL,

			/**
			 * If {@link ReadWriteEvent#type} {@link Type#isRead()} then {@link ReadWriteEvent#data} will contain some data returned from
			 * the device. If type is {@link Type#WRITE} then {@link ReadWriteEvent#data} was sent to the device.
			 */
			SUCCESS,

			/**
			 * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])},
			 * {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, etc. was called on {@link BleDevice#NULL}.
			 */
			NULL_DEVICE,

			/**
			 * Device is not {@link BleDeviceState#CONNECTED}.
			 */
			NOT_CONNECTED,

			/**
			 * Couldn't find a matching {@link ReadWriteEvent#target} for the {@link ReadWriteEvent#charUuid} (or
			 * {@link ReadWriteEvent#descUuid} if {@link ReadWriteEvent#target} is {@link Target#DESCRIPTOR}) which was given to
			 * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])}, etc. This most likely
			 * means that the internal call to {@link BluetoothGatt#discoverServices()} didn't find any
			 * {@link BluetoothGattService} that contained a {@link BluetoothGattCharacteristic} for {@link ReadWriteEvent#charUuid()}.
			 */
			NO_MATCHING_TARGET,

			/**
			 * You tried to do a read on a characteristic that is write-only, or
			 * vice-versa, or tried to read a notify-only characteristic, or etc., etc.
			 */
			OPERATION_NOT_SUPPORTED,

			/**
			 * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
			 * returned <code>false</code> for an unknown reason. This {@link Status} is only relevant for calls to
			 * {@link BleDevice#enableNotify(UUID, ReadWriteListener)} and {@link BleDevice#disableNotify(UUID, ReadWriteListener)}
			 * (or the various overloads).
			 */
			FAILED_TO_TOGGLE_NOTIFICATION,

			/**
			 * {@link BluetoothGattCharacteristic#setValue(byte[])} (or one of its overloads) or
			 * {@link BluetoothGattDescriptor#setValue(byte[])} (or one of its overloads) returned <code>false</code>.
			 */
			FAILED_TO_SET_VALUE_ON_TARGET,

			/**
			 * The call to {@link BluetoothGatt#readCharacteristic(BluetoothGattCharacteristic)}
			 * or {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic)}
			 * or etc. returned <code>false</code> and thus failed immediately
			 * for unknown reasons. No good remedy for this...perhaps try {@link BleManager#reset()}.
			 */
			FAILED_TO_SEND_OUT,

			/**
			 * The operation was cancelled by the device becoming {@link BleDeviceState#DISCONNECTED}.
			 */
			CANCELLED_FROM_DISCONNECT,

			/**
			 * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
			 * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
			 * {@link ReadWriteEvent#status} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
			 * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
			 * <br><br>
			 * Either way, the device was or will be disconnected.
			 */
			CANCELLED_FROM_BLE_TURNING_OFF,

			/**
			 * Used either when {@link ReadWriteEvent#type()} {@link Type#isRead()} and the stack returned a <code>null</code>
			 * value for {@link BluetoothGattCharacteristic#getValue()} despite the operation being otherwise "successful", <i>or</i>
			 * {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a null data parameter. For the read case, the library
			 * will throw an {@link BleManager.UhOhListener.UhOh#READ_RETURNED_NULL}, but hopefully it was just a temporary glitch. If the problem persists try {@link BleManager#reset()}.
			 */
			NULL_DATA,

			/**
			 * Used either when {@link ReadWriteEvent#type} {@link Type#isRead()} and the operation was "successful" but
			 * returned a zero-length array for {@link ReadWriteEvent#data}, <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) )
			 * was called with a non-null but zero-length data parameter. Note that {@link ReadWriteEvent#data} will be a zero-length array for
			 * all other error statuses as well, for example {@link #NO_MATCHING_TARGET}, {@link #NOT_CONNECTED}, etc. In other words it's never null.
			 */
			EMPTY_DATA,

			/**
			 * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
			 * example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
			 * returned a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted,
			 * whatever. Often this means that the device is about to become {@link BleDeviceState#DISCONNECTED}. {@link ReadWriteEvent#gattStatus()}
			 * will most likely be non-zero, and you can check the static fields in {@link BleStatuses} for more information.
			 *
			 * @see ReadWriteEvent#gattStatus()
			 */
			REMOTE_GATT_FAILURE,

			/**
			 * Operation took longer than time specified in {@link BleDeviceConfig#timeoutRequestFilter} so we cut it loose.
			 */
			TIMED_OUT;

			/**
			 * Returns <code>true</code> for {@link #CANCELLED_FROM_DISCONNECT} or {@link #CANCELLED_FROM_BLE_TURNING_OFF}.
			 */
			public boolean wasCancelled()
			{
				return this == CANCELLED_FROM_DISCONNECT || this == Status.CANCELLED_FROM_BLE_TURNING_OFF;
			}

			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * The type of operation for a {@link ReadWriteEvent} - read, write, poll, etc.
		 */
		public static enum Type implements UsesCustomNull
		{
			/**
			 * As of now, only used for {@link ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
			 */
			NULL,

			/**
			 * Associated with {@link BleDevice#read(UUID, ReadWriteListener)} or {@link BleDevice#readRssi(ReadWriteListener)}.
			 */
			READ,

			/**
			 * Associated with {@link BleDevice#write(UUID, byte[])} or {@link BleDevice#write(UUID, byte[], ReadWriteListener)}.
			 *
			 * @see #isWrite()
			 */
			WRITE,

			/**
			 * Similar to {@link #WRITE} but under the hood {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE} is used.
			 * See also {@link BluetoothGattCharacteristic#PROPERTY_WRITE_NO_RESPONSE}.
			 *
			 * @see #isWrite()
			 */
			WRITE_NO_RESPONSE,

			/**
			 * Similar to {@link #WRITE} but under the hood {@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED} is used.
			 * See also {@link BluetoothGattCharacteristic#PROPERTY_SIGNED_WRITE}.
			 *
			 * @see #isWrite()
			 */
			WRITE_SIGNED,

			/**
			 * Associated with {@link BleDevice#startPoll(UUID, Interval, ReadWriteListener)} or {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)}.
			 */
			POLL,

			/**
			 * Associated with {@link BleDevice#enableNotify(UUID, ReadWriteListener)} when we  actually get a notification.
			 */
			NOTIFICATION,

			/**
			 * Similar to {@link #NOTIFICATION}, kicked off from {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, but
			 * under the hood this is treated slightly differently.
			 */
			INDICATION,

			/**
			 * Associated with {@link BleDevice#startChangeTrackingPoll(UUID, Interval, ReadWriteListener)}
			 * or {@link BleDevice#enableNotify(UUID, Interval, ReadWriteListener)} where a force-read timeout is invoked.
			 */
			PSUEDO_NOTIFICATION,

			/**
			 * Associated with {@link BleDevice#enableNotify(UUID, ReadWriteListener)} and called when enabling the notification completes by writing to the
			 * Descriptor of the given {@link UUID}. {@link BleDevice.ReadWriteListener.Status#SUCCESS} doesn't <i>necessarily</i> mean that notifications will
			 * definitely now work (there may be other issues in the underlying stack), but it's a reasonable guarantee.
			 */
			ENABLING_NOTIFICATION,

			/**
			 * Opposite of {@link #ENABLING_NOTIFICATION}.
			 */
			DISABLING_NOTIFICATION;

			/**
			 * Returns <code>true</code> for every {@link Type} except {@link #isWrite()}, {@link #ENABLING_NOTIFICATION}, and
			 * {@link #DISABLING_NOTIFICATION}. Overall this convenience method is meant to tell you when we've <i>received</i> something from
			 * the device as opposed to writing something to it.
			 */
			public boolean isRead()
			{
				return !isWrite() && this != ENABLING_NOTIFICATION && this != DISABLING_NOTIFICATION;
			}

			/**
			 * Returns <code>true</code> if <code>this</code> is {@link #WRITE} or {@link #WRITE_NO_RESPONSE} or {@link #WRITE_SIGNED}.
			 */
			public boolean isWrite()
			{
				return this == WRITE || this == WRITE_NO_RESPONSE || this == WRITE_SIGNED;
			}

			/**
			 * Returns true if <code>this</code> is {@link #NOTIFICATION}, {@link #PSUEDO_NOTIFICATION}, or {@link #INDICATION}.
			 */
			public boolean isNotification()
			{
				return this.isNativeNotification() || this == PSUEDO_NOTIFICATION;
			}

			/**
			 * Subset of {@link #isNotification()}, returns <code>true</code> only for {@link #NOTIFICATION} and {@link #INDICATION}, i.e. only
			 * notifications who origin is an *actual* notification (or indication) sent from the remote BLE device.
			 */
			public boolean isNativeNotification()
			{
				return this == NOTIFICATION || this == INDICATION;
			}

			/**
			 * Returns the {@link BleDeviceConfig.HistoricalDataLogFilter.Source} equivalent
			 * for this {@link BleDevice.ReadWriteListener.Type}, or {@link BleDeviceConfig.HistoricalDataLogFilter.Source#NULL}.
			 */
			public BleDeviceConfig.HistoricalDataLogFilter.Source toHistoricalDataSource()
			{
				switch(this)
				{
					case READ:					return BleDeviceConfig.HistoricalDataLogFilter.Source.READ;
					case POLL:					return BleDeviceConfig.HistoricalDataLogFilter.Source.POLL;
					case NOTIFICATION:			return BleDeviceConfig.HistoricalDataLogFilter.Source.NOTIFICATION;
					case INDICATION:			return BleDeviceConfig.HistoricalDataLogFilter.Source.INDICATION;
					case PSUEDO_NOTIFICATION:	return BleDeviceConfig.HistoricalDataLogFilter.Source.PSUEDO_NOTIFICATION;
				}

				return BleDeviceConfig.HistoricalDataLogFilter.Source.NULL;
			}

			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * The type of GATT object, provided by {@link ReadWriteEvent#target}.
		 */
		public static enum Target implements UsesCustomNull
		{
			/**
			 * As of now, only used for {@link ConnectionFailListener.ConnectionFailEvent#txnFailReason()} in some cases.
			 */
			NULL,

			/**
			 * The {@link ReadWriteEvent} returned has to do with a {@link BluetoothGattCharacteristic} under the hood.
			 */
			CHARACTERISTIC,

			/**
			 * The {@link ReadWriteEvent} returned has to do with a {@link BluetoothGattDescriptor} under the hood.
			 */
			DESCRIPTOR,

			/**
			 * The {@link ReadWriteEvent} is coming in from using {@link BleDevice#readRssi(ReadWriteListener)} or
			 * {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)}.
			 */
			RSSI;

			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Provides a bunch of information about a completed read, write, or notification.
		 */
		@Immutable
		public static class ReadWriteEvent implements UsesCustomNull
		{
			/**
			 * Value used in place of <code>null</code>, either indicating that {@link #descUuid} isn't used for the {@link ReadWriteEvent}
			 * because {@link #target} is {@link Target#CHARACTERISTIC}, or that both {@link #descUuid} and {@link #charUuid} aren't applicable
			 * because {@link #target} is {@link Target#RSSI}.
			 */
			public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;

			/**
			 * The {@link BleDevice} this {@link ReadWriteEvent} is for.
			 */
			public BleDevice device() {  return m_device;  }
			private final BleDevice m_device;

			/**
			 * The type of operation, read, write, etc.
			 */
			public Type type() {  return m_type; }
			private final Type m_type;

			/**
			 * The type of GATT object this {@link ReadWriteEvent} is for, currently characteristic, descriptor, or rssi.
			 */
			public Target target() {  return m_target;  }
			private final Target m_target;

			/**
			 * The {@link UUID} of the service associated with this {@link ReadWriteEvent}. This will always be a non-null {@link UUID},
			 * even if {@link #target} is {@link Target#DESCRIPTOR}. If {@link #target} is {@link Target#RSSI} then this will be referentially equal
			 * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
			 */
			public UUID serviceUuid() {  return m_serviceUuid;  }
			private final UUID m_serviceUuid;

			/**
			 * The {@link UUID} of the characteristic associated with this {@link ReadWriteEvent}. This will always be a non-null {@link UUID},
			 * even if {@link #target} is {@link Target#DESCRIPTOR}. If {@link #target} is {@link Target#RSSI} then this will be referentially equal
			 * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
			 */
			public UUID charUuid() {  return m_charUuid;  }
			private final UUID m_charUuid;

			/**
			 * The {@link UUID} of the descriptor associated with this {@link ReadWriteEvent}. If {@link #target} is
			 * {@link Target#CHARACTERISTIC} or {@link Target#RSSI} then this will be referentially equal
			 * (i.e. you can use == to compare) to {@link #NON_APPLICABLE_UUID}.
			 */
			public UUID descUuid() {  return m_descUuid;  }
			private final UUID m_descUuid;

			/**
			 * The data sent to the peripheral if {@link ReadWriteEvent#type} {@link Type#isWrite()}, otherwise the data received from the
			 * peripheral if {@link ReadWriteEvent#type} {@link Type#isRead()}. This will never be <code>null</code>. For error cases it will be a
			 * zero-length array.
			 */
			public byte[] data() {  return m_data;  }
			private final byte[] m_data;

			/**
			 * This value gets updated as a result of a {@link BleDevice#readRssi(ReadWriteListener)} call. It will
			 * always be equivalent to {@link BleDevice#getRssi()} but is included here for convenience.
			 *
			 * @see BleDevice#getRssi()
			 * @see BleDevice#getRssiPercent()
			 * @see BleDevice#getDistance()
			 */
			public int rssi() {  return m_rssi;  }
			private final int m_rssi;

			/**
			 * Indicates either success or the type of failure. Some values of {@link Status} are not used for certain values of {@link Type}.
			 * For example a {@link Type#NOTIFICATION} cannot fail with {@link BleDevice.ReadWriteListener.Status#TIMED_OUT}.
			 */
			public Status status() {  return m_status;  }
			private final Status m_status;

			/**
			 * Time spent "over the air" - so in the native stack, processing in
			 * the peripheral's embedded software, what have you. This will
			 * always be slightly less than {@link #time_total()}.
			 */
			public Interval time_ota() {  return m_transitTime;  }
			private final Interval m_transitTime;

			/**
			 * Total time it took for the operation to complete, whether success
			 * or failure. This mainly includes time spent in the internal job
			 * queue plus {@link ReadWriteEvent#time_ota()}. This will always be
			 * longer than {@link #time_ota()}, though usually only slightly so.
			 */
			public Interval time_total() {  return m_totalTime;  }
			private final Interval m_totalTime;

			/**
			 * The native gatt status returned from the stack, if applicable. If the {@link #status} returned is, for example,
			 * {@link ReadWriteListener.Status#NO_MATCHING_TARGET}, then the operation didn't even reach the point where a gatt status is
			 * provided, in which case this member is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE} (value of
			 * {@value com.idevicesinc.sweetblue.BleStatuses#GATT_STATUS_NOT_APPLICABLE}). Otherwise it will be <code>0</code> for success or greater than
			 * <code>0</code> when there's an issue. <i>Generally</i> this value will only be meaningful when {@link #status} is
			 * {@link ReadWriteListener.Status#SUCCESS} or {@link ReadWriteListener.Status#REMOTE_GATT_FAILURE}. There are
			 * also some cases where this will be 0 for success but {@link #status} is for example
			 * {@link ReadWriteListener.Status#NULL_DATA} - in other words the underlying stack deemed the operation a success but SweetBlue
			 * disagreed. For this reason it's recommended to treat this value as a debugging tool and use {@link #status} for actual
			 * application logic if possible.
			 * <br><br>
			 * See {@link BluetoothGatt} for its static <code>GATT_*</code> status code members. Also see the source code of
			 * {@link BleStatuses} for SweetBlue's more comprehensive internal reference list of gatt status values. This list may not be
			 * totally accurate or up-to-date, nor may it match GATT_ values used by the bluetooth stack on your phone.
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			ReadWriteEvent(BleDevice device, P_Characteristic characteristic, UUID descUuid, Type type, Target target, byte[] data, Status status, int gattStatus, double totalTime, double transitTime)
			{
				this(device, characteristic.getServiceUuid(), characteristic.getUuid(), descUuid, type, target, data, status, gattStatus, totalTime, transitTime);
			}

			ReadWriteEvent(BleDevice device, UUID serviceUuid, UUID charUuid, UUID descUuid, Type type, Target target, byte[] data, Status status, int gattStatus, double totalTime, double transitTime)
			{
				this.m_device = device;
				this.m_serviceUuid = serviceUuid != null ? serviceUuid : NON_APPLICABLE_UUID;
				this.m_charUuid = charUuid != null ? charUuid : NON_APPLICABLE_UUID;
				this.m_descUuid = descUuid != null ? descUuid : NON_APPLICABLE_UUID;
				this.m_type = type;
				this.m_target = target;
				this.m_status = status;
				this.m_gattStatus = gattStatus;
				this.m_totalTime = Interval.secs(totalTime);
				this.m_transitTime = Interval.secs(transitTime);
				this.m_data = data != null ? data : EMPTY_BYTE_ARRAY;
				this.m_rssi = device.getRssi();
			}

			ReadWriteEvent(BleDevice device, Type type, int rssi, Status status, int gattStatus, double totalTime, double transitTime)
			{
				this.m_device = device;
				this.m_charUuid = NON_APPLICABLE_UUID;
				this.m_descUuid = NON_APPLICABLE_UUID;
				this.m_serviceUuid = NON_APPLICABLE_UUID;
				this.m_type = type;
				this.m_target = Target.RSSI;
				this.m_status = status;
				this.m_gattStatus = gattStatus;
				this.m_totalTime = Interval.secs(totalTime);
				this.m_transitTime = Interval.secs(transitTime);
				this.m_data = EMPTY_BYTE_ARRAY;
				this.m_rssi = status == Status.SUCCESS ? rssi : device.getRssi();
			}

			static ReadWriteEvent NULL(BleDevice device)
			{
				return new ReadWriteEvent(device, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, Type.NULL, Target.NULL, EMPTY_BYTE_ARRAY, Status.NULL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Interval.ZERO.secs(), Interval.ZERO.secs());
			}

			/**
			 * Forwards {@link BleDevice#getNativeCharacteristic(UUID, UUID)}, which will be non=null
			 * if {@link #target()} is {@link Target#CHARACTERISTIC} or {@link Target#DESCRIPTOR}.
			 */
			public @Nullable(Prevalence.NORMAL) BluetoothGattCharacteristic characteristic()
			{
				return device().getNativeCharacteristic(serviceUuid(), charUuid());
			}

			/**
			 * Forwards {@link BleDevice#getNativeDescriptor(UUID, UUID)}, which will be non=null
			 * if {@link #target()} is {@link Target#DESCRIPTOR}.
			 */
			public @Nullable(Prevalence.NORMAL) BluetoothGattDescriptor descriptor()
			{
				return device().getNativeDescriptor(charUuid(), descUuid());
			}

			/**
			 * Convenience method for checking if {@link ReadWriteEvent#status} equals {@link BleDevice.ReadWriteListener.Status#SUCCESS}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.SUCCESS;
			}

			/**
			 * Forwards {@link Status#wasCancelled()}.
			 */
			public boolean wasCancelled()
			{
				return status().wasCancelled();
			}

			/**
			 * Forwards {@link Type#isNotification()}.
			 */
			public boolean isNotification()
			{
				return type().isNotification();
			}

			/**
			 * Forwards {@link Type#isRead()}.
			 */
			public boolean isRead()
			{
				return type().isRead();
			}

			/**
			 * Forwards {@link Type#isWrite()}.
			 */
			public boolean isWrite()
			{
				return type().isWrite();
			}

			/**
			 * Returns the first byte from {@link #data()}, or 0x0 if not available.
			 */
			public byte data_byte()
			{
				return data().length > 0 ? data()[0] : 0x0;
			}

			/**
			 * Convenience method that attempts to parse the data as a UTF-8 string.
			 */
			public @Nullable(Prevalence.NEVER) String data_utf8()
			{
				return data_string("UTF-8");
			}

			/**
			 * Best effort parsing of {@link #data()} as a {@link String}. For now simply forwards {@link #data_utf8()}.
			 * In the future may try to autodetect encoding first.
			 */
			public @Nullable(Prevalence.NEVER) String data_string()
			{
				return data_utf8();
			}

			/**
			 * Convenience method that attempts to parse {@link #data()} as a {@link String} with the given charset, for example <code>"UTF-8"</code>.
			 */
			public @Nullable(Prevalence.NEVER) String data_string(final String charset)
			{
				return Utils.getStringValue(data(), charset);
			}

			/**
			 * Forwards {@link Type#isNull()}.
			 */
			@Override public boolean isNull()
			{
				return type().isNull();
			}

			@Override public String toString()
			{
				if (isNull())
				{
					return Type.NULL.toString();
				}
				else
				{
					if (target() == Target.RSSI)
					{
						return Utils.toString
						(
							this.getClass(),
							"status",			status(),
							"type",				type(),
							"target",			target(),
							"rssi",				rssi(),
							"gattStatus",		device().getManager().getLogger().gattStatus(gattStatus())
						);
					}
					else
					{
						return Utils.toString
						(
							this.getClass(),
							"status",			status(),
							"data",				Arrays.toString(data()),
							"type",				type(),
							"charUuid",			device().getManager().getLogger().uuidName(charUuid()),
							"gattStatus",		device().getManager().getLogger().gattStatus(gattStatus())
						);
					}
				}
			}
		}

		/**
		 * Called when a read or write is complete or when a notification comes in or when a notification is enabled/disabled.
		 */
		void onEvent(ReadWriteEvent e);
	}

	/**
	 * Provide an implementation to {@link BleDevice#setListener_State(StateListener)} and/or
	 * {@link BleManager#setListener_DeviceState(BleDevice.StateListener)} to receive state change events.
	 *
	 * @see BleDeviceState
	 * @see BleDevice#setListener_State(StateListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface StateListener
	{
		/**
		 * Subclass that adds the device field.
		 */
		@Immutable
		public static class StateEvent extends State.ChangeEvent<BleDeviceState>
		{
			/**
			 * The device undergoing the state change.
			 */
			public BleDevice device() {  return m_device;  }
			private final BleDevice m_device;

			/**
			 * The change in gattStatus that may have precipitated the state change, or {@link BleDeviceConfig#GATT_STATUS_NOT_APPLICABLE}.
			 * For example if {@link #didEnter(State)} with {@link BleDeviceState#DISCONNECTED} is <code>true</code> and
			 * {@link #didExit(State)} with {@link BleDeviceState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
			 * than zero and give some further hint as to why the connection failed.
			 * <br><br>
			 * See {@link ConnectionFailListener.ConnectionFailEvent#gattStatus()} for more information.
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			StateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
			{
				super(oldStateBits, newStateBits, intentMask);

				this.m_device = device;
				this.m_gattStatus = gattStatus;
			}

			@Override public String toString()
			{
				if( device().is(RECONNECTING_SHORT_TERM) )
				{
					return Utils.toString
					(
						this.getClass(),
						"device",			device().getName_debug(),
						"entered",			Utils.toString(enterMask(),						BleDeviceState.VALUES()),
						"exited", 			Utils.toString(exitMask(),						BleDeviceState.VALUES()),
						"current",			Utils.toString(newStateBits(),					BleDeviceState.VALUES()),
						"current_native",	Utils.toString(device().getNativeStateMask(),	BleDeviceState.VALUES()),
						"gattStatus",		device().m_logger.gattStatus(gattStatus())
					);
				}
				else
				{
					return Utils.toString
					(
						this.getClass(),
						"device",			device().getName_debug(),
						"entered",			Utils.toString(enterMask(),						BleDeviceState.VALUES()),
						"exited", 			Utils.toString(exitMask(),						BleDeviceState.VALUES()),
						"current",			Utils.toString(newStateBits(),					BleDeviceState.VALUES()),
						"gattStatus",		device().m_logger.gattStatus(gattStatus())
					);
				}
			}
		}

		/**
		 * Called when a device's bitwise {@link BleDeviceState} changes. As many bits as possible are flipped at the same time.
		 */
		void onEvent(StateEvent e);
	}

	/**
	 * Provide an implementation of this callback to {@link BleDevice#setListener_ConnectionFail(ConnectionFailListener)}.
	 *
	 * @see DefaultConnectionFailListener
	 * @see BleDevice#setListener_ConnectionFail(ConnectionFailListener)
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ConnectionFailListener
	{
		/**
		 * The reason for the connection failure.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectionFailEvent#status()} given
			 * to {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} will *never* be {@link ConnectionFailListener.Status#NULL}.
			 */
			NULL,

			/**
			 * A call was made to {@link BleDevice#connect()} or its overloads
			 * but {@link ConnectionFailEvent#device()} is already
			 * {@link BleDeviceState#CONNECTING} or {@link BleDeviceState#CONNECTED}.
			 */
			ALREADY_CONNECTING_OR_CONNECTED,

			/**
			 * {@link BleDevice#connect()} (or various overloads) was called on {@link BleDevice#NULL}.
			 */
			NULL_DEVICE,

			/**
			 * Couldn't connect through {@link BluetoothDevice#connectGatt(android.content.Context, boolean, BluetoothGattCallback)}
			 * because it (a) {@link Timing#IMMEDIATELY} returned <code>null</code>, (b) {@link Timing#EVENTUALLY} returned a bad
			 * {@link ConnectionFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
			 */
			NATIVE_CONNECTION_FAILED,

			/**
			 * {@link BluetoothGatt#discoverServices()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
			 * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectionFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
			 */
			DISCOVERING_SERVICES_FAILED,

			/**
			 * {@link BluetoothDevice#createBond()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
			 * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectionFailEvent#bondFailReason()}, or (c) {@link Timing#TIMED_OUT}.
			 * <br><br>
			 * NOTE: {@link BleDeviceConfig#bondingFailFailsConnection} must be <code>true</code> for this {@link Status} to be applicable.
			 *
			 * @see BondListener
			 */
			BONDING_FAILED,

			/**
			 * The {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Auth)} or
			 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
			 *
			 */
			AUTHENTICATION_FAILED,

			/**
			 * {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Init)} or
			 * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
			 *
			 */
			INITIALIZATION_FAILED,

			/**
			 * Remote peripheral randomly disconnected sometime during the connection process. Similar to {@link #NATIVE_CONNECTION_FAILED}
			 * but only occurs after the device is {@link BleDeviceState#CONNECTED} and we're going through
			 * {@link BleDeviceState#DISCOVERING_SERVICES}, or {@link BleDeviceState#AUTHENTICATING}, or what have you. It might
			 * be from the device turning off, or going out of range, or any other random reason.
			 */
			ROGUE_DISCONNECT,

			/**
			 * {@link BleDevice#disconnect()} was called sometime during the connection process.
			 */
			EXPLICIT_DISCONNECT,

			/**
			 * {@link BleManager#reset()} or {@link BleManager#turnOff()} (or
			 * overloads) were called sometime during the connection process.
			 * Basic testing reveals that this value will also be used when a
			 * user turns off BLE by going through their OS settings, airplane
			 * mode, etc., but it's not absolutely *certain* that this behavior
			 * is consistent across phones. For example there might be a phone
			 * that kills all connections before going through the ble turn-off
			 * process, in which case SweetBlue doesn't know the difference and
			 * {@link #ROGUE_DISCONNECT} will be used.
			 */
			BLE_TURNING_OFF;

			/**
			 * Returns true for {@link #EXPLICIT_DISCONNECT} or {@link #BLE_TURNING_OFF}.
			 */
			public boolean wasCancelled()
			{
				return this == EXPLICIT_DISCONNECT || this == BLE_TURNING_OFF;
			}

			/**
			 * Same as {@link #wasCancelled()}, at least for now, but just being more "explicit", no pun intended.
			 */
			boolean wasExplicit()
			{
				return wasCancelled();
			}

			/**
			 * Whether this reason honors a {@link Please#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
			 * <code>this</code> is {@link #ALREADY_CONNECTING_OR_CONNECTED}.
			 */
			public boolean allowsRetry()
			{
				return !this.wasCancelled() && this != ALREADY_CONNECTING_OR_CONNECTED;
			}

			@Override public boolean isNull()
			{
				return this == NULL;
			}

			/**
			 * Convenience method that returns whether this status is something that your app user would usually care about.
			 * If this returns <code>true</code> then perhaps you should pop up a {@link android.widget.Toast} or something of that nature.
			 */
			public boolean shouldBeReportedToUser()
			{
				return	this == NATIVE_CONNECTION_FAILED		||
						this == DISCOVERING_SERVICES_FAILED		||
						this == BONDING_FAILED					||
						this == AUTHENTICATION_FAILED			||
						this == INITIALIZATION_FAILED			||
						this == ROGUE_DISCONNECT				 ;
			}
		}

		/**
		 * For {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#DISCOVERING_SERVICES_FAILED}, and
		 * {@link Status#BONDING_FAILED}, gives further timing information on when the failure took place.
		 * For all other reasons, {@link ConnectionFailEvent#timing()} will be {@link #NOT_APPLICABLE}.
		 */
		public static enum Timing
		{
			/**
			 * For reasons like {@link ConnectionFailListener.Status#BLE_TURNING_OFF}, {@link ConnectionFailListener.Status#AUTHENTICATION_FAILED}, etc.
			 */
			NOT_APPLICABLE,

			/**
			 * The operation failed immediately, for example by the native stack method returning <code>false</code> from a method call.
			 */
			IMMEDIATELY,

			/**
			 * The operation failed in the native stack. {@link ConnectionFailListener.ConnectionFailEvent#gattStatus()}
			 * will probably be a positive number if {@link ConnectionFailListener.ConnectionFailEvent#status()} is
			 * {@link ConnectionFailListener.Status#NATIVE_CONNECTION_FAILED} or {@link ConnectionFailListener.Status#DISCOVERING_SERVICES_FAILED}.
			 * {@link ConnectionFailListener.ConnectionFailEvent#bondFailReason()} will probably be a positive number if
			 * {@link ConnectionFailListener.ConnectionFailEvent#status()} is {@link ConnectionFailListener.Status#BONDING_FAILED}.
			 */
			EVENTUALLY,

			/**
			 * The operation took longer than the time dictated by {@link BleDeviceConfig#timeoutRequestFilter}.
			 */
			TIMED_OUT;
		}

		/**
		 * Describes usage of the <code>autoConnect</code> parameter for
		 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
		 */
		@com.idevicesinc.sweetblue.annotations.Advanced
		public static enum AutoConnectUsage
		{
			/**
			 * Used when we didn't start the connection process, i.e. it came out of nowhere. Rare case but can happen, for example after
			 * SweetBlue considers a connect timed out based on {@link BleDeviceConfig#timeoutRequestFilter} but then it somehow
			 * does come in (shouldn't happen but who knows).
			 */
			UNKNOWN,

			/**
			 * Usage is not applicable to the{@link ConnectionFailEvent#status()} given.
			 */
			NOT_APPLICABLE,

			/**
			 * <code>autoConnect</code> was used.
			 */
			USED,

			/**
			 * <code>autoConnect</code> was not used.
			 */
			NOT_USED;
		}

		/**
		 * Return value for {@link ConnectionFailListener#onEvent(ConnectionFailEvent)}.
		 * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
		 */
		@Immutable
		public static class Please
		{
			static enum PE_Please
			{
				RETRY, RETRY_WITH_AUTOCONNECT_TRUE, RETRY_WITH_AUTOCONNECT_FALSE, DO_NOT_RETRY;

				boolean isRetry()
				{
					return this != DO_NOT_RETRY;
				}
			}

			private final PE_Please m_please;

			private Please(PE_Please please)
			{
				m_please = please;
			}

			PE_Please please()
			{
				return m_please;
			}

			/**
			 * Return this to retry the connection, continuing the connection fail retry loop. <code>autoConnect</code> passed to
			 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
			 * will be false or true based on what has worked in the past, or on {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			public static Please retry()
			{
				return new Please(PE_Please.RETRY);
			}

			/**
			 * Returns {@link #retry()} if the given condition holds <code>true</code>, {@link #doNotRetry()} otherwise.
			 */
			public static Please retryIf(boolean condition)
			{
				return condition ? retry() : doNotRetry();
			}

			/**
			 * Return this to stop the connection fail retry loop.
			 */
			public static Please doNotRetry()
			{
				return new Please(PE_Please.DO_NOT_RETRY);
			}

			/**
			 * Returns {@link #doNotRetry()} if the given condition holds <code>true</code>, {@link #retry()} otherwise.
			 */
			public static Please doNotRetryIf(boolean condition)
			{
				return condition ? doNotRetry() : retry();
			}

			/**
			 * Same as {@link #retry()}, but <code>autoConnect=true</code> will be passed to
			 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
			 * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectTrue()
			{
				return new Please(PE_Please.RETRY_WITH_AUTOCONNECT_TRUE);
			}

			/**
			 * Opposite of{@link #retryWithAutoConnectTrue()}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectFalse()
			{
				return new Please(PE_Please.RETRY_WITH_AUTOCONNECT_FALSE);
			}

			/**
			 * Returns <code>true</code> for everything except {@link #doNotRetry()}.
			 */
			public boolean isRetry()
			{
				return m_please != null && m_please.isRetry();
			}
		}

		/**
		 * Structure passed to {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} to provide more info about how/why the connection failed.
		 */
		@Immutable
		public static class ConnectionFailEvent implements UsesCustomNull
		{
			/**
			 * The {@link BleDevice} this {@link ConnectionFailEvent} is for.
			 */
			public BleDevice device() {  return m_device;  }
			private final BleDevice m_device;

			/**
			 * General reason why the connection failed.
			 */
			public Status status() {  return m_status;  }
			private final Status m_status;

			/**
			 * The failure count so far. This will start at 1 and keep incrementing for more failures.
			 */
			public int failureCountSoFar() {  return m_failureCountSoFar;  }
			private final int m_failureCountSoFar;

			/**
			 * How long the last connection attempt took before failing.
			 */
			public Interval attemptTime_latest() {  return m_latestAttemptTime;  }
			private final Interval m_latestAttemptTime;

			/**
			 * How long it's been since {@link BleDevice#connect()} (or overloads) were initially called.
			 */
			public Interval attemptTime_total() {  return m_totalAttemptTime;  }
			private final Interval m_totalAttemptTime;

			/**
			 * The gattStatus returned, if applicable, from native callbacks like {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)}
			 * or {@link BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)}.
			 * If not applicable, for example if {@link ConnectionFailEvent#status()} is {@link Status#EXPLICIT_DISCONNECT}, then this is set to
			 * {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
			 * <br><br>
			 * See {@link ReadWriteEvent#gattStatus()} for more information about gatt status codes in general.
			 *
			 * @see ReadWriteEvent#gattStatus
			 */
			public int gattStatus() {  return m_gattStatus;  }
			private final int m_gattStatus;

			/**
			 * See {@link BondEvent#failReason()}.
			 */
			public int bondFailReason() {  return m_bondFailReason;  }
			private final int m_bondFailReason;

			/**
			 * The highest state reached by the latest connection attempt.
			 */
			public BleDeviceState highestStateReached_latest() {  return m_highestStateReached_latest;  }
			private final BleDeviceState m_highestStateReached_latest;

			/**
			 * The highest state reached during the whole connection attempt cycle.
			 * <br><br>
			 * TIP: You can use this to keep the visual feedback in your connection progress UI "bookmarked" while the connection retries
			 * and goes through previous states again.
			 */
			public BleDeviceState highestStateReached_total() {  return m_highestStateReached_total;  }
			private final BleDeviceState m_highestStateReached_total;

			/**
			 * Whether <code>autoConnect=true</code> was passed to {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}.
			 * See more discussion at {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public AutoConnectUsage autoConnectUsage() {  return m_autoConnectUsage;  }
			private final AutoConnectUsage m_autoConnectUsage;

			/**
			 * Further timing information for {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#BONDING_FAILED}, and {@link Status#DISCOVERING_SERVICES_FAILED}.
			 */
			public Timing timing() {  return m_timing;  }
			private final Timing m_timing;

			/**
			 * If {@link ConnectionFailEvent#status()} is {@link Status#AUTHENTICATION_FAILED} or
			 * {@link Status#INITIALIZATION_FAILED} and {@link BleTransaction#fail()} was called somewhere in or
			 * downstream of {@link ReadWriteListener#onEvent(ReadWriteEvent)}, then the {@link ReadWriteEvent} passed there will be returned
			 * here. Otherwise, this will return a {@link ReadWriteEvent} for which {@link ReadWriteEvent#isNull()} returns <code>true</code>.
			 */
			public ReadWriteListener.ReadWriteEvent txnFailReason() {  return m_txnFailReason;  }
			private final ReadWriteListener.ReadWriteEvent m_txnFailReason;

			/**
			 * Returns a chronologically-ordered list of all {@link ConnectionFailEvent} instances returned through
			 * {@link ConnectionFailListener#onEvent(ConnectionFailEvent)} since the first call to {@link BleDevice#connect()},
			 * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
			 * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
			 * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#DISCONNECTED} for good.
			 */
			public ConnectionFailEvent[] history()  {  return m_history;  }
			private final ConnectionFailEvent[] m_history;

			ConnectionFailEvent(BleDevice device, Status reason, Timing timing, int failureCountSoFar, Interval latestAttemptTime, Interval totalAttemptTime, int gattStatus, BleDeviceState highestStateReached, BleDeviceState highestStateReached_total, AutoConnectUsage autoConnectUsage, int bondFailReason, ReadWriteListener.ReadWriteEvent txnFailReason, ArrayList<ConnectionFailEvent> history)
			{
				this.m_device = device;
				this.m_status = reason;
				this.m_timing = timing;
				this.m_failureCountSoFar = failureCountSoFar;
				this.m_latestAttemptTime = latestAttemptTime;
				this.m_totalAttemptTime = totalAttemptTime;
				this.m_gattStatus = gattStatus;
				this.m_highestStateReached_latest = highestStateReached != null ? highestStateReached : BleDeviceState.NULL;
				this.m_highestStateReached_total = highestStateReached_total != null ? highestStateReached_total : BleDeviceState.NULL;
				this.m_autoConnectUsage = autoConnectUsage;
				this.m_bondFailReason = bondFailReason;
				this.m_txnFailReason = txnFailReason;

				if( history == null )
				{
					this.m_history = EMPTY_HISTORY();
				}
				else
				{
					this.m_history = new ConnectionFailEvent[history.size()+1];
					for( int i = 0; i < history.size(); i++ )
					{
						this.m_history[i] = history.get(i);
					}

					this.m_history[this.m_history.length-1] = this;
				}

				m_device.getManager().ASSERT(highestStateReached != null, "highestState_latest shouldn't be null.");
				m_device.getManager().ASSERT(highestStateReached_total != null, "highestState_total shouldn't be null.");
			}

			private static ConnectionFailEvent[] s_emptyHistory = null;
			static ConnectionFailEvent[] EMPTY_HISTORY()
			{
				s_emptyHistory = s_emptyHistory != null ? s_emptyHistory : new ConnectionFailEvent[]{};

				return s_emptyHistory;
			}

			static ConnectionFailEvent NULL(BleDevice device)
			{
				return new ConnectionFailEvent(device, Status.NULL, Timing.NOT_APPLICABLE, 0, Interval.DISABLED, Interval.DISABLED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, BleDeviceState.NULL, AutoConnectUsage.NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, device.NULL_READWRITE_EVENT(), null);
			}

			static ConnectionFailEvent DUMMY(BleDevice device, Status reason)
			{
				return new ConnectionFailListener.ConnectionFailEvent(device, reason, Timing.TIMED_OUT, 0, Interval.ZERO, Interval.ZERO, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, BleDeviceState.NULL, AutoConnectUsage.NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, device.NULL_READWRITE_EVENT(), null);
			}

			/**
			 * Returns whether this {@link ConnectionFailEvent} instance is a "dummy" value. For now used for
			 * {@link BleDeviceConfig.ReconnectRequestFilter.ReconnectRequestEvent#connectionFailInfo()} in certain situations.
			 */
			@Override public boolean isNull()
			{
				return status().isNull();
			}

			/**
			 * Forwards {@link com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Status#shouldBeReportedToUser()}
			 * using {@link #status()}.
			 */
			public boolean shouldBeReportedToUser()
			{
				return status().shouldBeReportedToUser();
			}

			@Override public String toString()
			{
				if (isNull())
				{
					return Status.NULL.name();
				}
				else
				{
					if( status() == Status.BONDING_FAILED )
					{
						return Utils.toString
						(
							this.getClass(),
							"status", 				status(),
							"bondFailReason",		device().getManager().getLogger().gattUnbondReason(bondFailReason()),
							"failureCountSoFar",	failureCountSoFar()
						);
					}
					else
					{
						return Utils.toString
						(
							this.getClass(),
							"status", 				status(),
							"gattStatus",			device().getManager().getLogger().gattStatus(gattStatus()),
							"failureCountSoFar",	failureCountSoFar()
						);
					}
				}
			}
		}

		/**
		 * Return value is ignored if device is either {@link BleDeviceState#RECONNECTING_LONG_TERM} or reason
		 * {@link Status#allowsRetry()} is <code>false</code>. If the device is {@link BleDeviceState#RECONNECTING_LONG_TERM}
		 * then authority is deferred to {@link BleDeviceConfig.ReconnectRequestFilter}.
		 * <br><br>
		 * Otherwise, this method offers a more convenient way of retrying a connection, as opposed to manually doing it yourself. It also lets
		 * the library handle things in a slightly more optimized/cleaner fashion and so is recommended for that reason also.
		 * <br><br>
		 * NOTE that this callback gets fired *after* {@link StateListener} lets you know that the device is {@link BleDeviceState#DISCONNECTED}.
		 * <br><br>
		 * The time parameters like {@link ConnectionFailEvent#attemptTime_latest()} are of optional use to you to decide if connecting again
		 * is worth it. For example if you've been trying to connect for 10 seconds already, chances are that another connection attempt probably won't work.
		 */
		Please onEvent(ConnectionFailEvent e);
	}

	/**
	 * Default implementation of {@link ConnectionFailListener} that attempts a certain number of retries. An instance of this class is set by default
	 * for all new {@link BleDevice} instances using {@link BleDevice.DefaultConnectionFailListener#DEFAULT_CONNECTION_FAIL_RETRY_COUNT}.
	 * Use {@link BleDevice#setListener_ConnectionFail(ConnectionFailListener)} to override the default behavior.
	 *
	 * @see ConnectionFailListener
	 * @see BleDevice#setListener_ConnectionFail(ConnectionFailListener)
	 */
	@Immutable
	public static class DefaultConnectionFailListener implements ConnectionFailListener
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
		 * The default connection fail limit past which {@link DefaultConnectionFailListener} will start returning {@link Please#retryWithAutoConnectTrue()}.
		 */
		public static final int DEFAULT_FAIL_COUNT_BEFORE_USING_AUTOCONNECT = 2;

		/**
		 * The maximum amount of time to keep trying if connection is failing due to (what usually are) transient bonding failures
		 */
		public static final Interval MAX_RETRY_TIME_FOR_BOND_FAILURE = Interval.secs(120.0);

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

			//--- DRK > It has been noticed that bonding can fail several times due to the follow status code but then succeed,
			//---		so we just keep on trying for a little bit in case we can eventually make it.
			//---		NOTE: After testing for a little bit, this doesn't seem to work, regardless of how much time you give it.
//			if( e.bondFailReason() == BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN )
//			{
//				final Interval timeNow = e.attemptTime_total();
//				Interval timeSinceFirstUnbond = e.attemptTime_total();
//				final ConnectionFailEvent[] history = e.history();
//				for( int i = history.length-1; i >= 0; i-- )
//				{
//					final ConnectionFailEvent history_ith = history[i];
//
//					if( history_ith.bondFailReason() == BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN )
//					{
//						timeSinceFirstUnbond = history_ith.attemptTime_total();
//					}
//					else
//					{
//						break;
//					}
//				}
//
//				final Interval totalTimeFailingDueToBondingIssues = timeNow.minus(timeSinceFirstUnbond);
//
//				if( totalTimeFailingDueToBondingIssues.lt(MAX_RETRY_TIME_FOR_BOND_FAILURE) )
//				{
//					return Please.retry();
//				}
//			}

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

	/**
	 * Pass an instance of this listener to {@link BleDevice#setListener_Bond(BondListener)} or {@link BleDevice#bond(BondListener)}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface BondListener
	{
		/**
		 * Used on {@link BondEvent#status()} to roughly enumerate success or failure.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills soft contract of {@link UsesCustomNull}.
			 *
			 * @see #isNull().
			 */
			NULL,

			/**
			 * The {@link BleDevice#bond()} call succeeded.
			 */
			SUCCESS,

			/**
			 * {@link BleDevice#bond(BondListener)} (or overloads) was called on {@link BleDevice#NULL}.
			 */
			NULL_DEVICE,

			/**
			 * Already {@link BleDeviceState#BONDED} or in the process of {@link BleDeviceState#BONDING}.
			 */
			ALREADY_BONDING_OR_BONDED,

			/**
			 * The call to {@link BluetoothDevice#createBond()} returned <code>false</code> and thus failed immediately.
			 */
			FAILED_IMMEDIATELY,

			/**
			 * We received a {@link BluetoothDevice#ACTION_BOND_STATE_CHANGED} through our internal {@link BroadcastReceiver} that we went from
			 * {@link BleDeviceState#BONDING} back to {@link BleDeviceState#UNBONDED}, which means the attempt failed.
			 * See {@link BondEvent#failReason()} for more information.
			 */
			FAILED_EVENTUALLY,

			/**
			 * The bond operation took longer than the time set in {@link BleDeviceConfig#timeoutRequestFilter} so we cut it loose.
			 */
			TIMED_OUT,

			/**
			 * A call was made to {@link BleDevice#unbond()} at some point during the bonding process.
			 */
			CANCELLED_FROM_UNBOND,

			/**
			 * Cancelled from {@link BleManager} going {@link BleManagerState#TURNING_OFF} or
			 * {@link BleManagerState#OFF}, probably from calling {@link BleManager#reset()}.
			 */
			CANCELLED_FROM_BLE_TURNING_OFF;

			/**
			 * @return <code>true</code> for {@link #CANCELLED_FROM_BLE_TURNING_OFF} or {@link #CANCELLED_FROM_UNBOND}.
			 */
			public boolean wasCancelled()
			{
				return this == CANCELLED_FROM_BLE_TURNING_OFF || this == CANCELLED_FROM_UNBOND;
			}

			boolean canFailConnection()
			{
				return this == FAILED_IMMEDIATELY || this == FAILED_EVENTUALLY || this == TIMED_OUT;
			}

			ConnectionFailListener.Timing timing()
			{
				switch (this)
				{
					case FAILED_IMMEDIATELY:		return Timing.IMMEDIATELY;
					case FAILED_EVENTUALLY:			return Timing.EVENTUALLY;
					case TIMED_OUT:					return Timing.TIMED_OUT;
					default:						return Timing.NOT_APPLICABLE;
				}
			}

			/**
			 * @return <code>true</code> if <code>this</code> == {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Struct passed to {@link BondListener#onEvent(BondEvent)} to provide more information about a {@link BleDevice#bond()} attempt.
		 */
		@Immutable
		public static class BondEvent implements UsesCustomNull
		{
			/**
			 * The {@link BleDevice} that attempted to {@link BleDevice#bond()}.
			 */
			public BleDevice device() {  return m_device;  }
			private final BleDevice m_device;

			/**
			 * The {@link Status} associated with this event.
			 */
			public Status status() {  return m_status;  }
			private final Status m_status;

			/**
			 * If {@link #status()} is {@link BondListener.Status#FAILED_EVENTUALLY}, this integer will
			 * be one of the values enumerated in {@link BluetoothDevice} that start with <code>UNBOND_REASON</code> such as
			 * {@link BleStatuses#UNBOND_REASON_AUTH_FAILED}. Otherwise it will be equal to {@link BleDeviceConfig#BOND_FAIL_REASON_NOT_APPLICABLE}.
			 * See also a publically accessible list in {@link BleStatuses}.
			 */
			public int failReason() {  return m_failReason;  }
			private final int m_failReason;

			/**
			 * Tells whether the bond was created through an explicit call through SweetBlue, or otherwise. If
			 * {@link ChangeIntent#INTENTIONAL}, then {@link BleDevice#bond()} (or overloads) were called. If {@link ChangeIntent#UNINTENTIONAL},
			 * then the bond was created "spontaneously" as far as SweetBlue is concerned, whether through another app, the OS Bluetooth
			 * settings, or maybe from a request by the remote BLE device itself.
			 */
			public State.ChangeIntent intent() {  return m_intent;  }
			private final State.ChangeIntent m_intent;

			BondEvent(BleDevice device, Status status, int failReason, State.ChangeIntent intent)
			{
				m_device = device;
				m_status = status;
				m_failReason = failReason;
				m_intent = intent;
			}

			private static BondEvent NULL(final BleDevice device)
			{
				return new BondEvent(device, Status.NULL, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.NULL);
			}

			/**
			 * Shortcut for checking if {@link #status()} == {@link BondListener.Status#SUCCESS}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.SUCCESS;
			}

			/**
			 * Forwards {@link Status#wasCancelled()}.
			 */
			public boolean wasCancelled()
			{
				return status().wasCancelled();
			}

			@Override public String toString()
			{
				if( isNull() )
				{
					return NULL_STRING();
				}
				else
				{
					return Utils.toString
					(
						this.getClass(),
						"device",			device().getName_debug(),
						"status",			status(),
						"failReason",		device().getManager().getLogger().gattUnbondReason(failReason()),
						"intent",			intent()
					);
				}
			}

			@Override public boolean isNull()
			{
				return status().isNull();
			}
		}

		/**
		 * Called after a call to {@link BleDevice#bond(BondListener)} (or overloads),
		 * or when bonding through another app or the operating system settings.
		 */
		void onEvent(BondEvent e);
	}

	/**
	 * A callback that is used by various overloads of {@link BleDevice#loadHistoricalData()} that accept instances hereof.
	 * You can also set default listeners on {@link BleDevice#setListener_HistoricalDataLoad(HistoricalDataLoadListener)}
	 * and {@link BleManager#setListener_HistoricalDataLoad(BleDevice.HistoricalDataLoadListener)}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface HistoricalDataLoadListener
	{
		/**
		 * Enumerates the status codes for operations kicked off from {@link BleDevice#loadHistoricalData()} (or overloads).
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * Historical data is fully loaded to memory and ready to access synchronously (without blocking current thread)
			 * through {@link BleDevice#getHistoricalData_iterator(UUID)} (or overloads).
			 */
			LOADED,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called but the data was already loaded to memory.
			 */
			ALREADY_LOADED,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called but there was no data available to load to memory.
			 */
			NOTHING_TO_LOAD,

			/**
			 * {@link BleDevice#loadHistoricalData()} (or overloads) was called and the operation was successfully started -
			 * expect another {@link HistoricalDataLoadEvent} with {@link HistoricalDataLoadEvent#status()} being {@link #LOADED} shortly.
			 */
			STARTED_LOADING,

			/**
			 * Same idea as {@link #STARTED_LOADING}, not an error status, but letting you know that the load was already in progress
			 * when {@link BleDevice#loadHistoricalData()} (or overloads) was called a second time. This doesn't
			 * affect the actual loading process at all, and {@link #LOADED} will eventually be returned for both callbacks.
			 */
			ALREADY_LOADING;

			/**
			 * Returns true if <code>this==</code> {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event struct passed to {@link HistoricalDataLoadListener#onEvent(HistoricalDataLoadEvent)} that provides
		 * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
		 * (or overloads).
		 */
		@com.idevicesinc.sweetblue.annotations.Immutable
		public static class HistoricalDataLoadEvent
		{
			/**
			 * The {@link BleDevice} that the data is being loaded for.
			 */
			public BleDevice device() {  return m_device; }
			private final BleDevice m_device;

			/**
			 * The {@link UUID} that the data is being loaded for.
			 */
			public UUID uuid() {  return m_uuid;  }
			private final UUID m_uuid;

			/**
			 * The resulting time range spanning all of the data loaded to memory, or {@link EpochTimeRange#NULL} if not applicable.
			 */
			public EpochTimeRange range() {  return m_range; }
			private final EpochTimeRange m_range;

			/**
			 * The general status of the load operation.
			 */
			public Status status() {  return m_status; }
			private final Status m_status;

			HistoricalDataLoadEvent(final BleDevice device, final UUID uuid, final EpochTimeRange range, final Status status)
			{
				m_device = device;
				m_uuid = uuid;
				m_range = range;
				m_status = status;
			}

			/**
			 * Returns <code>true</code> if {@link #status()} is either {@link HistoricalDataLoadListener.Status#LOADED} or
			 *  {@link HistoricalDataLoadListener.Status#ALREADY_LOADED}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.LOADED || status() == Status.ALREADY_LOADED;
			}

			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"device", device().getName_debug(),
					"uuid", device().getManager().getLogger().uuidName(uuid()),
					"status", status()
				);
			}
		}

		/**
		 * Called when the historical data for a given characteristic {@link UUID} is done loading from disk.
		 */
		void onEvent(final HistoricalDataLoadEvent e);
	}

	/**
	 * A callback that is used by {@link BleDevice#select()} to listen for when a database query is done processing.
	 */
	@com.idevicesinc.sweetblue.annotations.Alpha
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface HistoricalDataQueryListener
	{
		/**
		 * Enumerates the status codes for operations kicked off from {@link BleDevice#select()}.
		 */
		public static enum Status implements UsesCustomNull
		{
			/**
			 * Fulfills soft contract of {@link UsesCustomNull}.
			 */
			NULL,

			/**
			 * Query completed successfully - {@link HistoricalDataQueryEvent#cursor()} may be empty but there were no exceptions or anything.
			 */
			SUCCESS,

			/**
			 * There is no backing table for the given {@link UUID}.
			 */
			NO_TABLE,

			/**
			 * General failure - this feature is still in {@link com.idevicesinc.sweetblue.annotations.Alpha} so expect more detailed error statuses in the future.
			 */
			ERROR;

			/**
			 * Returns true if <code>this==</code> {@link #NULL}.
			 */
			@Override public boolean isNull()
			{
				return this == NULL;
			}
		}

		/**
		 * Event struct passed to {@link HistoricalDataQueryListener#onEvent(HistoricalDataQueryEvent)} that provides
		 * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
		 * (or overloads).
		 */
		@com.idevicesinc.sweetblue.annotations.Immutable
		public static class HistoricalDataQueryEvent
		{
			/**
			 * The {@link BleDevice} that the data is being queried for.
			 */
			public BleDevice device() {  return m_device; }
			private final BleDevice m_device;

			/**
			 * The {@link UUID} that the data is being queried for.
			 */
			public UUID uuid() {  return m_uuid;  }
			private final UUID m_uuid;

			/**
			 * The general status of the query operation.
			 */
			public Status status() {  return m_status; }
			private final Status m_status;

			/**
			 * The resulting {@link Cursor} from the database query. This will never be null, just an empty cursor if anything goes wrong.
			 */
			public @Nullable(Prevalence.NEVER) Cursor cursor() {  return m_cursor; }
			private final Cursor m_cursor;

			/**
			 * The resulting {@link Cursor} from the database query. This will never be null, just an empty cursor if anything goes wrong.
			 */
			public @Nullable(Prevalence.NEVER) String rawQuery() {  return m_rawQuery; }
			private final String m_rawQuery;

			public HistoricalDataQueryEvent(final BleDevice device, final UUID uuid, final Cursor cursor, final Status status, final String rawQuery)
			{
				m_device = device;
				m_uuid = uuid;
				m_cursor = cursor;
				m_status = status;
				m_rawQuery = rawQuery;
			}

			/**
			 * Returns <code>true</code> if {@link #status()} is {@link HistoricalDataQueryListener.Status#SUCCESS}.
			 */
			public boolean wasSuccess()
			{
				return status() == Status.SUCCESS;
			}

			@Override public String toString()
			{
				return Utils.toString
				(
					this.getClass(),
					"device",			device().getName_debug(),
					"uuid",				device().getManager().getLogger().uuidName(uuid()),
					"status",			status()
				);
			}
		}

		/**
		 * Called when the historical data for a given characteristic {@link UUID} is done querying.
		 */
		void onEvent(final HistoricalDataQueryEvent e);
	}

	static ConnectionFailListener DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultConnectionFailListener();

	//--- DRK > Some reusable empty-array-type instances so we don't have to create them from scratch over and over on demand.
	private static final UUID[] EMPTY_UUID_ARRAY = new UUID[0];
	private static final ArrayList<UUID> EMPTY_LIST = new ArrayList<UUID>();

	static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	static final FutureData EMPTY_FUTURE_DATA = new PresentData(EMPTY_BYTE_ARRAY);

	final Object m_threadLock = new Object();

	final P_NativeDeviceWrapper m_nativeWrapper;

	private double m_timeSinceLastDiscovery;
	private EpochTime m_lastDiscoveryTime = EpochTime.NULL;

	final P_BleDevice_Listeners m_listeners;
	private final P_ServiceManager m_serviceMngr;
	private final P_DeviceStateTracker m_stateTracker;
	private final P_DeviceStateTracker m_stateTracker_shortTermReconnect;
	private final P_PollManager m_pollMngr;

	private final BleManager m_mngr;
	private final P_Logger m_logger;
	private final P_TaskQueue m_queue;
	final P_TransactionManager m_txnMngr;
	private final P_ReconnectManager m_reconnectMngr_longTerm;
	private final P_ReconnectManager m_reconnectMngr_shortTerm;
	private final P_ConnectionFailManager m_connectionFailMngr;
	private final P_RssiPollManager m_rssiPollMngr;
	private final P_RssiPollManager m_rssiPollMngr_auto;
	private final P_Task_Disconnect m_dummyDisconnectTask;
	private final P_HistoricalDataManager m_historicalDataMngr;
	final P_BondManager m_bondMngr;

	private ReadWriteListener m_defaultReadWriteListener = null;

	private TimeEstimator m_writeTimeEstimator;
	private TimeEstimator m_readTimeEstimator;

	private final PA_Task.I_StateListener m_taskStateListener;

	private final BleDeviceOrigin m_origin;
	private BleDeviceOrigin m_origin_latest;

	private int m_rssi = 0;
	private Integer m_knownTxPower = null;
	private List<UUID> m_advertisedServices = EMPTY_LIST;
	private byte[] m_scanRecord = EMPTY_BYTE_ARRAY;

	private boolean m_useAutoConnect = false;
	private boolean m_alwaysUseAutoConnect = false;
	private Boolean m_lastConnectOrDisconnectWasUserExplicit = null;
	private boolean m_lastDisconnectWasBecauseOfBleTurnOff = false;
	private boolean m_underwentPossibleImplicitBondingAttempt = false;

	private BleDeviceConfig m_config = null;

	private BondListener.BondEvent m_nullBondEvent = null;
	private ReadWriteListener.ReadWriteEvent m_nullReadWriteEvent = null;
	private ConnectionFailListener.ConnectionFailEvent m_nullConnectionFailEvent = null;

	private final boolean m_isNull;

	/**
	 * Field for app to associate any data it wants with instances of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 *
	 * @see BleManager#appData
	 */
	public Object appData;

	BleDevice(BleManager mngr, BluetoothDevice device_native, String name_normalized, String name_native, BleDeviceOrigin origin, BleDeviceConfig config_nullable, boolean isNull)
	{
		m_mngr = mngr;
		m_origin = origin;
		m_origin_latest = m_origin;
		m_isNull = isNull;

		if (isNull)
		{
			m_rssiPollMngr = null;
			m_rssiPollMngr_auto = null;
			// setConfig(config_nullable);
			m_nativeWrapper = new P_NativeDeviceWrapper(this, device_native, name_normalized, name_native);
			m_queue = null;
			m_listeners = null;
			m_logger = null;
			m_serviceMngr = new P_ServiceManager(this);
			m_stateTracker = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/false);
			m_stateTracker_shortTermReconnect = null;
			m_bondMngr = new P_BondManager(this);
			stateTracker().set(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, true);
			m_pollMngr = new P_PollManager(this);
			m_txnMngr = new P_TransactionManager(this);
			m_taskStateListener = null;
			m_reconnectMngr_longTerm = null;
			m_reconnectMngr_shortTerm = null;
			m_connectionFailMngr = new P_ConnectionFailManager(this);
			m_dummyDisconnectTask = null;
			m_historicalDataMngr = null;
		}
		else
		{
			m_rssiPollMngr = new P_RssiPollManager(this);
			m_rssiPollMngr_auto = new P_RssiPollManager(this);
			setConfig(config_nullable);
			m_nativeWrapper = new P_NativeDeviceWrapper(this, device_native, name_normalized, name_native);
			m_queue = m_mngr != null ? getManager().getTaskQueue() : null;
			m_listeners = new P_BleDevice_Listeners(this);
			m_logger = m_mngr != null ? getManager().getLogger() : null;
			m_serviceMngr = new P_ServiceManager(this);
			m_stateTracker = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/false);
			m_stateTracker_shortTermReconnect = new P_DeviceStateTracker(this, /*forShortTermReconnect=*/true);
			m_bondMngr = new P_BondManager(this);
			stateTracker().set(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.UNDISCOVERED, true, BleDeviceState.DISCONNECTED, true, m_bondMngr.getNativeBondingStateOverrides());
			m_pollMngr = new P_PollManager(this);
			m_txnMngr = new P_TransactionManager(this);
			m_taskStateListener = m_listeners.m_taskStateListener;
			m_reconnectMngr_longTerm = new P_ReconnectManager(this, /*isShortTerm=*/false);
			m_reconnectMngr_shortTerm = new P_ReconnectManager(this, /*isShortTerm=*/true);
			m_connectionFailMngr = new P_ConnectionFailManager(this);
			m_dummyDisconnectTask = new P_Task_Disconnect(this, null, /*explicit=*/false, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING, /*cancellable=*/true);
			m_historicalDataMngr = new P_HistoricalDataManager(this);
		}
	}

	void notifyOfPossibleImplicitBondingAttempt()
	{
		m_underwentPossibleImplicitBondingAttempt = true;
	}

	P_DeviceStateTracker stateTracker_main()
	{
		return m_stateTracker;
	}

	void stateTracker_updateBoth(E_Intent intent, int status, Object ... statesAndValues)
	{
		m_stateTracker_shortTermReconnect.update(intent, status, statesAndValues);
		stateTracker_main().update(intent, status, statesAndValues);
	}

	P_DeviceStateTracker stateTracker()
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

	P_ReconnectManager reconnectMngr()
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
	public void setConfig(@Nullable(Prevalence.RARE) BleDeviceConfig config_nullable)
	{
		if (isNull())  return;

		m_config = config_nullable == null ? null : config_nullable.clone();

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
		Integer nForAverageRunningWriteTime = BleDeviceConfig.integer(conf_device().nForAverageRunningWriteTime, conf_mngr().nForAverageRunningWriteTime);
		m_writeTimeEstimator = nForAverageRunningWriteTime == null ? null : new TimeEstimator(nForAverageRunningWriteTime);

		Integer nForAverageRunningReadTime = BleDeviceConfig.integer(conf_device().nForAverageRunningReadTime, conf_mngr().nForAverageRunningReadTime);
		m_readTimeEstimator = nForAverageRunningReadTime == null ? null : new TimeEstimator(nForAverageRunningReadTime);
	}

	BleDeviceConfig conf_device()
	{
		return m_config != null ? m_config : conf_mngr();
	}

	BleManagerConfig conf_mngr()
	{
		if (getManager() != null)
		{
			return getManager().m_config;
		}
		else
		{
			return BleManagerConfig.NULL;
		}
	}

	/**
	 * How the device was originally created, either from scanning or explicit creation.
	 * <br><br>
	 * NOTE: That devices for which this returns {@link BleDeviceOrigin#EXPLICIT} may still be
	 * {@link BleManager.DiscoveryListener.LifeCycle#REDISCOVERED} through {@link BleManager#startScan()}.
	 */
	public BleDeviceOrigin getOrigin()
	{
		return m_origin;
	}

	/**
	 * Returns the last time the device was {@link BleManager.DiscoveryListener.LifeCycle#DISCOVERED}
	 * or {@link BleManager.DiscoveryListener.LifeCycle#REDISCOVERED}. If {@link #getOrigin()} returns
	 * {@link BleDeviceOrigin#EXPLICIT} then this will return {@link EpochTime#NULL} unless or until
	 * the device is {@link BleManager.DiscoveryListener.LifeCycle#REDISCOVERED}.
	 */
	public EpochTime getLastDiscoveryTime()
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
	 * (c) app user cleared app data between app sessions, (d) etc., etc.
	 * <br><br>
	 * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}, then from a user experience perspective, the user may not have wanted
	 * the disconnect to happen, and thus *probably* would want to be automatically connected again as soon as the device is discovered.
	 * <br><br>
	 * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}, then the last reason the device was {@link BleDeviceState#DISCONNECTED} was because
	 * {@link BleDevice#disconnect()} was called, which most-likely means the user doesn't want to automatically connect to this device again.
	 * <br><br>
	 * See further explanation at {@link BleDeviceConfig#manageLastDisconnectOnDisk}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public State.ChangeIntent getLastDisconnectIntent()
	{
		if( isNull() )  return State.ChangeIntent.NULL;

		boolean hitDisk = BleDeviceConfig.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
		State.ChangeIntent lastDisconnect = getManager().m_diskOptionsMngr.loadLastDisconnect(getMacAddress(), hitDisk);

		return lastDisconnect;
	}

	/**
	 * Set a listener here to be notified whenever this device's state changes.
	 */
	public void setListener_State(@Nullable(Prevalence.NORMAL) StateListener listener_nullable)
	{
		if( isNull() )  return;

		stateTracker_main().setListener(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever a connection fails and to
	 * have control over retry behavior.
	 */
	public void setListener_ConnectionFail(@Nullable(Prevalence.NORMAL) ConnectionFailListener listener_nullable)
	{
		if( isNull() )  return;

		m_connectionFailMngr.setListener(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever a bond attempt succeeds. This
	 * will catch attempts to bond both through {@link #bond()} and when bonding
	 * through the operating system settings or from other apps.
	 */
	public void setListener_Bond(@Nullable(Prevalence.NORMAL) BondListener listener_nullable)
	{
		if( isNull() )  return;

		m_bondMngr.setListener(listener_nullable);
	}

	/**
	 * Sets a default backup {@link ReadWriteListener} that will be called for all calls to {@link #read(UUID, ReadWriteListener)},
	 * {@link #write(UUID, byte[], ReadWriteListener)}, {@link #enableNotify(UUID, ReadWriteListener)}, etc.
	 * <br><br>
	 * NOTE: This will be called after the {@link ReadWriteListener} provided directly through the method params.
	 */
	public void setListener_ReadWrite(@Nullable(Prevalence.NORMAL) ReadWriteListener listener_nullable)
	{
		if( isNull() )  return;

		if (listener_nullable != null)
		{
			m_defaultReadWriteListener = new P_WrappingReadWriteListener(listener_nullable, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread);
		}
		else
		{
			m_defaultReadWriteListener = null;
		}
	}

	/**
	 * Sets a default backup {@link com.idevicesinc.sweetblue.BleDevice.HistoricalDataLoadListener} that will be invoked
	 * for all historical data loads to memory for all uuids.
	 */
	public void setListener_HistoricalDataLoad(@Nullable(Prevalence.NORMAL) final HistoricalDataLoadListener listener_nullable)
	{
		if( isNull() )  return;

		m_historicalDataMngr.setListener(listener_nullable);
	}

	/**
	 * Returns the connection failure retry count during a retry loop. Basic example use case is to provide a callback to
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)} and update your application's UI with this method's return value downstream of your
	 * {@link ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)} override.
	 */
	public int getConnectionRetryCount()
	{
		if( isNull() )  return 0;

		return m_connectionFailMngr.getRetryCount();
	}

	/**
	 * Returns the bitwise state mask representation of {@link BleDeviceState} for this device.
	 *
	 * @see BleDeviceState
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public int getStateMask()
	{
		return stateTracker_main().getState();
	}

	/**
	 * Returns the actual native state mask representation of the {@link BleDeviceState} for this device.
	 * The main purpose of this is to reflect what's going on under the hood while {@link BleDevice#is(BleDeviceState)}
	 * with {@link BleDeviceState#RECONNECTING_SHORT_TERM} is <code>true</code>.
	 */
	@Advanced
	public int getNativeStateMask()
	{
		return stateTracker().getState();
	}

	/**
	 * See similar explanation for {@link #getAverageWriteTime()}.
	 *
	 * @see #getAverageWriteTime()
	 * @see BleManagerConfig#nForAverageRunningReadTime
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public Interval getAverageReadTime()
	{
		return m_readTimeEstimator != null ? Interval.secs(m_readTimeEstimator.getRunningAverage()) : Interval.ZERO;
	}

	/**
	 * Returns the average round trip time in seconds for all write operations started with {@link #write(UUID, byte[])} or
	 * {@link #write(UUID, byte[], ReadWriteListener)}. This is a running average with N being defined by
	 * {@link BleManagerConfig#nForAverageRunningWriteTime}. This may be useful for estimating how long a series of
	 * reads and/or writes will take. For example for displaying the estimated time remaining for a firmware update.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public Interval getAverageWriteTime()
	{
		return m_writeTimeEstimator != null ? Interval.secs(m_writeTimeEstimator.getRunningAverage()) : Interval.ZERO;
	}

	/**
	 * Returns the raw RSSI retrieved from when the device was discovered,
	 * rediscovered, or when you call {@link #readRssi()} or {@link #startRssiPoll(Interval)}.
	 *
	 * @see #getDistance()
	 */
	public int getRssi()
	{
		return m_rssi;
	}

	/**
	 * Raw RSSI from {@link #getRssi()} is a little cryptic, so this gives you a friendly 0%-100% value for signal strength.
	 */
	public Percent getRssiPercent()
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
	public Distance getDistance()
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
	@com.idevicesinc.sweetblue.annotations.Advanced
	public int getTxPower()
	{
		if (isNull())
		{
			return 0;
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
				final int toReturn = defaultTxPower == null ? BleDeviceConfig.DEFAULT_TX_POWER : defaultTxPower;

				return toReturn;
			}
		}
	}

	/**
	 * Returns the scan record from when we discovered the device. May be empty but never <code>null</code>.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) byte[] getScanRecord()
	{
		return m_scanRecord;
	}

	/**
	 * Returns the advertised services, if any, parsed from {@link #getScanRecord()}. May be empty but never <code>null</code>.
	 */
	public @Nullable(Prevalence.NEVER) UUID[] getAdvertisedServices()
	{
		UUID[] toReturn = m_advertisedServices.size() > 0 ? new UUID[m_advertisedServices.size()] : EMPTY_UUID_ARRAY;
		return m_advertisedServices.toArray(toReturn);
	}

	/**
	 * Returns whether the device is in any of the provided states.
	 *
	 * @see #is(BleDeviceState)
	 */
	public boolean isAny(BleDeviceState... states)
	{
		for (int i = 0; i < states.length; i++)
		{
			if (is(states[i]))  return true;
		}

		return false;
	}

	/**
	 * Returns whether the device is in all of the provided states.
	 *
	 * @see #isAny(BleDeviceState...)
	 */
	public boolean isAll(BleDeviceState... states)
	{
		for (int i = 0; i < states.length; i++)
		{
			if( !is(states[i]) )  return false;
		}

		return true;
	}

	/**
	 * Convenience method to tell you whether a call to {@link #connect()} (or overloads) has a chance of succeeding.
	 * For example if the device is {@link BleDeviceState#CONNECTING_OVERALL} or {@link BleDeviceState#INITIALIZED}
	 * then this will return <code>false</code>.
	 */
	public boolean isConnectable()
	{
		if( isAny(BleDeviceState.INITIALIZED, BleDeviceState.CONNECTING_OVERALL) )
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
	public boolean is(final BleDeviceState state)
	{
		return state.overlaps(getStateMask());
	}

	/**
	 * @deprecated Use {@link #isAny(int)} instead.
	 */
	public boolean is(final int mask_BleDeviceState)
	{
		return (getStateMask() & mask_BleDeviceState) != 0x0;
	}

	/**
	 * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
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

	/**
	 * Similar to {@link #is(BleDeviceState)} and {@link #isAny(BleDeviceState...)} but allows you to give a simple query
	 * made up of {@link BleDeviceState} and {@link Boolean} pairs. So an example would be
	 * <code>myDevice.is({@link BleDeviceState#CONNECTING}, true, {@link BleDeviceState#RECONNECTING_LONG_TERM}, false)</code>.
	 */
	public boolean is(Object... query)
	{
		return is_query(getStateMask(), query);
	}

	boolean isAny_internal(BleDeviceState... states)
	{
		for (int i = 0; i < states.length; i++)
		{
			if (is_internal(states[i]))
				return true;
		}

		return false;
	}

	boolean is_internal(BleDeviceState state)
	{
		return state.overlaps(stateTracker().getState());
	}

//	boolean is_internal(Object... query)
//	{
//		return is_query(/* internal= */true, query);
//	}

	static boolean is_query(final int stateMask, Object... query)
	{
		if (query == null || query.length == 0)  return false;

		final boolean internal = false;

		for (int i = 0; i < query.length; i += 2)
		{
			final Object first = query[i];
			final Object second = i + 1 < query.length ? query[i + 1] : null;

			if (first == null || second == null)  return false;

			if (!(first instanceof BleDeviceState) || !(second instanceof Boolean))
			{
				return false;
			}

			final BleDeviceState state = (BleDeviceState) first;
			final Boolean value = (Boolean) second;
			final boolean overlap = state.overlaps(stateMask);

			if (value && !overlap)					return false;
			else if (!value && overlap)				return false;
		}

		return true;
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
	public Interval getTimeInState(BleDeviceState state)
	{
		return Interval.millis(stateTracker_main().getTimeInState(state.ordinal()));
	}

	/**
	 * Same as {@link #setName(String, UUID, BleDevice.ReadWriteListener)} but will not attempt to propagate the
	 * name change to the remote device. Only {@link #getName_override()} will be affected by this.
	 */
	public void setName(final String name)
	{
		setName(name, null, null);
	}

	/**
	 * Same as {@link #setName(String, UUID, BleDevice.ReadWriteListener)} but you can use this
	 * if you don't care much whether the device name change actually successfully reaches
	 * the remote device. The write will be attempted regardless.
	 */
	public @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setName(final String name, final UUID characteristicUuid)
	{
		return setName(name, characteristicUuid);
	}

	/**
	 * Sets the local name of the device and also attempts a {@link #write(java.util.UUID, byte[], BleDevice.ReadWriteListener)}
	 * using the given {@link UUID}. Further calls to {@link #getName_override()} will immediately reflect the name given here.
	 * Further calls to {@link #getName_native()}, {@link #getName_debug()} and {@link #getName_normalized()} will only reflect
	 * the name given here if the write is successful. It is somewhat assumed that doing this write will cause the remote device
	 * to use the new name given here for its device information service {@link Uuids#DEVICE_NAME}.
	 * If {@link BleDeviceConfig#saveNameChangesToDisk} is <code>true</code> then this name
	 * will always be returned for {@link #getName_override()}, even if you kill/restart the app.
	 */
	public @Nullable(Prevalence.NEVER) ReadWriteListener.ReadWriteEvent setName(final String name, final UUID characteristicUuid, final ReadWriteListener listener)
	{
		if( !isNull() )
		{
			m_nativeWrapper.setName_override(name);

			final boolean saveToDisk = BleDeviceConfig.bool(conf_device().saveNameChangesToDisk, conf_mngr().saveNameChangesToDisk);

			getManager().m_diskOptionsMngr.saveName(getMacAddress(), name, saveToDisk);
		}

		if( characteristicUuid != null )
		{
			final ReadWriteListener listener_wrapper = new ReadWriteListener()
			{
				@Override public void onEvent(ReadWriteEvent e)
				{
					if( e.wasSuccess() )
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
	public void clearName()
	{
		m_nativeWrapper.clearName_override();
		getManager().m_diskOptionsMngr.clearName(getMacAddress());
	}

	/**
	 * By default returns the same value as {@link #getName_native()}.
	 * If you call {@link #setName(String)} (or overloads)
	 * then calling this will return the same string provided in that setter.
	 */
	public @Nullable(Prevalence.NEVER) String getName_override()
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
	public @Nullable(Prevalence.NEVER) String getName_native()
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
	public @Nullable(Prevalence.NEVER) String getName_normalized()
	{
		return m_nativeWrapper.getNormalizedName();
	}

	/**
	 * Returns a name useful for logging and debugging. As of this writing it is
	 * {@link #getName_normalized()} plus the last four digits of the device's
	 * MAC address from {@link #getMacAddress()}. {@link BleDevice#toString()}
	 * uses this.
	 */
	public @Nullable(Prevalence.NEVER) String getName_debug()
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
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.RARE) BluetoothDevice getNative()
	{
		return m_nativeWrapper.getDevice();
	}

	/**
	 * Returns the native descriptor for the given UUID in case you need lower-level access. You should only call this after
	 * {@link BleDeviceState#DISCOVERING_SERVICES} has completed.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NORMAL) BluetoothGattDescriptor getNativeDescriptor(final UUID charUuid, final UUID descUUID)
	{
		final UUID serviceUuid = null;

		final BluetoothGattCharacteristic char_native = getNativeCharacteristic(serviceUuid, charUuid);

		if( char_native == null )  return null;

		final BluetoothGattDescriptor desc_native = char_native.getDescriptor(descUUID);

		return desc_native;
	}

	/**
	 * Returns the native characteristic for the given UUID in case you need lower-level access. You should only call this after
	 * {@link BleDeviceState#DISCOVERING_SERVICES} has completed.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID characteristicUuid)
	{
		final UUID serviceUuid = null;

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		if (characteristic == null)  return null;

		return characteristic.getGuaranteedNative();
	}

	/**
	 * Overload of {@link #getNativeCharacteristic(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NORMAL) BluetoothGattCharacteristic getNativeCharacteristic(final UUID serviceUuid, final UUID characteristicUuid)
	{
		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		if (characteristic == null)  return null;

		return characteristic.getGuaranteedNative();
	}

	/**
	 * Returns the native service for the given UUID in case you need lower-level access. You should only call this after
	 * {@link BleDeviceState#DISCOVERING_SERVICES} has completed.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NORMAL) BluetoothGattService getNativeService(final UUID uuid)
	{
		final P_Service service = m_serviceMngr.get(uuid);

		if (service == null)  return null;

		return service.getNative();
	}

	/**
	 * Returns all {@link BluetoothGattService} instances once {@link BleDevice#is(BleDeviceState)} with
	 * {@link BleDeviceState#SERVICES_DISCOVERED} returns <code>true</code>.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) Iterator<BluetoothGattService> getNativeServices()
	{
		return m_serviceMngr.getNativeServices();
	}

	/**
	 * Convenience overload of {@link #getNativeServices()} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) List<BluetoothGattService> getNativeServices_List()
	{
		return m_serviceMngr.getNativeServices_List();
	}

	/**
	 * Returns all {@link BluetoothGattService} instances once {@link BleDevice#is(BleDeviceState)} with
	 * {@link BleDeviceState#SERVICES_DISCOVERED} returns <code>true</code>.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics()
	{
		return m_serviceMngr.getNativeCharacteristics();
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics()} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List()
	{
		return m_serviceMngr.getNativeCharacteristics_List();
	}

	/**
	 * Same as {@link #getNativeCharacteristics()} but you can filter on the service {@link UUID}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Prevalence.NEVER) Iterator<BluetoothGattCharacteristic> getNativeCharacteristics(UUID service)
	{
		return m_serviceMngr.getNativeCharacteristics(service);
	}

	/**
	 * Convenience overload of {@link #getNativeCharacteristics(UUID)} that returns a {@link List}.
	 * <br><br>
	 * WARNING: Please see the WARNING for {@link #getNative()}.
	 */
	public @Nullable(Prevalence.NEVER) List<BluetoothGattCharacteristic> getNativeCharacteristics_List(UUID service)
	{
		return m_serviceMngr.getNativeCharacteristics_List(service);
	}

	/**
	 * See pertinent warning for {@link #getNative()}. Generally speaking, this
	 * will return <code>null</code> if the BleDevice is {@link BleDeviceState#DISCONNECTED}.
	 * <br><br>
	 * NOTE: If you are forced to use this please contact library developers to
	 * discuss possible feature addition or report bugs.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NORMAL) BluetoothGatt getNativeGatt()
	{
		return m_nativeWrapper.getGatt();
	}

	/**
	 * Returns this devices's manager.
	 */
	public BleManager getManager()
	{
		if (isNull())
		{
			return BleManager.s_instance;
		}
		else
		{
			return m_mngr;
		}
	}

	/**
	 * Returns the MAC address of this device, as retrieved from the native stack.
	 */
	public @Nullable(Prevalence.NEVER) String getMacAddress()
	{
		return m_nativeWrapper.getAddress();
	}

	/**
	 * Same as {@link #bond()} but you can pass a listener to be notified of the details behind success or failure.
	 *
	 * @return (same as {@link #bond()}).
	 */
	public @Nullable(Prevalence.NEVER) BondListener.BondEvent bond(BondListener listener)
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

		bond_justAddTheTask(E_TransactionLockBehavior.PASSES);

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
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #unbond()
	 */
	public @Nullable(Prevalence.NEVER) BondListener.BondEvent bond()
	{
		return this.bond(null);
	}

	/**
	 * Opposite of {@link #bond()}.
	 *
	 * @return <code>true</code> if successfully {@link BleDeviceState#UNBONDED}, <code>false</code> if already {@link BleDeviceState#UNBONDED}.
	 *
	 * @see #bond()
	 */
	public boolean unbond()
	{
		final boolean alreadyUnbonded = is(UNBONDED);

		unbond_internal(null, BondListener.Status.CANCELLED_FROM_UNBOND);

		return !alreadyUnbonded;
	}

	/**
	 * Starts a connection process, or does nothing if already {@link BleDeviceState#CONNECTED} or {@link BleDeviceState#CONNECTING}.
	 * Use {@link #setListener_ConnectionFail(ConnectionFailListener)} and {@link #setListener_State(StateListener)} to receive callbacks for
	 * progress and errors.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect()
	{
		return connect((StateListener) null);
	}

	/**
	 * Same as {@link #connect()} but calls {@link #setListener_State(StateListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(StateListener stateListener)
	{
		return connect(stateListener, null);
	}

	/**
	 * Same as {@link #connect()} but calls {@link #setListener_ConnectionFail(ConnectionFailListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(ConnectionFailListener failListener)
	{
		return connect(null, failListener);
	}

	/**
	 * Same as {@link #connect()} but calls {@link #setListener_State(StateListener)} and
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(StateListener stateListener, ConnectionFailListener failListener)
	{
		return connect(null, null, stateListener, failListener);
	}

	/**
	 * Same as {@link #connect(com.idevicesinc.sweetblue.BleDevice.StateListener, com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener)}
	 * with reversed arguments.
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(ConnectionFailListener failListener, StateListener stateListener)
	{
		return connect(stateListener, failListener);
	}

	/**
	 * Same as {@link #connect()} but provides a hook for the app to do some kind of authentication handshake if it wishes. This is popular with
	 * commercial BLE devices where you don't want hobbyists or competitors using your devices for nefarious purposes - like releasing a better application
	 * for your device than you ;-). Usually the characteristics read/written inside this transaction are encrypted and so one way or another will require
	 * the device to become {@link BleDeviceState#BONDED}. This should happen automatically for you, i.e you shouldn't need to call {@link #bond()} yourself.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #connect()
	 * @see BleDeviceState#AUTHENTICATING
	 * @see BleDeviceState#AUTHENTICATED
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn)
	{
		return connect(authenticationTxn, (StateListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Auth)} but calls {@link #setListener_State(StateListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn, StateListener stateListener)
	{
		return connect(authenticationTxn, stateListener, (ConnectionFailListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Auth)} but calls
	 * {@link #setListener_State(StateListener)} and
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn, StateListener stateListener, ConnectionFailListener failListener)
	{
		return connect(authenticationTxn, null, stateListener, failListener);
	}

	/**
	 * Same as {@link #connect()} but provides a hook for the app to do some kind of initialization before it's considered fully
	 * {@link BleDeviceState#INITIALIZED}. For example if you had a BLE-enabled thermometer you could use this transaction to attempt an initial
	 * temperature read before updating your UI to indicate "full" connection success, even though BLE connection itself already succeeded.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #connect()
	 * @see BleDeviceState#INITIALIZING
	 * @see BleDeviceState#INITIALIZED
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Init initTxn)
	{
		return connect(initTxn, (StateListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Init)} but calls {@link #setListener_State(StateListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Init initTxn, StateListener stateListener)
	{
		return connect(initTxn, stateListener, (ConnectionFailListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Init)} but calls {@link #setListener_State(StateListener)} and
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Init initTxn, StateListener stateListener, ConnectionFailListener failListener)
	{
		return connect(null, initTxn, stateListener, failListener);
	}

	/**
	 * Combination of {@link #connect(BleTransaction.Auth)} and {@link #connect(BleTransaction.Init)}. See those two methods for explanation.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #connect()
	 * @see #connect(BleTransaction.Auth)
	 * @see #connect(BleTransaction.Init)
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn)
	{
		return connect(authenticationTxn, initTxn, (StateListener) null, (ConnectionFailListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Auth, BleTransaction.Init)} but calls {@link #setListener_State(StateListener)} for you.
	 *
	 * @return (same as {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, StateListener stateListener)
	{
		return connect(authenticationTxn, initTxn, stateListener, (ConnectionFailListener) null);
	}

	/**
	 * Same as {@link #connect(BleTransaction.Auth, BleTransaction.Init)} but calls {@link #setListener_State(StateListener)} and
	 * {@link #setListener_ConnectionFail(ConnectionFailListener)} for you.
	 *
	 * @return	If the attempt could not even "leave the gate" for some resaon, a valid {@link ConnectionFailEvent} is returned telling you why. Otherwise
	 * 			this method will still return a non-null instance but {@link ConnectionFailEvent#isNull()} will be <code>true</code>.
	 * 			<br><br>
	 * 			NOTE: your {@link ConnectionFailListener} will still be called even if this method early-outs.
	 * 			<br><br>
	 * 			TIP:	You can use the return value as an optimization. Many apps will call this method (or its overloads) and throw up a spinner until receiving a
	 * 					callback to {@link ConnectionFailListener}. However if {@link ConnectionFailEvent#isNull()} for the return value is <code>false</code>, meaning
	 * 					the connection attempt couldn't even start for some reason, then you don't have to throw up the spinner in the first place.
	 */
	public @Nullable(Prevalence.NEVER) ConnectionFailListener.ConnectionFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, StateListener stateListener, ConnectionFailListener failListener)
	{
		if (stateListener != null)
		{
			setListener_State(stateListener);
		}

		if (failListener != null)
		{
			setListener_ConnectionFail(failListener);
		}

		m_connectionFailMngr.onExplicitConnectionStarted();

		final ConnectionFailListener.ConnectionFailEvent info_earlyOut = connect_earlyOut();

		if (info_earlyOut != null )  return info_earlyOut;

		m_lastConnectOrDisconnectWasUserExplicit = true;

		if (isAny(CONNECTED, CONNECTING, CONNECTING_OVERALL))
		{
			//--- DRK > Making a judgement call that an explicit connect call here means we bail out of the long term reconnect state.
			stateTracker_main().remove(RECONNECTING_LONG_TERM, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			final ConnectionFailListener.ConnectionFailEvent info_alreadyConnected = ConnectionFailListener.ConnectionFailEvent.DUMMY(this, Status.ALREADY_CONNECTING_OR_CONNECTED);

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
	 *
	 * @see ConnectionFailListener.Status#EXPLICIT_DISCONNECT
	 */
	public boolean disconnect()
	{
		return disconnect_private(Status.EXPLICIT_DISCONNECT);
	}

	/**
	 * Same as {@link #disconnect()} but this call roughly simulates the disconnect as if it's because of the remote device going down, going out of range, etc.
	 * For example {@link #getLastDisconnectIntent()} will be {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL} instead of
	 * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}.
	 * <br><br>
	 * If the device is currently {@link BleDeviceState#CONNECTING_OVERALL} then your
	 * {@link com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener#onEvent(com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent)}
	 * implementation will be called with {@link ConnectionFailListener.Status#ROGUE_DISCONNECT}.
	 * <br><br>
	 * NOTE: One major difference between this and an actual remote disconnect is that this will not cause the device to enter
	 * {@link BleDeviceState#RECONNECTING_SHORT_TERM} or {@link BleDeviceState#RECONNECTING_LONG_TERM}.
	 */
	public boolean disconnect_remote()
	{
		return disconnect_private(Status.ROGUE_DISCONNECT);
	}

	private boolean disconnect_private(final Status status)
	{
		if (isNull())  return false;

		final boolean alreadyDisconnected = is(DISCONNECTED);
		final boolean reconnecting_longTerm = is(RECONNECTING_LONG_TERM);

		disconnectWithReason(/*priority=*/null, status, Timing.NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, NULL_READWRITE_EVENT());

		return !alreadyDisconnected || reconnecting_longTerm;
	}

	/**
	 * Convenience method that calls {@link BleManager#undiscover(BleDevice)}.
	 *
	 * @return <code>true</code> if the device was successfully {@link BleDeviceState#UNDISCOVERED}, <code>false</code> if BleDevice isn't known to the {@link BleManager}.
	 *
	 * @see BleManager#undiscover(BleDevice)
	 */
	public boolean undiscover()
	{
		if (isNull())  return false;

		return getManager().undiscover(this);
	}

	/**
	 * Convenience forwarding of {@link BleManager#clearSharedPreferences(String)}.
	 *
	 * @see BleManager#clearSharedPreferences(BleDevice)
	 */
	public void clearSharedPreferences()
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
	public boolean equals(@Nullable(Prevalence.NORMAL) final BleDevice device_nullable)
	{
		if (device_nullable == null)												return false;
		if (device_nullable == this)												return true;
		if (device_nullable.getNative() == null || this.getNative() == null)		return false;
		if( this.isNull() && device_nullable.isNull() )								return true;

		return device_nullable.getNative().equals(this.getNative());
	}

	/**
	 * Returns {@link #equals(BleDevice)} if object is an instance of {@link BleDevice}. Otherwise calls super.
	 *
	 * @see BleDevice#equals(BleDevice)
	 */
	@Override public boolean equals(@Nullable(Prevalence.NORMAL) final Object object_nullable)
	{
		if( object_nullable == null )  return false;

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
	public void startPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		m_pollMngr.startPoll(serviceUuid, characteristicUuid, Interval.secs(interval), listener, /*trackChanges=*/false, /*usingNotify=*/false);
	}

	/**
	 * Same as {@link #startPoll(java.util.UUID, Interval, BleDevice.ReadWriteListener)} but without a listener.
	 * <br><br>
	 * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
	 */
	public void startPoll(final UUID characteristicUuid, final Interval interval)
	{
		startPoll(characteristicUuid, interval, null);
	}

	/**
	 * Overload of {@link #startPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public void startPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		m_pollMngr.startPoll(serviceUuid, characteristicUuid, Interval.secs(interval), listener, /*trackChanges=*/false, /*usingNotify=*/false);
	}

	/**
	 * Overload of {@link #startPoll(UUID, Interval)} for when you have characteristics with identical uuids under different services.
	 */
	public void startPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval)
	{
		startPoll(serviceUuid, characteristicUuid, interval, null);
	}

	/**
	 * Convenience to call {@link #startPoll(java.util.UUID, Interval, BleDevice.ReadWriteListener)} for multiple
	 * characteristic uuids all at once.
	 */
	public void startPoll(final UUID[] uuids, final Interval interval, final ReadWriteListener listener)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			startPoll(uuids[i], interval, listener);
		}
	}

	/**
	 * Same as {@link #startPoll(java.util.UUID[], Interval, BleDevice.ReadWriteListener)} but without a listener.
	 * <br><br>
	 * See {@link #read(java.util.UUID)} for an explanation of why you would do this.
	 */
	public void startPoll(final UUID[] uuids, final Interval interval)
	{
		startPoll(uuids, interval, null);
	}

	/**
	 * Similar to {@link #startPoll(UUID, Interval, ReadWriteListener)} but only
	 * invokes a callback when a change in the characteristic value is detected.
	 * Use this in preference to {@link #enableNotify(UUID, ReadWriteListener)} if possible,
	 * due to instability issues (rare, but still) with notifications on Android.
	 * <br><br>
	 * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
	 */
	public void startChangeTrackingPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		m_pollMngr.startPoll(serviceUuid, characteristicUuid, Interval.secs(interval), listener, /*trackChanges=*/true, /*usingNotify=*/false);
	}

	/**
	 * Overload of {@link #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public void startChangeTrackingPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		m_pollMngr.startPoll(serviceUuid, characteristicUuid, Interval.secs(interval), listener, /*trackChanges=*/true, /*usingNotify=*/false);
	}

	/**
	 * Stops a poll(s) started by either {@link #startPoll(UUID, Interval, ReadWriteListener)} or
	 * {@link #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)}. This will stop all polls matching the provided parameters.
	 *
	 * @see #startPoll(UUID, Interval, ReadWriteListener)
	 * @see #startChangeTrackingPoll(UUID, Interval, ReadWriteListener)
	 */
	public void stopPoll(final UUID characteristicUuid, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		stopPoll_private(serviceUuid, characteristicUuid, null, listener);
	}

	/**
	 * Same as {@link #stopPoll(java.util.UUID, BleDevice.ReadWriteListener)} but without the listener.
	 */
	public void stopPoll(final UUID characteristicUuid)
	{
		stopPoll(characteristicUuid, (ReadWriteListener) null);
	}

	/**
	 * Same as {@link #stopPoll(UUID, ReadWriteListener)} but with added filtering for the poll {@link Interval}.
	 */
	public void stopPoll(final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		stopPoll_private(serviceUuid, characteristicUuid, interval != null ? interval.secs() : null, listener);
	}

	/**
	 * Same as {@link #stopPoll(java.util.UUID, Interval, BleDevice.ReadWriteListener)} but without the listener.
	 */
	public void stopPoll(final UUID characteristicUuid, final Interval interval)
	{
		final UUID serviceUuid = null;

		stopPoll_private(serviceUuid, characteristicUuid, interval != null ? interval.secs() : null, null);
	}

	/**
	 * Overload of {@link #stopPoll(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
	{
		stopPoll_private(serviceUuid, characteristicUuid, null, listener);
	}

	/**
	 * Overload of {@link #stopPoll(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	public void stopPoll(final UUID serviceUuid, final UUID characteristicUuid)
	{
		stopPoll(serviceUuid, characteristicUuid, (ReadWriteListener) null);
	}

	/**
	 * Overload of {@link #stopPoll(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval, final ReadWriteListener listener)
	{
		stopPoll_private(serviceUuid, characteristicUuid, interval != null ? interval.secs() : null, listener);
	}

	/**
	 * Overload of {@link #stopPoll(UUID, Interval)} for when you have characteristics with identical uuids under different services.
	 */
	public void stopPoll(final UUID serviceUuid, final UUID characteristicUuid, final Interval interval)
	{
		stopPoll_private(serviceUuid, characteristicUuid, interval != null ? interval.secs() : null, null);
	}

	/**
	 * Calls {@link #stopPoll(java.util.UUID, Interval, BleDevice.ReadWriteListener)} multiple times for you.
	 */
	public void stopPoll(final UUID[] uuids, final Interval interval, final ReadWriteListener listener)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			stopPoll(uuids[i], interval, listener);
		}
	}

	/**
	 * Calls {@link #stopPoll(java.util.UUID, Interval)} multiple times for you.
	 */
	public void stopPoll(final UUID[] uuids, final Interval interval)
	{
		stopPoll(uuids, interval, null);
	}

	/**
	 * Calls {@link #stopPoll(java.util.UUID)} multiple times for you.
	 */
	public void stopPoll(final UUID[] uuids)
	{
		stopPoll(uuids, null, null);
	}

	/**
	 * Writes to the device without a callback.
	 *
	 * @return (same as {@link #write(UUID, byte[], ReadWriteListener)}).
	 *
	 * @see #write(UUID, byte[], ReadWriteListener)
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final byte[] data)
	{
		return this.write(characteristicUuid, new PresentData(data), (ReadWriteListener) null);
	}

	/**
	 * Writes to the device with a callback.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #write(UUID, byte[])
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final byte[] data, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		return write(serviceUuid, characteristicUuid, new PresentData(data), listener);
	}

	/**
	 * Overload of {@link #write(UUID, byte[])} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final byte[] data)
	{
		return this.write(serviceUuid, characteristicUuid, new PresentData(data), (ReadWriteListener) null);
	}

	/**
	 * Overload of {@link #write(UUID, byte[], ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final byte[] data, final ReadWriteListener listener)
	{
		return write(serviceUuid, characteristicUuid, new PresentData(data), new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread));
	}


	/**
	 * Writes to the device without a callback.
	 *
	 * @return (same as {@link #write(UUID, FutureData, ReadWriteListener)}).
	 *
	 * @see #write(UUID, FutureData, ReadWriteListener)
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final FutureData futureData)
	{
		return this.write(characteristicUuid, futureData, (ReadWriteListener) null);
	}

	/**
	 * Writes to the device with a callback.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 *
	 * @see #write(UUID, FutureData)
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID characteristicUuid, final FutureData futureData, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		return write(serviceUuid, characteristicUuid, futureData, listener);
	}

	/**
	 * Overload of {@link #write(UUID, FutureData)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData)
	{
		return this.write(serviceUuid, characteristicUuid, futureData, (ReadWriteListener) null);
	}

	/**
	 * Overload of {@link #write(UUID, FutureData, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent write(final UUID serviceUuid, final UUID characteristicUuid, final FutureData futureData, final ReadWriteListener listener)
	{
		return write_internal(serviceUuid, characteristicUuid, futureData, new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread));
	}

	/**
	 * Same as {@link #readRssi(ReadWriteListener)} but use this method when you don't much care when/if the RSSI is actually updated.
	 *
	 * @return (same as {@link #readRssi(ReadWriteListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent readRssi()
	{
		return readRssi(null);
	}

	/**
	 * Wrapper for {@link BluetoothGatt#readRemoteRssi()}. This will eventually update the value returned by {@link #getRssi()} but it is not
	 * instantaneous. When a new RSSI is actually received the given listener will be called. The device must be {@link BleDeviceState#CONNECTED} for
	 * this call to succeed. When the device is not {@link BleDeviceState#CONNECTED} then the value returned by
	 * {@link #getRssi()} will be automatically updated every time this device is discovered (or rediscovered) by a scan operation.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent readRssi(final ReadWriteListener listener)
	{
		final ReadWriteEvent earlyOutResult = m_serviceMngr.getEarlyOutEvent(Uuids.INVALID, Uuids.INVALID, EMPTY_FUTURE_DATA, Type.READ, ReadWriteListener.Target.RSSI);

		if (earlyOutResult != null)
		{
			invokeReadWriteCallback(listener, earlyOutResult);

			return earlyOutResult;
		}

		P_WrappingReadWriteListener wrappingListener = listener != null ? new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread) : null;
		readRssi_internal(Type.READ, wrappingListener);

		return NULL_READWRITE_EVENT();
	}

	/**
	 * Same as {@link #startPoll(UUID, Interval, ReadWriteListener)} but for when you don't care when/if the RSSI is actually updated.
	 * <br><br>
	 * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#DISCONNECTED}.
	 */
	public void startRssiPoll(final Interval interval)
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
	public void startRssiPoll(final Interval interval, final ReadWriteListener listener)
	{
		if (isNull())  return;

		m_rssiPollMngr.start(interval.secs(), listener);

		m_rssiPollMngr_auto.stop();
	}

	/**
	 * Stops an RSSI poll previously started either by {@link #startRssiPoll(Interval)} or {@link #startRssiPoll(Interval, ReadWriteListener)}.
	 */
	public void stopRssiPoll()
	{
		if (isNull())  return;

		m_rssiPollMngr.stop();

		final Interval autoPollRate = BleDeviceConfig.interval(conf_device().rssiAutoPollRate, conf_mngr().rssiAutoPollRate);

		if (!Interval.isDisabled(autoPollRate))
		{
			m_rssiPollMngr_auto.start(autoPollRate.secs(), null);
		}
	}

	void readRssi_internal(Type type, P_WrappingReadWriteListener listener)
	{
		m_queue.add(new P_Task_ReadRssi(this, listener, m_txnMngr.getCurrent(), getOverrideReadWritePriority(), type));
	}

	/**
	 * Returns a new {@link com.idevicesinc.sweetblue.utils.HistoricalData} instance using
	 * {@link com.idevicesinc.sweetblue.BleDeviceConfig#historicalDataFactory} if available.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
	{
		final BleDeviceConfig.HistoricalDataFactory factory_device = conf_device().historicalDataFactory;
		final BleDeviceConfig.HistoricalDataFactory factory_mngr = conf_mngr().historicalDataFactory;
		final BleDeviceConfig.HistoricalDataFactory factory = factory_device != null ? factory_device : factory_mngr;

		if( factory != null )
		{
			return factory.newHistoricalData(data, epochTime);
		}
		else
		{
			return new HistoricalData(data, epochTime);
		}
	}

	/**
	 * Returns the database table name for the underlying store of historical data for the given {@link UUID}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) String getHistoricalDataTableName(final UUID uuid)
	{
		return getManager().m_historicalDatabase.getTableName(getMacAddress(), uuid);
	}

	/**
	 * Provides a means to perform a raw SQL query on the database storing the historical data for this device. Use {@link #getHistoricalDataTableName(UUID)}
	 * to generate table names and {@link HistoricalDataColumn} to get column names.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Alpha
	public @Nullable(Prevalence.NEVER) HistoricalDataQueryListener.HistoricalDataQueryEvent queryHistoricalData(final String query)
	{
		final Cursor cursor = getManager().m_historicalDatabase.query(query);

		return new BleDevice.HistoricalDataQueryListener.HistoricalDataQueryEvent(this, Uuids.INVALID, cursor, BleDevice.HistoricalDataQueryListener.Status.SUCCESS, query);
	}

	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Alpha
	public void queryHistoricalData(final String query, final HistoricalDataQueryListener listener)
	{
		if( isNull() )
		{
			listener.onEvent(new HistoricalDataQueryListener.HistoricalDataQueryEvent(this, Uuids.INVALID, new EmptyCursor(), HistoricalDataQueryListener.Status.NULL, query));

			return;
		}

		m_historicalDataMngr.post(new Runnable()
		{
			@Override public void run()
			{
				final BleDevice.HistoricalDataQueryListener.HistoricalDataQueryEvent e = queryHistoricalData(query);

				BleDevice.this.getManager().getUpdateLoop().postIfNeeded(new Runnable()
				{
					@Override public void run()
					{
						listener.onEvent(e);
					}
				});
			}
		});
	}

	@com.idevicesinc.sweetblue.annotations.Advanced
	@com.idevicesinc.sweetblue.annotations.Alpha
	public @Nullable(Prevalence.NEVER) HistoricalDataQuery.Part_Select select()
	{
		final HistoricalDataQuery.Part_Select select = HistoricalDataQuery.select(this, getManager().m_historicalDatabase);

		return select;
	}

	/**
	 * Returns a cursor capable of random access to the database-persisted historical data for this device.
	 * Unlike calls to methods like {@link #getHistoricalData_iterator(UUID)} and other overloads,
	 * this call does not force bulk data load into memory.
	 * <br><br>
	 * NOTE: You must call {@link HistoricalDataCursor#close()} when you are done with the data.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) HistoricalDataCursor getHistoricalData_cursor(final UUID uuid)
	{
		return getHistoricalData_cursor(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
	}

	/**
	 * Same as {@link #getHistoricalData_cursor(UUID)} but constrains the results to the given time range.
	 * <br><br>
	 * NOTE: You must call {@link HistoricalDataCursor#close()} when you are done with the data.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) HistoricalDataCursor getHistoricalData_cursor(final UUID uuid, final EpochTimeRange range)
	{
		return m_historicalDataMngr.getCursor(uuid, range);
	}

	/**
	 * Loads all historical data to memory for this device.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void loadHistoricalData()
	{
		loadHistoricalData(null, null);
	}

	/**
	 * Loads all historical data to memory for this device for the given {@link UUID}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void loadHistoricalData(final UUID uuid)
	{
		loadHistoricalData(uuid, null);
	}

	/**
	 * Loads all historical data to memory for this device with a callback for when it's complete.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void loadHistoricalData(final HistoricalDataLoadListener listener)
	{
		loadHistoricalData(null, listener);
	}

	/**
	 * Loads all historical data to memory for this device for the given {@link UUID}.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void loadHistoricalData(final UUID uuid, final HistoricalDataLoadListener listener)
	{
		if( isNull() )  return;

		m_historicalDataMngr.load(uuid, listener);
	}

	/**
	 * Returns whether the device is currently loading any historical data to memory, either through
	 * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData_iterator(UUID)} (or overloads).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean isHistoricalDataLoading()
	{
		return m_historicalDataMngr.isLoading(null);
	}

	/**
	 * Returns whether the device is currently loading any historical data to memory for the given uuid, either through
	 * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData_iterator(UUID)} (or overloads).
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean isHistoricalDataLoading(final UUID uuid)
	{
		return m_historicalDataMngr.isLoading(uuid);
	}

	/**
	 * Returns <code>true</code> if the historical data for all historical data for
	 * this device is loaded into memory.
	 * Use {@link com.idevicesinc.sweetblue.BleDevice.HistoricalDataLoadListener}
	 * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
	 * returns <code>false</code> then this will also always return <code>false</code>.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean isHistoricalDataLoaded()
	{
		return m_historicalDataMngr.isLoaded(null);
	}

	/**
	 * Returns <code>true</code> if the historical data for a given uuid is loaded into memory.
	 * Use {@link com.idevicesinc.sweetblue.BleDevice.HistoricalDataLoadListener}
	 * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
	 * returns <code>false</code> then this will also always return <code>false</code>.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean isHistoricalDataLoaded(final UUID uuid)
	{
		return m_historicalDataMngr.isLoaded(uuid);
	}

	/**
	 * Returns the cached data from the lastest successful read or notify received for a given uuid.
	 * Basically if you receive a {@link ReadWriteListener.ReadWriteEvent} for which {@link ReadWriteListener.ReadWriteEvent#isRead()}
	 * and {@link ReadWriteListener.ReadWriteEvent#wasSuccess()} both return <code>true</code> then {@link ReadWriteListener.ReadWriteEvent#data()},
	 * will be cached and is retrievable by this method.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 *
	 * @return The cached value from a previous read or notify, or {@link HistoricalData#NULL} otherwise.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) HistoricalData getHistoricalData_latest(final UUID uuid)
	{
		return getHistoricalData_atOffset(uuid, getHistoricalDataCount(uuid) - 1);
	}

	/**
	 * Returns an iterator that will iterate through all {@link HistoricalData} entries.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) Iterator<HistoricalData> getHistoricalData_iterator(final UUID uuid)
	{
		return getHistoricalData_iterator(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
	}

	/**
	 * Returns an iterator that will iterate through all {@link HistoricalData} entries within the range provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) Iterator<HistoricalData> getHistoricalData_iterator(final UUID uuid, final EpochTimeRange range)
	{
		if( isNull() ) return new EmptyIterator<HistoricalData>();

		return m_historicalDataMngr.getIterator(uuid, EpochTimeRange.denull(range));
	}

	/**
	 * Provides all historical data through the "for each" provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 *
	 * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean getHistoricalData_forEach(final UUID uuid, final ForEach_Void<HistoricalData> forEach)
	{
		return getHistoricalData_forEach(uuid, EpochTimeRange.FROM_MIN_TO_MAX, forEach);
	}

	/**
	 * Provides all historical data through the "for each" provided within the range provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 *
	 * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Void<HistoricalData> forEach)
	{
		if( isNull() ) return false;

		return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
	}

	/**
	 * Provides all historical data through the "for each" provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 *
	 * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean getHistoricalData_forEach(final UUID uuid, final ForEach_Breakable<HistoricalData> forEach)
	{
		return getHistoricalData_forEach(uuid, EpochTimeRange.FROM_MIN_TO_MAX, forEach);
	}

	/**
	 * Provides all historical data through the "for each" provided within the range provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 *
	 * @return <code>true</code> if there are any entries, <code>false</code> otherwise.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Breakable<HistoricalData> forEach)
	{
		if( isNull() ) return false;

		return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but returns the data from the chronological offset, i.e. <code>offsetFromStart=0</code>
	 * will return the earliest {@link HistoricalData}. Use in combination with {@link #getHistoricalDataCount(java.util.UUID)} to iterate
	 * "manually" through this device's historical data for the given characteristic.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) HistoricalData getHistoricalData_atOffset(final UUID uuid, final int offsetFromStart)
	{
		return getHistoricalData_atOffset(uuid, EpochTimeRange.FROM_MIN_TO_MAX, offsetFromStart);
	}

	/**
	 * Same as {@link #getHistoricalData_atOffset(java.util.UUID, int)} but offset is relative to the time range provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public @Nullable(Prevalence.NEVER) HistoricalData getHistoricalData_atOffset(final UUID uuid, final EpochTimeRange range, final int offsetFromStart)
	{
		if( isNull() ) return HistoricalData.NULL;

		return m_historicalDataMngr.getWithOffset(uuid, EpochTimeRange.denull(range), offsetFromStart);
	}

	/**
	 * Returns the number of historical data entries that have been logged for the device's given characteristic.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public int getHistoricalDataCount(final UUID uuid)
	{
		return getHistoricalDataCount(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
	}

	/**
	 * Returns the number of historical data entries that have been logged
	 * for the device's given characteristic within the range provided.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public int getHistoricalDataCount(final UUID uuid, final EpochTimeRange range)
	{
		if( isNull() ) return 0;

		return m_historicalDataMngr.getCount(uuid, EpochTimeRange.denull(range));
	}

	/**
	 * Returns <code>true</code> if there is any historical data at all for this device.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean hasHistoricalData()
	{
		return hasHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX);
	}

	/**
	 * Returns <code>true</code> if there is any historical data at all for this device within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean hasHistoricalData(final EpochTimeRange range)
	{
		if( isNull() ) return false;

		return m_historicalDataMngr.hasHistoricalData(range);
	}

	/**
	 * Returns <code>true</code> if there is any historical data for the given uuid.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean hasHistoricalData(final UUID uuid)
	{
		return hasHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
	}

	/**
	 * Returns <code>true</code> if there is any historical data for any of the given uuids.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean hasHistoricalData(final UUID[] uuids)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			if( hasHistoricalData(uuids[i], EpochTimeRange.FROM_MIN_TO_MAX) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns <code>true</code> if there is any historical data for the given uuid within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range)
	{
		return m_historicalDataMngr.hasHistoricalData(uuid, range);
	}

	/**
	 * Manual way to add data to the historical data list managed by this device. You may want to use this if,
	 * for example, your remote BLE device is capable of taking and caching independent readings while not connected.
	 * After you connect with this device and download the log you can add it manually here.
	 * Really you can use this for any arbitrary historical data though, even if it's not associated with a characteristic.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final byte[] data, final EpochTime epochTime)
	{
		if( isNull() ) return;

		m_historicalDataMngr.add_single(uuid, data, epochTime, BleDeviceConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
	}

	/**
	 * Just an overload of {@link #addHistoricalData(UUID, byte[], EpochTime)} with the data and epochTime parameters switched around.
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final EpochTime epochTime, final byte[] data)
	{
		this.addHistoricalData(uuid, data, epochTime);
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but uses {@link System#currentTimeMillis()} for the timestamp.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final byte[] data)
	{
		if( isNull() ) return;

		m_historicalDataMngr.add_single(uuid, data, new EpochTime(), BleDeviceConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)}.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final HistoricalData historicalData)
	{
		if( isNull() ) return;

		m_historicalDataMngr.add_single(uuid, historicalData, BleDeviceConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final Iterator<HistoricalData> historicalData)
	{
		if( isNull() ) return;

		m_historicalDataMngr.add_multiple(uuid, historicalData);
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final List<HistoricalData> historicalData)
	{
		addHistoricalData(uuid, historicalData.iterator());
	}

	/**
	 * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void addHistoricalData(final UUID uuid, final ForEach_Returning<HistoricalData> historicalData)
	{
		if( isNull() ) return;

		m_historicalDataMngr.add_multiple(uuid, historicalData);
	}

	/**
	 * One method to remove absolutely all "metadata" related to this device that is stored on disk and/or cached in memory in any way.
	 * This method is useful if for example you have a "forget device" feature in your app.
	 */
	public void clearAllData()
	{
		clearName();
		clearHistoricalData();
		clearSharedPreferences();
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData()
	{
		if( isNull() ) return;

		m_historicalDataMngr.clearEverything();
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final long count)
	{
		clearHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX, count);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final EpochTimeRange range)
	{
		clearHistoricalData(range, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final EpochTimeRange range, final long count)
	{
		if( isNull() ) return;

		m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/false);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID}.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final UUID uuid)
	{
		clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
	}

	/**
	 * Overload of {@link #clearHistoricalData(UUID)} that just calls that method multiple times.
	 */
	public void clearHistoricalData(final UUID ... uuids)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			final UUID ith = uuids[i];

			clearHistoricalData(ith);
		}
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID}.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final UUID uuid, final long count)
	{
		clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID} within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final UUID uuid, final EpochTimeRange range)
	{
		clearHistoricalData(uuid, range, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID} within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData(final UUID uuid, final EpochTimeRange range, final long count)
	{
		if( isNull() ) return;

		m_historicalDataMngr.delete(uuid, range, count, /*memoryOnly=*/false);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly()
	{
		clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final long count)
	{
		clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, count);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final EpochTimeRange range)
	{
		clearHistoricalData_memoryOnly(range, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final EpochTimeRange range, final long count)
	{
		if( isNull() ) return;

		m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/true);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID}.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final UUID uuid)
	{
		clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID}.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final UUID uuid, final long count)
	{
		clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
	}

	/**
	 * Clears all {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID} within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range)
	{
		clearHistoricalData_memoryOnly(characteristicUuid, range, Long.MAX_VALUE);
	}

	/**
	 * Clears the first <code>count</code> number of {@link com.idevicesinc.sweetblue.utils.HistoricalData} tracked by this device for a particular
	 * characteristic {@link java.util.UUID} within the given range.
	 *
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.HistoricalDataLogFilter
	 * @see com.idevicesinc.sweetblue.BleDeviceConfig.DefaultHistoricalDataLogFilter
	 */
	@com.idevicesinc.sweetblue.annotations.Advanced
	public void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range, final long count)
	{
		if( isNull() ) return;

		m_historicalDataMngr.delete(characteristicUuid, range, count, /*memoryOnly=*/true);
	}

	/**
	 * Same as {@link #read(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} but you can use this
	 * if you don't immediately care about the result. The callback will still be posted to {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener}
	 * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} and
	 * {@link BleManager#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	public ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid)
	{
		final UUID serviceUuid = null;

		return read_internal(serviceUuid, characteristicUuid, Type.READ, null);
	}

	/**
	 * Reads a characteristic from the device.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent read(final UUID characteristicUuid, final ReadWriteListener listener)
	{
		final ReadWriteListener listener_override = getManager() == null ? listener : new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread);

		final UUID serviceUuid = null;

		return read_internal(serviceUuid, characteristicUuid, Type.READ, listener_override);
	}

	/**
	 * Overload of {@link #read(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid)
	{
		return read_internal(serviceUuid, characteristicUuid, Type.READ, null);
	}

	/**
	 * Overload of {@link #read(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent read(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
	{
		final ReadWriteListener listener_override = getManager() == null ? listener : new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread);

		return read_internal(serviceUuid, characteristicUuid, Type.READ, listener_override);
	}

	/**
	 * Returns <code>true</code> if notifications are enabled for the given uuid.
	 * NOTE: {@link #isNotifyEnabling(UUID)} may return true here even if this returns false.
	 *
	 * @see #isNotifyEnabling(UUID)
	 */
	public boolean isNotifyEnabled(final UUID uuid)
	{
		if( isNull() )  return false;

		final UUID serviceUuid = null;

		final E_NotifyState notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

		return notifyState == E_NotifyState.ENABLED;
	}

	/**
	 * Returns <code>true</code> if SweetBlue is in the process of enabling notifications for the given uuid.
	 *
	 * @see #isNotifyEnabled(UUID)
	 */
	public boolean isNotifyEnabling(final UUID uuid)
	{
		if( isNull() )  return false;

		final UUID serviceUuid = null;

		final E_NotifyState notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

		return notifyState == E_NotifyState.ENABLING;
	}

	/**
	 * Overload for {@link #enableNotify(UUID)}.
	 */
	public void enableNotify(final UUID[] uuids)
	{
		this.enableNotify(uuids, Interval.INFINITE, null);
	}

	/**
	 * Overload for {@link #enableNotify(UUID, ReadWriteListener)}.
	 */
	public void enableNotify(final UUID[] uuids, ReadWriteListener listener)
	{
		this.enableNotify(uuids, Interval.INFINITE, listener);
	}

	/**
	 * Overload for {@link #enableNotify(UUID, Interval)}.
	 */
	public void enableNotify(final UUID[] uuids, final Interval forceReadTimeout)
	{
		this.enableNotify(uuids, forceReadTimeout, null);
	}

	/**
	 * Overload for {@link #enableNotify(UUID, Interval, ReadWriteListener)}.
	 */
	public void enableNotify(final UUID[] uuids, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			final UUID ith = uuids[i];

			enableNotify(ith, forceReadTimeout, listener);
		}
	}

	/**
	 * Same as {@link #enableNotify(java.util.UUID, com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} but you can use
	 * this if you don't need a callback. Callbacks will still be posted to {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener}
	 * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} and
	 * {@link BleManager#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid)
	{
		return this.enableNotify(null, characteristicUuid, Interval.INFINITE, null);
	}

	/**
	 * Enables notification on the given characteristic. The listener will be called both for the notifications themselves and for the actual
	 * registration for the notification. <code>switch</code> on {@link Type#ENABLING_NOTIFICATION}
	 * and {@link Type#NOTIFICATION} (or {@link Type#INDICATION}) in your listener to distinguish between these.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, ReadWriteListener listener)
	{
		return this.enableNotify(null, characteristicUuid, Interval.INFINITE, listener);
	}

	/**
	 * Same as {@link #enableNotify(java.util.UUID, Interval, BleDevice.ReadWriteListener)} but you can use
	 * this if you don't need a callback. Callbacks will still be posted to {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener}
	 * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} and
	 * {@link BleManager#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, final Interval forceReadTimeout)
	{
		return this.enableNotify(null, characteristicUuid, forceReadTimeout, null);
	}

	/**
	 * Same as {@link #enableNotify(UUID, ReadWriteListener)} but forces a read after a given amount of time. If you received {@link ReadWriteListener.Status#SUCCESS} for
	 * {@link Type#ENABLING_NOTIFICATION} but haven't received an actual notification in some time it may be a sign that notifications have broken
	 * in the underlying stack.
	 *
	 * @return (same as {@link #enableNotify(UUID, ReadWriteListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		return this.enableNotify(null, characteristicUuid, forceReadTimeout, listener);
	}

	/**
	 * Overload of {@link #enableNotify(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid)
	{
		return this.enableNotify(serviceUuid, characteristicUuid, Interval.INFINITE, null);
	}

	/**
	 * Overload of {@link #enableNotify(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, ReadWriteListener listener)
	{
		return this.enableNotify(serviceUuid, characteristicUuid, Interval.INFINITE, listener);
	}

	/**
	 * Overload of {@link #enableNotify(UUID, Interval)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout)
	{
		return this.enableNotify(serviceUuid, characteristicUuid, forceReadTimeout, null);
	}

	/**
	 * Overload of {@link #enableNotify(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent enableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		final ReadWriteEvent earlyOutResult = m_serviceMngr.getEarlyOutEvent(serviceUuid, characteristicUuid, EMPTY_FUTURE_DATA, Type.ENABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

		if (earlyOutResult != null)
		{
			invokeReadWriteCallback(listener, earlyOutResult);

			if (earlyOutResult.status() == ReadWriteListener.Status.NO_MATCHING_TARGET || (Interval.INFINITE.equals(forceReadTimeout) || Interval.DISABLED.equals(forceReadTimeout)))
			{
				//--- DRK > No need to put this notify in the poll manager because either the characteristic wasn't found
				//--- or the notify (or indicate) property isn't supported and we're not doing a backing read poll.
				return earlyOutResult;
			}
		}

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);
		final E_NotifyState notifyState = m_pollMngr.getNotifyState(serviceUuid, characteristicUuid);
		final boolean shouldSendOutNotifyEnable = notifyState == E_NotifyState.NOT_ENABLED && (earlyOutResult == null || earlyOutResult.status() != ReadWriteListener.Status.OPERATION_NOT_SUPPORTED);

		final ReadWriteEvent result;
		final boolean isConnected = is(CONNECTED);

		if (shouldSendOutNotifyEnable && characteristic != null && isConnected)
		{
			m_bondMngr.bondIfNeeded(characteristic, CharacteristicEventType.ENABLE_NOTIFY);

			P_WrappingReadWriteListener wrappingListener = new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread);
			m_queue.add(new P_Task_ToggleNotify(this, characteristic, /*enable=*/true, wrappingListener));

			m_pollMngr.onNotifyStateChange(serviceUuid, characteristicUuid, E_NotifyState.ENABLING);

			result = NULL_READWRITE_EVENT();
		}
		else if (notifyState == E_NotifyState.ENABLED)
		{
			if (listener != null && isConnected )
			{
				result = m_pollMngr.newAlreadyEnabledResult(characteristic, serviceUuid, characteristicUuid);

				invokeReadWriteCallback(listener, result);
			}
			else
			{
				result = NULL_READWRITE_EVENT();
			}

			if( !isConnected )
			{
				getManager().ASSERT(false, "Notification is enabled but we're not connected!");
			}
		}
		else
		{
			result = NULL_READWRITE_EVENT();
		}

		m_pollMngr.startPoll(serviceUuid, characteristicUuid, forceReadTimeout.secs(), listener, /*trackChanges=*/true, /*usingNotify=*/true);

		return result;
	}

	/**
	 * Disables all notifications enabled by {@link #enableNotify(UUID, ReadWriteListener)} or
	 * {@link #enableNotify(UUID, Interval, ReadWriteListener)}. The listener
	 * provided should be the same one that you passed to {@link #enableNotify(UUID, ReadWriteListener)}. Listen for
	 * {@link Type#DISABLING_NOTIFICATION} in your listener to know when the remote device actually confirmed.
	 *
	 * @return (see similar comment for return value of {@link #connect(BleTransaction.Auth, BleTransaction.Init, StateListener, ConnectionFailListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		return this.disableNotify_private(serviceUuid, characteristicUuid, null, listener);
	}

	/**
	 * Same as {@link #disableNotify(UUID, ReadWriteListener)} but filters on the given {@link Interval}.
	 *
	 * @return (same as {@link #disableNotify(UUID, ReadWriteListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		final UUID serviceUuid = null;

		return this.disableNotify_private(serviceUuid, characteristicUuid, Interval.secs(forceReadTimeout), listener);
	}

	/**
	 * Same as {@link #disableNotify(java.util.UUID, BleDevice.ReadWriteListener)} but you can use this if you don't care about the result.
	 * The callback will still be posted to {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener}
	 * instances (if any) provided to {@link BleDevice#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)} and
	 * {@link BleManager#setListener_ReadWrite(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener)}.
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid)
	{
		final UUID serviceUuid = null;

		return this.disableNotify_private(serviceUuid, characteristicUuid, null, null);
	}

	/**
	 * Same as {@link #disableNotify(UUID, ReadWriteListener)} but filters on the given {@link Interval} without a listener.
	 *
	 * @return (same as {@link #disableNotify(UUID, ReadWriteListener)}).
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID characteristicUuid, final Interval forceReadTimeout)
	{
		final UUID serviceUuid = null;

		return this.disableNotify_private(serviceUuid, characteristicUuid, Interval.secs(forceReadTimeout), null);
	}

	/**
	 * Overload of {@link #disableNotify(UUID, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final ReadWriteListener listener)
	{
		return this.disableNotify_private(serviceUuid, characteristicUuid, null, listener);
	}

	/**
	 * Overload of {@link #disableNotify(UUID, Interval, ReadWriteListener)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		return this.disableNotify_private(serviceUuid, characteristicUuid, Interval.secs(forceReadTimeout), listener);
	}

	/**
	 * Overload of {@link #disableNotify(UUID)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid)
	{
		return this.disableNotify_private(serviceUuid, characteristicUuid, null, null);
	}

	/**
	 * Overload of {@link #disableNotify(UUID, Interval)} for when you have characteristics with identical uuids under different services.
	 */
	public ReadWriteListener.ReadWriteEvent disableNotify(final UUID serviceUuid, final UUID characteristicUuid, final Interval forceReadTimeout)
	{
		return this.disableNotify_private(serviceUuid, characteristicUuid, Interval.secs(forceReadTimeout), null);
	}

	/**
	 * Overload for {@link #disableNotify(UUID, ReadWriteListener)}.
	 */
	public void disableNotify(final UUID[] uuids, final ReadWriteListener listener)
	{
		this.disableNotify(uuids, null, listener);
	}

	/**
	 * Overload for {@link #disableNotify(UUID)}.
	 */
	public void disableNotify(final UUID[] uuids)
	{
		this.disableNotify(uuids, null, null);
	}

	/**
	 * Overload for {@link #disableNotify(UUID, Interval)}.
	 */
	public void disableNotify(final UUID[] uuids, final Interval forceReadTimeout)
	{
		this.disableNotify(uuids, forceReadTimeout, null);
	}

	/**
	 * Overload for {@link #disableNotify(UUID, Interval, BleDevice.ReadWriteListener)}.
	 */
	public void disableNotify(final UUID[] uuids, final Interval forceReadTimeout, final ReadWriteListener listener)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			final UUID ith = uuids[i];

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
	 *         {@link BleDeviceState#PERFORMING_OTA} or is not {@link BleDeviceState#INITIALIZED}.
	 *
	 * @see BleManagerConfig#includeOtaReadWriteTimesInAverage
	 * @see BleManagerConfig#autoScanDuringOta
	 * @see #performTransaction(BleTransaction)
	 */
	public boolean performOta(final BleTransaction.Ota txn)
	{
		if( performTransaction_earlyOut(txn) )		return false;

		if ( is(PERFORMING_OTA) )
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
	public boolean performTransaction(final BleTransaction txn)
	{
		if( performTransaction_earlyOut(txn) )		return false;

		m_txnMngr.performAnonTransaction(txn);

		return true;
	}

	private boolean performTransaction_earlyOut(final BleTransaction txn)
	{
		if ( txn == null )							return true;
		if (isNull())								return true;
		if (!is_internal(INITIALIZED))				return true;
		if ( m_txnMngr.getCurrent() != null )		return true;

		return false;
	}

	/**
	 * Returns the device's name and current state for logging and debugging purposes.
	 */
	@Override public String toString()
	{
		if (isNull())
		{
			return NULL_STRING();
		}
		else
		{
			return getName_debug() + " " + stateTracker_main().toString();
		}
	}

	private boolean shouldAddOperationTime()
	{
		boolean includeFirmwareUpdateReadWriteTimesInAverage = BleDeviceConfig.bool(conf_device().includeOtaReadWriteTimesInAverage, conf_mngr().includeOtaReadWriteTimesInAverage);

		return includeFirmwareUpdateReadWriteTimesInAverage || !is(PERFORMING_OTA);
	}

	void addReadTime(double timeStep)
	{
		if (!shouldAddOperationTime())
			return;

		if (m_readTimeEstimator != null)
		{
			m_readTimeEstimator.addTime(timeStep);
		}
	}

	void addWriteTime(double timeStep)
	{
		if (!shouldAddOperationTime())  return;

		if (m_writeTimeEstimator != null)
		{
			m_writeTimeEstimator.addTime(timeStep);
		}
	}

	void setToAlwaysUseAutoConnectIfItWorked()
	{
		m_alwaysUseAutoConnect = m_useAutoConnect;
	}

	boolean shouldUseAutoConnect()
	{
		return m_useAutoConnect;
	}

	P_BleDevice_Listeners getListeners()
	{
		return m_listeners;
	}

	P_TaskQueue getTaskQueue()
	{
		return m_queue;
	}

	// PA_StateTracker getStateTracker(){ return m_stateTracker; }
	BleTransaction getOtaTxn()
	{
		return m_txnMngr.m_otaTxn;
	}

	P_PollManager getPollManager()
	{
		return m_pollMngr;
	}

	P_ServiceManager getServiceManager()
	{
		return m_serviceMngr;
	}

	void onNewlyDiscovered(final BluetoothDevice device_native, List<UUID> advertisedServices_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
	{
		m_origin_latest = origin;

		clear_discovery();

		m_nativeWrapper.updateNativeDevice(device_native);

		onDiscovered_private(advertisedServices_nullable, rssi, scanRecord_nullable);

		stateTracker_main().update(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), UNDISCOVERED, false, DISCOVERED, true, ADVERTISING, origin==BleDeviceOrigin.FROM_DISCOVERY, DISCONNECTED, true);
	}

	void onRediscovered(final BluetoothDevice device_native, List<UUID> advertisedServices_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
	{
		m_origin_latest = origin;

		m_nativeWrapper.updateNativeDevice(device_native);

		onDiscovered_private(advertisedServices_nullable, rssi, scanRecord_nullable);

		stateTracker_main().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), ADVERTISING, true);
	}

	void onUndiscovered(E_Intent intent)
	{
		clear_undiscovery();

		if( m_reconnectMngr_longTerm != null )  m_reconnectMngr_longTerm.stop();
		if( m_reconnectMngr_shortTerm != null )  m_reconnectMngr_shortTerm.stop();
		if( m_rssiPollMngr != null )  m_rssiPollMngr.stop();
		if( m_rssiPollMngr_auto != null )  m_rssiPollMngr_auto.stop();

		stateTracker_main().set(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, DISCOVERED, false, ADVERTISING, false, m_bondMngr.getNativeBondingStateOverrides(), DISCONNECTED, true);
	}

	double getTimeSinceLastDiscovery()
	{
		return m_timeSinceLastDiscovery;
	}

	private void onDiscovered_private(List<UUID> advertisedServices_nullable, final int rssi, byte[] scanRecord_nullable)
	{
		m_lastDiscoveryTime = EpochTime.now();
		m_timeSinceLastDiscovery = 0.0;
		updateRssi(rssi);
		m_advertisedServices = advertisedServices_nullable == null || advertisedServices_nullable.size() == 0 ? m_advertisedServices : advertisedServices_nullable;
		m_scanRecord = scanRecord_nullable != null ? scanRecord_nullable : m_scanRecord;
	}

	void updateRssi(final int rssi)
	{
		m_rssi = rssi;
	}

	void update(double timeStep)
	{
		m_timeSinceLastDiscovery += timeStep;

		m_pollMngr.update(timeStep);
		m_txnMngr.update(timeStep);
		m_reconnectMngr_longTerm.update(timeStep);
		m_reconnectMngr_shortTerm.update(timeStep);
		m_rssiPollMngr.update(timeStep);
	}

	void bond_justAddTheTask(E_TransactionLockBehavior lockBehavior)
	{
		m_queue.add(new P_Task_Bond(this, /*explicit=*/true, /*partOfConnection=*/false, m_taskStateListener, lockBehavior));
	}

	void unbond_justAddTheTask()
	{
		unbond_justAddTheTask(null);
	}

	void unbond_justAddTheTask(final PE_TaskPriority priority_nullable)
	{
		m_queue.add(new P_Task_Unbond(this, m_taskStateListener, priority_nullable));
	}

	void unbond_internal(final PE_TaskPriority priority_nullable, final BondListener.Status status)
	{
		unbond_justAddTheTask(priority_nullable);

		final boolean wasBonding = is(BONDING);

		stateTracker_updateBoth(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_BondManager.OVERRIDE_UNBONDED_STATES);

		if (wasBonding)
		{
			m_bondMngr.invokeCallback(status, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);
		}
	}

	private ConnectionFailListener.ConnectionFailEvent connect_earlyOut()
	{
		if (isNull())
		{
			final ConnectionFailListener.ConnectionFailEvent info = ConnectionFailListener.ConnectionFailEvent.DUMMY(this, Status.NULL_DEVICE);

			m_connectionFailMngr.invokeCallback(info);

			return info;
		}

		return null;
	}

	void attemptReconnect()
	{
		if (connect_earlyOut() != null )  return;

		m_lastConnectOrDisconnectWasUserExplicit = true;

		if (isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
		{
			final ConnectionFailListener.ConnectionFailEvent info = ConnectionFailListener.ConnectionFailEvent.DUMMY(this, Status.ALREADY_CONNECTING_OR_CONNECTED);

			m_connectionFailMngr.invokeCallback(info);

			return;
		}

		connect_private(m_txnMngr.m_authTxn, m_txnMngr.m_initTxn, /*isReconnect=*/true);
	}

	private void connect_private(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, final boolean isReconnect)
	{
		if (is_internal(INITIALIZED))
		{
			getManager().ASSERT(false, "Device is initialized but not connected!");

			return;
		}

		m_txnMngr.onConnect(authenticationTxn, initTxn);

		final Object[] extraBondingStates;

		if( is(UNBONDED) )
		{
			final boolean tryBondingWhileDisconnected_manageOnDisk = BleDeviceConfig.bool(conf_device().tryBondingWhileDisconnected_manageOnDisk, conf_mngr().tryBondingWhileDisconnected_manageOnDisk);
			final boolean doPreBond = getManager().m_diskOptionsMngr.loadNeedsBonding(getMacAddress(), tryBondingWhileDisconnected_manageOnDisk);

			if( doPreBond )
			{
				bond_justAddTheTask(E_TransactionLockBehavior.PASSES);

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
		if( !/*still*/is_internal(CONNECTING_OVERALL) )
		{
			return;
		}

		m_queue.add(new P_Task_Connect(this, m_taskStateListener));

		onConnecting(/* definitelyExplicit= */true, isReconnect, extraBondingStates, /*bleConnect=*/true);
	}

	void onConnecting(boolean definitelyExplicit, boolean isReconnect, final Object[] extraBondingStates, final boolean bleConnect)
	{
		m_lastConnectOrDisconnectWasUserExplicit = definitelyExplicit;

		if (bleConnect && is_internal(/* already */CONNECTING))
		{
			P_Task_Connect task = getTaskQueue().getCurrent(P_Task_Connect.class, this);
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

			if( stateTracker() != stateTracker_main() )
			{
				stateTracker_main().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNBONDED, stateTracker().is(UNBONDED), BONDING, stateTracker().is(BONDING), BONDED, stateTracker().is(BONDED));
			}
		}
	}

	void onNativeConnect(boolean explicit)
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
			String message = "nativelyConnected=" + m_logger.gattConn(m_nativeWrapper.getConnectionState()) + " gatt==" + m_nativeWrapper.getGatt();
			// getManager().ASSERT(false, message);
			getManager().ASSERT(m_nativeWrapper.isNativelyConnected(), message);

			return;
		}

		getManager().ASSERT(m_nativeWrapper.getGatt() != null);

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
			getManager().onDiscovered_fromRogueAutoConnect(this, /*newlyDiscovered=*/true, m_advertisedServices, getScanRecord(), getRssi());
		}

		//--- DRK > Some trapdoor logic for bad android ble bug.
		int nativeBondState = m_nativeWrapper.getNativeBondState();
		if (nativeBondState == BluetoothDevice.BOND_BONDED)
		{
			//--- DRK > Trying to catch fringe condition here of stack lying to
			// us about bonded state.
			//--- This is not about finding a logic error in my code.
			getManager().ASSERT(getManager().getNative().getAdapter().getBondedDevices().contains(m_nativeWrapper.getDevice()));
		}
		m_logger.d(m_logger.gattBondState(m_nativeWrapper.getNativeBondState()));

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

		m_serviceMngr.clear();
		m_queue.add(new P_Task_DiscoverServices(this, m_taskStateListener));

		//--- DRK > We check up top, but check again here cause we might have been disconnected on another thread in the mean time.
		//--- Even without this check the library should still be in a goodish state. Might send some weird state
		//--- callbacks to the app but eventually things settle down and we're good again.
		if (m_nativeWrapper.isNativelyConnected())
		{
			stateTracker().update(lastConnectDisconnectIntent(), BluetoothGatt.GATT_SUCCESS, extraFlags, DISCOVERING_SERVICES, true);
		}
	}

	void onNativeConnectFail(PE_TaskState state, int gattStatus, AutoConnectUsage autoConnectUsage)
	{
		m_nativeWrapper.closeGattIfNeeded(/* disconnectAlso= */true);

		if (state == PE_TaskState.SOFTLY_CANCELLED || state == PE_TaskState.NO_OP)	return;

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
		final ConnectionFailListener.Status connectionFailStatus = ConnectionFailListener.Status.NATIVE_CONNECTION_FAILED;

		if (isAny_internal(CONNECTED, CONNECTING, CONNECTING_OVERALL))
		{
			setStateToDisconnected(attemptingReconnect, E_Intent.UNINTENTIONAL, gattStatus, /*forceMainStateTracker=*/false, P_BondManager.OVERRIDE_EMPTY_STATES);
		}

		m_txnMngr.cancelAllTransactions();

		if (wasConnecting)
		{
			ConnectionFailListener.Timing timing = state == PE_TaskState.FAILED_IMMEDIATELY ? ConnectionFailListener.Timing.IMMEDIATELY : ConnectionFailListener.Timing.EVENTUALLY;

			if (state == PE_TaskState.TIMED_OUT)
			{
				timing = ConnectionFailListener.Timing.TIMED_OUT;
			}

			Please.PE_Please retry = m_connectionFailMngr.onConnectionFailed(connectionFailStatus, timing, attemptingReconnect, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, highestState, autoConnectUsage, NULL_READWRITE_EVENT());

			if (!attemptingReconnect && retry == Please.PE_Please.RETRY_WITH_AUTOCONNECT_TRUE)
			{
				m_useAutoConnect = true;
			}
			else if (!attemptingReconnect && retry == Please.PE_Please.RETRY_WITH_AUTOCONNECT_FALSE)
			{
				m_useAutoConnect = false;
			}
			else
			{
				m_useAutoConnect = m_alwaysUseAutoConnect;
			}
		}
	}

	void onServicesDiscovered()
	{
		m_serviceMngr.clear();
		m_serviceMngr.loadDiscoveredServices();

		m_txnMngr.runAuthOrInitTxnIfNeeded(BluetoothGatt.GATT_SUCCESS, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
	}

	void onFullyInitialized(final int gattStatus, Object... extraFlags)
	{
		m_reconnectMngr_longTerm.stop();
		m_reconnectMngr_shortTerm.stop();
		m_connectionFailMngr.onFullyInitialized();

		//--- DRK > Saving last disconnect as unintentional here in case for some
		//--- reason app is hard killed or something and we never get a disconnect callback.
		final boolean hitDisk = BleDeviceConfig.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
		getManager().m_diskOptionsMngr.saveLastDisconnect(getMacAddress(), State.ChangeIntent.UNINTENTIONAL, hitDisk);

		stateTracker().update(lastConnectDisconnectIntent(), gattStatus, extraFlags, RECONNECTING_LONG_TERM, false, CONNECTING_OVERALL, false, AUTHENTICATING, false, AUTHENTICATED, true, INITIALIZING, false, INITIALIZED, true);

		stateTracker_main().remove(BleDeviceState.RECONNECTING_SHORT_TERM, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	private void setStateToDisconnected(final boolean attemptingReconnect_longTerm, final E_Intent intent, final int gattStatus, final boolean forceMainStateTracker, final Object[] overrideBondingStates)
	{
		//--- DRK > Device probably wasn't advertising while connected so here we reset the timer to keep
		//--- it from being immediately undiscovered after disconnection.
		m_timeSinceLastDiscovery = 0.0;

		m_serviceMngr.clear();
		m_txnMngr.clearQueueLock();

		final P_DeviceStateTracker tracker = forceMainStateTracker ? stateTracker_main() : stateTracker();

		tracker.set
		(
				intent,
				gattStatus,
				DISCOVERED, true,
				DISCONNECTED, true,
				BONDING, m_nativeWrapper.isNativelyBonding(),
				BONDED, m_nativeWrapper.isNativelyBonded(),
				UNBONDED, m_nativeWrapper.isNativelyUnbonded(),
				RECONNECTING_LONG_TERM, attemptingReconnect_longTerm,
				ADVERTISING, !attemptingReconnect_longTerm && m_origin_latest == BleDeviceOrigin.FROM_DISCOVERY,

				overrideBondingStates
		);

		if( tracker != stateTracker_main() )
		{
			stateTracker_main().update
			(
				intent,
				gattStatus,
				BONDING,					tracker.is(BONDING),
				BONDED,						tracker.is(BONDED),
				UNBONDED,					tracker.is(UNBONDED)
			);
		}
	}

	void disconnectWithReason(ConnectionFailListener.Status connectionFailReasonIfConnecting, Timing timing, int gattStatus, int bondFailReason, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		disconnectWithReason(null, connectionFailReasonIfConnecting, timing, gattStatus, bondFailReason, txnFailReason);
	}

	void disconnectWithReason(PE_TaskPriority disconnectPriority_nullable, ConnectionFailListener.Status connectionFailReasonIfConnecting, Timing timing, int gattStatus, int bondFailReason, ReadWriteListener.ReadWriteEvent txnFailReason)
	{
		if (isNull())  return;

		final boolean cancelled = connectionFailReasonIfConnecting != null && connectionFailReasonIfConnecting.wasCancelled();
		final boolean explicit = connectionFailReasonIfConnecting != null && connectionFailReasonIfConnecting.wasExplicit();
		final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

		if( explicit )
		{
			m_reconnectMngr_shortTerm.stop();
		}

		if (cancelled)
		{
			m_useAutoConnect = m_alwaysUseAutoConnect;

			m_connectionFailMngr.onExplicitDisconnect();
		}

		final boolean wasConnecting = is_internal(CONNECTING_OVERALL);
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

			if (isAny_internal(CONNECTED, CONNECTING_OVERALL, INITIALIZED))
			{
				m_queue.add(new P_Task_Disconnect(this, m_taskStateListener, /*explicit=*/true, disconnectPriority_nullable, taskIsCancellable, saveLastDisconnectAfterTaskCompletes));

				m_queue.clearQueueOf(PA_Task_RequiresConnection.class, this);
			}

			final Object[] overrideBondingStates = m_bondMngr.getOverrideBondStatesForDisconnect(connectionFailReasonIfConnecting);
			final boolean forceMainStateTracker = explicit;

			setStateToDisconnected(attemptingReconnect_longTerm, intent, gattStatus, forceMainStateTracker, overrideBondingStates);

			m_txnMngr.cancelAllTransactions();
			// m_txnMngr.clearAllTxns();

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

		if (wasConnecting)
		{
			if (getManager().ASSERT(connectionFailReasonIfConnecting != null))
			{
				m_connectionFailMngr.onConnectionFailed(connectionFailReasonIfConnecting, timing, attemptingReconnect_longTerm, gattStatus, bondFailReason, highestState, AutoConnectUsage.NOT_APPLICABLE, txnFailReason);
			}
		}
	}

	boolean lastDisconnectWasBecauseOfBleTurnOff()
	{
		return m_lastDisconnectWasBecauseOfBleTurnOff;
	}

	private void saveLastDisconnect(final boolean explicit)
	{
		if( !is(INITIALIZED) )  return;

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

	void onNativeDisconnect(final boolean wasExplicit, final int gattStatus, final boolean attemptShortTermReconnect, final boolean saveLastDisconnect)
	{
		if (!wasExplicit && !attemptShortTermReconnect)
		{
			//--- DRK > Just here so it's easy to filter out in logs.
			m_logger.w("Disconnected Implicitly and attemptShortTermReconnect="+attemptShortTermReconnect);
		}

		m_lastDisconnectWasBecauseOfBleTurnOff = getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF);
		m_lastConnectOrDisconnectWasUserExplicit = wasExplicit;

		final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

		if( saveLastDisconnect )
		{
			saveLastDisconnect(wasExplicit);
		}

		m_pollMngr.resetNotifyStates();

		if( attemptShortTermReconnect )
		{
			m_nativeWrapper.closeGattIfNeeded(/* disconnectAlso= */false);
		}

		final int overrideOrdinal = getManager().getTaskQueue().getCurrentOrdinal();

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

		final boolean isDisconnectedAfterReconnectingShortTermStateCallback = is(DISCONNECTED);
		final boolean isConnectingBle = is(CONNECTING);
		final boolean ignoreKindOf = isConnectingBle && wasExplicit;
		final boolean cancelTasks;

		if( !ignoreKindOf )
		{
			if (isDisconnectedAfterReconnectingShortTermStateCallback || wasExplicit)
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

		if( ignoreKindOf )
		{
			//--- DRK > Shouldn't be possible to get here for now, just being future-proof.
			if( cancelTasks )
			{
				softlyCancelTasks(overrideOrdinal);
			}

			m_txnMngr.cancelAllTransactions();

			return;
		}

		// BEGIN CALLBACKS TO USER

		final E_Intent intent = wasExplicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
		setStateToDisconnected(isAttemptingReconnect_longTerm, intent, gattStatus, /*forceMainStateTracker=*/attemptShortTermReconnect == false, P_BondManager.OVERRIDE_EMPTY_STATES);

		//--- DRK > Technically user could have called connect() in callbacks above....bad form but we need to account for it.
		final boolean isConnectingOverall_1 = is_internal(CONNECTING_OVERALL);
		final boolean isStillAttemptingReconnect_longTerm = is_internal(RECONNECTING_LONG_TERM);
		final ConnectionFailListener.Status connectionFailReason_nullable;
		if (!m_reconnectMngr_shortTerm.isRunning() && wasConnectingOverall)
		{
			if (getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF))
			{
				connectionFailReason_nullable = ConnectionFailListener.Status.BLE_TURNING_OFF;
			}
			else
			{
				connectionFailReason_nullable = ConnectionFailListener.Status.ROGUE_DISCONNECT;
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
		if( cancelTasks )
		{
			softlyCancelTasks(overrideOrdinal);
		}

		final Please.PE_Please retrying;
		if (!isConnectingOverall_1 && !m_reconnectMngr_shortTerm.isRunning())
		{
			if (connectionFailReason_nullable != null)
			{
				retrying = m_connectionFailMngr.onConnectionFailed(connectionFailReason_nullable, Timing.NOT_APPLICABLE, isStillAttemptingReconnect_longTerm, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, highestState, AutoConnectUsage.NOT_APPLICABLE, NULL_READWRITE_EVENT());
			}
			else
			{
				retrying = Please.PE_Please.DO_NOT_RETRY;
			}
		}
		else
		{
			retrying = Please.PE_Please.DO_NOT_RETRY;
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
		if( is(DISCONNECTED) && !is(RECONNECTING_LONG_TERM) && m_reconnectMngr_longTerm.isRunning() == false && m_reconnectMngr_shortTerm.isRunning() == false )
		{
			if( m_nativeWrapper.isNativelyConnecting() ||  m_nativeWrapper.isNativelyConnected() )
			{
				m_queue.add(new P_Task_Disconnect(this, m_taskStateListener, /*explicit=*/false, null, /*cancellable=*/true));
			}
		}

		//--- DRK > Not actually entirely sure how, it may be legitimate, but a connect task can still be
		//--- hanging out in the queue at this point, so we just make sure to clear the queue as a failsafe.
		//--- TODO: Understand the conditions under which a connect task can still be queued...might be a bug upstream.
		if (!isConnectingOverall_2 && retrying == Please.PE_Please.DO_NOT_RETRY)
		{
			m_queue.clearQueueOf(P_Task_Connect.class, this);
		}
	}

	private void softlyCancelTasks(final int overrideOrdinal)
	{
		m_dummyDisconnectTask.setOverrideOrdinal(overrideOrdinal);
		m_queue.softlyCancelTasks(m_dummyDisconnectTask);
		m_queue.clearQueueOf(PA_Task_RequiresConnection.class, this);
	}

	private void stopPoll_private(final UUID serviceUuid, final UUID characteristicUuid, final Double interval, final ReadWriteListener listener)
	{
		m_pollMngr.stopPoll(serviceUuid, characteristicUuid, interval, listener, /* usingNotify= */false);
	}

	ReadWriteListener.ReadWriteEvent read_internal(final UUID serviceUuid, final UUID characteristicUuid, final Type type, final ReadWriteListener listener)
	{
		final ReadWriteEvent earlyOutResult = m_serviceMngr.getEarlyOutEvent(serviceUuid, characteristicUuid, EMPTY_FUTURE_DATA, type, ReadWriteListener.Target.CHARACTERISTIC);

		if (earlyOutResult != null)
		{
			invokeReadWriteCallback(listener, earlyOutResult);

			return earlyOutResult;
		}

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		final boolean requiresBonding = m_bondMngr.bondIfNeeded(characteristic, BondFilter.CharacteristicEventType.READ);

		m_queue.add(new P_Task_Read(this, characteristic, type, requiresBonding, listener, m_txnMngr.getCurrent(), getOverrideReadWritePriority()));

		return NULL_READWRITE_EVENT();
	}

	ReadWriteListener.ReadWriteEvent write_internal(final UUID serviceUuid, final UUID characteristicUuid, final FutureData data, final P_WrappingReadWriteListener listener)
	{
		final ReadWriteEvent earlyOutResult = m_serviceMngr.getEarlyOutEvent(serviceUuid, characteristicUuid, data, Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC);

		if (earlyOutResult != null)
		{
			invokeReadWriteCallback(listener, earlyOutResult);

			return earlyOutResult;
		}

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		final boolean requiresBonding = m_bondMngr.bondIfNeeded(characteristic, BondFilter.CharacteristicEventType.WRITE);

		m_queue.add(new P_Task_Write(this, characteristic, data, requiresBonding, listener, m_txnMngr.getCurrent(), getOverrideReadWritePriority()));

		return NULL_READWRITE_EVENT();
	}

	private ReadWriteListener.ReadWriteEvent disableNotify_private(UUID serviceUuid, UUID characteristicUuid, Double forceReadTimeout, ReadWriteListener listener)
	{
		final ReadWriteEvent earlyOutResult = m_serviceMngr.getEarlyOutEvent(serviceUuid, characteristicUuid, EMPTY_FUTURE_DATA, Type.DISABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

		if (earlyOutResult != null)
		{
			invokeReadWriteCallback(listener, earlyOutResult);

			return earlyOutResult;
		}

		final P_Characteristic characteristic = m_serviceMngr.getCharacteristic(serviceUuid, characteristicUuid);

		if (characteristic != null && is(CONNECTED))
		{
			P_WrappingReadWriteListener wrappingListener = new P_WrappingReadWriteListener(listener, getManager().m_mainThreadHandler, getManager().m_config.postCallbacksToMainThread);
			m_queue.add(new P_Task_ToggleNotify(this, characteristic, /* enable= */false, wrappingListener));
		}

		m_pollMngr.stopPoll(serviceUuid, characteristicUuid, forceReadTimeout, listener, /* usingNotify= */true);

		return NULL_READWRITE_EVENT();
	}

	E_Intent lastConnectDisconnectIntent()
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

	private PE_TaskPriority getOverrideReadWritePriority()
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

	void invokeReadWriteCallback(final ReadWriteListener listener_nullable, final ReadWriteListener.ReadWriteEvent event)
	{
		if( event.wasSuccess() && event.isRead() && event.target() == ReadWriteListener.Target.CHARACTERISTIC )
		{
			final EpochTime timestamp = new EpochTime();
			final BleDeviceConfig.HistoricalDataLogFilter.Source source = event.type().toHistoricalDataSource();

			m_historicalDataMngr.add_single(event.charUuid(), event.data(), timestamp, source);
		}

		m_txnMngr.onReadWriteResult(event);

		if (listener_nullable != null)
		{
			listener_nullable.onEvent(event);
		}

		if (m_defaultReadWriteListener != null)
		{
			m_defaultReadWriteListener.onEvent(event);
		}

		if (getManager() != null && getManager().m_defaultReadWriteListener != null)
		{
			getManager().m_defaultReadWriteListener.onEvent(event);
		}

		m_txnMngr.onReadWriteResultCallbacksCalled();
	}

	ReadWriteListener.ReadWriteEvent NULL_READWRITE_EVENT()
	{
		if (m_nullReadWriteEvent != null)
		{
			return m_nullReadWriteEvent;
		}

		m_nullReadWriteEvent = ReadWriteListener.ReadWriteEvent.NULL(this);

		return m_nullReadWriteEvent;
	}

	ConnectionFailListener.ConnectionFailEvent NULL_CONNECTIONFAIL_INFO()
	{
		if (m_nullConnectionFailEvent != null)
		{
			return m_nullConnectionFailEvent;
		}

		m_nullConnectionFailEvent = ConnectionFailListener.ConnectionFailEvent.NULL(this);

		return m_nullConnectionFailEvent;
	}

	BondListener.BondEvent NULL_BOND_EVENT()
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
	@Override public boolean isNull()
	{
		return m_isNull;
	}

	/**
	 * Convenience method that casts {@link #appData} for you.
	 */
	public <T> T appData()
	{
		return (T) appData;
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getNext()
	{
		return getManager().getDevice_next(this);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice, BleDeviceState)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getNext(final BleDeviceState state)
	{
		return getManager().getDevice_next(this, state);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_next(BleDevice, Object...)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getNext(final Object ... query)
	{
		return getManager().getDevice_next(this, query);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getPrevious()
	{
		return getManager().getDevice_previous(this);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice, BleDeviceState)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getPrevious(final BleDeviceState state)
	{
		return getManager().getDevice_previous(this, state);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDevice_previous(BleDevice, Object...)}.
	 */
	public @Nullable(Prevalence.NEVER) BleDevice getPrevious(final Object ... query)
	{
		return getManager().getDevice_previous(this, query);
	}

	/**
	 * Convenience forwarding of {@link BleManager#getDeviceIndex(BleDevice)}.
	 */
	public int getIndex()
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
}
