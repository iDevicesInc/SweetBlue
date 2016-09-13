package com.idevicesinc.sweetblue.listeners;

public interface EnablerDoneListener
{
    enum ScanTypeAvailable
    {
        NONE,
        BLE,
        CLASSIC
    }

    void onFinished(ScanTypeAvailable scanTypeAvailable);
}
