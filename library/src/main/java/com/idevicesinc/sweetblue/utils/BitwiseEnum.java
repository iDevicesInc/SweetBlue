package com.idevicesinc.sweetblue.utils;


/**
 * Contract to force <code>enum</code> implementors to comply to common bitwise operations.
 */
public interface BitwiseEnum extends Flag
{
	/**
	 * Does a bitwise OR for this state and the given state.
	 */
	int or(BitwiseEnum state);

	/**
	 * Does a bitwise OR for this state and the given bits.
	 */
	int or(int bits);

	/**
	 * Convenience method for checking if <code>({@link #bit()} & mask) != 0x0</code>.
	 */
	boolean overlaps(int mask);

	/**
	 * Same as {@link Enum#name()}.
	 */
	String name();
}
