package com.idevicesinc.sweetblue.utils;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Use this class to build out a GATT database for your simulated devices when unit testing. It contains builder classes to make it easier
 * to build out the database. Start by calling {@link #addService(UUID)}.
 */
public class GattDatabase
{

    private final List<BluetoothGattService> m_services;


    public GattDatabase()
    {
        m_services = new ArrayList<>();
    }

    /**
     * Add a new service to the database.
     */
    public final ServiceBuilder addService(UUID serviceUuid)
    {
        return new ServiceBuilder(this, serviceUuid);
    }

    /**
     * Return the list of {@link BluetoothGattService}s in this database.
     */
    public final List<BluetoothGattService> getServiceList()
    {
        return m_services;
    }



    private void addService(BluetoothGattService service)
    {
        m_services.add(service);
    }


    /**
     * Builder class used to help building out {@link BluetoothGattService}s.
     */
    public final static class ServiceBuilder
    {

        private final GattDatabase m_database;
        private final List<BluetoothGattCharacteristic> m_characteristics;
        private final UUID m_serviceUuid;

        private BluetoothGattService m_service;

        private boolean m_isPrimary = true;


        private ServiceBuilder(GattDatabase database, UUID serviceUuid)
        {
            m_database = database;
            m_serviceUuid = serviceUuid;
            m_characteristics = new ArrayList<>();
        }

        /**
         * Set this service to the type {@link BluetoothGattService#SERVICE_TYPE_PRIMARY}. This is the default, so it shouldn't be necessary
         * to call this, but leaving it here for explicitness.
         */
        public final ServiceBuilder primary()
        {
            m_isPrimary = true;
            return this;
        }

        /**
         * Set this service to the type {@link BluetoothGattService#SERVICE_TYPE_SECONDARY}.
         */
        public final ServiceBuilder secondary()
        {
            m_isPrimary = false;
            return this;
        }

        /**
         * Add a new {@link BluetoothGattCharacteristic} to this service.
         */
        public final CharacteristicBuilder addCharacteristic(UUID charUuid)
        {
            return new CharacteristicBuilder(this, charUuid);
        }

        /**
         * Complete this service, and create a new one to be entered into the database.
         */
        public final ServiceBuilder newService(UUID serviceUuid)
        {
            build();
            return new ServiceBuilder(m_database, serviceUuid);
        }

        /**
         * Builds the current {@link BluetoothGattService}, and returns the parent {@link GattDatabase}.
         */
        public final GattDatabase build()
        {
            m_service = new BluetoothGattService(m_serviceUuid, m_isPrimary ? BluetoothGattService.SERVICE_TYPE_PRIMARY : BluetoothGattService.SERVICE_TYPE_SECONDARY);
            for (BluetoothGattCharacteristic ch : m_characteristics)
            {
                m_service.addCharacteristic(ch);
            }
            m_database.addService(m_service);
            return m_database;
        }


        private GattDatabase getDatabase()
        {
            return m_database;
        }

        private void addCharacteristic(BluetoothGattCharacteristic characteristic)
        {
            m_characteristics.add(characteristic);
        }
    }


    /**
     * Builder class used to create and configure a {@link BluetoothGattCharacteristic} to be entered into a {@link BluetoothGattService}.
     */
    public final static class CharacteristicBuilder
    {

        private final UUID m_charUuid;
        private final ServiceBuilder m_serviceBuilder;
        private final List<BluetoothGattDescriptor> m_descriptors;

        private BluetoothGattCharacteristic m_characteristic;

        private byte[] m_value;
        private int m_properties;
        private int m_permissions;


        private CharacteristicBuilder(ServiceBuilder serviceBuilder, UUID charUuid)
        {
            m_serviceBuilder = serviceBuilder;
            m_charUuid = charUuid;
            m_descriptors = new ArrayList<>();
        }

        /**
         * Set this {@link BluetoothGattCharacteristic}'s properties. You can also use {@link #setProperties(int...)}, but it's recommended
         * you use {@link #setProperties()} instead.
         */
        public final CharacteristicBuilder setProperties(int properties)
        {
            m_properties = properties;
            return this;
        }

        /**
         * Set this {@link BluetoothGattCharacteristic}'s properties. You can also use {@link #setProperties(int)}, but it's recommended
         * you use {@link #setProperties()} instead.
         */
        public final CharacteristicBuilder setProperties(int... properties)
        {
            m_properties = 0;
            if (properties != null && properties.length > 0)
            {
                for (int prop : properties)
                {
                    m_properties |= prop;
                }
            }
            return this;
        }

        /**
         * Set the properties for this {@link BluetoothGattCharacteristic}.
         */
        public final Properties setProperties()
        {
            return new Properties(this);
        }

        /**
         * Set this {@link BluetoothGattCharacteristic}'s permissions. You can also use {@link #setPermissions(int...)}, but it's recommended
         * you use {@link #setPermissions()} instead.
         */
        public final CharacteristicBuilder setPermissions(int permissions)
        {
            m_permissions = permissions;
            return this;
        }

        /**
         * Set this {@link BluetoothGattCharacteristic}'s permissions. You can also use {@link #setPermissions(int)}, but it's recommended
         * you use {@link #setPermissions()} instead.
         */
        public final CharacteristicBuilder setPermissions(int... permissions)
        {
            m_permissions = 0;
            if (permissions != null && permissions.length > 0)
            {
                for (int perm : permissions)
                {
                    m_permissions |= perm;
                }
            }
            return this;
        }

        /**
         * Set the permissions for this {@link BluetoothGattCharacteristic}.
         */
        public final CharacteristicPermissions setPermissions()
        {
            return new CharacteristicPermissions(this);
        }

        /**
         * Set the default value for this {@link BluetoothGattCharacteristic}.
         */
        public final CharacteristicBuilder setValue(byte[] value)
        {
            m_value = value;
            return this;
        }

        /**
         * Add a new {@link BluetoothGattDescriptor} to be added to this {@link BluetoothGattCharacteristic}.
         */
        public final DescriptorBuilder addDescriptor(UUID descriptorUuid)
        {
            return new DescriptorBuilder(this, descriptorUuid);
        }

        /**
         * Build this {@link BluetoothGattCharacteristic}, and add it to it's parent {@link BluetoothGattService}.
         */
        public final ServiceBuilder build()
        {
            m_characteristic = new BluetoothGattCharacteristic(m_charUuid, m_properties, m_permissions);
            m_characteristic.setValue(m_value);
            for (BluetoothGattDescriptor desc : m_descriptors)
            {
                m_characteristic.addDescriptor(desc);
            }
            m_serviceBuilder.addCharacteristic(m_characteristic);
            return m_serviceBuilder;
        }

        /**
         * Calls {@link #build()}, then creates a new {@link BluetoothGattCharacteristic}.
         */
        public final CharacteristicBuilder newCharacteristic(UUID charUuid)
        {
            build();
            return new CharacteristicBuilder(m_serviceBuilder, charUuid);
        }

        /**
         * Calls {@link #build()}, then builds the parent {@link BluetoothGattService}, and add it to the database.
         */
        public final GattDatabase completeService()
        {
            return build().build();
        }



        private void addDescriptor(BluetoothGattDescriptor descriptor)
        {
            m_descriptors.add(descriptor);
        }
    }

    /**
     * Builder class used to create and configure a {@link BluetoothGattDescriptor} to be added to a {@link BluetoothGattCharacteristic}.
     */
    public final static class DescriptorBuilder
    {

        private final UUID m_descUuid;
        private final CharacteristicBuilder m_charBuilder;

        private BluetoothGattDescriptor m_descriptor;

        private int m_permissions;
        private byte[] m_value;


        private DescriptorBuilder(CharacteristicBuilder charBuilder, UUID descUuid)
        {
            m_descUuid = descUuid;
            m_charBuilder = charBuilder;
        }

        /**
         * Set this {@link BluetoothGattDescriptor}'s permissions. You can also use {@link #setPermissions(int...)}, but it's recommended
         * you use {@link #setPermissions()} instead.
         */
        public final DescriptorBuilder setPermissions(int permissions)
        {
            m_permissions = permissions;
            return this;
        }

        /**
         * Set this {@link BluetoothGattDescriptor}'s permissions. You can also use {@link #setPermissions(int)}, but it's recommended
         * you use {@link #setPermissions()} instead.
         */
        public final DescriptorBuilder setPermissions(int... permissions)
        {
            m_permissions = 0;
            if (permissions != null && permissions.length > 0)
            {
                for (int perm : permissions)
                {
                    m_permissions |= perm;
                }
            }
            return this;
        }

        public final DescriptorBuilder setValue(byte[] value)
        {
            m_value = value;
            return this;
        }

        /**
         * Set the permissions for this {@link BluetoothGattDescriptor}.
         */
        public final DescriptorPermissions setPermissions()
        {
            return new DescriptorPermissions(this);
        }

        /**
         * Build this {@link BluetoothGattDescriptor}, and add it to its parent {@link BluetoothGattCharacteristic}.
         */
        public final CharacteristicBuilder build()
        {
            m_descriptor = new BluetoothGattDescriptor(m_descUuid, m_permissions);
            m_descriptor.setValue(m_value);
            m_charBuilder.addDescriptor(m_descriptor);
            return m_charBuilder;
        }

        /**
         * Calls {@link #build()}, then creates a new {@link BluetoothGattDescriptor} to add to the same {@link BluetoothGattCharacteristic}.
         */
        public final DescriptorBuilder newDescriptor(UUID descriptorUuid)
        {
            build();
            return new DescriptorBuilder(m_charBuilder, descriptorUuid);
        }

        /**
         * Calls {@link #build()}, then builds the parent {@link BluetoothGattCharacteristic}.
         */
        public final ServiceBuilder completeChar()
        {
            return build().build();
        }

        /**
         * Calls {@link #build()}, then builds the parent {@link BluetoothGattCharacteristic}, then builds the parent {@link BluetoothGattService}.
         */
        public final GattDatabase completeService()
        {
            return build().completeService();
        }

    }


    public static class Properties
    {

        private final CharacteristicBuilder m_charBuilder;
        private int m_properties;


        private Properties(CharacteristicBuilder builder)
        {
            m_charBuilder = builder;
        }


        public final Properties read()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_READ;
            return this;
        }

        public final Properties write()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_WRITE;
            return this;
        }

        public final Properties readWrite()
        {
            return read().write();
        }

        public final Properties readWriteNotify()
        {
            return readWrite().notify_prop();
        }

        public final Properties readWriteIndicate()
        {
            return readWrite().indicate();
        }

        public final Properties signed_write()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
            return this;
        }

        public final Properties write_no_response()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
            return this;
        }

        public final Properties notify_prop()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_NOTIFY;
            return this;
        }

        public final Properties indicate()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_INDICATE;
            return this;
        }

        public final Properties broadcast()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_BROADCAST;
            return this;
        }

        public final Properties extended_props()
        {
            m_properties |= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
            return this;
        }

        public final CharacteristicBuilder build()
        {
            m_charBuilder.setProperties(m_properties);
            return m_charBuilder;
        }

        public final CharacteristicPermissions setPermissions()
        {
            return new CharacteristicPermissions(build());
        }

        public final GattDatabase completeService()
        {
            return build().completeService();
        }
    }


    public static class Permissions<T extends Permissions>
    {

        private int m_permissions;


        public final T read()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ;
            return (T) this;
        }

        public final T read_encrypted()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
            return (T) this;
        }

        public final T read_encrypted_mitm()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
            return (T) this;
        }

        public final T write()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE;
            return (T) this;
        }

        public final T readWrite()
        {
            return (T) read().write();
        }

        public final T signed_write()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED;
            return (T) this;
        }

        public final T signed_write_mitm()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;
            return (T) this;
        }

        public final T write_encrypted()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
            return (T) this;
        }

        public final T write_encrypted_mitm()
        {
            m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
            return (T) this;
        }


        final int getPermissions()
        {
            return m_permissions;
        }

    }

    public final static class CharacteristicPermissions extends Permissions<CharacteristicPermissions>
    {

        private final CharacteristicBuilder m_charBuilder;


        private CharacteristicPermissions(CharacteristicBuilder builder)
        {
            m_charBuilder = builder;
        }

        public final Properties setProperties()
        {
            return new Properties(build());
        }

        public final CharacteristicBuilder build()
        {
            m_charBuilder.setPermissions(getPermissions());
            return m_charBuilder;
        }

        public final ServiceBuilder completeChar()
        {
            return build().build();
        }

        public final GattDatabase completeService()
        {
            return build().completeService();
        }
    }

    public final static class DescriptorPermissions extends Permissions<DescriptorPermissions>
    {

        private final DescriptorBuilder m_descBuilder;


        private DescriptorPermissions(DescriptorBuilder builder)
        {
            m_descBuilder = builder;
        }


        public final DescriptorBuilder build()
        {
            m_descBuilder.setPermissions(getPermissions());
            return m_descBuilder;
        }

        public final CharacteristicBuilder completeDesc()
        {
            return build().build();
        }

        public final ServiceBuilder completeChar()
        {
            return build().completeChar();
        }

        public final GattDatabase completeService()
        {
            return build().completeService();
        }

    }

}
