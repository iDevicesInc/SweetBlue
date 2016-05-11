package com.idevicesinc.sweetblue;


import android.os.Looper;

public class UnitTestThread extends Thread
{

    private final UnitTestRunnable testRunnable;
    private boolean mRunning = true;
    private BleManager mManager;


    public UnitTestThread(UnitTestRunnable testRunnable)
    {
        super("TestThread");
        this.testRunnable = testRunnable;
    }

    @Override public void run()
    {
        Looper.prepare();
        if (testRunnable != null)
        {
            mManager = testRunnable.setup(Looper.myLooper());
        }
        while (mRunning)
        {
            if (mManager != null)
            {
                mManager.update(System.currentTimeMillis());
            }
            if (testRunnable != null)
            {
                mRunning = testRunnable.update();
            }
            try
            {
                Thread.sleep(25);
            } catch (Exception e)
            {
            }
        }
        Looper.loop();
    }
}
