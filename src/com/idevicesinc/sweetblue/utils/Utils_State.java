package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleDeviceState;

/**
 * Some helper utilities for dealing with {@link State} implementors.
 */
public class Utils_State extends Utils
{
	public static boolean query(final int stateMask, Object... query)
	{
		if (query == null || query.length == 0)  return false;

		final boolean internal = false;

		for (int i = 0; i < query.length; i += 2)
		{
			final Object first = query[i];
			final Object second = i + 1 < query.length ? query[i + 1] : null;

			if (first == null || second == null)  return false;

			if (!(first instanceof State) || !(second instanceof Boolean))
			{
				return false;
			}

			final State state = (State) first;
			final Boolean value = (Boolean) second;
			final boolean overlap = state.overlaps(stateMask);

			if (value && !overlap)					return false;
			else if (!value && overlap)				return false;
		}

		return true;
	}
}
