package com.idevicesinc.sweetblue.ble_util;

import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.State.ChangeIntent;

/**
 * 
 * @author dougkoellmer
 */
public class DeviceList extends ScrollView implements BleManager.DiscoveryListener
{
	private static final int BASE_COLOR = 0x00115395;
	private static final int LIGHT_ALPHA = 0x33000000;
	private static final int DARK_ALPHA = 0x44000000;
	
	private final LinearLayout m_list;
	private final BleManager m_bleMngr;
	
	public DeviceList(Context context, BleManager bleMngr)
	{
		super(context);
		
		m_bleMngr = bleMngr;
		m_bleMngr.setListener_Discovery(this);
		
		m_list = new LinearLayout(context);
		m_list.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		m_list.setOrientation(LinearLayout.VERTICAL);
		this.addView(m_list);
	}
	
	private void colorList()
	{
		for( int i = 0; i < m_list.getChildCount(); i++ )
		{
			View ithView = m_list.getChildAt(i);
			
			int alphaMask = i%2 == 0 ? LIGHT_ALPHA : DARK_ALPHA;
			int color = BASE_COLOR | alphaMask;
			
			ithView.setBackgroundColor(color);
		}
	}
 
	@Override public void onDiscoveryEvent(DiscoveryEvent event)
	{
		if( event.was(LifeCycle.DISCOVERED) )
		{
			DeviceListEntry entry = new DeviceListEntry(getContext(), event.device());
			entry.setLayoutParams(new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			entry.setBackgroundColor(0xff00ff00);
			
			m_list.addView(entry);
			
			colorList();
		}
		else if( event.was(LifeCycle.UNDISCOVERED) )
		{
			for( int i = 0; i < m_list.getChildCount(); i++ )
			{
				DeviceListEntry entry = (DeviceListEntry) m_list.getChildAt(i);
				
				if( entry.getDevice().equals(event.device()) )
				{
					m_list.removeViewAt(i);
					colorList();
					
					return;
				}
			}
		}
	}
}
