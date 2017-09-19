package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.UUID;

/**
 * Provides a way to control timeout behavior for various {@link BleTask} instances. Assign an instance to {@link BleDeviceConfig#taskTimeoutRequestFilter}.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
@com.idevicesinc.sweetblue.annotations.Advanced
public interface TaskTimeoutRequestFilter
{
    /**
     * Event passed to {@link TaskTimeoutRequestFilter#onEvent(TaskTimeoutRequestEvent)} that provides
     * information about the {@link BleTask} that will soon be executed.
     */
    @Immutable
    class TaskTimeoutRequestEvent extends Event
    {
        /**
         * The {@link BleDevice} associated with the {@link #task()}, or {@link BleDevice#NULL} if
         * {@link #task()} {@link BleTask#isDeviceSpecific()} does not return <code>true</code>.
         */
        public BleDevice device(){  return m_device;  }
        private BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()  {  return m_device.getMacAddress();  }

        /**
         * The {@link BleServer} associated with the {@link #task()}, or {@link BleServer#NULL} if
         * {@link #task()} {@link BleTask#isServerSpecific()} does not return <code>true</code>.
         */
        public BleServer server(){  return m_server;  }
        private BleServer m_server;

        /**
         * Returns the manager.
         */
        public BleManager manager(){  return m_manager;  }
        private BleManager m_manager;

        /**
         * The type of task for which we are requesting a timeout.
         */
        public BleTask task(){  return m_task;  }
        private BleTask m_task;

        /**
         * The ble characteristic {@link UUID} associated with the task if {@link BleTask#usesCharUuid()}
         * returns <code>true</code>, or {@link Uuids#INVALID} otherwise.
         */
        public UUID charUuid(){  return m_charUuid;  }
        private UUID m_charUuid;

        /**
         * The ble descriptor {@link UUID} associated with the task, or {@link Uuids#INVALID} otherwise.
         * For now only associated with {@link BleTask#TOGGLE_NOTIFY}.
         */
        public UUID descUuid(){  return m_descUuid;  }
        private UUID m_descUuid;

        void init(BleManager manager, BleDevice device, BleServer server, BleTask task, UUID charUuid, UUID descUuid)
        {
            m_manager = manager;
            m_device = device;
            m_server = server;
            m_task = task;
            m_charUuid = charUuid;
            m_descUuid = descUuid;
        }

        @Override public String toString()
        {
            if( device() != BleDevice.NULL )
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "device",		device(),
                    "task",			task(),
                    "charUuid",		charUuid()
                );
            }
            else
            {
                return Utils_String.toString
                (
                    this.getClass(),
                    "server",		server(),
                    "task",			task(),
                    "charUuid",		charUuid()
                );
            }
        }
    }

    /**
     * Use static constructor methods to create instances to return from {@link TaskTimeoutRequestFilter#onEvent(TaskTimeoutRequestEvent)}.
     */
    @Immutable
    class Please
    {
        private final Interval m_interval;

        Please(Interval interval)
        {
            m_interval = interval;
        }


        Interval interval()
        {
            return m_interval;
        }

        /**
         * Tells SweetBlue to wait for the given interval before timing out the task.
         */
        public static Please setTimeoutFor(final Interval interval)
        {
            return new Please(interval);
        }

        /**
         * Tells SweetBlue to not timeout the task at all.
         * <br><br>
         * WARNING: This can be dangerous to use because if a task never finishes it will block all other operations indefinitely.
         */
        public static Please doNotUseTimeout()
        {
            return new Please(Interval.DISABLED);
        }

    }

    /**
     * Implement this to have fine-grained control over {@link BleTask} timeout behavior.
     */
    Please onEvent(TaskTimeoutRequestEvent e);
}
