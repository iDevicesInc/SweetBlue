package com.idevicesinc.sweetblue.toolbox.view;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.Collections;
import java.util.Comparator;
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

    private BleDevice m_device;
    private Map<BluetoothGattCharacteristic, List<BluetoothGattDescriptor>> m_charDescMap;
    private List<BluetoothGattCharacteristic> m_characteristicList;

    public CharacteristicAdapter(@NonNull BleDevice device, @NonNull List<BluetoothGattCharacteristic> charList)
    {
        m_device = device;
        m_charDescMap = new HashMap<>(charList.size());
        m_characteristicList = charList;

        Collections.sort(m_characteristicList, new CharacteristicComparator());

        for (BluetoothGattCharacteristic ch : charList)
        {
            m_charDescMap.put(ch, ch.getDescriptors());

            // Start updating each characteristic
            m_device.read(ch.getUuid(), new BleDevice.ReadWriteListener()
            {
                @Override
                public void onEvent(ReadWriteEvent e)
                {
                    // Refresh the UI
                    notifyDataSetChanged();
                }
            });
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
            h.valueLabel = (TextView) convertView.findViewById(R.id.valueLabel);
            h.value = (TextView) convertView.findViewById(R.id.value);

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

        {  //TODO:  Make dynamic based on if we can read or not
            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            {
                h.value.setVisibility(View.GONE);
                h.valueLabel.setVisibility(View.GONE);
            }
            else
            {
                String valueString = "Loading...";
                try
                {
                    // Look up the value type to use for this characteristic
                    Uuids.GATTCharacteristic gc = Uuids.GATTCharacteristic.getCharacteristicForUUID(characteristic.getUuid());

                    if (gc != null)
                    {
                        valueString = gc.getDisplayType().toString(characteristic.getValue());

                    }
                    else
                        valueString = Uuids.GATTCharacteristicDisplayType.Hex.toString(characteristic.getValue());
                }
                catch (Exception e)
                {
                    Log.d("OhNoes", "something bad happened");
                }
                h.value.setText(valueString);

                h.value.setVisibility(View.VISIBLE);
                h.valueLabel.setVisibility(View.VISIBLE);
            }
        }

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

    private void readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        m_device.read(characteristic.getUuid(), new BleDevice.ReadWriteListener()
        {
            @Override
            public void onEvent(ReadWriteEvent e)
            {
                // Update corresponding item here
            }
        });
    }

    private static final class CharViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView properties;
        private TextView valueLabel;
        private TextView value;
    }

    private static final class DescViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView value;
    }

    private static class CharacteristicComparator implements Comparator<BluetoothGattCharacteristic>
    {
        public int valueForCharacteristic(BluetoothGattCharacteristic bgc)
        {
            int value = 0;

            int properties = bgc.getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
                value = 3;
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                value = value == 0 ? 1 : 2;

            Log.d("++--", "char named " + bgc.getUuid() + " has sort key " + value);

            return value;
        }

        public int compare(BluetoothGattCharacteristic bgc1, BluetoothGattCharacteristic bgc2)
        {
            return valueForCharacteristic(bgc2) - valueForCharacteristic(bgc1);
        }
    }
}
