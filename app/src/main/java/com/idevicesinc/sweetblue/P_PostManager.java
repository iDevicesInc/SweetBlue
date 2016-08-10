package com.idevicesinc.sweetblue;


import android.os.Handler;

public final class P_PostManager
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

    public final void postToMain(Runnable action)
    {
        mUIHandler.post(action);
    }

    public final void post(Runnable action)
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

    public final void postCallback(Runnable action)
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

    public final void postToUpdateThread(Runnable action)
    {
        mUpdateHandler.post(action);
    }

    public final void postToMainDelayed(Runnable action, long delay)
    {
        mUIHandler.postDelayed(action, delay);
    }

    public final void postDelayed(Runnable action, long delay)
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

    public final void postCallbackDelayed(Runnable action, long delay)
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

    public final void postToUpdateThreadDelayed(Runnable action, long delay)
    {
        mUpdateHandler.postDelayed(action, delay);
    }

    public final void removeUICallbacks(Runnable uiRunnable)
    {
        mUIHandler.removeCallbacks(uiRunnable);
    }

    public final void removeUpdateCallbacks(Runnable updateRunnable)
    {
        mUpdateHandler.removeCallbacks(updateRunnable);
    }


    final boolean isOnSweetBlueThread()
    {
        return Thread.currentThread() == mUpdateHandler.getLooper().getThread();
    }

}
