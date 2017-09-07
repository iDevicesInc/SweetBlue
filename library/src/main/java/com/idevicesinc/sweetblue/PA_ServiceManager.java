package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Pointer;
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

    public abstract NativeBleGattService getServiceDirectlyFromNativeNode(final UUID uuid);

    protected abstract List<BluetoothGattService> getNativeServiceList_original();


    public NativeBleCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final NativeBleGattService service_ith = new NativeBleGattService(serviceList_native.get(i));
                final NativeBleCharacteristic characteristic = getCharacteristic(service_ith, charUuid);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return NativeBleCharacteristic.NULL;
        }
        else
        {
            final NativeBleGattService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);
            return getCharacteristic(service_nullable, charUuid);
        }
    }

    public NativeBleCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid, final DescriptorFilter filter)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final NativeBleGattService service_ith = new NativeBleGattService(serviceList_native.get(i));
                final NativeBleCharacteristic characteristic = getCharacteristic(service_ith, charUuid, filter);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return NativeBleCharacteristic.NULL;
        }
        else
        {
            final NativeBleGattService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            return getCharacteristic(service_nullable, charUuid, filter);
        }
    }

    private NativeBleCharacteristic getCharacteristic(final NativeBleGattService service, final UUID charUuid)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList_native.size(); j++)
            {
                final BluetoothGattCharacteristic char_jth = charList_native.get(j);

                if (char_jth.getUuid().equals(charUuid))
                {
                    return new NativeBleCharacteristic(char_jth);
                }
            }
        }

        return NativeBleCharacteristic.NULL;
    }

    private NativeBleCharacteristic getCharacteristic(final NativeBleGattService service, final UUID charUuid, DescriptorFilter filter)
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
                        return new NativeBleCharacteristic(char_jth);
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
                                    return new NativeBleCharacteristic(char_jth);
                                }
                            }
                        }
                        else
                        {
                            final DescriptorFilter.DescriptorEvent event = new DescriptorFilter.DescriptorEvent(service.getService(), char_jth, null, P_Const.EMPTY_FUTURE_DATA);
                            final DescriptorFilter.Please please = filter.onEvent(event);
                            if (please.isAccepted())
                            {
                                return new NativeBleCharacteristic(char_jth);
                            }
                        }
                    }
                }
            }
            return NativeBleCharacteristic.NULL;
        }
        else
        {
            return NativeBleCharacteristic.NULL;
        }
    }

    private List<BluetoothGattService> getNativeServiceList_cloned()
    {
        final List<BluetoothGattService> list_native = getNativeServiceList_original();

        return list_native == P_Const.EMPTY_SERVICE_LIST ? list_native : new ArrayList<>(list_native);
    }

    private List<BluetoothGattCharacteristic> getNativeCharacteristicList_original(final NativeBleGattService service)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> list_native = service.getService().getCharacteristics();

            return list_native == null ? P_Const.EMPTY_CHARACTERISTIC_LIST : list_native;
        }
        else
            return P_Const.EMPTY_CHARACTERISTIC_LIST;
    }

    private List<BluetoothGattCharacteristic> getNativeCharacteristicList_cloned(final NativeBleGattService service)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> list_native = getNativeCharacteristicList_original(service);

            return list_native == P_Const.EMPTY_CHARACTERISTIC_LIST ? list_native : new ArrayList<>(list_native);
        }
        else
            return P_Const.EMPTY_CHARACTERISTIC_LIST;
    }

    private List<BluetoothGattDescriptor> getNativeDescriptorList_original(final NativeBleCharacteristic characteristic)
    {
        if (!characteristic.isNull())
        {
            final List<BluetoothGattDescriptor> list_native = characteristic.getCharacteristic().getDescriptors();

            return list_native == null ? P_Const.EMPTY_DESCRIPTOR_LIST : list_native;
        }
        else
            return P_Const.EMPTY_DESCRIPTOR_LIST;

    }

    private List<BluetoothGattDescriptor> getNativeDescriptorList_cloned(final NativeBleCharacteristic characteristic)
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
            final NativeBleGattService service_ith = new NativeBleGattService(serviceList_native.get(i));

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
            final NativeBleGattService service_ith = new NativeBleGattService(serviceList_native.get(i));

            if (serviceUuid_nullable == null || !service_ith.isNull() && serviceUuid_nullable.equals(service_ith.getService().getUuid()))
            {
                final List<BluetoothGattCharacteristic> charList_native = getNativeCharacteristicList_original(service_ith);

                for (int j = 0; j < charList_native.size(); j++)
                {
                    final NativeBleCharacteristic char_jth = new NativeBleCharacteristic(charList_native.get(j));

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

    private NativeBleDescriptor getDescriptor(final NativeBleCharacteristic characteristic, final UUID descUuid)
    {
        if (!characteristic.isNull())
        {
            final List<BluetoothGattDescriptor> list_native = getNativeDescriptorList_original(characteristic);

            for (int i = 0; i < list_native.size(); i++)
            {
                final BluetoothGattDescriptor ith = list_native.get(i);

                if (ith.getUuid().equals(descUuid))
                {
                    return new NativeBleDescriptor(ith);
                }
            }
        }

        return NativeBleDescriptor.NULL;
    }

    private NativeBleDescriptor getDescriptor(final NativeBleGattService service, final UUID charUuid_nullable, final UUID descUuid)
    {
        if (!service.isNull())
        {
            final List<BluetoothGattCharacteristic> charList = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList.size(); j++)
            {
                final NativeBleCharacteristic char_jth = new NativeBleCharacteristic(charList.get(j));

                if (charUuid_nullable == null || !char_jth.isNull() && charUuid_nullable.equals(char_jth.getCharacteristic().getUuid()))
                {
                    final NativeBleDescriptor descriptor = getDescriptor(char_jth, descUuid);

                    return descriptor;
                }
            }
        }

        return NativeBleDescriptor.NULL;
    }

    public Iterator<BluetoothGattDescriptor> getDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return getDescriptors_List(serviceUuid_nullable, charUuid_nullable).iterator();
    }

    public List<BluetoothGattDescriptor> getDescriptors_List(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return collectAllNativeDescriptors(serviceUuid_nullable, charUuid_nullable, null);
    }

    public NativeBleDescriptor getDescriptor(final UUID serviceUuid_nullable, final UUID charUuid_nullable, final UUID descUuid)
    {
        if (serviceUuid_nullable == null)
        {
            final List<BluetoothGattService> serviceList = getNativeServiceList_original();

            for (int i = 0; i < serviceList.size(); i++)
            {
                final NativeBleGattService service_ith = new NativeBleGattService(serviceList.get(i));
                final NativeBleDescriptor descriptor = getDescriptor(service_ith, charUuid_nullable, descUuid);

                return descriptor;
            }
        }
        else
        {
            final NativeBleGattService service = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            return getDescriptor(service, charUuid_nullable, descUuid);
        }

        return NativeBleDescriptor.NULL;
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
