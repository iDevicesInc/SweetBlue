package com.idevicesinc.sweetblue;


import android.os.Handler;

public class P_PostManager
{

    private final Handler mUIHandler;
    private final Handler mUpdateHandler;
    private final BleManager mManager;


    public P_PostManager(BleManager mgr, Handler uiHandler, Handler updateHandler)
    {
        mManager = mgr;
        mUIHandler = uiHandler;
        mUpdateHandler = updateHandler;
    }

    public void postToMain(Runnable action)
    {
        mUIHandler.post(action);
    }

    public void post(Runnable action)
    {
        if (mManager.mConfig.runOnUIThread)
        {
            mUIHandler.post(action);
        }
        else
        {
            mUpdateHandler.post(action);
        }
    }

    public void postCallback(Runnable action)
    {
        if (mManager.mConfig.postCallbacksToUIThread)
        {
            mUIHandler.post(action);
        }
        else
        {
            if (isOnSweetBlueThread())
            {
                action.run();
            }
            else
            {
                mUpdateHandler.post(action);
            }
        }
    }

    public void postToUpdateThread(Runnable action)
    {
        mUpdateHandler.post(action);
    }

    public void postToMainDelayed(Runnable action, long delay)
    {
        mUIHandler.postDelayed(action, delay);
    }

    public void postDelayed(Runnable action, long delay)
    {
        if (mManager.mConfig.runOnUIThread)
        {
            mUIHandler.postDelayed(action, delay);
        }
        else
        {
            mUpdateHandler.postDelayed(action, delay);
        }
    }

    public void postCallbackDelayed(Runnable action, long delay)
    {
        if (mManager.mConfig.postCallbacksToUIThread)
        {
            mUIHandler.postDelayed(action, delay);
        }
        else
        {
            mUpdateHandler.postDelayed(action, delay);
        }
    }

    public void postToUpdateThreadDelayed(Runnable action, long delay)
    {
        mUpdateHandler.postDelayed(action, delay);
    }

    public void removeCallbacks(Runnable updateRunnable)
    {
        mUIHandler.removeCallbacks(updateRunnable);
    }

    boolean isOnSweetBlueThread()
    {
        return Thread.currentThread() == mUpdateHandler.getLooper().getThread();
    }

}
