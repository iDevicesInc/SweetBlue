package com.idevicesinc.sweetblue.ble_util;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.idevicesinc.sweetblue.*;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * 
 * @author dougkoellmer
 */
public class BleBar extends LinearLayout
{
	private static final Interval SCAN_TIMEOUT = Interval.secs(5.0);
	
	private final Button m_enable;
	private final Button m_disable;
	private final Button m_unbondAll;
	private final Button m_nuke;
	private final Button m_infiniteScan;
	private final Button m_xSec;
	private final Button m_xSecRepeated;
	private final Button m_stop;
	
	private final BleManager m_bleMngr;
	
	public BleBar(Context context, BleManager bleMngr)
	{
		super(context);
		
		m_bleMngr = bleMngr;
		
		setOrientation(VERTICAL);
		int padding = context.getResources().getDimensionPixelSize(R.dimen.default_padding);
		this.setPadding(padding, padding, padding, padding);
		
		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.ble_button_bar, null);
		
		m_enable = (Button) inner.findViewById(R.id.ble_enable_button);
		m_disable = (Button) inner.findViewById(R.id.ble_disable_button);
		m_unbondAll = (Button) inner.findViewById(R.id.ble_unbond_all_button);
		m_nuke = (Button) inner.findViewById(R.id.ble_nuke_button);
		
		
		m_enable.setOnClickListener(new OnClickListener()
		{	
			@Override public void onClick(View v)
			{
				m_bleMngr.turnOn();
			}
		});
		
		m_disable.setOnClickListener(new OnClickListener()
		{	
			@Override public void onClick(View v)
			{
				m_bleMngr.turnOff();
			}
		});
		
		m_unbondAll.setOnClickListener(new OnClickListener()
		{	
			@Override public void onClick(View v)
			{
				m_bleMngr.unbondAll();
			}
		});
		
		m_nuke.setOnClickListener(new OnClickListener()
		{	
			@Override public void onClick(View v)
			{
				m_bleMngr.dropTacticalNuke();
			}
		});
		
		m_infiniteScan = (Button) inner.findViewById(R.id.scan_infinite_button);
		m_xSec = (Button) inner.findViewById(R.id.scan_for_x_sec_button);
		m_xSecRepeated = (Button) inner.findViewById(R.id.scan_for_x_sec_repeated_button);
		m_stop = (Button) inner.findViewById(R.id.scan_stop_button);
		
		m_infiniteScan.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_bleMngr.startScan();
			}
		});
		
		m_xSec.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_bleMngr.startScan(SCAN_TIMEOUT);
			}
		});
		
		m_xSecRepeated.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_bleMngr.startPeriodicScan(SCAN_TIMEOUT, SCAN_TIMEOUT);
			}
		});
		
		m_stop.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				//--- DRK > Catch-all, stop both periodic and manual scanning.
				m_bleMngr.stopPeriodicScan();
				m_bleMngr.stopScan();
			}
		});
		
		int timeout = (int) SCAN_TIMEOUT.secs();
				
		m_xSec.setText(m_xSec.getText().toString().replace("{{seconds}}", timeout+""));
		String repeatedText = m_xSecRepeated.getText().toString().replace("{{seconds}}", timeout+"");
		m_xSecRepeated.setText(repeatedText);
		
		this.addView(inner);
		
		BleStatusBar scanBar = new BleStatusBar(context, bleMngr);
		scanBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.addView(scanBar);
	}
}
