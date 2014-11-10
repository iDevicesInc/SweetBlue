package com.idevicesinc.sweetblue.ble_util;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.UhOh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.Window;

/**
 * 
 * @author dougkoellmer
 */
public class AlertManager implements BleManager.UhOhListener
{
	private final Context m_context;
	private final BleManager m_bleMngr;
	
	public AlertManager(Context context, BleManager bleMngr)
	{
		m_context = context;
		m_bleMngr = bleMngr;
		
		m_bleMngr.setListener_UhOh(this);
	}
	
	public void showBleNotSupported()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		final AlertDialog dialog = builder.create();
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		String dismiss = m_context.getResources().getString(R.string.generic_ok);
		String message = m_context.getResources().getString(R.string.ble_not_supported);

		OnClickListener clickListener = new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		};
		
		dialog.setMessage(message);
		dialog.setButton(DialogInterface.BUTTON_NEUTRAL, dismiss, clickListener);
		dialog.show();
	}

	@Override public void onUhOh(BleManager manager, UhOh reason)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		final AlertDialog dialog = builder.create();

		OnClickListener clickListener = new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
				
				if( which == DialogInterface.BUTTON_POSITIVE )
				{
					m_bleMngr.dropTacticalNuke();
				}
			}
		};
		
		String title = m_context.getResources().getString(R.string.uhoh_title);
		title = title.replace("{{reason}}", reason.name());
		dialog.setTitle(title);
		
		if( reason.getRemedy() == UhOh.Remedy.NUKE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_nuke));
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, m_context.getResources().getString(R.string.uhoh_message_nuke_drop), clickListener);
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, m_context.getResources().getString(R.string.uhoh_message_nuke_cancel), clickListener);
		}
		else if( reason.getRemedy() == UhOh.Remedy.RESTART_PHONE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_phone_restart));
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, m_context.getResources().getString(R.string.uhoh_message_phone_restart_ok), clickListener);
		}
		else if( reason.getRemedy() == UhOh.Remedy.WAIT_AND_SEE )
		{
			dialog.setMessage(m_context.getResources().getString(R.string.uhoh_message_weirdness));
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, m_context.getResources().getString(R.string.uhoh_message_weirdness_ok), clickListener);
		}
		
		dialog.show();
	}
}
