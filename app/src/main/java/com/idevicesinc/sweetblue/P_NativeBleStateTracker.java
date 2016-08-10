package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.listeners.NativeStateListener;
import com.idevicesinc.sweetblue.listeners.P_EventFactory;


public final class P_NativeBleStateTracker extends P_StateTracker
{


    private final BleManager mManager;
    private NativeStateListener mStateListener;


    P_NativeBleStateTracker(BleManager mgr)
    {
        super(BleManagerState.VALUES());
        mManager = mgr;
    }

    public final void setListener(NativeStateListener listener)
    {
        mStateListener = listener;
    }

    @Override final void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
    {
        if (mStateListener != null)
        {
            final NativeStateListener.NativeStateEvent event = P_EventFactory.newNativeStateEvent(mManager, oldStateBits, newStateBits, intentMask);
            mStateListener.onEvent(event);
        }
    }
}
