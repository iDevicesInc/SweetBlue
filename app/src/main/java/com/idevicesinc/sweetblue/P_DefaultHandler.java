package com.idevicesinc.sweetblue;

import android.os.Looper;


final class P_DefaultHandler extends PA_Handler
{

    public P_DefaultHandler(Looper looper)
    {
        super(looper);
    }

    @Override public final void post(Runnable action)
    {
        getHandler().post(action);
    }

    @Override public final void postDelayed(Runnable action, long delay)
    {
        getHandler().postDelayed(action, delay);
    }

    @Override public final void removeCallbacks(Runnable action)
    {
        getHandler().removeCallbacks(action);
    }
}
