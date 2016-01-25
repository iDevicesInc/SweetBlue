package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.annotations.*;

/**
 * Interface implemented by some enums and mostly other {@link Immutable} structs
 * that have custom "null" behavior in place of Java's built-in <code>null</code>.
 * <br><br>
 * NOTE: There is also an informal "statically polymorphic" contract imposed by this
 * interface that says an implementor must have a <code>public static final NULL</code> member or method.
 * For enums this simply means having a NULL entry.
 * <br><br>
 * NOTE ALSO: Another soft contract imposed by this interface is that the library (in this case SweetBlue)
 * hosting the implementor must never return an actually <code>null</code> reference of the implementor.
 */
public interface UsesCustomNull
{
	/**
	 * Returns <code>true</code> if the object's state represents what would otherwise be Java's built-in <code>null</code>.
	 */
	boolean isNull();
}
