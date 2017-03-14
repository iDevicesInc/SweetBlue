package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;

/**
 * Provide an implementation to {@link BleManager#setListener_NativeState(NativeManagerStateListener)} to receive callbacks
 * when the {@link BleManager} undergoes a *native* {@link BleManagerState} change. This is similar to {@link ManagerStateListener}
 * but reflects what is going on in the actual underlying stack, which may lag slightly behind the
 * abstracted state reflected by {@link ManagerStateListener}. Most apps will not find this callback useful.
 */
@Advanced
@com.idevicesinc.sweetblue.annotations.Lambda
public interface NativeManagerStateListener extends ManagerStateListener
{

    /**
     * Class declared here to be make it implicitly imported for overrides.
     */
    @Advanced
    @Immutable
    class NativeStateEvent extends ManagerStateListener.StateEvent
    {
        NativeStateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
        {
            super(manager, oldStateBits, newStateBits, intentMask);
        }
    }

    /**
     * Called when the manager's native bitwise {@link BleManagerState} changes. As many bits as possible are flipped at the same time.
     */
    @Advanced
    void onEvent(final NativeManagerStateListener.NativeStateEvent e);

}
