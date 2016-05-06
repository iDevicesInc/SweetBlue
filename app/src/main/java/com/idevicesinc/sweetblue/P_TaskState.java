package com.idevicesinc.sweetblue;


enum P_TaskState
{

    /**
     * The initial state of a task. It's only ever in this state for a very short period of time, before
     * it gets put into the queue.
     */
    CREATED,

    /**
     * The task is queued in the task queue.
     */
    QUEUED,

    /**
     * The task was once executing, got interrupted, and is now back in the queue waiting to execute again.
     */
    REQUEUED,

    /**
     * The task is executing it's task.
     */
    EXECUTING,


    // ending states
    SUCCEEDED,
    TIMED_OUT,
    INTERRUPTED,
    REDUNDANT,
    CANCELLED,
    FAILED,
    FAILED_IMMEDIATELY;

    public boolean isEndingState()
    {
        return ordinal() > EXECUTING.ordinal();
    }

}
