package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.*;

/**
 * Interface implemented by some enums and other {@link Immutable} structs
 * that have custom "null" behavior in place of Java's built-in <code>null</code>.
 */
public interface UsesCustomNull
{
	/**
	 * Returns <code>true</code> if the object's state represents what would otherwise be Java's built-in <code>null</code>.
	 */
	boolean isNull();
}
