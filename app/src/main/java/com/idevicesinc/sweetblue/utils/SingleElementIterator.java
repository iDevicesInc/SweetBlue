package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Extendable;

import java.util.Iterator;

/**
 * Convenience class for implementing an {@link java.util.Iterator} with a single element.
 */
@Extendable
public class SingleElementIterator<T> implements Iterator<T>
{
	private T m_element;

	private boolean m_removable;

	public SingleElementIterator(T element)
	{
		m_element = element;
		m_removable = m_element != null;
	}

	@Override public boolean hasNext()
	{
		return m_element != null;
	}

	@Override public T next()
	{
		final T toReturn = m_element;

		m_element = null;

		return toReturn;
	}

	/**
	 * Optionally override this method to implement remove logic.
	 */
	protected void onRemove()
	{

	}

	@Override public void remove()
	{
		if( !m_removable )
		{
			throw new IllegalStateException("The single element has already been removed");
		}
		else
		{
			m_removable = false;

			onRemove();
		}
	}
}
