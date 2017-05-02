package com.idevicesinc.sweetblue.toolbox;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleServicesActivity extends Activity
{

    private static final String CUSTOM = "CUSTOM SERVICE";
    private Map<UUID, Field> uuidFields;


    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String mac = getIntent().getStringExtra("mac");
        BleDevice device = BleManager.get(this).getDevice(mac);
        if (!device.isNull())
        {
            setTitle(device.getName_debug());
            List<BluetoothGattService> services = device.getNativeServices_List();
            StringBuilder b = new StringBuilder();
            for (BluetoothGattService s : services)
            {
                b.append(getServiceName(s)).append("\n");
            }
            String names = b.toString();
            int i = 0;
            i++;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private String getServiceName(BluetoothGattService service)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(service.getUuid());
        if (field == null)
        {
            return CUSTOM;
        }
        else
        {
            return field.getName().replace("_UUID", "").replace("_", " ");
        }
    }

    private Map<UUID, Field> getUuidFields()
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
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

}
