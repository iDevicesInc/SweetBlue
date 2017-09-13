package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import android.os.Handler;
import android.os.Looper;

import com.idevicesinc.sweetblue.utils.Interval;


final class P_TaskQueue
{
	private final ArrayList<PA_Task> m_queue = new ArrayList<PA_Task>();
	private final AtomicReference<PA_Task> m_current;
	private long m_updateCount;
	private final P_Logger m_logger;
	private final BleManager m_mngr;
	private double m_time = 0.0;
	private double m_timeSinceEnding = 0.0;
	
	private Handler m_executeHandler = null;
	
	private int m_currentOrdinal;

	
	P_TaskQueue(BleManager mngr)
	{
		m_mngr = mngr;
		m_logger = mngr.getLogger();

		m_current = new AtomicReference<>(null);
		
		initHandler(); 
	}
	
	final int assignOrdinal()
	{
		final int toReturn = m_currentOrdinal;

		m_currentOrdinal++;

		return toReturn;
	}

	public final Handler getExecuteHandler()
	{
		return m_executeHandler;
	}

	final int getCurrentOrdinal()
	{
		return m_currentOrdinal;
	}

	public final PA_Task peek()
	{
		return m_queue.size() > 0 ? m_queue.get(0) : null;
	}

	private void initHandler()
	{
		final Thread thread = new Thread()
		{
			@Override public void run()
			{
				Looper.prepare();
				m_executeHandler = new Handler(Looper.myLooper());
				Looper.loop();
			}
		};

		thread.start();
	}

	private boolean tryCancellingCurrentTask(PA_Task newTask)
	{
		if( getCurrent() != null && getCurrent().isCancellableBy(newTask) )
		{
//			int soonestSpot = U_BtTaskQueue.findSoonestSpot(m_queue, newTask);

//			if( soonestSpot == 0 )
			{
				endCurrentTask(PE_TaskState.CANCELLED);
				addAtIndex(newTask, 0);

				return true;
			}
		}

		return false;
	}

	private boolean tryInterruptingCurrentTask(PA_Task newTask)
	{
		if( getCurrent() != null && getCurrent().isInterruptableBy(newTask) )
		{
//			int soonestSpot = U_BtTaskQueue.findSoonestSpot(m_queue, newTask);

//			if( soonestSpot == 0 )
			{
				PA_Task current_saved = getCurrent();
				endCurrentTask(PE_TaskState.INTERRUPTED);
				addAtIndex(newTask, 0);
				addAtIndex(current_saved, 1);

				return true;
			}
		}

		return false;
	}

	private boolean tryInsertingIntoQueue(PA_Task newTask)
	{
		int soonestSpot = PU_TaskQueue.findSoonestSpot(m_queue, newTask);

		if( soonestSpot >= 0 )
		{
			addAtIndex(newTask, soonestSpot);

			return true;
		}

		return false;
	}

	private void addToBack(PA_Task task)
	{
		addAtIndex(task, -1);
	}
	
	public final void softlyCancelTasks(final PA_Task task)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				for( int i = 0; i < m_queue.size()-1; i++ )
				{
					PA_Task ithTask = m_queue.get(i);
					if( ithTask.isSoftlyCancellableBy(task) )
					{
						ithTask.attemptToSoftlyCancel(task);
					}
				}

				if( getCurrent() != null )
				{
					if( getCurrent().isSoftlyCancellableBy(task) )
					{
						getCurrent().attemptToSoftlyCancel(task);
					}
				}
			}
		});
	}
	
	private void addAtIndex(PA_Task task, int index)
	{
		if( index >= 0 )
		{
			m_queue.add(index, task);
		}
		else
		{
			m_queue.add(task);
			
			index = m_queue.size()-1;
		}

		task.assignDefaultOrdinal(this);
		
		softlyCancelTasks(task);
		
		task.onAddedToQueue(this);
		
		print();
	}

	public final void add(final PA_Task newTask)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				newTask.init();
				add_updateThread(newTask);
			}
		});
	}

	final void addNow(final PA_Task newTask)
	{
		if (!m_mngr.getPostManager().isOnSweetBlueThread())
		{
			throw new Error("Tried to add a task when not on the SweetBlue update thread!");
		}
		newTask.init();
		add_updateThread(newTask);
	}

	private void add_updateThread(final PA_Task newTask)
	{
		// Check the idle status to ensure the new task gets executed as soon as possible (rather than
		// waiting until the idle interval's next tick)
		m_mngr.checkIdleStatus();
		if( tryCancellingCurrentTask(newTask) )
		{
			if( getCurrent() == null )
			{
				dequeue();
			}
		}
		else if( tryInterruptingCurrentTask(newTask) ) {}
		else if( tryInsertingIntoQueue(newTask) ) {}
		else { addToBack(newTask); }

		//--- DRK > Commenting out because of possible race condition when immediate native stack errors recurse
		//---		back to front-end before front-end's state reflects the fact that the task (added to empty queue) is going on.
		//---		See start of service discovery for an example.
//		while( getCurrent() == null && m_queue.size() > 0 )
//		{
//			if( false == dequeue() )
//			{
//				break;
//			}
//		}
	}

	final double getTime()
	{
		return m_time;
	}

	public final boolean update(double timeStep, long currentTime)
	{
		boolean executingTask = false;

		m_time += timeStep;

		if (getCurrent() == null)
			m_timeSinceEnding += timeStep;

		if( m_executeHandler == null )
		{
			m_logger.d("Waiting for execute handler to initialize.");

			return executingTask;
		}

		if( m_current.get() == null )
		{
			executingTask = dequeue();
		}

		if( getCurrent() != null )
		{
			getCurrent().update_internal(timeStep, currentTime);
			executingTask = true;
		}

		m_updateCount++;

		return executingTask;
	}

	private boolean hasDelayTimePassed()
	{
		Interval delayTime = m_mngr.m_config.delayBetweenTasks;
		if (Interval.isDisabled(delayTime))
			return true;

		return m_timeSinceEnding >= delayTime.secs();
	}

	private synchronized boolean dequeue()
	{
		if ( !m_mngr.ASSERT(m_current.get() == null) )  return false;
		if ( m_queue.size() == 0 )  return false;
		if ( !hasDelayTimePassed() )	return false;

		for( int i = 0; i < m_queue.size(); i++ )
		{
			PA_Task newPotentialCurrent = m_queue.get(i);
			
			if( newPotentialCurrent.isArmable() )
			{
				m_queue.remove(i);
				m_current.set(newPotentialCurrent);
				newPotentialCurrent.arm();
				if (!newPotentialCurrent.tryExecuting())
				{
					print();
				}
				return true;
			}
		}
		return false;
	}

	public final long getUpdateCount()
	{
		return m_updateCount;
	}

	public final PA_Task getCurrent()
	{
//		return m_pendingEndingStateForCurrentTask != null ? null : m_current;
		return m_current.get();
	}

	private boolean endCurrentTask(PE_TaskState endingState)
	{
		if( !m_mngr.ASSERT(endingState.isEndingState()) )	return false;
		if( getCurrent() == null ) 							return false;
//		if( m_pendingEndingStateForCurrentTask != null )	return false;
		
		PA_Task current_saved = m_current.get();
		m_current.set(null);
		m_timeSinceEnding = 0.0;
		current_saved.setEndingState(endingState);

		boolean printed = false;

		if( m_queue.size() > 0 && getCurrent() == null )
		{
			if( endingState.canGoToNextTaskImmediately() )
			{
				printed = dequeue();
			}
			else
			{
				//--- DRK > Posting to prevent potential stack overflow if queue is really big and all tasks are failing in a row.
				m_mngr.getPostManager().forcePostToUpdate(new Runnable()
				{
					@Override public void run()
					{
						if( m_queue.size() > 0 && getCurrent() == null )
						{
							dequeue();
						}
					}
				});
			}
		}

		if (!printed)
		{
			print();
		}
		
//		m_pendingEndingStateForCurrentTask = endingState;
		
		return true;
	}

	public final void interrupt(Class<? extends PA_Task> taskClass, BleManager manager)
	{
		PA_Task current = getCurrent(taskClass, manager);

		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, manager, null, null) )
		{
			tryEndingTask(current, PE_TaskState.INTERRUPTED);

			add(current);
		}
	}


	public final boolean succeed(Class<? extends PA_Task> taskClass, BleManager manager)
	{
		return tryEndingTask(taskClass, manager, null, null, PE_TaskState.SUCCEEDED);
	}

	public final boolean succeed(Class<? extends PA_Task> taskClass, BleDevice device)
	{
		return tryEndingTask(taskClass, null, device, null, PE_TaskState.SUCCEEDED);
	}

	public final boolean succeed(Class<? extends PA_Task> taskClass, BleServer server)
	{
		return tryEndingTask(taskClass, null, null, server, PE_TaskState.SUCCEEDED);
	}


	public final boolean fail(Class<? extends PA_Task> taskClass, BleManager manager)
	{
		return tryEndingTask(taskClass, manager, null, null, PE_TaskState.FAILED);
	}

	public final boolean fail(Class<? extends PA_Task> taskClass, BleDevice device)
	{
		return tryEndingTask(taskClass, null, device, null, PE_TaskState.FAILED);
	}

	public final boolean fail(Class<? extends PA_Task> taskClass, BleServer server)
	{
		return tryEndingTask(taskClass, null, null, server, PE_TaskState.FAILED);
	}

	private boolean tryEndingTask(final Class<? extends PA_Task> taskClass, final BleManager mngr_nullable, final BleDevice device_nullable, final BleServer server_nullable, final PE_TaskState endingState)
	{
		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, mngr_nullable, device_nullable, server_nullable ) )
		{
			return endCurrentTask(endingState);
		}
		
		return false;
	}

	final void tryEndingTask(final PA_Task task, final PE_TaskState endingState)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override public void run()
			{
				tryEndingTask_updateThread(task, endingState);
			}
		});
	}

	private void tryEndingTask_updateThread(final PA_Task task, final PE_TaskState endingState)
	{
		if( task != null && task == getCurrent() )
		{
			if( !endCurrentTask(endingState) )
			{
				m_mngr.ASSERT(false);
			}
		}
	}

	public final boolean isCurrent(Class<? extends PA_Task> taskClass, BleManager mngr)
	{
		return PU_TaskQueue.isMatch(getCurrent(), taskClass, mngr, null, null);
	}

	public final boolean isCurrent(Class<? extends PA_Task> taskClass, BleDevice device)
	{
		return PU_TaskQueue.isMatch(getCurrent(), taskClass, null, device, null);
	}

	public final boolean isCurrent(Class<? extends PA_Task> taskClass, BleServer server)
	{
		return PU_TaskQueue.isMatch(getCurrent(), taskClass, null, null, server);
	}

	private boolean isInQueue(Class<? extends PA_Task> taskClass, BleManager mngr_nullable, BleDevice device_nullable, BleServer server_nullable)
	{
		for( int i = 0; i < m_queue.size(); i++ )
		{
			if( PU_TaskQueue.isMatch(m_queue.get(i), taskClass, mngr_nullable, device_nullable, server_nullable) )
			{
				return true;
			}
		}
		
		return false;
	}

	private int positionInQueue(Class<? extends PA_Task> taskClass, BleManager mngr_nullable, BleDevice device_nullable, BleServer server_nullable)
	{
		for( int i = 0; i < m_queue.size(); i++ )
		{
			if( PU_TaskQueue.isMatch(m_queue.get(i), taskClass, mngr_nullable, device_nullable, server_nullable) )
			{
				return i;
			}
		}

		return -1;
	}

	public final int getSize()
	{
		return m_queue.size();
	}

	public final List<PA_Task> getRaw()
	{
		return m_queue;
	}

	public final int positionInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
	{
		return positionInQueue(taskClass, mngr, null, null);
	}

	public final int positionInQueue(Class<? extends PA_Task> taskClass, BleDevice device)
	{
		return positionInQueue(taskClass, null, device, null);
	}

	public final int positionInQueue(Class<? extends PA_Task> taskClass, BleServer server)
	{
		return positionInQueue(taskClass, null, null, server);
	}

	public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
	{
		return isInQueue(taskClass, mngr, null, null);
	}

	public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleDevice device)
	{
		return isInQueue(taskClass, null, device, null);
	}

	public final boolean isInQueue(Class<? extends PA_Task> taskClass, BleServer server)
	{
		return isInQueue(taskClass, null, null, server);
	}

	public final boolean isCurrentOrInQueue(Class<? extends PA_Task> taskClass, BleManager mngr)
	{
		return isCurrent(taskClass, mngr) || isInQueue(taskClass, mngr);
	}

	public final <T extends PA_Task> T get(Class<T> taskClass, BleManager mngr)
	{
		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, mngr, null, null) )
		{
			return (T) getCurrent();
		}

		for( int i = 0; i < m_queue.size(); i++ )
		{
			if( PU_TaskQueue.isMatch(m_queue.get(i), taskClass, mngr, null, null) )
			{
				return (T) m_queue.get(i);
			}
		}

		return null;
	}

	public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleDevice device)
	{
		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, null, device, null) )
		{
			return (T) getCurrent();
		}

		return null;
	}

	public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleManager mngr)
	{
		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, mngr, null, null) )
		{
			return (T) getCurrent();
		}

		return null;
	}

	public final <T extends PA_Task> T getCurrent(Class<T> taskClass, BleServer server)
	{
		if( PU_TaskQueue.isMatch(getCurrent(), taskClass, null, null, server) )
		{
			return (T) getCurrent();
		}

		return null;
	}

	final void print()
	{
		if( m_logger.isEnabled() )
		{
			m_logger.i(this.toString());
		}
	}

	private void clearQueueOf$removeFromQueue(int index)
	{
		PA_Task task = m_queue.remove(index);

		if( task.wasSoftlyCancelled() )
		{
			task.setEndingState(PE_TaskState.SOFTLY_CANCELLED);
		}
		else
		{
			task.setEndingState(PE_TaskState.CLEARED_FROM_QUEUE);
		}
		
		print();
	}

	public final void clearQueueOf(final Class<? extends PA_Task> taskClass, final BleManager mngr)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				for( int i = m_queue.size()-1; i >= 0; i-- )
				{
					if( PU_TaskQueue.isMatch(m_queue.get(i), taskClass, mngr, null, null) )
					{
						clearQueueOf$removeFromQueue(i);
					}
				}
			}
		});
	}

	public final void clearQueueOf(final Class<? extends PA_Task> taskClass, final BleDevice device, final int ordinal)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				for( int i = m_queue.size()-1; i >= 0; i-- )
				{
					final PA_Task task_ith = m_queue.get(i);

					if( ordinal <= -1 || ordinal >= 0 && task_ith.getOrdinal() <= ordinal )
					{
						if( PU_TaskQueue.isMatch(task_ith, taskClass, null, device, null) )
						{
							clearQueueOf$removeFromQueue(i);
						}
					}
				}
			}
		});
	}

	public final void clearQueueOf(final Class<? extends PA_Task> taskClass, final BleServer server)
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				for( int i = m_queue.size()-1; i >= 0; i-- )
				{
					if( PU_TaskQueue.isMatch(m_queue.get(i), taskClass, null, null, server) )
					{
						clearQueueOf$removeFromQueue(i);
					}
				}
			}
		});
	}

	public final void clearQueueOfAll()
	{
		m_mngr.getPostManager().runOrPostToUpdateThread(new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = m_queue.size() - 1; i >= 0; i-- )
				{
					clearQueueOf$removeFromQueue(i);
				}
			}
		});
	}

	@Override public final String toString()
	{
		final String current = m_current.get() != null ? m_current.get().toString() : "no current task";
//		if( m_pendingEndingStateForCurrentTask != null)
//		{
//			current += "(" + m_pendingEndingStateForCurrentTask.name() +")";
//		}
		
		final String queue = m_queue.size() > 0 ? m_queue.toString() : "[queue empty]";
		
		final String toReturn = current + " " + queue;
		
		return toReturn;
	}
}
