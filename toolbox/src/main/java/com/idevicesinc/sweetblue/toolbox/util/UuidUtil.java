package com.idevicesinc.sweetblue.toolbox.util;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import android.content.Context;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class UuidUtil
{

    public static String CUSTOM_SERVICE = "CUSTOM SERVICE";
    public static String CUSTOM_CHARACTERISTIC = "CUSTOM CHARACTERISTIC";
    public static String CUSTOM_DESCRIPTOR = "CUSTOM DESCRIPTOR";

    private static Map<UUID, Field> uuidFields;

    public static void makeStrings(Context context)
    {
        CUSTOM_SERVICE = context.getString(R.string.custom_service);

        CUSTOM_CHARACTERISTIC = context.getString(R.string.custom_characteristic);

        CUSTOM_DESCRIPTOR = context.getString(R.string.custom_descriptor);
    }

    public static String getServiceName(BluetoothGattService service)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(service.getUuid());
        if (field == null)
        {
            return CUSTOM_SERVICE;
        }
        else
        {
            return field.getName().replace("_UUID", "").replace("_", " ");
        }
    }

    public static String getCharacteristicName(BluetoothGattCharacteristic characteristic)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(characteristic.getUuid());
        if (field == null)
        {
            return CUSTOM_CHARACTERISTIC;
        }
        else
        {
            return field.getName().replace("_UUID", "").replace("_", " ");
        }
    }

    public static String getDescriptorName(BluetoothGattDescriptor descriptor)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(descriptor.getUuid());
        if (field == null)
        {
            return CUSTOM_DESCRIPTOR;
        }
        else
        {
            return field.getName().replace("_UUID", "").replace("_", " ");
        }
    }

    public static Map<UUID, Field> getUuidFields()
    {
        if (uuidFields == null)
        {
            try
            {
                Field[] fields = Uuids.class.getDeclaredFields();
                Map<UUID, Field> map = new HashMap<>(fields.length);
                for (Field f : fields)
                {
                    if (f.getType() == UUID.class)
                    {
                        map.put((UUID) f.get(f), f);
                    }
                }
                return map;
            } catch (Exception e)
            {
                e.printStackTrace();
                return new HashMap<>();
            }
        }
        else
        {
            return uuidFields;
        }
    }

    public static String getShortUuid(UUID uuid)
    {
        long msb = uuid.getMostSignificantBits();
        byte[] msbBytes = Utils_Byte.longToBytes(msb);
        String hex = Utils_Byte.bytesToHexString(msbBytes);
        hex = "0x" + hex.substring(4, 8);
        return hex;
    }

}
