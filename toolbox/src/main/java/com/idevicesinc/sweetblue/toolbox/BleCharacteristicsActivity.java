package com.idevicesinc.sweetblue.toolbox;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ExpandableListView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.toolbox.view.CharacteristicAdapter;
import java.util.List;
import java.util.UUID;


public class BleCharacteristicsActivity extends BaseActivity
{

    private BleDevice m_device;
    private BluetoothGattService m_service;
    private List<BluetoothGattCharacteristic> m_characteristicList;

    private CharacteristicAdapter m_adapter;
    private ExpandableListView m_charListView;



    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characteristics);

        final String mac = getIntent().getStringExtra("mac");
        final String uuid = getIntent().getStringExtra("uuid");
        m_device = BleManager.get(this).getDevice(mac);
        m_service = m_device.getNativeService(UUID.fromString(uuid));
        m_characteristicList = m_service.getCharacteristics();

        m_adapter = new CharacteristicAdapter(m_characteristicList);

        m_charListView = find(R.id.expandingListView);

        m_charListView.setAdapter(m_adapter);

    }
}
