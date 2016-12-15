package com.idevicesinc.sweetblue;

import android.os.Handler;
import android.os.HandlerThread;


final class P_SweetBlueHandlerThread extends HandlerThread
{

    private Handler m_handler;


    public P_SweetBlueHandlerThread()
    {
        super("SweetBlue");
    }


    public Handler prepareHandler()
    {
        m_handler = new Handler(getLooper());
        return m_handler;
    }

}
