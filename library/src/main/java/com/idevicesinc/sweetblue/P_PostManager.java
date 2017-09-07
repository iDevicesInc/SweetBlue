package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Utils;

final class P_PostManager
{

    private final P_SweetHandler m_uiHandler;
    private final P_SweetHandler m_updateHandler;
    private final BleManager m_manager;


    P_PostManager(BleManager mgr, P_SweetHandler uiHandler, P_SweetHandler updateHandler)
    {
        m_uiHandler = uiHandler;
        m_updateHandler = updateHandler;
        m_manager = mgr;
    }

    public final void postToMain(Runnable action)
    {
        if (Utils.isOnMainThread())
        {
            action.run();
        }
        else
        {
            m_uiHandler.post(action);
        }
    }

    public final void post(Runnable action)
    {
        if (m_manager.m_config.runOnMainThread)
        {
            if (Utils.isOnMainThread())
            {
                action.run();
            }
            else
            {
                m_uiHandler.post(action);
            }
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

    public final void postCallback(Runnable action)
    {
        if (m_manager.m_config.postCallbacksToMainThread)
        {
            postToMain(action);
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
        m_updateHandler.post(action);
    }

    public final void runOrPostToUpdateThread(Runnable action)
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

    final P_SweetHandler getUIHandler()
    {
        return m_uiHandler;
    }

    final P_SweetHandler getUpdateHandler()
    {
        return m_updateHandler;
    }

    final boolean isOnSweetBlueThread()
    {
        return Thread.currentThread() == m_updateHandler.getThread();
    }

    final void quit()
    {
        if (m_updateHandler instanceof P_SweetBlueThread)
        {
            ((P_SweetBlueThread) m_updateHandler).quit();
        }
        if (m_uiHandler instanceof P_SweetBlueThread)
        {
            ((P_SweetBlueThread) m_uiHandler).quit();
        }

    }

}
