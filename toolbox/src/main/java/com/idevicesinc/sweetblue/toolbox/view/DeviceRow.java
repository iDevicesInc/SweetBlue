package com.idevicesinc.sweetblue.toolbox.view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;


public class DeviceRow extends FrameLayout
{

    private static String CONNECT;
    private static String DISCONNECT;
    private static String BOND;
    private static String UNBOND;

    private BleDevice m_device;
    private final View m_content;
    private TextView m_name;
    private TextView m_rssi;
    private TextView m_connectTextView;
    private TextView m_bondTextView;
    private ImageView m_connectImageView;
    private ImageView m_bondImageView;


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
        m_rssi.setText(m_device.getRssiPercent().toString());
        refreshConnectTextView();
        refreshBondTextView();
        refreshRssiStatusTextView();
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
        {
            m_connectTextView.setText(CONNECT);
            Log.d("++--", "Device " + m_device.getName_normalized() + " is DISCONNECTED");
            m_connectImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.gray));
            m_connectImageView.setBackgroundResource(R.drawable.grey_ring);
        }
        else
        {
            m_connectTextView.setText(DISCONNECT);
            m_connectImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));

            if (m_device.is(BleDeviceState.CONNECTING_OVERALL))
                m_connectImageView.setBackgroundResource(R.drawable.yellow_circle);
            else
                m_connectImageView.setBackgroundResource(R.drawable.green_circle);

            Log.d("++--", "Device " + m_device.getName_normalized() + " is NOT DISCONNECTED");
        }
    }

    private void refreshRssiStatusTextView()
    {
        m_rssi.setText("Signal Strength: " + m_device.getRssiPercent());
    }

    private void refreshBondTextView()
    {
        // Inspect device state and update labels accordingly
        if (!hasDevice())
            return;

        if (m_device.isAny(BleDeviceState.BONDED, BleDeviceState.BONDING))
        {
            m_bondTextView.setText(UNBOND);
            if (m_device.is(BleDeviceState.BONDING))
            {
                m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));
                m_bondImageView.setBackgroundResource(R.drawable.yellow_circle);
            }
            else
            {
                m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));
                m_bondImageView.setBackgroundResource(R.drawable.green_circle);
            }
        }
        else
        {
            m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.gray));
            m_bondImageView.setBackgroundResource(R.drawable.grey_ring);
            m_bondTextView.setText(BOND);
        }
    }

    private void getViews()
    {
        m_name = (TextView) findViewById(R.id.name);
        m_rssi = (TextView) findViewById(R.id.rssiStatusLabel);

        m_connectImageView = (ImageView) findViewById(R.id.connectImageView);

        m_connectTextView = (TextView) findViewById(R.id.connectTextView);

        LinearLayout ll = (LinearLayout) findViewById(R.id.connectLayout);
        ll.setOnClickListener(new OnClickListener()
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


        m_bondImageView = (ImageView) findViewById(R.id.bondImageView);

        m_bondTextView = (TextView) findViewById(R.id.bondTextView);

        ll = (LinearLayout) findViewById(R.id.bondLayout);
        ll.setOnClickListener(new OnClickListener()
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
                // TODO - Filter out the current relevant state here, instead of entire state string
                updateStatus(e);

                refreshConnectTextView();

                refreshBondTextView();

            }
        }
    }

    private void updateStatus(BleDevice.StateListener.StateEvent e)
    {
        if (e.didEnter(BleDeviceState.CONNECTING) || e.didEnter(BleDeviceState.RETRYING_BLE_CONNECTION))
        {
            m_rssi.setText("Connecting...");
        }
        else if (e.didEnter(BleDeviceState.DISCOVERING_SERVICES))
        {
            m_rssi.setText("Discovering Services...");
        }
        else if (e.didEnter(BleDeviceState.AUTHENTICATING))
        {
            m_rssi.setText("Authenticating...");
        }
        else if (e.didEnter(BleDeviceState.INITIALIZING))
        {
            m_rssi.setText("Initializing...");
        }
        else if (e.didEnter(BleDeviceState.INITIALIZED))
        {
            m_rssi.setText("Connected");
        }
        else if (e.didEnter(BleDeviceState.DISCONNECTED))
        {
            m_rssi.setText("Signal Strength: " + m_device.getRssiPercent());
        }
        else if (e.didEnter(BleDeviceState.BONDING))
        {
            m_rssi.setText("Bonding...");
        }
        else if (e.didEnter(BleDeviceState.BONDED) || e.didEnter(BleDeviceState.UNBONDED))
        {
            if (m_device.is(BleDeviceState.DISCONNECTED))
            {
                m_rssi.setText("Signal Strength: " + m_device.getRssiPercent());
            }
            else if (m_device.is(BleDeviceState.INITIALIZED))
            {
                m_rssi.setText("Connected");
            }
        }
        else
        {
            return;
        }

        // Update rest of the view
        refreshConnectTextView();
        refreshBondTextView();
    }


}
