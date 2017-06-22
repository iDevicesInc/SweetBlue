package com.idevicesinc.sweetblue.toolbox.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.utils.Interval;


public class DeviceRow extends FrameLayout
{

    private final static String RSSI_BASE = "%d dBm";

    private static String CONNECT;
    private static String DISCONNECT;
    private static String BOND;
    private static String UNBOND;

    private BleDevice m_device;
    private final View m_content;
    private TextView m_name;
    private TextView m_status;
    private TextView m_rssi;
    private ReselectableSpinner m_spinner;
    private TextView m_connectTextView;
    private TextView m_bondTextView;

    public DeviceRow(Context context)
    {
        super(context);
        m_content = LayoutInflater.from(context).inflate(R.layout.device_layout, null);
        addView(m_content);
        if (CONNECT == null)
        {
            CONNECT = context.getString(R.string.connect);
            DISCONNECT = context.getString(R.string.disconnect);
            BOND = context.getString(R.string.bond);
            UNBOND = context.getString(R.string.unbond);
        }
        getViews();
    }

    public void setBleDevice(BleDevice device)
    {
        m_device = device;
        m_device.setListener_State(new StateListener());
        m_name.setText(m_device.getName_debug());
        m_status.setText(m_device.printState());
        m_rssi.setText(m_device.getRssiPercent().toString());
    }

    public void clearDevice()
    {
        m_device.setListener_State((DeviceStateListener) null);
        m_device = null;
    }

    public boolean hasDevice()
    {
        return m_device != null;
    }

    public boolean isConnected()
    {
        return hasDevice() && m_device.is(BleDeviceState.INITIALIZED);
    }

    public String macAddress()
    {
        return hasDevice() ? m_device.getMacAddress() : "00:00:00:00:00:00";
    }

    private void refreshConnectTextView()
    {
        // Inspect device state and update labels accordingly
        if (!hasDevice())
            return;

        if (m_device.is(BleDeviceState.DISCONNECTED))
            m_connectTextView.setText(CONNECT);
        else
            m_connectTextView.setText(DISCONNECT);
    }

    private void refreshBondTextView()
    {
        // Inspect device state and update labels accordingly
        if (!hasDevice())
            return;

        if (m_device.isAny(BleDeviceState.BONDED, BleDeviceState.BONDING))
            m_bondTextView.setText(UNBOND);
        else
            m_bondTextView.setText(BOND);
    }

    private void getViews()
    {
        m_name = (TextView) findViewById(R.id.name);
        m_status = (TextView) findViewById(R.id.deviceStatus);
        m_rssi = (TextView) findViewById(R.id.rssi);

        m_connectTextView = (TextView) findViewById(R.id.connectTextView);
        m_connectTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Perform action depending on what our text says
                if (m_connectTextView.getText().equals(CONNECT))
                    connectClicked();
                else if (m_connectTextView.getText().equals(DISCONNECT))
                    disconnectClicked();

                refreshConnectTextView();
            }
        });

        m_bondTextView = (TextView) findViewById(R.id.bondTextView);
        m_bondTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Perform action depending on what our text says
                if (m_bondTextView.getText().equals(BOND))
                    bondClicked();
                else if (m_bondTextView.getText().equals(UNBOND))
                    unbondClicked();

                refreshBondTextView();
            }
        });
    }

    private void unbondClicked()
    {
        if (m_device != null)
        {
            m_device.unbond();
        }
    }

    private void bondClicked()
    {
        if (m_device != null)
        {
            m_device.bond();
        }
    }

    private void disconnectClicked()
    {
        if (m_device != null)
        {
            m_device.disconnect();
        }
    }

    private void connectClicked()
    {
        if (m_device != null)
        {
            m_device.connect();
        }
    }

    private final class StateListener implements DeviceStateListener
    {

        @Override public void onEvent(BleDevice.StateListener.StateEvent e)
        {
            if (m_device != null)
            {
                m_status.setText(m_device.printState());

                m_content.setBackgroundColor(getResources().getColor(hasDevice() && isConnected() ? R.color.green : R.color.white));

                refreshConnectTextView();

                refreshBondTextView();
            }
        }
    }


}
