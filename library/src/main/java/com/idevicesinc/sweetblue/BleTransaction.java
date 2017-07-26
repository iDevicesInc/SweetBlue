package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * Abstract base class for transactions passed to various methods of {@link BleDevice}. Transactions provide a convenient way
 * to encapsulate a series of reads and writes for things like authentication handshakes, firmware updates, etc. You optionally
 * provide subclass instances to the various overloads of {@link BleDevice#connect()}. Normally in your {@link #start(BleDevice)}
 * method you then do some reads/writes and call {@link #succeed()} or {@link #fail()} depending on the {@link Status} returned.
 * <br><br>
 * NOTE: Nested subclasses here are only meant for tagging to enforce type-correctness and don't yet provide any differing contracts or implementations.
 * 
 * @see BleDevice#performOta(BleTransaction.Ota)
 * @see BleDevice#connect(BleTransaction.Auth)
 * @see BleDevice#connect(BleTransaction.Init)
 * @see BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)
 * @see BleDevice#performTransaction(BleTransaction)
 */
public abstract class BleTransaction
{
	/**
	 * Tagging subclass to force type-discrepancy for various {@link BleDevice#connect()} overloads.
	 */
	public abstract static class Init extends BleTransaction{}
	
	/**
	 * Tagging subclass to force type-discrepancy for various {@link BleDevice#connect()} overloads.
	 */
	public abstract static class Auth extends BleTransaction{}
	
	/**
	 * Tagging subclass to force type-correctness for {@link BleDevice#performOta(BleTransaction.Ota)}.
	 */
	public abstract static class Ota extends BleTransaction{}
	
	/**
	 * Values are passed to {@link BleTransaction#onEnd(BleDevice, EndReason)}.
	 */
	protected static enum EndReason
	{
		/**
		 * {@link BleTransaction#succeed()} was called.
		 */
		SUCCEEDED,
		
		/**
		 * The transaction's {@link BleDevice} became {@link BleDeviceState#DISCONNECTED}
		 * or/and {@link BleManager} went {@link BleManagerState#OFF}.
		 */
		CANCELLED,
		
		/**
		 * {@link BleTransaction#fail()} was called.
		 */
		FAILED;
	}
	
	private final double m_timeout;
	private double m_timeTracker;
	private boolean m_isRunning;
	private BleDevice m_device = null;
	private PI_EndListener m_listener;
	
	public BleTransaction()
	{
		m_timeout = 0.0;
	}
	
	void init(BleDevice device, PI_EndListener listener )
	{
		if( m_device != null )
		{
			if( m_device != device )
			{
				throw new Error("Cannot currently reuse transactions across devices.");
			}
		}
		
		m_device = device;
		m_listener = listener;
	}
	
	void deinit()
	{
		//--- DRK > Intentionally not nulling out m_device here.
		m_listener = null;
	}
	
	/**
	 * Returns the device this transaction is running on.
	 */
	public BleDevice getDevice()
	{
		return m_device;
	}
	
	/**
	 * Implement this method to kick off your transaction. Usually you kick off some reads/writes inside
	 * your override and call {@link #succeed()} or {@link #fail()} depending on how things went.
	 */
	protected abstract void start(BleDevice device);
	
	/**
	 * Called when a transaction ends, either due to the transaction itself finishing itself
	 * through {@link #fail()} or {@link #succeed()}, or from the library implicitly ending
	 * the transaction, for example if {@link #getDevice()} becomes {@link BleDeviceState#DISCONNECTED}.
	 * 
	 * Override this method to wrap up any loose ends or notify UI or what have you.
	 */
	protected void onEnd(BleDevice device, EndReason reason){}
	
	/**
	 * Optional convenience method to override if you want to do periodic updates or time-based calculations.
	 */
	protected void update(double timeStep){}
	
	/**
	 * Returns whether the transaction is currently running.
	 */
	public boolean isRunning()
	{
		return m_isRunning;
	}
	
	void start_internal()
	{
		m_isRunning = true;
		m_timeTracker = 0.0;
		
		start(m_device);
	}
	
	private boolean end(final EndReason reason, final BleDevice.ReadWriteListener.ReadWriteEvent failReason)
	{
		if( !m_isRunning )
		{
			//--- DRK > Can be due to a legitimate race condition, so warning might be a little much.
//				m_device.getManager().getLogger().w("Transaction is already ended!");

			return false;
		}

		m_device.getManager().getLogger().i("transaction " + reason.name());

		m_isRunning = false;

		if( m_listener != null )
		{
			m_listener.onTransactionEnd(this, reason, failReason);
		}

		if( m_device.getManager().m_config.postCallbacksToMainThread && !Utils.isOnMainThread() )
		{
			m_device.getManager().getPostManager().postToMain(new Runnable()
			{
				@Override public void run()
				{
					onEnd(m_device, reason);
				}
			});
		}
		else
		{
			onEnd(m_device, reason);
		}
		
		return true;
	}
	
	protected final void cancel()
	{
		end(EndReason.CANCELLED, m_device.NULL_READWRITE_EVENT());
	}
	
	/**
	 * Call this from subclasses to indicate that the transaction has failed. Usually you call this in your
	 * {@link ReadWriteListener#onEvent(Event)} when {@link Status} is something other than {@link Status#SUCCESS}. If you do so,
	 * {@link BleDevice.ConnectionFailListener.ConnectionFailEvent#txnFailReason()} will be set.
	 * 
	 * @return <code>false</code> if the transaction wasn't running to begin with.
	 */
	protected final boolean fail()
	{
		final BleDevice.ReadWriteListener.ReadWriteEvent failReason = m_device.m_txnMngr.m_failReason;
		
		return this.end(EndReason.FAILED, failReason);
	}
		
	/**
	 * Call this from subclasses to indicate that the transaction has succeeded.
	 * 
	 * @return {@link Boolean#FALSE} if the transaction wasn't running to begin with.
	 */
	protected final boolean succeed()
	{
		return end(EndReason.SUCCEEDED, m_device.NULL_READWRITE_EVENT());
	}
	
	void update_internal(double timeStep)
	{
		m_timeTracker += timeStep;
		
		if( m_timeout > 0.0 )
		{
			if( m_timeTracker >= m_timeout )
			{
			}
		}
		
		update(timeStep);
	}
	
	/**
	 * Returns the total time that this transaction has been running. You can use this in {@link #update(double)}
	 * for example to {@link #fail()} or {@link #succeed()} a transaction that has taken longer than a certain
	 * amount of time.
	 * 
	 * @see BleTransaction#update(double)
	 */
	protected double getTime()
	{
		return m_timeTracker;
	}
	
	/**
	 * Default is {@link Boolean#FALSE}. Optionally override if you want your transaction's reads/writes to execute "atomically".
	 * This means that if you're connected to multiple devices only the reads/writes of this transaction's device
	 * will be executed until this transaction is finished.
	 */
	protected boolean needsAtomicity()
	{
		return false;
	}
}
