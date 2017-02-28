package com.idevicesinc.sweetblue.utils;

/**
 * Simple dummy implementation of {@link FutureData} that just returns whatever is passed into the constructor.
 */
public final class PresentData implements FutureData
{
	private final byte[] m_data;

	/**
	 * The data sent to this constructor will simply be returned by {@link #getData()}.
	 */
	public PresentData(final byte[] data)
	{
		m_data = data;
	}

	/**
	 * Returns the data sent into the constructor {@link #PresentData(byte[])}
	 */
	@Override public byte[] getData()
	{
		return m_data;
	}
}
