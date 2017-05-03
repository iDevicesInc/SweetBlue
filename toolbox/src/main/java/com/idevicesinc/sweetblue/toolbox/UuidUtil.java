package com.idevicesinc.sweetblue.toolbox;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class UuidUtil
{

    public static final String CUSTOM_SERVICE = "CUSTOM SERVICE";
    public static final String CUSTOM_CHARACTERISTIC = "CUSTOM CHARACTERISTIC";

    private static Map<UUID, Field> uuidFields;


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
