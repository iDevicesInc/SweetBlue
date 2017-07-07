package com.idevicesinc.sweetblue.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for byte and bit twiddling.
 */
public final class Utils_Byte extends Utils
{

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();


	private Utils_Byte(){super();}


	/**
	 * Converts a var-args of {@link BitwiseEnum}s to an int of their bit values
     */
	public static int toBits(final BitwiseEnum ... enums)
	{
		int bits = 0x0;

		for( int i = 0; i < enums.length; i++ )
		{
			bits |= enums[i].bit();
		}

		return bits;
	}

	/**
	 * Convert a hex string into a byte array
     */
	public static byte[] hexStringToBytes(String string)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		//TODO:  Remove 0x prefix, ignore whitespace

		for(int idx = 0; idx + 2 <= string.length(); idx += 2)
		{
			String hexStr = string.substring(idx, idx + 2);
			int intValue = Integer.parseInt(hexStr, 16);
			baos.write(intValue);
		}

		return baos.toByteArray();
	}

	/**
	 * Convert a byte array to a hex string
     */
	public static String bytesToHexString(final byte[] bytes)
	{
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Convert a byte array to a hex string
	 */
	public static String bytesToBinaryString(final byte[] bytes)
	{
		return bytesToBinaryString(bytes, -1);
	}

	/**
	 * Convert a byte array to a hex string
	 * Also inserts spaces between every count bytes (for legibility)
	 */
	public static String bytesToBinaryString(final byte[] bytes, int bytesBetweenSpaces)
	{
		if (bytes == null)
			return null;

		StringBuilder sb = new StringBuilder();

		int count = 0;

		for (byte b : bytes)
		{
			for (int i = 0; i < 4; ++i)
			{
				int downShift = (3 - i) * 2;
				int quarterByte = (b>>downShift) & 0x3;
				switch (quarterByte)
				{
					case 0x0:
						sb.append("00");
						break;
					case 0x1:
						sb.append("01");
						break;
					case 0x2:
						sb.append("10");
						break;
					case 0x3:
						sb.append("11");
						break;
				}
			}

			if (++count >= bytesBetweenSpaces && bytesBetweenSpaces != -1)
				sb.append(' ');
		}

		return sb.toString();
	}

	public static byte[] binaryStringToBytes(String s) throws Exception
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Iterate over the string and make a byte array
		int b = 0;
		int count;
		for (count = 0; count < s.length();)
		{
			int index = s.length() - 1 - count;
			char c = s.charAt(index);

			if (c != '0' && c != '1')
			{
				// Whitespace is simply ignored
				if (Character.isWhitespace(c))
					continue;

				// Anything else is an error
				//FIXME:  Custom exception type
				throw new Exception("Illegal character " + c + " encountered while parsing binary string");
			}

			int val = c == '1' ? 1 : 0;
			b >>>= 1;
			b |= (val<<7);

			if (((++count) % 8) == 0)
			{
				os.write(b);
				b = 0;
			}
		}

		// Final byte (if needed)
		int extraBytes = (count % 8);
		if (extraBytes > 0)
		{
			b >>>= (8-extraBytes);
			os.write(b);
		}

		// Make, then reverse the output array
		byte output[] = os.toByteArray();
		for (int i = 0; i < output.length / 2; ++i)
		{
			byte temp = output[i];
			int swapIndex = output.length - 1 - i;
			output[i] = output[swapIndex];
			output[swapIndex] = temp;
		}

		return output;
	}

	// Not sure what this was added for. It seems like it's for a pretty specific case. Marking as deprecated for now,
	// and will remove in 3.0, unless we get complaints
	@Deprecated
	public static List<byte[]> fileToBinaryDataList(Context context, String file, int offset)
	{
		List<byte[]> binaryData = new ArrayList<byte[]>();
		BufferedReader reader = null;

		try
		{
			InputStream stream = context.getAssets().open(file);

			String currentLine;
			reader = new BufferedReader(new InputStreamReader(stream));

			while((currentLine = reader.readLine()) != null)
			{
				String rawLine = currentLine.substring(1);
				//				Log.d("", ".");
				//				Log.d("", "rawLine:     " + rawLine);
				byte[] data = hexStringToBytes(rawLine);
				//				Log.d("", "data_before: " + bytesToHex(data));

				long data_1 = 0x0 | data[1];
				data_1 <<= 8;
				data_1 &= 0xff00;

				long data_2 = 0x0 | data[2];
				data_2 &= 0x00ff;

				long addr = data_1 + data_2;

				//				Log.d("", "addr:        "+addr);
				//				Log.d("", "offset:      "+offset);
				long type = data[3];
				type &= 0x00ff;
				//				Log.d("", "type:        "+type);
				if((type == 0) && (addr < offset))
				{
					continue;
				}

				// patch up address
				addr -= offset;
				data[1] = (byte) ((addr & 0xff00) >>> 8);
				data[2] = (byte) (addr & 0xff);

				//				Log.d("", "data_after: "+bytesToHex(data));
				//				Log.d("", ".");

				// Cut off checksum
				byte[] subBytes = subBytes(data, 0, data.length - 1);
				binaryData.add(subBytes);
			}
		}
		catch(IOException e)
		{
			return null;
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if(binaryData != null)
		{
			Collections.reverse(binaryData);
		}

		return binaryData;
	}

	/**
	 * Create a new byte array from the given source, with the given ranges
     */
	public static byte[] subBytes(byte[] source, int sourceBegin_index_inclusive, int sourceEnd_index_exclusive)
	{
		byte[] destination = new byte[sourceEnd_index_exclusive - sourceBegin_index_inclusive];
		System.arraycopy(source, sourceBegin_index_inclusive, destination, 0, sourceEnd_index_exclusive - sourceBegin_index_inclusive);
		return destination;
	}

	/**
	 * Create a new byte array from the given source, starting at the given begin index
     */
	public static byte[] subBytes(final byte[] source, final int sourceBegin)
	{
		return subBytes(source, sourceBegin, source.length-1);
	}

	/**
	 * Copy from one byte array to another, with the given size, and offsets
     */
	public static void memcpy(byte[] dest, byte[] source, int size, int destOffset, int sourceOffset)
	{
		for(int i = 0; i < size; i++)
		{
			dest[i+destOffset] = source[i+sourceOffset];
		}
	}

	/**
	 * Copy from one byte array to another with the given size
     */
	public static void memcpy(byte[] dest, byte[] source, int size)
	{
		memcpy(dest, source, size, /*destOffset=*/0, /*destOffset=*/0);
	}

	/**
	 * Set a value to size indexes in the given byte array
     */
	public static void memset(byte[] data, byte value, int size)
	{
		for(int i = 0; i < size; i++)
		{
			data[i] = value;
		}
	}

	/**
	 * Set a value to size indexes in the given byte array starting at the given offset
	 */
	public static void memset(byte[] data, byte value, int offset, int size)
	{
		if (offset < 0 || data.length - offset < size)
		{
			return;
		}
		for(int i = offset; i < size; i++)
		{
			data[i] = value;
		}
	}

	/**
	 * Compare two byte arrays. Returns <code>true</code> if each value matches for the given size
     */
	public static boolean memcmp(byte[] buffer1, byte[] buffer2, int size)
	{
		for(int i = 0; i < size; i++)
		{
			if(buffer1[i] != buffer2[i])
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Old method, not meant to be used any more. Please use {@link #bytesToInt(byte[])}, and {@link #reverseBytes(byte[])}, if needed.
	 * @deprecated
     */
	@Deprecated
	public static int getIntValue(byte[] data)
	{
		//--- DRK > Have to pad it out from 3 to 4 bytes then flip byte endianness...not required in iOS version.
		byte[] data_padded = new byte[4];
		memcpy(data_padded, data, data.length);
		int value = ByteBuffer.wrap(data_padded).getInt();
		value = Integer.reverseBytes(value);

		return value;
	}

	/**
	 * Reverses the byte array order. This is useful when dealing with bluetooth hardware that is in a different endianness than android.
     */
	public static void reverseBytes(byte[] data)
	{
		for( int i = 0; i < data.length/2; i++ )
		{
			byte first = data[i];
			byte last = data[data.length-1-i];

			data[i] = last;
			data[data.length-1-i] = first;
		}
	}

	/**
	 * Returns a short, from the given byte, ensuring that it is unsigned.
     */
	public static short unsignedByte(byte value)
	{
		return (short) (value & 0xff);
	}

	/**
	 * Convert a short to a byte array
     */
	public static byte[] shortToBytes(short l)
	{
		byte[] result = new byte[2];
		for( short i = 1; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to a short
     */
	public static short bytesToShort(byte[] b)
	{
		short result = 0;
		int loopLimit = Math.min(2, b.length);
		for( short i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}

		return result;
	}

	/**
	 * Outputs a {@link Pointer} from the given byte array, starting at the offset provided. If there is not enough space to
	 * create the short, the {@link Pointer} value will be <code>null</code>.
     */
	public static Pointer<Short> bytesToShort(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Short.SIZE / 8))
		{
			return new Pointer<>(null);
		}
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Short.SIZE / 8));
			return new Pointer<>(bytesToShort(bytes));
		}
	}

	/**
	 * Convert a boolean to a byte (<code>true</code> is 0x1, <code>false</code> is 0x0).
     */
	public static byte boolToByte(final boolean value)
	{
		return (byte) (value ? 0x1 : 0x0);
	}

	/**
	 * Convert a byte to a boolean
     */
	public static boolean byteToBool(byte val)
	{
		return val == 0x0 ? false : true;
	}

	/**
	 * Convert a byte to a boolean from the given byte array, at the given offset. If the offset is out of range, the {@link Pointer} value
	 * will be <code>null</code>.
     */
	public static Pointer<Boolean> byteToBool(byte[] b, int offset)
	{
		if (offset < 0 || offset >= b.length)
		{
			return new Pointer<>(null);
		}
		else
		{
			return new Pointer<>(b[offset] == 0x0 ? false : true);
		}
	}

	/**
	 * Convert an int to a byte array
     */
	public static byte[] intToBytes(int l)
	{
		byte[] result = new byte[4];
		for( int i = 3; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to an int
     */
	public static int bytesToInt(byte[] b)
	{
		int result = 0;
		int loopLimit = Math.min(4, b.length);
		for( int i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}

		return result;
	}

	/**
	 * Convert bytes to an int from the given byte array, starting at the given offset. If the offset is an invalid index (less than 0, or doesn't leave enough
	 * room in the array to create an int) the {@link Pointer} value will be <code>null</code>.
     */
	public static Pointer<Integer> bytesToInt(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Integer.SIZE / 8))
		{
			return new Pointer<>(null);
		}
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Integer.SIZE / 8));
			return new Pointer<>(bytesToInt(bytes));
		}
	}

	/**
	 * Convert a long to a byte array
     */
	public static byte[] longToBytes(long l)
	{
		byte[] result = new byte[8];
		for( int i = 7; i >= 0; i-- )
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	/**
	 * Convert a byte array to a long
     */
	public static long bytesToLong(byte[] b)
	{
		long result = 0;
		int loopLimit = Math.min(8, b.length);
		for( int i = 0; i < loopLimit; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Convert bytes to a long from the given byte array, starting at the given offset. If the offset is an invalid index (less than 0, or doesn't leave enough
	 * room in the array to create a long) the {@link Pointer} value will be <code>null</code>.
	 */
	public static Pointer<Long> bytesToLong(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Long.SIZE / 8))
		{
			return new Pointer<>(null);
		}
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Long.SIZE / 8));
			return new Pointer<>(bytesToLong(bytes));
		}
	}

	/**
	 * Convert a float to a byte array
	 */
	public static byte[] floatToBytes(float f)
	{
		int intBits = Float.floatToIntBits(f);
		return intToBytes(intBits);
	}

	/**
	 * Convert a byte array to a float
     */
	public static float bytesToFloat(byte[] b)
	{
		int intBits = bytesToInt(b);
		return Float.intBitsToFloat(intBits);
	}

	/**
	 * Convert bytes to a float from the given byte array, starting at the given offset. If the offset is an invalid index (less than 0, or doesn't leave enough
	 * room in the array to create a float) the {@link Pointer} value will be <code>null</code>.
	 */
	public static Pointer<Float> bytesToFloat(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Float.SIZE / 8))
		{
			return new Pointer<>(null);
		}
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Float.SIZE / 8));
			return new Pointer<>(bytesToFloat(bytes));
		}
	}

	/**
	 * Convert a double to a byte array
	 */
	public static byte[] doubleToBytes(double d)
	{
		long longBits = Double.doubleToLongBits(d);
		return longToBytes(longBits);
	}

	/**
	 * Convert a byte array to a double
     */
	public static double bytesToDouble(byte[] b)
	{
		long longBits = bytesToLong(b);
		return Double.longBitsToDouble(longBits);
	}

	/**
	 * Convert bytes to a double from the given byte array, starting at the given offset. If the offset is an invalid index (less than 0, or doesn't leave enough
	 * room in the array to create a double) the {@link Pointer} value will be <code>null</code>.
	 */
	public static Pointer<Double> bytesToDouble(byte[] b, int offset)
	{
		if (offset < 0 || b.length - offset < (Double.SIZE / 8))
		{
			return new Pointer<>(null);
		}
		else
		{
			byte[] bytes = subBytes(b, offset, offset + (Double.SIZE / 8));
			return new Pointer<>(bytesToDouble(bytes));
		}
	}

}
