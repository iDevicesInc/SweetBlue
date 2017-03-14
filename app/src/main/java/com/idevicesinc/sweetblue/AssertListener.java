package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Mostly only for SweetBlue library developers. Provide an implementation to
 * {@link BleManager#setListener_Assert(AssertListener)} to be notified whenever
 * an assertion fails through {@link BleManager#ASSERT(boolean, String)}.
 */
@Advanced
@com.idevicesinc.sweetblue.annotations.Lambda
public interface AssertListener
{

    /**
     * Struct passed to {@link AssertListener#onEvent(AssertListener.AssertEvent)}.
     */
    @Immutable
    class AssertEvent extends Event
    {
        /**
         * The {@link BleManager} instance for your application.
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * Message associated with the assert, or an empty string.
         */
        public String message(){  return m_message;  }
        private final String m_message;

        /**
         * Stack trace leading up to the assert.
         */
        public StackTraceElement[] stackTrace(){  return m_stackTrace;  }
        private final StackTraceElement[] m_stackTrace;

        AssertEvent(BleManager manager, String message, StackTraceElement[] stackTrace)
        {
            m_manager = manager;
            m_message = message;
            m_stackTrace = stackTrace;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "message",			message(),
                            "stackTrace",		stackTrace()
                    );
        }
    }

    /**
     * Provides additional info about the circumstances surrounding the assert.
     */
    void onEvent(final AssertEvent e);

}
