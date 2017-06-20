package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.*;

/**
 * Most of the methods of core SweetBlue classes like {@link BleDevice}, {@link BleManager}, and {@link BleServer}
 * must be called from the main thread, similar to how Android will complain if you try to edit a {@link android.view.View}
 * from another thread.
 *
 * @see BleManagerConfig#allowCallsFromAllThreads
 */
public final class WrongThreadError extends Error
{
	public WrongThreadError(final String message)
	{
		super(message);
	}
}
