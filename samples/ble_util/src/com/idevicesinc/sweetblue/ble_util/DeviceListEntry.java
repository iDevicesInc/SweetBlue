package com.idevicesinc.sweetblue.ble_util;

import android.content.Context;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Info;
import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Please;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.State.ChangeIntent;

/**
 * 
 * @author dougkoellmer
 */
public class DeviceListEntry extends FrameLayout implements BleDevice.StateListener
{	
	private final BleDevice m_device;
	private final Button m_connect;
	private final Button m_disconnect;
	private final Button m_bond;
	private final Button m_unbond;
	private final TextView m_status;
	private final TextView m_name;
	
	public DeviceListEntry(Context context, BleDevice device)
	{
		super(context);
		
		m_device = device;
		m_device.setListener_State(this);
		m_device.setListener_ConnectionFail(new BleDevice.DefaultConnectionFailListener()
		{
			@Override public Please onConnectionFail(Info moreInfo)
			{
				Please please = super.onConnectionFail(moreInfo);
				
				if( please == Please.DO_NOT_RETRY )
				{
					Toast.makeText(getContext(), moreInfo.device().getName_debug() + " connection failed with " + moreInfo.failureCountSoFar() + " retries - " + moreInfo.reason(), Toast.LENGTH_LONG).show();
				}
				
				return please;
			}
		});
		
		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.device_entry, null);
		
		m_connect = (Button) inner.findViewById(R.id.connect_button);
		m_disconnect = (Button) inner.findViewById(R.id.disconnect_button);
		m_bond = (Button) inner.findViewById(R.id.bond_button);
		m_unbond = (Button) inner.findViewById(R.id.unbond_button);
		m_status = (TextView) inner.findViewById(R.id.device_status);
		m_name = (TextView) inner.findViewById(R.id.device_name);
		
		m_connect.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.connect();
			}
		});
		
		m_disconnect.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.disconnect();
			}
		});
		
		m_bond.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.bond();
			}
		});
		
		m_unbond.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.unbond();
			}
		});

		updateStatus(m_device.getStateMask());
		
		String name = m_device.getName_normalized();
		if( name.length() == 0 )
		{
			name = m_device.getMacAddress();
		}
		else
		{
			name += "(" + m_device.getMacAddress() + ")";
		}
		m_name.setText(name);
		
		this.addView(inner);
		
		if( device.getLastDisconnectIntent() == ChangeIntent.UNINTENTIONAL )
		{
			device.connect();
		}
	}
	
	public BleDevice getDevice()
	{
		return m_device;
	}
	
	private void updateStatus(int deviceStateMask)
	{
		SpannableString status = Utils.makeStateString(BleDeviceState.values(), deviceStateMask);
		m_status.setText(status);
	}

	@Override public void onStateChange(ChangeEvent event)
	{
		updateStatus(event.newStateBits());
	}
}
