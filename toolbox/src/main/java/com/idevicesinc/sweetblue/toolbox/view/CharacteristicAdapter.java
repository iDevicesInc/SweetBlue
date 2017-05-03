package com.idevicesinc.sweetblue.toolbox.view;


import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.UuidUtil;

import java.util.List;


public class CharacteristicAdapter extends ArrayAdapter<BluetoothGattCharacteristic>
{

    private final static String READ = "Read";
    private final static String WRITE = "Write";
    private final static String NOTIFY = "Notify";
    private final static String INDICATE = "Indicate";
    private final static String BROADCAST = "Broadcast";
    private final static String SIGNED_WRITE = "Signed Write";
    private final static String WRITE_NO_RESPONSE = "Write No Response";
    private final static String EXTENDED_PROPS = "Extended Properties";



    private List<BluetoothGattCharacteristic> m_characteristicList;


    public CharacteristicAdapter(@NonNull Context context, @NonNull List<BluetoothGattCharacteristic> charList)
    {
        super(context, R.layout.characteristic_layout, charList);

        m_characteristicList = charList;
    }

    @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        ViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(getContext(), R.layout.characteristic_layout, null);
            h = new ViewHolder();
            h.name = (TextView) convertView.findViewById(R.id.characteristicName);
            h.uuid = (TextView) convertView.findViewById(R.id.uuid);
            h.properties = (TextView) convertView.findViewById(R.id.properties);
            convertView.setTag(h);
        }
        else
        {
            h = (ViewHolder) convertView.getTag();
        }
        final BluetoothGattCharacteristic characteristic = m_characteristicList.get(position);
        final String name = UuidUtil.getCharacteristicName(characteristic);
        return super.getView(position, convertView, parent);
    }

    private static String getPropertyString(BluetoothGattCharacteristic characteristic)
    {
        StringBuilder b = new StringBuilder();
        int propMask = characteristic.getProperties();
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0)
        {
            b.append(BROADCAST);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(READ);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(WRITE_NO_RESPONSE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(WRITE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(NOTIFY);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(INDICATE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(SIGNED_WRITE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(EXTENDED_PROPS);
        }
        return b.toString();
    }

    private static final class ViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView properties;
        private TextView value;
    }
}
