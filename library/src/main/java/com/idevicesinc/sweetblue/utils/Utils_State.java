package com.idevicesinc.sweetblue.utils;


/**
 * Some helper utilities for dealing with {@link State} implementors.
 */
public final class Utils_State extends Utils
{
	private Utils_State(){super();}

	public static boolean query(final int stateMask, Object... query)
	{
		if (query == null || query.length == 0)  return false;

		final boolean internal = false;

		for (int i = 0; i < query.length; i += 2)
		{
			final Object first = query[i];
			final Object second = i + 1 < query.length ? query[i + 1] : null;

			if (first == null && second == null)
			{
				return false;
			}
			else if( first != null && second != null )
			{
				if( (first instanceof State) && (second instanceof Boolean) )
				{
					final State state = (State) first;
					final Boolean value = (Boolean) second;
					final boolean overlap = state.overlaps(stateMask);

					if (value && !overlap)					return false;
					else if (!value && overlap)				return false;
				}
				else if( (first instanceof State) && (second instanceof State) )
				{
					final State state_first = (State) first;
					final State state_second = (State) second;
					final boolean overlap_first = state_first.overlaps(stateMask);
					final boolean overlap_second = state_second.overlaps(stateMask);

					if( overlap_first == false && overlap_second == false )
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
			else if( first != null && second == null )
			{
				final State state = (State) first;
				final boolean overlap = state.overlaps(stateMask);

				if( false == overlap )
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}

		return true;
	}
}
