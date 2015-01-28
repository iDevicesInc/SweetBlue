package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * Abstract base class for transactions passed to various methods of {@link BleDevice}. Transactions provide a convenient way
 * to encapsulate a series of reads and writes for things like authentication handshakes, firmware updates, etc. You optionally
 * provide subclass instances to the various overloads of {@link BleDevice#connect()}. Normally in your {@link #start(BleDevice)}
 * method you then do some reads/writes and call {@link #succeed()} or {@link #fail()} depending on the {@link Status} returned.
 * 
 * @see BleDevice#updateFirmware(BleTransaction)
 * @see BleDevice#connectAndAuthenticate(BleTransaction)
 * @see BleDevice#connectAndInitialize(BleTransaction)
 * @see BleDevice#connect(BleTransaction, BleTransaction)
 */
public abstract class BleTransaction
{
	/**
	 * Values are passed to {@link BleTransaction#onEnd(BleDevice, EndReason)}.
	 * 
	 * 
	 */
	protected static enum EndReason
	{
		/**
		 * {@link BleTransaction#succeed()} was called.
		 */
		SUCCEEDED,
		
		/**
		 * The transaction's {@link BleDevice} became {@link BleDeviceState#DISCONNECTED}
		 * or/and {@link BleManager} went {@link BleState#OFF}.
		 */
		CANCELLED,
		
		/**
		 * {@link BleTransaction#fail()} was called.
		 */
		FAILED;
	}
	
	static interface PI_EndListener
	{
		void onTransactionEnd(BleTransaction txn, EndReason reason);
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
	
	private boolean end(final EndReason reason)
	{
		synchronized (m_device.m_threadLock )
		{
			if( !m_isRunning )
			{
				//--- DRK > Can be due to a legitimate race condition, so warning might be a little much.
//				m_device.getManager().getLogger().w("Transaction is already ended!");
				
				return false;
			}
		
			m_device.getManager().getLogger().i(reason.name());
			
			m_isRunning = false;
			
			if( m_listener != null )
			{
				m_listener.onTransactionEnd(this, reason);
			}
			
			if( m_device.getManager().m_config.postCallbacksToMainThread && !Utils.isOnMainThread() )
			{
				m_device.getManager().m_mainThreadHandler.post(new Runnable()
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
		}
		
		return true;
	}
	
	final void cancel()
	{
		end(EndReason.CANCELLED);
	}
	
	/**
	 * Call this from subclasses to indicate that the transaction has failed. Usually you call this in your
	 * {@link BleDevice.ReadWriteListener#onResult(BleDevice.ReadWriteListener.Result)}
	 * when {@link Status} is something other than {@link Status#SUCCESS}.
	 * 
	 * @return {@link Boolean#FALSE} if the transaction wasn't running to begin with.
	 */
	protected final boolean fail()
	{
		return end(EndReason.FAILED);
	}
	
	/**
	 * Call this from subclasses to indicate that the transaction has succeeded.
	 * 
	 * @return {@link Boolean#FALSE} if the transaction wasn't running to begin with.
	 */
	protected final boolean succeed()
	{
		return end(EndReason.SUCCEEDED);
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
