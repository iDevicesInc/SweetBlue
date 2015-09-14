package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.*;

/**
 * Most of the methods of core SweetBlue classes like {@link BleDevice}, {@link BleManager}, and {@link BleServer}
 * must be called from the main thread, similar to how Android will complain if you try to edit a {@link android.view.View}
 * from another thread.
 */
public class WrongThreadError extends Error
{
	public WrongThreadError()
	{
		super("This method must be called on the main thread.");
	}
}
