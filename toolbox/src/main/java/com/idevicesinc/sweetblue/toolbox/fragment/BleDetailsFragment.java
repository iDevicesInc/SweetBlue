package com.idevicesinc.sweetblue.toolbox.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.BleServicesActivity;
import com.idevicesinc.sweetblue.utils.AdvertisingFlag;
import com.idevicesinc.sweetblue.utils.BleScanInfo;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleDetailsFragment extends Fragment implements BleServicesActivity.Listener
{

    private BleDevice m_device;

    private TextView m_status;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.layout_bledevice_details, null);

        m_device = ((BleServicesActivity) getActivity()).getDevice();

        m_status = (TextView) layout.findViewById(R.id.status);

        m_status.setText(m_device.printState());

        setupViews(layout);

        return layout;
    }

    private void setupViews(final View view)
    {

        TextView nativeName = (TextView) view.findViewById(R.id.nativeName);

        nativeName.setText(m_device.getName_native());

        TextView mac = (TextView) view.findViewById(R.id.macAddress);
        mac.setText(m_device.getMacAddress());

        final BleScanInfo info = m_device.getScanInfo();

        TextView txPowerLabel = (TextView) view.findViewById(R.id.txPowerLabel);
        TextView txPower = (TextView) view.findViewById(R.id.txPower);
        TextView advNameLabel = (TextView) view.findViewById(R.id.advNameLabel);
        TextView advName = (TextView) view.findViewById(R.id.advName);
        TextView serviceUuidLabel = (TextView) view.findViewById(R.id.serviceUuidLabel);
        TextView serviceUuids = (TextView) view.findViewById(R.id.serviceUuids);
        TextView manIdLabel = (TextView) view.findViewById(R.id.manufacturerIdLabel);
        TextView manId = (TextView) view.findViewById(R.id.manufacturerId);
        TextView manDataLabel = (TextView) view.findViewById(R.id.manufacturerDataLabel);
        TextView manData = (TextView) view.findViewById(R.id.manufacturerData);
        TextView advFlagsLabel = (TextView) view.findViewById(R.id.advFlagsLabel);
        TextView advFlags = (TextView) view.findViewById(R.id.advFlags);

        if (info.getTxPower().value != null && info.getTxPower().value != -1)
        {
            txPower.setText(String.valueOf(m_device.getTxPower()));
        }
        else
        {
            txPower.setVisibility(View.GONE);
            txPowerLabel.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(info.getName()))
        {
            advName.setText(info.getName());
        }
        else
        {
            advNameLabel.setVisibility(View.GONE);
            advName.setVisibility(View.GONE);
        }

        if (info.getServiceUUIDS().size() > 0 || info.getServiceData().size() > 0)
        {
            List<UUID> uuids = info.getServiceUUIDS();
            Map<UUID, byte[]> data = info.getServiceData();
            StringBuilder b = new StringBuilder();
            boolean both = info.getServiceUUIDS().size() > 0 && info.getServiceData().size() > 0;
            for (UUID u : uuids)
            {
                b.append(getString(R.string.uuid_colon)).append(" ").append(u).append("\n");
            }
            if (both)
            {
                b.append("\n");
            }
            for (UUID u : data.keySet())
            {
                b.append(R.string.uuid_colon).append(" ").append(u).append("\n").append(getString(R.string.data_colon)).append(" ").append(Utils_Byte.bytesToHexString(data.get(u))).append("\n");
            }
            serviceUuids.setText(b.toString());
        }
        else
        {
            serviceUuidLabel.setVisibility(View.GONE);
            serviceUuids.setVisibility(View.GONE);
        }

        if (info.getManufacturerId() != -1)
        {
            manId.setText(String.valueOf(info.getManufacturerId()));
        }
        else
        {
            manIdLabel.setVisibility(View.GONE);
            manId.setVisibility(View.GONE);
        }

        if (info.getManufacturerData() != null && info.getManufacturerData().length > 0)
        {
            manData.setText(Utils_Byte.bytesToHexString(info.getManufacturerData()));
        }
        else
        {
            manDataLabel.setVisibility(View.GONE);
            manData.setVisibility(View.GONE);
        }

        if (info.getAdvFlags().value != null && info.getAdvFlags().value != -1)
        {
            StringBuilder b = new StringBuilder();
            for (AdvertisingFlag flag : AdvertisingFlag.values())
            {
                if (flag.overlaps(info.getAdvFlags().value))
                {
                    b.append(flag.name().replace("_", " ")).append("\n");
                }
            }
            advFlags.setText(b.toString());
        }
        else
        {
            advFlagsLabel.setVisibility(View.GONE);
            advFlags.setVisibility(View.GONE);
        }
    }

    public BleDetailsFragment register(BleServicesActivity activity)
    {
        activity.registerListener(this);
        return this;
    }

    @Override
    public void onEvent(BleDevice.StateListener.StateEvent e)
    {
        if (m_status != null && m_device != null)
        {
            m_status.setText(m_device.printState());
        }
    }
}
