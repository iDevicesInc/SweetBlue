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
import com.idevicesinc.sweetblue.toolbox.UuidUtil;
import java.util.ArrayList;


public class BleServiceAdapter extends ArrayAdapter<BluetoothGattService>
{

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

        final String serviceName = UuidUtil.getServiceName(service);
        h.name.setText(serviceName);

        final String uuid = serviceName.equals(UuidUtil.CUSTOM_SERVICE) ? service.getUuid().toString() : UuidUtil.getShortUuid(service.getUuid());

        h.uuid.setText(uuid);

        final String type = service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "PRIMARY SERVICE" : "SECONDARY SERVICE";
        h.type.setText(type);

        return convertView;
    }


    private final static class ViewHolder
    {
        private TextView name;
        private TextView uuid;
        private TextView type;
    }

}
