package com.idevicesinc.sweetblue;


public interface P_SweetHandler
{

    void post(Runnable action);
    void postDelayed(Runnable action, long delay);
    void removeCallbacks(Runnable action);
    Thread getThread();

}
