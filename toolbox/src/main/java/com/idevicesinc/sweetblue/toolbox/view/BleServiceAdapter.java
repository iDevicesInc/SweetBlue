package com.idevicesinc.sweetblue.toolbox.view;


import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BleServiceAdapter extends ArrayAdapter<BluetoothGattService>
{

    private static final String CUSTOM = "CUSTOM SERVICE";


    private Map<UUID, Field> uuidFields;
    private ArrayList<BluetoothGattService> m_serviceList;


    public BleServiceAdapter(@NonNull Context context, @NonNull ArrayList<BluetoothGattService> serviceList)
    {
        super(context, R.layout.service_layout, serviceList);

        m_serviceList = serviceList;
    }

    @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        ViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(getContext(), R.layout.service_layout, null);
            h = new ViewHolder();
            h.name = (TextView) convertView.findViewById(R.id.serviceName);
            h.uuid = (TextView) convertView.findViewById(R.id.uuid);
            h.type = (TextView) convertView.findViewById(R.id.serviceType);
            convertView.setTag(h);
        }
        else
        {
            h = (ViewHolder) convertView.getTag();
        }
        final BluetoothGattService service = m_serviceList.get(position);

        final String serviceName = getServiceName(service);
        h.name.setText(serviceName);

        final String uuid = serviceName.equals(CUSTOM) ? service.getUuid().toString() : getShortUuid(service.getUuid());

        h.uuid.setText(uuid);

        final String type = service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "PRIMARY SERVICE" : "SECONDARY SERVICE";
        h.type.setText(type);

        return convertView;
    }

    private String getShortUuid(UUID uuid)
    {
        long msb = uuid.getMostSignificantBits();
        byte[] msbBytes = Utils_Byte.longToBytes(msb);
        String hex = Utils_Byte.bytesToHexString(msbBytes);
        hex = "0x" + hex.substring(4, 8);
        return hex;
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


    private final static class ViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView type;
    }

}
