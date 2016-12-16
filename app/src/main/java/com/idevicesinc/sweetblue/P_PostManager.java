package com.idevicesinc.sweetblue;


import android.os.Handler;

final class P_PostManager
{

    private final Handler m_uiHandler;
    private final Handler m_updateHandler;
    private final BleManager m_manager;


    P_PostManager(BleManager mgr, Handler uiHandler, Handler updateHandler)
    {
        m_uiHandler = uiHandler;
        m_updateHandler = updateHandler;
        m_manager = mgr;
    }

    public final void postToMain(Runnable action)
    {
        m_uiHandler.post(action);
    }

    public final void post(Runnable action)
    {
        if (m_manager.m_config.runOnMainThread)
        {
            m_uiHandler.post(action);
        }
        else
        {
            m_updateHandler.post(action);
        }
    }

    public final void postCallback(Runnable action)
    {
        if (m_manager.m_config.postCallbacksToMainThread)
        {
            m_uiHandler.post(action);
        }
        else
        {
            if (isOnSweetBlueThread())
            {
                action.run();
            }
            else
            {
                m_updateHandler.post(action);
            }
        }
    }

    public final void postToUpdateThread(Runnable action)
    {
        if (isOnSweetBlueThread())
        {
            action.run();
        }
        else
        {
            m_updateHandler.post(action);
        }
    }

    public final void forcePostToUpdate(Runnable action)
    {
        m_updateHandler.post(action);
    }

    public final void postToMainDelayed(Runnable action, long delay)
    {
        m_uiHandler.postDelayed(action, delay);
    }

    public final void postDelayed(Runnable action, long delay)
    {
        if (m_manager.m_config.runOnMainThread)
        {
            m_uiHandler.postDelayed(action, delay);
        }
        else
        {
            m_updateHandler.postDelayed(action, delay);
        }
    }

    public final void postCallbackDelayed(Runnable action, long delay)
    {
        if (m_manager.m_config.postCallbacksToMainThread)
        {
            m_uiHandler.postDelayed(action, delay);
        }
        else
        {
            m_updateHandler.postDelayed(action, delay);
        }
    }

    public final void postToUpdateThreadDelayed(Runnable action, long delay)
    {
        m_updateHandler.postDelayed(action, delay);
    }

    public final void removeUICallbacks(Runnable uiRunnable)
    {
        m_uiHandler.removeCallbacks(uiRunnable);
    }

    public final void removeUpdateCallbacks(Runnable updateRunnable)
    {
        m_updateHandler.removeCallbacks(updateRunnable);
    }


    final boolean isOnSweetBlueThread()
    {
        return Thread.currentThread() == m_updateHandler.getLooper().getThread();
    }

}
