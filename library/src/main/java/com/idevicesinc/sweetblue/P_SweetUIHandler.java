package com.idevicesinc.sweetblue;


import android.os.Handler;
import android.os.Looper;


final class P_SweetUIHandler implements P_SweetHandler
{

    private final Handler m_handler;
    private P_SweetBlueThread m_thread;


    public P_SweetUIHandler(BleManager mgr)
    {
        boolean unitTest = true;
        if (mgr.m_config.unitTest == null)
        {
            try
            {
                Class.forName("org.junit.Assert");
            } catch (ClassNotFoundException e)
            {
                unitTest = false;
            }
        }
        else
        {
            unitTest = mgr.m_config.unitTest;
        }
        if (unitTest)
        {
            m_thread = new P_SweetBlueThread();
            m_handler = null;
        }
        else
        {
            m_handler = new Handler(Looper.getMainLooper());
        }
    }


    @Override public void post(Runnable action)
    {
        if (m_handler != null)
        {
            m_handler.post(action);
        }
        else
        {
            m_thread.post(action);
        }
    }

    @Override public void postDelayed(Runnable action, long delay)
    {
        if (m_handler != null)
        {
            m_handler.postDelayed(action, delay);
        }
        else
        {
            m_thread.postDelayed(action, delay);
        }
    }

    @Override public void removeCallbacks(Runnable action)
    {
        if (m_handler != null)
        {
            m_handler.removeCallbacks(action);
        }
        else
        {
            m_thread.removeCallbacks(action);
        }
    }

    @Override public Thread getThread()
    {
        if (m_handler != null)
        {
            return m_handler.getLooper().getThread();
        }
        else
        {
            return m_thread.getThread();
        }
    }
}
