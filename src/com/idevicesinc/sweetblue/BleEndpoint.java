package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.content.Context;

import com.idevicesinc.sweetblue.annotations.Immutable;

/**
 * Abstract base class for {@link BleDevice} and {@link BleServer}, mostly just to statically tie their APIs together
 * wherever possible. That is, not much actual shared implementation exists in this class as of this writing.
 */
public abstract class BleEndpoint
{
	/**
	 * Base interface for {@link BleDevice.ConnectionFailListener} and {@link BleServer.ConnectionFailListener}.
	 */
	@com.idevicesinc.sweetblue.annotations.Lambda
	public static interface ConnectionFailListener
	{
		/**
		 * Describes usage of the <code>autoConnect</code> parameter for either {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
		 * or {@link BluetoothGattServer#connect(BluetoothDevice, boolean)}.
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
			 * Usage is not applicable.
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
		 * Return value for {@link BleDevice.ConnectionFailListener#onEvent(BleDevice.ConnectionFailListener.ConnectionFailEvent)}
		 * and {@link BleServer.ConnectionFailListener#onEvent(BleServer.ConnectionFailListener.ConnectionFailEvent)}.
		 * Generally you will only return {@link #retry()} or {@link #doNotRetry()}, but there are more advanced options as well.
		 */
		@Immutable
		public static class Please
		{
			/*package*/ static final int PE_Please_NULL								= -1;
			/*package*/ static final int PE_Please_RETRY							=  0;
			/*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_TRUE		=  1;
			/*package*/ static final int PE_Please_RETRY_WITH_AUTOCONNECT_FALSE		=  2;
			/*package*/ static final int PE_Please_DO_NOT_RETRY						=  3;

			/*package*/ static final boolean isRetry(final int please__PE_Please)
			{
				return please__PE_Please != PE_Please_DO_NOT_RETRY && please__PE_Please != PE_Please_NULL;
			}

			private final int m_please__PE_Please;

			private Please(final int please__PE_Please)
			{
				m_please__PE_Please = please__PE_Please;
			}

			/*package*/ int/*__PE_Please*/ please()
			{
				return m_please__PE_Please;
			}

			/**
			 * Return this to retry the connection, continuing the connection fail retry loop. <code>autoConnect</code> passed to
			 * {@link BluetoothDevice#connectGatt(Context, boolean, android.bluetooth.BluetoothGattCallback)}
			 * will be false or true based on what has worked in the past, or on {@link BleDeviceConfig#alwaysUseAutoConnect}.
			 */
			public static Please retry()
			{
				return new Please(PE_Please_RETRY);
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
				return new Please(PE_Please_DO_NOT_RETRY);
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
				return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_TRUE);
			}

			/**
			 * Opposite of{@link #retryWithAutoConnectTrue()}.
			 */
			@com.idevicesinc.sweetblue.annotations.Advanced
			public static Please retryWithAutoConnectFalse()
			{
				return new Please(PE_Please_RETRY_WITH_AUTOCONNECT_FALSE);
			}

			/**
			 * Returns <code>true</code> for everything except {@link #doNotRetry()}.
			 */
			public boolean isRetry()
			{
				return isRetry(m_please__PE_Please);
			}
		}
	}
}
