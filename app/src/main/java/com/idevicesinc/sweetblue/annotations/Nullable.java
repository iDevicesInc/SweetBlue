package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;

/**
 * This annotation is used to tag methods and parameters to give extra information
 * on how they handle <code>null</code>. SweetBlue strives to never return <code>null</code>
 * if it doesn't have to, preferring things like {@link BleDeviceState#NULL} for example,
 * but in some situations it's unavoidable.
 * <br><br>
 * NOTE: This annotation is only used in places where the library authors have judged that
 * the API by itself may be unclear about its <code>null</code> handling for newcomers.
 * In general, if this annotation isn't used, you may assume {@link Prevalence#NEVER}.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable
{
	Prevalence value();// default Prevalence.NORMAL;
	
	/**
	 * Generally used for {@link ElementType#METHOD} to hint at how often it should return <code>null</code>.
	 */
	public static enum Prevalence
	{
		/**
		 * For method return values, returning <code>null</code> is a part of normal program execution.
		 * <br><br>
		 * For method input parameters, passing <code>null</code> is normal and expected.
		 */
		NORMAL,
		
		/**
		 * For method return values, returning <code>null</code> is a rare occurrence that may signify some deeper issue.
		 * <br><br>
		 * For method input parameters, passing <code>null</code> is handled as a defensive measure but is rarely expected, and may signify some deeper issue upstream.
		 * An assertion may be thrown and unit tests may fail, but an {@link Exception} won't be thrown for program integrity's sake.
		 */
		RARE,
		
		/**
		 * For method return values, returning <code>null</code> will never happen.
		 * <br><br>
		 * For method input parameters, passing <code>null</code> is not allowed and may result in an {@link Exception}.
		 */
		NEVER;
	}
}
