package com.idevicesinc.sweetblue;


import android.os.Handler;
import android.os.HandlerThread;


final class P_SweetBlueHandlerThread extends HandlerThread
{

    private Handler m_handler;
    private BleManager m_manager;


    public P_SweetBlueHandlerThread(BleManager mgr)
    {
        super("SweetBlue");
        m_manager = mgr;
    }


    public Handler prepareHandler()
    {
        m_handler = new Handler(getLooper());
        return m_handler;
    }

}
