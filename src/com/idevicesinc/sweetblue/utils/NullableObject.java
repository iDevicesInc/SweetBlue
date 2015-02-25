package com.idevicesinc.sweetblue.utils;

/**
 * Tagging interface implemented by enums that use custom <code>NULL</code> entries in place of Java's built-in <code>null</code>.
 */
public interface NullableObject
{
	/**
	 * Returns <code>true</code> if the enum is meant to replace Java's built-in <code>null</code>.
	 */
	boolean isNull();
}
