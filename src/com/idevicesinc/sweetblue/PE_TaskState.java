package com.idevicesinc.sweetblue;

/**
 * 
 * @author dougkoellmer
 *
 */
enum PE_TaskState
{
	CREATED,	// very transient, not really listened for
	QUEUED,		// task is in queue...previous state can be CREATED or INTERRUPTED
	
	ARMED,		// task is current but not yet executing...configurable time for being in this state but usually ends up just being one timeStep. 
	EXECUTING,
	
	// ending states
	SUCCEEDED,
	TIMED_OUT,
	INTERRUPTED,
	CANCELLED,
	SOFTLY_CANCELLED, // set after arming (preemptively cancels execution) or is mutated from the SUCCEEDED state if task is softly cancelled while already executing.
	FAILED,
	CLEARED_FROM_QUEUE,
	REDUNDANT,
	NO_OP;
	
	public boolean isEndingState()
	{
		return this.ordinal() > EXECUTING.ordinal();
	}
}
	