package com.idevicesinc.sweetblue;

import android.Manifest.permission;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.Utils;

final class P_WakeLockManager
{
	private static final String WAKE_LOCK_TAG = "SWEETBLUE_WAKE_LOCK";
	
	private int m_count;
	private final WakeLock m_wakeLock;
	private final BleManager m_mngr;
	
	public P_WakeLockManager(BleManager mngr, boolean enabled)
	{
		m_mngr = mngr;
		
		if( enabled )
		{
			if( !Utils.hasPermission(mngr.getApplicationContext(), permission.WAKE_LOCK) )
			{
				Log.e(P_WakeLockManager.class.getSimpleName(), "PERMISSION REQUIRED: " + permission.WAKE_LOCK + ". Or set BleManagerConfig#manageCpuWakeLock to false to disable wake lock management.");
				
				m_wakeLock = null;
				
				return;
			}
			
			final PowerManager powerMngr = (PowerManager) m_mngr.getApplicationContext().getSystemService(Context.POWER_SERVICE);

			m_wakeLock = powerMngr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
		}
		else
		{
			m_wakeLock = null;
		}
	}
	
	public void clear()
	{
		if( m_count >= 1 )
		{
			releaseLock();
		}
		
		m_count = 0;
	}
	
	public void push()
	{
		m_count++;
		
		if( m_count == 1 )
		{
			if( m_wakeLock != null )
			{
				m_wakeLock.acquire();
			}
		}
	}
	
	private void releaseLock()
	{
		if( m_wakeLock == null )  return;
		
		try
		{
			m_wakeLock.release();
		}
		
		//--- DRK > Just looking at the source for release(), it can throw a RuntimeException if it's somehow
		//---		overreleased, like maybe app mismanages it. Just being defensive here.
		catch(RuntimeException e)
		{
			m_mngr.ASSERT(false, e.getMessage());
		}
	}
	
	public void pop()
	{
		m_count--;
		
		if( m_count == 0 )
		{
			releaseLock();
		}
		else if( m_count < 0 )
		{
			m_count = 0;
		}
	}
}
