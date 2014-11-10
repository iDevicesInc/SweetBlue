package com.idevicesinc.sweetblue.ble_util;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * @author dougkoellmer
 */
public class BleStatusBar extends FrameLayout implements BleManager.StateListener, BleManager.NativeStateListener
{
	private final BleManager m_bleMngr;
	
	private final TextView m_status;
	private final TextView m_nativeStatus;
	
	public BleStatusBar(Context context, BleManager bleMngr)
	{
		super(context);
		
		m_bleMngr = bleMngr;
		m_bleMngr.setListener_State(this);
		m_bleMngr.setListener_NativeState(this);
		
		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.ble_status_bar, null);
		
		m_status = (TextView) inner.findViewById(R.id.ble_status);
		m_nativeStatus = (TextView) inner.findViewById(R.id.native_ble_status);
		
		updateStatus();
		
		this.addView(inner);
	}
	
	private void updateStatus()
	{
		SpannableString status = Utils.makeStateString(BleState.values(), m_bleMngr.getStateMask());
		m_status.setText(status);
		
		status = Utils.makeStateString(BleState.values(), m_bleMngr.getNativeStateMask());
		m_nativeStatus.setText(status);
	}
	
	@Override public void onBleStateChange(BleManager manager, int oldStateBits, int newStateBits)
	{
		updateStatus();
	}

	@Override public void onNativeBleStateChange(BleManager manager, int oldStateBits, int newStateBits)
	{
		updateStatus();
	}
}
