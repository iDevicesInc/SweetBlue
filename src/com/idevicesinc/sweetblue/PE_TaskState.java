package com.idevicesinc.sweetblue;

/**
 * 
 * 
 *
 */
enum PE_TaskState
{
	CREATED,				// very transient, not really listened for at the moment.
	QUEUED,					// task is in queue...previous state can be CREATED or INTERRUPTED
	
	ARMED,					// task is current but not yet executing...there's a configurable time for being in this state but usually ends up just being one timeStep. 
	EXECUTING,
	
	// ending states
	SUCCEEDED,
	TIMED_OUT,
	INTERRUPTED,			// put back on queue...next state will be QUEUED.
	CANCELLED,
	SOFTLY_CANCELLED,		// set after arming (preemptively cancels execution) or is mutated from the SUCCEEDED state if task is softly cancelled while already executing.
	FAILED,
	CLEARED_FROM_QUEUE,
	REDUNDANT,
	FAILED_IMMEDIATELY;		// same as FAILED but to indicate that operation couldn't even be sent off, presumably due to very exceptional conditions.
	
	public boolean isEndingState()
	{
		return this.ordinal() > EXECUTING.ordinal();
	}

	public boolean canGoToNextTaskImmediately()
	{
		return this == SUCCEEDED || this == TIMED_OUT;
	}
}
	
