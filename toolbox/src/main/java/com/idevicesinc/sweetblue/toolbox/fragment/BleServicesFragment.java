package com.idevicesinc.sweetblue.toolbox.fragment;

import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.BleCharacteristicsActivity;
import com.idevicesinc.sweetblue.toolbox.activity.BleServicesActivity;
import com.idevicesinc.sweetblue.toolbox.view.BleServiceAdapter;
import java.util.ArrayList;
import java.util.List;


public class BleServicesFragment extends Fragment implements BleServicesActivity.Listener
{

    private BleDevice m_device;
    private ListView m_serviceListView;
    private ArrayList<BluetoothGattService> m_serviceList;
    private BleServiceAdapter m_adapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.layout_service_list, null);

        m_device = ((BleServicesActivity) getActivity()).getDevice();

        List<BluetoothGattService> serviceList = m_device.getNativeServices_List();

        if (serviceList == null)
        {
            serviceList = new ArrayList<>(0);
        }

        m_serviceList = new ArrayList<>(serviceList);

        m_adapter = new BleServiceAdapter(getContext(), m_serviceList);

        m_serviceListView = (ListView) layout.findViewById(R.id.serviceListView);

        m_serviceListView.setAdapter(m_adapter);
        m_serviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                final BluetoothGattService service = m_serviceList.get(position);
                Intent intent = new Intent(getActivity(), BleCharacteristicsActivity.class);
                intent.putExtra("mac", m_device.getMacAddress());
                intent.putExtra("uuid", service.getUuid().toString());
                startActivity(intent);
            }
        });

        return layout;
    }

    public BleServicesFragment register(BleServicesActivity activity)
    {
        activity.registerListener(this);
        return this;
    }

    @Override
    public void onEvent(BleDevice.StateListener.StateEvent e)
    {
        if (m_device != null && m_adapter != null)
        {
            if (m_device.getNativeServices_List() != null && m_device.getNativeServices_List().size() > 0)
            {
                m_serviceList.addAll(m_device.getNativeServices_List());
                m_adapter.notifyDataSetChanged();
            }
        }
    }
}
