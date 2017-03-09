package com.idevicesinc.sweetblue.utils;

/**
 * Provides a way to be able to change a value declared in the outer scope of an anonymous inline class.
 */
public final class Pointer<T>
{
	public T value = null;

	public Pointer(final T value_in)
	{
		this.value = value_in;
	}

	public Pointer()
	{
		this.value = null;
	}
}
