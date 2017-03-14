package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleManager#reset(ResetListener)}
 * to be notified when a reset operation is complete.
 *
 * @see BleManager#reset(ResetListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ResetListener
{

    /**
     * Enumeration of the progress of the reset.
     * More entries may be added in the future.
     */
    enum Progress
    {
        /**
         * The reset has completed successfully.
         */
        COMPLETED;
    }

    /**
     * Struct passed to {@link ResetListener#onEvent(ResetListener.ResetEvent)}.
     */
    @Immutable
    class ResetEvent extends Event
    {
        /**
         * The {@link BleManager} the reset was applied to.
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * The progress of the reset.
         */
        public ResetListener.Progress progress(){  return m_progress;  }
        private final ResetListener.Progress m_progress;

        ResetEvent(BleManager manager, ResetListener.Progress progress)
        {
            m_manager = manager;
            m_progress = progress;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "progress",		progress()
                    );
        }
    }

    /**
     * The reset event, for now only fired when the reset is completed. Hopefully the bluetooth stack is OK now.
     */
    void onEvent(final ResetListener.ResetEvent e);

}
