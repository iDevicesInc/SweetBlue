package com.idevicesinc.sweetblue.toolbox.view;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.BleCharacteristicsActivity;
import com.idevicesinc.sweetblue.toolbox.activity.WriteValueActivity;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.toolbox.util.ViewUtil;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CharacteristicAdapter extends BaseExpandableListAdapter
{
    private static String READ;
    private static String WRITE;
    private static String NOTIFY;
    private static String INDICATE;
    private static String BROADCAST;
    private static String SIGNED_WRITE;
    private static String WRITE_NO_RESPONSE;
    private static String EXTENDED_PROPS;

    private BleCharacteristicsActivity mParent;
    private BleDevice mDevice;
    private BluetoothGattService mService;
    private Map<BluetoothGattCharacteristic, List<BluetoothGattDescriptor>> mCharDescMap;
    private List<BluetoothGattCharacteristic> mCharacteristicList;

    public CharacteristicAdapter(BleCharacteristicsActivity parent, @NonNull BleDevice device, @NonNull BluetoothGattService service, @NonNull List<BluetoothGattCharacteristic> charList)
    {
        mParent = parent;

        READ = mParent.getString(R.string.read);
        WRITE = mParent.getString(R.string.write);
        NOTIFY = mParent.getString(R.string.notify);
        INDICATE = mParent.getString(R.string.indicate);
        BROADCAST = mParent.getString(R.string.broadcast);
        SIGNED_WRITE = mParent.getString(R.string.signed_write);
        EXTENDED_PROPS = mParent.getString(R.string.extended_properties);
        WRITE_NO_RESPONSE = mParent.getString(R.string.write_no_response);
        mDevice = device;
        mService = service;
        mCharDescMap = new HashMap<>(charList.size());
        mCharacteristicList = charList;

        for (BluetoothGattCharacteristic ch : charList)
            mCharDescMap.put(ch, ch.getDescriptors());

        Collections.sort(mCharacteristicList, new CharacteristicComparator());
    }

    @Override public int getGroupCount()
    {
        return mCharacteristicList.size();
    }

    @Override public int getChildrenCount(int groupPosition)
    {
        final BluetoothGattCharacteristic ch = mCharacteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> dList = mCharDescMap.get(ch);
        int count = dList != null ? dList.size() : 0;
        return count;
    }

    @Override public BluetoothGattCharacteristic getGroup(int groupPosition)
    {
        return mCharacteristicList.get(groupPosition);
    }

    @Override public BluetoothGattDescriptor getChild(int groupPosition, int childPosition)
    {
        final BluetoothGattCharacteristic ch = mCharacteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> dList = mCharDescMap.get(ch);
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

    @Override public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, final ViewGroup parent)
    {
        final BluetoothGattCharacteristic characteristic = mCharacteristicList.get(groupPosition);
        final Context context = parent.getContext();
        final ExpandableListView elv = (ExpandableListView)parent;

        // Figure out how we shuold format the characteristic
        Uuids.GATTCharacteristic gc = Uuids.GATTCharacteristic.getCharacteristicForUUID(characteristic.getUuid());
        Uuids.GATTDisplayType dt = gc != null ? gc.getDisplayType() : Uuids.GATTDisplayType.Hex;

        final CharViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(context, R.layout.characteristic_layout, null);

            h = new CharViewHolder();
            h.parentLayout = (RelativeLayout) convertView.findViewById(R.id.parentLayout);
            h.name = (TextView) convertView.findViewById(R.id.characteristicName);
            h.uuid = (TextView) convertView.findViewById(R.id.uuid);
            h.uuidOriginalTextSize = h.uuid.getTextSize();
            h.properties = (TextView) convertView.findViewById(R.id.properties);
            h.valueDisplayTypeLabel = (TextView) convertView.findViewById(R.id.valueDisplayTypeLabel);
            h.value = (TextView) convertView.findViewById(R.id.value);
            h.displayType = dt;
            h.expandArrow = (ImageView) convertView.findViewById(R.id.expandArrow);

            // We have to do the following to 'fix' clicking on the cell itself
            h.parentLayout.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (elv.isGroupExpanded(groupPosition))
                        elv.collapseGroup(groupPosition);
                    else
                        elv.expandGroup(groupPosition);
                }
            });

            {
                View v = convertView.findViewById(R.id.fakeOverflowMenu);

                final View anchor = convertView.findViewById(R.id.fakeOverflowMenuAnchor);

                v.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        //Creating the instance of PopupMenu
                        PopupMenu popup = new PopupMenu(context, anchor);
                        //Inflating the Popup using xml file
                        popup.getMenuInflater().inflate(R.menu.char_value_type_popup, popup.getMenu());

                        popup.getMenu().getItem(h.displayType.ordinal()).setChecked(true);

                        //registering popup with OnMenuItemClickListener
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                        {
                            public boolean onMenuItemClick(MenuItem item)
                            {
                                switch (item.getItemId())
                                {
                                    case R.id.displayTypeBoolean:
                                        h.displayType = Uuids.GATTDisplayType.Boolean;
                                        break;

                                    case R.id.displayTypeBitfield:
                                        h.displayType = Uuids.GATTDisplayType.Bitfield;
                                        break;

                                    case R.id.displayTypeUnsignedInteger:
                                        h.displayType = Uuids.GATTDisplayType.UnsignedInteger;
                                        break;

                                    case R.id.displayTypeSignedInteger:
                                        h.displayType = Uuids.GATTDisplayType.SignedInteger;
                                        break;

                                    case R.id.displayTypeDecimal:
                                        h.displayType = Uuids.GATTDisplayType.Decimal;
                                        break;

                                    case R.id.displayTypeString:
                                        h.displayType = Uuids.GATTDisplayType.String;
                                        break;

                                    case R.id.displayTypeHex:
                                        h.displayType = Uuids.GATTDisplayType.Hex;
                                        break;
                                }

                                //TODO:  Refresh type label

                                refreshValue(h, characteristic);
                                return true;
                            }
                        });

                        popup.show();
                    }
                });
            }

            convertView.setTag(h);
        }
        else
        {
            h = (CharViewHolder) convertView.getTag();
        }

        refreshCharacteristicView(elv, groupPosition, h, characteristic);

        return convertView;
    }

    private void refreshCharacteristicView(final ExpandableListView parent, int groupPosition, final CharViewHolder cvh, final BluetoothGattCharacteristic bgc)
    {
        final String name = UuidUtil.getCharacteristicName(bgc);
        final Context context = parent.getContext();

        boolean writable = (bgc.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
        boolean readable = (bgc.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0;

        // Make value editable or not depending on what's allowed
        if (writable)
        {
            cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
            cvh.value.setVisibility(View.VISIBLE);
            cvh.value.setTextColor(context.getResources().getColor(R.color.item_title_blue));
            cvh.value.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Navigate to the write activity
                    mParent.openWriteCharacteristicActivity(mService.getUuid(), bgc.getUuid());
                }
            });
        }
        else if (readable)
        {
            // Adjust format of text
            cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
            cvh.value.setVisibility(View.VISIBLE);
            cvh.value.setTextColor(context.getResources().getColor(R.color.primary_gray));
        }
        else
        {
            // Hide the value area
            cvh.valueDisplayTypeLabel.setVisibility(View.GONE);
            cvh.value.setVisibility(View.GONE);
        }

        cvh.name.setText(name);

        final String uuid;

        if (name.equals(UuidUtil.CUSTOM_CHARACTERISTIC))
        {
            uuid = bgc.getUuid().toString();
        }
        else
        {
            uuid = UuidUtil.getShortUuid(bgc.getUuid());
        }
        cvh.uuid.setText(uuid);

        final String properties = getPropertyString(bgc);

        cvh.properties.setText(properties);

        // Update expand arrow
        {
            if (getChildrenCount(groupPosition) < 1)
                cvh.expandArrow.setVisibility(View.GONE);
            else
                cvh.expandArrow.setVisibility(View.VISIBLE);

            boolean expanded = parent.isGroupExpanded(groupPosition);
            cvh.expandArrow.setImageResource(expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
        }

        // Remove ripple if not clickable
        {
            if (getChildrenCount(groupPosition) < 1)
                cvh.parentLayout.setBackground(null);
        }

        refreshValue(cvh, bgc);

        // Shrink text if too large...
        //FIXME:  Find a less horrible way of doing this

        // First, reset the text to the original size
        cvh.uuid.setTextSize(TypedValue.COMPLEX_UNIT_PX, cvh.uuidOriginalTextSize);

        // Now, post a runnable to shrink it down
        ViewUtil.fixOversizedText(cvh.uuid);
    }


    private void refreshValue(CharViewHolder cvh, BluetoothGattCharacteristic bgc)
    {
        {
            String valueString = cvh.name.getContext().getString(R.string.loading);

            if ((bgc.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
                valueString = cvh.name.getContext().getString(R.string.write_value);

            if (bgc.getValue() != null)
            {
                try
                {
                    Uuids.GATTDisplayType dt = Uuids.GATTDisplayType.values()[cvh.displayType.ordinal()];

                    valueString = dt.toString(bgc.getValue());
                }
                catch (Exception e)
                {
                    valueString = "<" + cvh.name.getContext().getString(R.string.error) + ">";
                }
            }
            cvh.value.setText(valueString);

            cvh.value.setVisibility(View.VISIBLE);
            cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
        }
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

        final BluetoothGattCharacteristic characteristic = mCharacteristicList.get(groupPosition);
        final List<BluetoothGattDescriptor> descList = mCharDescMap.get(characteristic);
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

        // Set the UUID
        h.uuid.setText(uuid);

        // Show value (as hex, for now)
        String hexString = (descriptor != null && descriptor.getValue() != null ? Utils_Byte.bytesToHexString(descriptor.getValue()) : "<null>");
        h.value.setText(hexString);

        // Text shrink hack
        ViewUtil.fixOversizedText(h.uuid);

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
        mDevice.read(characteristic.getUuid(), new BleDevice.ReadWriteListener()
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
        private RelativeLayout parentLayout;
        private TextView name;
        private TextView uuid;
        private float uuidOriginalTextSize;
        private TextView properties;
        private TextView valueDisplayTypeLabel;
        private TextView value;
        private Uuids.GATTDisplayType displayType;
        private ImageView expandArrow;
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

            return value;
        }

        public int compare(BluetoothGattCharacteristic bgc1, BluetoothGattCharacteristic bgc2)
        {
            return valueForCharacteristic(bgc2) - valueForCharacteristic(bgc1);
        }
    }
}
