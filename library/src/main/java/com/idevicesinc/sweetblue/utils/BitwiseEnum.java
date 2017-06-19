package com.idevicesinc.sweetblue.utils;


/**
 * Contract to force <code>enum</code> implementors to comply to common bitwise operations.
 */
public interface BitwiseEnum
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
	 * Returns the bit (0x1, 0x2, 0x4, etc.) this enum represents based on the {@link #ordinal()}.
	 */
	int bit();

	/**
	 * Convenience method for checking if <code>({@link #bit()} & mask) != 0x0</code>.
	 */
	boolean overlaps(int mask);

	/**
	 * Same as {@link Enum#ordinal()}.
	 */
	int ordinal();

	/**
	 * Same as {@link Enum#name()}.
	 */
	String name();
}
