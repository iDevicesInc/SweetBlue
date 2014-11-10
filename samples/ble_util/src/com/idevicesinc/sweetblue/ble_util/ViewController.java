package com.idevicesinc.sweetblue.ble_util;

import com.idevicesinc.sweetblue.BleManager;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 
 * @author dougkoellmer
 */
public class ViewController extends LinearLayout
{
	private static enum State
	{
		DEVICE_LIST,
		DEVICE_DETAIL;
	}
	
	private final BleManager m_bleMngr;
	private final FrameLayout m_inner;
	
	private State m_state = null; 
	
	public ViewController(Context context, BleManager bleMngr)
	{
		super(context);
		
		m_bleMngr = bleMngr;
		
		this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		this.setOrientation(VERTICAL);
		
		BleBar bleStateBar = new BleBar(context, bleMngr);
		bleStateBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		bleStateBar.setBackgroundColor(0x33A21615);
		this.addView(bleStateBar);
		
		m_inner = new FrameLayout(context);
		m_inner.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		this.addView(m_inner);
		
		setState(State.DEVICE_LIST);
	}
	
	private void setState(State state)
	{
		if( m_state != null )
		{
			m_inner.removeAllViews();
		}
		
		m_state = state;
		
		View newView = null;
		
		if( m_state == State.DEVICE_LIST )
		{
			newView = new DeviceList(getContext(), m_bleMngr);
		}
		else if( m_state == State.DEVICE_DETAIL )
		{
		}
		
		if( newView != null )
		{
			newView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			m_inner.addView(newView);
		}
	}
}
