package com.idevicesinc.sweetblue.utils;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Class which simply houses static final empty constructs which are used throughout the library (rather than instantiating new ones
 * every time)
 */
public final class P_Const
{

    private static final Iterator<BluetoothGattService> EMPTY_SERVICE_ITERATOR          = new EmptyIterator<>();

    public final static List<BluetoothGattService> EMPTY_SERVICE_LIST                   = new ArrayList<BluetoothGattService>(0)
    {
        @Override public Iterator<BluetoothGattService> iterator()
        {
            return EMPTY_SERVICE_ITERATOR;
        }
    };

    public static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST     = new ArrayList<>(0);

    public static final List<BluetoothGattDescriptor> EMPTY_DESCRIPTOR_LIST             = new ArrayList<>(0);

    public final static byte[] EMPTY_BYTE_ARRAY                                         = new byte[0];

    public static final UUID[] EMPTY_UUID_ARRAY			                                = new UUID[0];

    public static final FutureData EMPTY_FUTURE_DATA	                                = new PresentData(EMPTY_BYTE_ARRAY);

}
