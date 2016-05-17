package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.ManagerStateListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;
import com.idevicesinc.sweetblue.utils.State;


class P_BleStateTracker extends P_StateTracker
{

    private ManagerStateListener mListener;
    private final BleManager mManager;


    public P_BleStateTracker(BleManager mgr)
    {
        super(BleManagerState.VALUES());
        mManager = mgr;
    }

    public void setListener(ManagerStateListener listener)
    {
        mListener = listener;
    }

    @Override void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
    {
        if( mListener != null )
        {
            final ManagerStateListener.StateEvent event = P_EventFactory.newManagerStateEvent(mManager, oldStateBits, newStateBits, intentMask);
            mManager.mPostManager.postCallback(new Runnable()
            {
                @Override public void run()
                {
                    mListener.onEvent(event);
                }
            });
        }
    }

    @Override
    public String toString()
    {
        return super.toString(BleManagerState.VALUES());
    }
}
