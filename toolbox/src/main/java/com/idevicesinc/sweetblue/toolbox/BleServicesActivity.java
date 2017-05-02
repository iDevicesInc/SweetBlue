package com.idevicesinc.sweetblue.toolbox;


import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.view.BleServiceAdapter;
import com.idevicesinc.sweetblue.toolbox.view.ReselectableSpinner;
import java.util.ArrayList;


public class BleServicesActivity extends BaseActivity
{

    private BleDevice m_device;
    private ArrayList<BluetoothGattService> m_serviceList;

    private BleServiceAdapter m_adapter;
    private TextView m_status;
    private ReselectableSpinner m_connectSpinner;
    private ListView m_serviceListView;


    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleservices);

        String mac = getIntent().getStringExtra("mac");
        m_device = BleManager.get(this).getDevice(mac);
        m_serviceList = new ArrayList<>(m_device.getNativeServices_List());

        m_adapter = new BleServiceAdapter(this, m_serviceList);
        m_status = find(R.id.status);
        m_connectSpinner = find(R.id.connectSpinner);
        m_serviceListView = find(R.id.serviceListView);

        m_status.setText(m_device.printState());
        m_serviceListView.setAdapter(m_adapter);
        m_connectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                String option = (String) parent.getItemAtPosition(position);
                switch (option)
                {
                    case "Connect":
                        m_device.connect();
                        break;
                    case "Disconnect":
                        m_device.disconnect();
                        break;
                    case "Bond":
                        m_device.bond();
                        break;
                    case "Unbond":
                        m_device.unbond();
                        break;
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent)
            {
            }
        });

        m_device.setListener_State(new DeviceStateListener()
        {
            @Override public void onEvent(BleDevice.StateListener.StateEvent e)
            {
                m_status.setText(m_device.printState());
            }
        });
    }

    @Override protected void onDestroy()
    {
        m_device.setListener_State((DeviceStateListener) null);
        super.onDestroy();
    }
}
