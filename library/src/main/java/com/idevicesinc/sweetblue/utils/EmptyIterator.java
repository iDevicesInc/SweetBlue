package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.Extendable;

import java.util.Iterator;

/**
 * Convenience class for implementing an {@link java.util.Iterator} with no elements.
 */
@Extendable
public class EmptyIterator<T> implements Iterator<T>
{
	@Override public boolean hasNext()
	{
		return false;
	}

	@Override public T next()
	{
		return null;
	}

	@Override public void remove()
	{
		throw new IllegalStateException("Nothing to remove on an empty iterator.");
	}
}
