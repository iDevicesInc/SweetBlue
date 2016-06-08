package com.idevicesinc.sweetblue;


public enum P_TaskPriority
{
    TRIVIAL,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    ATOMIC_TRANSACTION,
    /**
     * This has it's own priority, as this NEEDS to complete before anything else (namely turning off BLE).
     */
    UNBOND;
}
