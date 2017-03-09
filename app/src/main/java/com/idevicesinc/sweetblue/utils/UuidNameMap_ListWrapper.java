package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Extendable;

import java.util.List;

/**
 * Convenience implementation that wraps a {@link java.util.List} of other {@link UuidNameMap} instances.
 */
@Extendable
public class UuidNameMap_ListWrapper implements UuidNameMap
{
	private final List<UuidNameMap> m_maps;

	public UuidNameMap_ListWrapper(final List<UuidNameMap> maps)
	{
		m_maps = maps;
	}

	public UuidNameMap_ListWrapper()
	{
		m_maps = null;
	}

	@Override public String getUuidName(String uuid)
	{
		String debugName = null;

		if( m_maps != null )
		{
			for(int i = 0; i < m_maps.size(); i++ )
			{
				String actualDebugName = m_maps.get(i).getUuidName(uuid);
				debugName = actualDebugName != null ? actualDebugName : debugName;
			}
		}

		debugName = debugName == null ? uuid : debugName;
		debugName = debugName == null ? "" : debugName;

		return debugName;
	}
}
