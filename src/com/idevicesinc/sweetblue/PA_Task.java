package com.idevicesinc.sweetblue;

import android.os.Handler;

import com.idevicesinc.sweetblue.utils.Interval;


/**
 * 
 * 
 */
abstract class PA_Task
{
	static interface I_StateListener
	{
		void onStateChange(PA_Task task, PE_TaskState state);
	}
	
	static final double TIMEOUT_DEFAULT = BleManagerConfig.DEFAULT_TASK_TIMEOUT;
	static final double TIMEOUT_CONNECTION = BleManagerConfig.DEFAULT_TASK_TIMEOUT;
	
	private 	  BleDevice m_device; 
	private final BleManager m_manager;
	
	private double m_timeout;
	private double m_executionDelay = 0.0;
	
	private double m_resettableTimeExecuting = 0.0;
	private double m_totalTimeExecuting = 0.0;
	private double m_totalTimeArmedAndExecuting = 0.0;
	private double m_totalTimeQueuedAndArmedAndExecuting = 0.0;
	
	private double m_addedToQueueTime = -1.0;
	
	private final I_StateListener m_stateListener;
	
	private PE_TaskState m_state = null;
	
	private P_TaskQueue m_queue;
	private Handler m_executeHandler;
	
	private int m_maxRetries;
	private int m_retryCount;
	
	private int m_updateCount = 0;
	
	private long m_timeCreated;
	private long m_timeExecuted;
	
	private boolean m_softlyCancelled = false;
	
	protected final P_Logger m_logger;
	
	private final Runnable m_executeRunnable = new Runnable()
	{
		@Override public void run()
		{
			//--- DRK > Theoretically the task could be ended externally before
			//---		we actually make it here.
			if( m_state /*still*/== PE_TaskState.EXECUTING )
			{
				execute_wrapper();
			}
		}
	};
	
	public PA_Task(BleDevice device, I_StateListener listener)
	{
		this(device, listener, TIMEOUT_DEFAULT);
	}
	
	public PA_Task(BleDevice device, I_StateListener listener, double timeout)
	{
		this(device.getManager(), listener, timeout);
		
		m_device = device;
	}
	
	public PA_Task(BleManager manager, I_StateListener listener)
	{
		this(manager, listener, TIMEOUT_DEFAULT);
	}
	
	public PA_Task(BleManager manager, I_StateListener listener, double timeout)
	{
		m_device = null;
		m_manager = manager;
		m_maxRetries = 0;
		m_timeout = timeout;
		m_logger = m_manager.getLogger();
		m_timeCreated = System.currentTimeMillis();
		
		if( listener == null && this instanceof I_StateListener )
		{
			//--- DRK > Can't pass this pointer from subclass up through super(), otherwise that would be cleaner.
			m_stateListener = (I_StateListener) this;
		}
		else
		{
			m_stateListener = listener;
		}
	}
	
	void init()
	{
		setState(PE_TaskState.CREATED);
	}
	
	private void setState(PE_TaskState newState)
	{
		if( !m_manager.ASSERT(newState != m_state) )  return;
		
		m_state = newState;
		
		if( m_state.isEndingState() && m_logger.isEnabled() )
		{
			String logText = this.toString();
			if( m_queue != null )
			{
				logText += " - " + m_queue.getUpdateCount();
			}
			
			m_logger.i(logText);
		}
		
		if( m_stateListener != null )  m_stateListener.onStateChange(this, m_state);
	}
	
	PE_TaskState getState()
	{
		return m_state;
	}
	
	void onAddedToQueue(P_TaskQueue queue)
	{
		m_queue = queue;
		setState(PE_TaskState.QUEUED);
		m_retryCount = 0;
		m_updateCount = 0;
		m_addedToQueueTime = m_addedToQueueTime == -1.0 ? m_queue.getTime() : m_addedToQueueTime;
	}
	
	void resetTimeout(double newTimeout)
	{
		//--- DRK > Can be called upstream from different thread than the update loop,
		//---		so preventing clashes here with this.update method.
		synchronized (this)
		{
			m_timeout = newTimeout;
			m_resettableTimeExecuting = 0.0;
		}
	}
	
	protected void timeout()
	{
		m_queue.tryEndingTask(this, PE_TaskState.TIMED_OUT);
	}
	
	protected void redundant()
	{
		m_queue.tryEndingTask(this, PE_TaskState.REDUNDANT);
	}
	
	protected void succeed()
	{
		m_queue.tryEndingTask(this, PE_TaskState.SUCCEEDED);
	}
	
	protected void fail()
	{
		m_queue.tryEndingTask(this, PE_TaskState.FAILED);
	}
	
	protected void failImmediately()
	{
		m_queue.tryEndingTask(this, PE_TaskState.FAILED_IMMEDIATELY);
	}
	
	protected void noOp()
	{
		m_queue.tryEndingTask(this, PE_TaskState.NO_OP);
	}
	
	protected void selfInterrupt()
	{
		boolean wasExecuting = this.getState() == PE_TaskState.EXECUTING || this.getState() == PE_TaskState.ARMED;
		
		if( wasExecuting )
		{
			m_queue.tryEndingTask(this, PE_TaskState.INTERRUPTED);
			
			m_queue.add(this);
		}
	}
	
	protected void softlyCancel()
	{
		m_maxRetries = 0;
		m_queue.tryEndingTask(this, PE_TaskState.SOFTLY_CANCELLED);
	}
	
	protected void failWithoutRetry()
	{
		m_maxRetries = 0;
		fail();
	}

	void arm(Handler executeHandler)
	{
		setState(PE_TaskState.ARMED);
		
		m_executeHandler = executeHandler;
		m_totalTimeQueuedAndArmedAndExecuting = m_queue.getTime() - m_addedToQueueTime;
		m_totalTimeArmedAndExecuting = 0.0;
		m_totalTimeExecuting = 0.0;
		m_resettableTimeExecuting = 0.0;
		m_retryCount = 0;
		m_updateCount = 0;
	}
	
	protected boolean isExecutable()
	{
		return true;
	}
	
	private void execute_wrapper()
	{
		m_timeExecuted = System.currentTimeMillis();
		
		execute();
	}
	
	abstract void execute();
	
	void setEndingState(PE_TaskState endingState)
	{
		if( m_softlyCancelled && endingState == PE_TaskState.SUCCEEDED )
		{
			endingState = PE_TaskState.SOFTLY_CANCELLED;
		}
		
		if( !m_manager.ASSERT(endingState.isEndingState()) )  return;
		
		//--- DRK > Might be true for timeouts...overall just being defensive but not assertion-level defense.
		if( m_state == endingState )  return;
				
		if( !m_manager.ASSERT(!m_state.isEndingState()) )  return;

		setState(endingState);
	}
	
	void update_internal(double timeStep)
	{
		synchronized (this)
		{
			m_totalTimeArmedAndExecuting += timeStep;
			m_totalTimeQueuedAndArmedAndExecuting += timeStep;
			m_updateCount++;
			
			if( m_totalTimeArmedAndExecuting >= m_executionDelay )
			{
				if( m_state == PE_TaskState.ARMED )
				{
					//--- DRK > Force at least one time step between becoming armed and executing.
					//---		TODO: Possibly gate this with an optional time requirement.
					//---				For example we might want to give the ble stack time to "settle" after
					//---				a heavy operation...yes i know how ridiculous that sounds...
					if( m_updateCount > 1 )
					{
						//--- DRK > Debug code to delay reads and writes and such.
	//					if( m_device != null && m_device.is(E_DeviceState.INITIALIZED) )
	//					{
	//						if( m_totalTimeArmedAndExecuting < 2.0 )
	//						{
	//							return;
	//						}
	//					}
						
						if( m_softlyCancelled )
						{
							softlyCancel();
							
							return;
						}
						
						if( isExecutable() )
						{
							setState(PE_TaskState.EXECUTING);
							
							if( executeOnSeperateThread() )
							{
								//--- DRK > Executing on separate thread in case this method is called on the main thread,
								//---		or a synchronization block in BtTaskQueue indirectly blocks the main thread.
								//---		Some things like a failing scan call can block its thread for several seconds.
								m_executeHandler.post(m_executeRunnable);
							}
							else
							{
								execute_wrapper();
							}
							
							return;
						}
						else
						{
							failWithoutRetry();
							
							return;
						}
					}
				}
				else if( m_state == PE_TaskState.EXECUTING )
				{
					m_resettableTimeExecuting += timeStep;
					m_totalTimeExecuting += timeStep;
					
					if( m_timeout != Interval.INFINITE.seconds )
					{
						if( m_resettableTimeExecuting >= m_timeout )
						{
							timeout();
							
							return;
						}
					}
				}
			}
			
			this.update(timeStep);
		}
	}
	
	protected void update(double timeStep){}
	
	public double getTotalTimeExecuting()
	{
		return (System.currentTimeMillis() - m_timeExecuted)/1000.0;
	}
	
	public double getTotalTime()
	{
		return (System.currentTimeMillis() - m_timeCreated)/1000.0;
	}
	
	public BleDevice getDevice()
	{
		return m_device;
	}
	
	public BleManager getManager()
	{
		return m_manager;
	}
	
	public double getTimeout()
	{
		return m_timeout;
	}
	
	protected P_TaskQueue getQueue()
	{
		return m_queue;
	}
	
	public abstract PE_TaskPriority getPriority();
	
	public boolean isMoreImportantThan(PA_Task task)
	{
		return isMoreImportantThan_default(task);
	}
	
	/**
	 * Default implementation to call by subsubclasses if they want to skip their immediate parent's implementation.
	 */
	protected boolean isMoreImportantThan_default(PA_Task task)
	{
		return this.getPriority().ordinal() > task.getPriority().ordinal();
	}
	
	public boolean isInterruptableBy(PA_Task task)
	{
		return false;
	}
	
	public boolean isCancellableBy(PA_Task task)
	{
		return false;
	}
	
	protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		return false;
	}
	
	protected String getToStringAddition()
	{
		return null;
	}
	
	@Override public String toString()
	{
		String name = this.getClass().getSimpleName();
		name = name.replace("BtTask_", "");
		
		String deviceEntry = getDevice() != null ? " " + getDevice().getName_debug(): "";
		String addition = getToStringAddition() != null ? " " + getToStringAddition() : "";
		return name + "(" + m_state.name() + deviceEntry + addition + ")";
	}
	
	public void setSoftlyCancelled()
	{
		m_softlyCancelled = true;
	}
	
	public boolean executeOnSeperateThread()
	{
		return false;
	}
	
	public boolean isExplicit()
	{
		return false;
	}
}
