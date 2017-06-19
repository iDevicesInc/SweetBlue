package com.idevicesinc.sweetblue.toolbox.view;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.UuidUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CharacteristicAdapter extends BaseExpandableListAdapter
{

    private final static String READ = "Read";
    private final static String WRITE = "Write";
    private final static String NOTIFY = "Notify";
    private final static String INDICATE = "Indicate";
    private final static String BROADCAST = "Broadcast";
    private final static String SIGNED_WRITE = "Signed Write";
    private final static String WRITE_NO_RESPONSE = "Write No Response";
    private final static String EXTENDED_PROPS = "Extended Properties";


    private Map<BluetoothGattCharacteristic, List<BluetoothGattDescriptor>> m_charDescMap;
    private List<BluetoothGattCharacteristic> m_characteristicList;


    public CharacteristicAdapter(@NonNull List<BluetoothGattCharacteristic> charList)
    {
        m_charDescMap = new HashMap<>(charList.size());
        m_characteristicList = charList;
        for (BluetoothGattCharacteristic ch : charList)
        {
            m_charDescMap.put(ch, ch.getDescriptors());
        }
    }

    @Override public int getGroupCount()
    {
        return m_characteristicList.size();
    }

    @Override public int getChildrenCount(int groupPosition)
    {
        final BluetoothGattCharacteristic ch = m_characteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> dList = m_charDescMap.get(ch);
        return dList != null ? dList.size() : 0;
    }

    @Override public BluetoothGattCharacteristic getGroup(int groupPosition)
    {
        return m_characteristicList.get(groupPosition);
    }

    @Override public BluetoothGattDescriptor getChild(int groupPosition, int childPosition)
    {
        final BluetoothGattCharacteristic ch = m_characteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> dList = m_charDescMap.get(ch);
        return dList != null ? dList.get(childPosition) : null;
    }

    @Override public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    @Override public boolean hasStableIds()
    {
        return true;
    }

    @Override public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        final CharViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(parent.getContext(), R.layout.characteristic_layout, null);
            h = new CharViewHolder();
            h.name = (TextView) convertView.findViewById(R.id.characteristicName);
            h.uuid = (TextView) convertView.findViewById(R.id.uuid);
            h.properties = (TextView) convertView.findViewById(R.id.properties);

            convertView.setTag(h);
        }
        else
        {
            h = (CharViewHolder) convertView.getTag();
        }

        final BluetoothGattCharacteristic characteristic = m_characteristicList.get(groupPosition);
        final String name = UuidUtil.getCharacteristicName(characteristic);
        h.name.setText(name);

        final String uuid;

        if (name.equals(UuidUtil.CUSTOM_CHARACTERISTIC))
        {
            uuid = characteristic.getUuid().toString();
        }
        else
        {
            uuid = UuidUtil.getShortUuid(characteristic.getUuid());
        }
        h.uuid.setText(uuid);

        final String properties = getPropertyString(characteristic);

        h.properties.setText(properties);
        return convertView;
    }

    @Override public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        final DescViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(parent.getContext(), R.layout.descriptor_layout, null);
            h = new DescViewHolder();
            h.name = (TextView) convertView.findViewById(R.id.descriptorName);
            h.uuid = (TextView) convertView.findViewById(R.id.uuid);
            h.value = (TextView) convertView.findViewById(R.id.value);
            convertView.setTag(h);
        }
        else
        {
            h = (DescViewHolder) convertView.getTag();
        }

        final BluetoothGattCharacteristic characteristic = m_characteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> descList = m_charDescMap.get(characteristic);
        final BluetoothGattDescriptor descriptor = descList.get(childPosition);

        final String name = UuidUtil.getDescriptorName(descriptor);
        h.name.setText(name);

        final String uuid;
        if (name.equals(UuidUtil.CUSTOM_DESCRIPTOR))
        {
            uuid = descriptor.getUuid().toString();
        }
        else
        {
            uuid = UuidUtil.getShortUuid(descriptor.getUuid());
        }

        h.uuid.setText(uuid);
        return convertView;
    }

    @Override public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return false;
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


    private static final class CharViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView properties;
        private TextView value;
    }

    private static final class DescViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView value;
    }
}
