package com.idevicesinc.sweetblue;

import android.os.Build;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.UUID;

/**
 * As of now there are two main default uses for this class...
 * <br><br>
 * The first is that in at least some cases it's not possible to determine beforehand whether a given characteristic requires
 * bonding, so implementing this interface on {@link BleManagerConfig#bondFilter} lets the app give
 * a hint to the library so it can bond before attempting to read or write an encrypted characteristic.
 * Providing these hints lets the library handle things in a more deterministic and optimized fashion, but is not required.
 * <br><br>
 * The second is that some android devices have issues when it comes to bonding. So far the worst culprits
 * are certain Sony and Motorola phones, so if it looks like {@link Build#MANUFACTURER}
 * is either one of those, {@link BleDeviceConfig.DefaultBondFilter} is set to unbond upon discoveries and disconnects.
 * Please look at the source of {@link BleDeviceConfig.DefaultBondFilter} for the most up-to-date spec.
 * The problem seems to be associated with mismanagement of pairing keys by the OS and
 * this brute force solution seems to be the only way to smooth things out.
 */
@com.idevicesinc.sweetblue.annotations.Advanced
@com.idevicesinc.sweetblue.annotations.Lambda
public interface BondFilter
{
    /**
     * Just a dummy subclass of {@link DeviceStateListener.StateEvent} so that this gets auto-imported for implementations of {@link BondFilter}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    class StateChangeEvent extends DeviceStateListener.StateEvent
    {
        StateChangeEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
        {
            super(device, oldStateBits, newStateBits, intentMask, gattStatus);
        }
    }

    /**
     * An enumeration of the type of characteristic operation for a {@link CharacteristicEvent}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    enum CharacteristicEventType
    {
        /**
         * Started from {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#startPoll(UUID, Interval, ReadWriteListener)}, etc.
         */
        READ,

        /**
         * Started from {@link BleDevice#write(UUID, byte[], ReadWriteListener)} or overloads.
         */
        WRITE,

        /**
         * Started from {@link BleDevice#enableNotify(UUID, ReadWriteListener)} or overloads.
         */
        ENABLE_NOTIFY;
    }

    /**
     * Struct passed to {@link BondFilter#onEvent(CharacteristicEvent)}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Immutable
    class CharacteristicEvent extends Event
    {
        /**
         * Returns the {@link BleDevice} in question.
         */
        public BleDevice device(){  return m_device;  }
        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()  {  return m_device.getMacAddress();  }

        /**
         * Returns the {@link UUID} of the characteristic in question.
         */
        public UUID charUuid(){  return m_uuid;  }
        private final UUID m_uuid;

        /**
         * Returns the type of characteristic operation, read, write, etc.
         */
        public CharacteristicEventType type(){  return m_type;  }
        private final CharacteristicEventType m_type;

        CharacteristicEvent(BleDevice device, UUID uuid, CharacteristicEventType type)
        {
            m_device = device;
            m_uuid = uuid;
            m_type = type;
        }

        @Override public String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
                "device",		device().getName_debug(),
                "charUuid",		device().getManager().getLogger().charName(charUuid()),
                "type",			type()
            );
        }
    }

    /**
     * Return value for the various interface methods of {@link BondFilter}.
     * Use static constructor methods to create instances.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Immutable
    class Please
    {
        private final Boolean m_bond;
        private final BondListener m_bondListener;

        Please(Boolean bond, BondListener listener)
        {
            m_bond = bond;
            m_bondListener = listener;
        }

        Boolean bond_private()
        {
            return m_bond;
        }

        BondListener listener()
        {
            return m_bondListener;
        }

        /**
         * Device should be bonded if it isn't already.
         */
        public static Please bond()
        {
            return new Please(true, null);
        }

        /**
         * Returns {@link #bond()} if the given condition holds <code>true</code>, {@link #doNothing()} otherwise.
         */
        public static Please bondIf(boolean condition)
        {
            return condition ? bond() : doNothing();
        }

        /**
         * Same as {@link #bondIf(boolean)} but lets you pass a {@link BondListener} as well.
         */
        public static Please bondIf(boolean condition, BondListener listener)
        {
            return condition ? bond(listener) : doNothing();
        }

        /**
         * Same as {@link #bond()} but lets you pass a {@link BondListener} as well.
         */
        public static Please bond(BondListener listener)
        {
            return new Please(true, listener);
        }

        /**
         * Device should be unbonded if it isn't already.
         */
        public static Please unbond()
        {
            return new Please(false, null);
        }

        /**
         * Returns {@link #bond()} if the given condition holds <code>true</code>, {@link #doNothing()} otherwise.
         */
        public static Please unbondIf(boolean condition)
        {
            return condition ? unbond() : doNothing();
        }

        /**
         * Device's bond state should not be affected.
         */
        public static Please doNothing()
        {
            return new Please(null, null);
        }
    }

    /**
     * Called after a device undergoes a change in its {@link BleDeviceState}.
     */
    Please onEvent(StateChangeEvent e);

    /**
     * Called immediately before reading, writing, or enabling notification on a characteristic.
     */
    Please onEvent(CharacteristicEvent e);
}
