package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


abstract class PA_ServiceManager
{

    PA_ServiceManager()
    {
    }

    public abstract BleServiceWrapper getServiceDirectlyFromNativeNode(final UUID uuid);

    protected abstract List<BluetoothGattService> getNativeServiceList_original();


    public BleCharacteristicWrapper getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final BleServiceWrapper service_ith = new BleServiceWrapper(serviceList_native.get(i));
                final BleCharacteristicWrapper characteristic = getCharacteristic(service_ith, charUuid);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return BleCharacteristicWrapper.NULL;
        }
        else
        {
            final BleServiceWrapper service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);
            if (service_nullable.hasUhOh())
                return new BleCharacteristicWrapper(service_nullable.getUhOh());
            else
                return getCharacteristic(service_nullable, charUuid);
        }
    }

    public BleCharacteristicWrapper getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid, final DescriptorFilter filter)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final BleServiceWrapper service_ith = new BleServiceWrapper(serviceList_native.get(i));
                final BleCharacteristicWrapper characteristic = getCharacteristic(service_ith, charUuid, filter);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return BleCharacteristicWrapper.NULL;
        }
        else
        {
            final BleServiceWrapper service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            if (service_nullable.hasUhOh())
                return new BleCharacteristicWrapper(service_nullable.getUhOh());
            else
                return getCharacteristic(service_nullable, charUuid, filter);
        }
    }

    private BleCharacteristicWrapper getCharacteristic(final BleServiceWrapper service, final UUID charUuid)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList_native.size(); j++)
            {
                final BluetoothGattCharacteristic char_jth = charList_native.get(j);

                if (char_jth.getUuid().equals(charUuid))
                {
                    return new BleCharacteristicWrapper(char_jth);
                }
            }
        }

        return BleCharacteristicWrapper.NULL;
    }

    private BleCharacteristicWrapper getCharacteristic(final BleServiceWrapper service, final UUID charUuid, DescriptorFilter filter)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList_native.size(); j++)
            {
                final BluetoothGattCharacteristic char_jth = charList_native.get(j);

                if (char_jth.getUuid().equals(charUuid))
                {

                    if (filter == null)
                    {
                        return new BleCharacteristicWrapper(char_jth);
                    }
                    else
                    {
                        final UUID descUuid = filter.descriptorUuid();
                        if (descUuid != null)
                        {
                            final BluetoothGattDescriptor desc = char_jth.getDescriptor(filter.descriptorUuid());
                            if (desc != null)
                            {
                                final DescriptorFilter.DescriptorEvent event = new DescriptorFilter.DescriptorEvent(service.getService(), char_jth, desc, new PresentData(desc.getValue()));
                                final DescriptorFilter.Please please = filter.onEvent(event);
                                if (please.isAccepted())
                                {
                                    return new BleCharacteristicWrapper(char_jth);
                                }
                            }
                        }
                        else
                        {
                            final DescriptorFilter.DescriptorEvent event = new DescriptorFilter.DescriptorEvent(service.getService(), char_jth, null, P_Const.EMPTY_FUTURE_DATA);
                            final DescriptorFilter.Please please = filter.onEvent(event);
                            if (please.isAccepted())
                            {
                                return new BleCharacteristicWrapper(char_jth);
                            }
                        }
                    }
                }
            }
            return BleCharacteristicWrapper.NULL;
        }
        else
        {
            return BleCharacteristicWrapper.NULL;
        }
    }

    private List<BluetoothGattService> getNativeServiceList_cloned()
    {
        final List<BluetoothGattService> list_native = getNativeServiceList_original();

        return list_native == P_Const.EMPTY_SERVICE_LIST ? list_native : new ArrayList<>(list_native);
    }

    private List<BluetoothGattCharacteristic> getNativeCharacteristicList_original(final BleServiceWrapper service)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> list_native = service.getService().getCharacteristics();

            return list_native == null ? P_Const.EMPTY_CHARACTERISTIC_LIST : list_native;
        }
        else
            return P_Const.EMPTY_CHARACTERISTIC_LIST;
    }

    private List<BluetoothGattCharacteristic> getNativeCharacteristicList_cloned(final BleServiceWrapper service)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> list_native = getNativeCharacteristicList_original(service);

            return list_native == P_Const.EMPTY_CHARACTERISTIC_LIST ? list_native : new ArrayList<>(list_native);
        }
        else
            return P_Const.EMPTY_CHARACTERISTIC_LIST;
    }

    private List<BluetoothGattDescriptor> getNativeDescriptorList_original(final BleCharacteristicWrapper characteristic)
    {
        if (!characteristic.isNull())
        {
            final List<BluetoothGattDescriptor> list_native = characteristic.getCharacteristic().getDescriptors();

            return list_native == null ? P_Const.EMPTY_DESCRIPTOR_LIST : list_native;
        }
        else
            return P_Const.EMPTY_DESCRIPTOR_LIST;

    }

    private List<BluetoothGattDescriptor> getNativeDescriptorList_cloned(final BleCharacteristicWrapper characteristic)
    {
        if (!characteristic.isNull())
        {
            final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

            return list_native == P_Const.EMPTY_DESCRIPTOR_LIST ? list_native : new ArrayList<>(list_native);
        }
        else
            return P_Const.EMPTY_DESCRIPTOR_LIST;
    }

    private List<BluetoothGattCharacteristic> collectAllNativeCharacteristics(final UUID serviceUuid_nullable, final Object forEach_nullable)
    {
        final ArrayList<BluetoothGattCharacteristic> characteristics = forEach_nullable == null ? new ArrayList<BluetoothGattCharacteristic>() : null;
        final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

        for (int i = 0; i < serviceList_native.size(); i++)
        {
            final BleServiceWrapper service_ith = new BleServiceWrapper(serviceList_native.get(i));

            if (serviceUuid_nullable == null || !service_ith.isNull() && serviceUuid_nullable.equals(service_ith.getService().getUuid()))
            {
                final List<BluetoothGattCharacteristic> nativeChars = getNativeCharacteristicList_original(service_ith);

                if (forEach_nullable != null)
                {
                    if (Utils.doForEach_break(forEach_nullable, nativeChars))
                    {
                        return P_Const.EMPTY_CHARACTERISTIC_LIST;
                    }
                }
                else
                {
                    characteristics.addAll(nativeChars);
                }
            }
        }

        return characteristics;
    }

    private List<BluetoothGattDescriptor> collectAllNativeDescriptors(
            final UUID serviceUuid_nullable, final UUID charUuid_nullable, final Object forEach_nullable)
    {
        final ArrayList<BluetoothGattDescriptor> toReturn = forEach_nullable == null ? new ArrayList<BluetoothGattDescriptor>() : null;
        final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

        for (int i = 0; i < serviceList_native.size(); i++)
        {
            final BleServiceWrapper service_ith = new BleServiceWrapper(serviceList_native.get(i));

            if (serviceUuid_nullable == null || !service_ith.isNull() && serviceUuid_nullable.equals(service_ith.getService().getUuid()))
            {
                final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service_ith);

                for (int j = 0; j < charList_native.size(); j++)
                {
                    final BleCharacteristicWrapper char_jth = new BleCharacteristicWrapper(charList_native.get(j));

                    if (charUuid_nullable == null || !char_jth.isNull() && charUuid_nullable.equals(char_jth.getCharacteristic().getUuid()))
                    {
                        final List<BluetoothGattDescriptor> descriptors = getNativeDescriptorList_original(char_jth);

                        if (forEach_nullable != null)
                        {
                            if (Utils.doForEach_break(forEach_nullable, descriptors))
                            {
                                return P_Const.EMPTY_DESCRIPTOR_LIST;
                            }
                        }
                        else
                        {
                            toReturn.addAll(descriptors);
                        }
                    }
                }
            }
        }

        return toReturn;
    }

    public Iterator<BluetoothGattService> getServices()
    {
        return getServices_List().iterator();
    }

    public List<BluetoothGattService> getServices_List()
    {
        return getNativeServiceList_cloned();
    }

    public Iterator<BluetoothGattCharacteristic> getCharacteristics(
            final UUID serviceUuid_nullable)
    {
        return getCharacteristics_List(serviceUuid_nullable).iterator();
    }

    public List<BluetoothGattCharacteristic> getCharacteristics_List(
            final UUID serviceUuid_nullable)
    {
        return collectAllNativeCharacteristics(serviceUuid_nullable, /*forEach=*/null);
    }

    private BleDescriptorWrapper getDescriptor(final BleCharacteristicWrapper characteristic, final UUID descUuid)
    {
        if (!characteristic.isNull())
        {
            final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

            for (int i = 0; i < list_native.size(); i++)
            {
                final BluetoothGattDescriptor ith = list_native.get(i);

                if (ith.getUuid().equals(descUuid))
                {
                    return new BleDescriptorWrapper(ith);
                }
            }
        }

        return BleDescriptorWrapper.NULL;
    }

    private BleDescriptorWrapper getDescriptor(final BleServiceWrapper service, final UUID charUuid_nullable, final UUID descUuid)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> charList = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList.size(); j++)
            {
                final BleCharacteristicWrapper char_jth = new BleCharacteristicWrapper(charList.get(j));

                if (charUuid_nullable == null || !char_jth.isNull() && charUuid_nullable.equals(char_jth.getCharacteristic().getUuid()))
                {
                    final BleDescriptorWrapper descriptor = getDescriptor(char_jth, descUuid);

                    return descriptor;
                }
            }
        }

        return BleDescriptorWrapper.NULL;
    }

    public Iterator<BluetoothGattDescriptor> getDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return getDescriptors_List(serviceUuid_nullable, charUuid_nullable).iterator();
    }

    public List<BluetoothGattDescriptor> getDescriptors_List(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return collectAllNativeDescriptors(serviceUuid_nullable, charUuid_nullable, null);
    }

    public BleDescriptorWrapper getDescriptor(final UUID serviceUuid_nullable, final UUID charUuid_nullable, final UUID descUuid)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList = getNativeServiceList_original();

            for (int i = 0; i < serviceList.size(); i++)
            {
                final BleServiceWrapper service_ith = new BleServiceWrapper(serviceList.get(i));
                final BleDescriptorWrapper descriptor = getDescriptor(service_ith, charUuid_nullable, descUuid);

                return descriptor;
            }
        }
        else
        {
            final BleServiceWrapper service = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            if (service.hasUhOh())
                return new BleDescriptorWrapper(service.getUhOh());
            else
                return getDescriptor(service, charUuid_nullable, descUuid);
        }

        return BleDescriptorWrapper.NULL;
    }

    public void getServices(final Object forEach)
    {
        Utils.doForEach_break(forEach, getNativeServiceList_original());
    }

    public void getCharacteristics(final UUID serviceUuid, final Object forEach)
    {
        collectAllNativeCharacteristics(serviceUuid, forEach);
    }

    public void getDescriptors(final UUID serviceUuid, final UUID charUuid, final Object forEach)
    {
        collectAllNativeDescriptors(serviceUuid, charUuid, forEach);
    }

    protected static boolean equals(final BluetoothGattService one, final BluetoothGattService another)
    {
        if (one == another)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

}
