package com.idevicesinc.sweetblue;

import android.os.Handler;

import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * 
 */
abstract class PA_CallbackWrapper
{
	protected final Handler m_handler;
	protected final boolean m_forcePostToMain;
	
	PA_CallbackWrapper(Handler handler, boolean postToMain)
	{
		m_handler = handler;
		m_forcePostToMain = postToMain;
	}
	
	protected boolean postToMain()
	{
		return m_forcePostToMain && !Utils.isOnMainThread();
	}
}
