package com.idevicesinc.sweetblue;


import android.os.Handler;
import android.os.Looper;


abstract class PA_Handler
{

    private Handler m_handler;
    private final Looper m_looper;


    public PA_Handler(Looper looper)
    {
        m_looper = looper;
        m_handler = new Handler(m_looper);
    }

    public Looper getLooper()
    {
        return m_looper;
    }

    protected Handler getHandler()
    {
        return m_handler;
    }

    public abstract void post(Runnable action);
    public abstract void postDelayed(Runnable action, long delay);
    public abstract void removeCallbacks(Runnable action);

}
