package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.GenericListener_Void;

/**
 * Provide an implementation to {@link BleManager#setListener_State(ManagerStateListener)} to receive callbacks
 * when the {@link BleManager} undergoes a {@link BleManagerState} change.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ManagerStateListener extends GenericListener_Void<BleManager.StateListener.StateEvent>
{

    // TODO - Uncomment the below for version 3 (or better yet, copy it from BleManager in case of changes/bug fixes)


//    /**
//     * Subclass that adds the manager field.
//     */
//    @Immutable
//    class StateEvent extends State.ChangeEvent<BleManagerState>
//    {
//        /**
//         * The singleton manager undergoing the state change.
//         */
//        public BleManager manager(){  return m_manager;  }
//        private final BleManager m_manager;
//
//        StateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
//        {
//            super(oldStateBits, newStateBits, intentMask);
//
//            this.m_manager = manager;
//        }
//
//        @Override public String toString()
//        {
//            return Utils_String.toString
//                    (
//                            this.getClass(),
//                            "entered",			Utils_String.toString(enterMask(), BleManagerState.VALUES()),
//                            "exited",			Utils_String.toString(exitMask(), BleManagerState.VALUES()),
//                            "current",			Utils_String.toString(newStateBits(), BleManagerState.VALUES())
//                    );
//        }
//    }

    /**
     * Called when the manager's abstracted {@link BleManagerState} changes.
     */
    void onEvent(final BleManager.StateListener.StateEvent e);
}
