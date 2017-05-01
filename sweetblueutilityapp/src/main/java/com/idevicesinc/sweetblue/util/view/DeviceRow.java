package com.idevicesinc.sweetblue.util.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.util.R;


public class DeviceRow extends FrameLayout
{

    private static String CONNECT;
    private static String DISCONNECT;
    private static String BOND;
    private static String UNBOND;

    private BleDevice m_device;
    private final View m_content;
    private TextView m_name;
    private TextView m_status;
    private Spinner m_spinner;


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
        m_name.setText(m_device.getName_normalized());
        m_status.setText(m_device.printState());
    }

    public void clearDevice()
    {
        m_device.setListener_State((DeviceStateListener) null);
        m_device = null;
    }

    private void getViews()
    {
        m_name = (TextView) findViewById(R.id.name);
        m_status = (TextView) findViewById(R.id.deviceStatus);
        m_spinner = (Spinner) findViewById(R.id.deviceSpinner);
        m_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                final String selection = (String) parent.getItemAtPosition(position);
                if (selection.equals(CONNECT))
                {
                    connectClicked();
                }
                else if (selection.equals(DISCONNECT))
                {
                    disconnectClicked();
                }
                else if (selection.equals(BOND))
                {
                    bondClicked();
                }
                else if (selection.equals(UNBOND))
                {
                    unbondClicked();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent)
            {
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
            }
        }
    }


}
