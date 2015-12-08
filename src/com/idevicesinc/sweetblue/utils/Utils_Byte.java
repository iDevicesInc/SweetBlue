package com.idevicesinc.sweetblue.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for byte and bit twiddling.
 */
public class Utils_Byte extends Utils
{
	private Utils_Byte(){super();}

	public static int toBits(final BitwiseEnum ... enums)
	{
		int bits = 0x0;

		for( int i = 0; i < enums.length; i++ )
		{
			bits |= enums[i].bit();
		}

		return bits;
	}

	public static byte[] hexStringToBytes(String string)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		for(int idx = 0; idx + 2 <= string.length(); idx += 2)
		{
			String hexStr = string.substring(idx, idx + 2);
			int intValue = Integer.parseInt(hexStr, 16);
			baos.write(intValue);
		}

		return baos.toByteArray();
	}

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

	public static byte[] subBytes(byte[] source, int sourceBegin_index_inclusive, int sourceEnd_index_exclusive)
	{
		byte[] destination = new byte[sourceEnd_index_exclusive - sourceBegin_index_inclusive];
		System.arraycopy(source, sourceBegin_index_inclusive, destination, 0, sourceEnd_index_exclusive - sourceBegin_index_inclusive);
		return destination;
	}

	public static byte[] subBytes(final byte[] source, final int sourceBegin)
	{
		return subBytes(source, sourceBegin, source.length-1);
	}

	public static void memcpy(byte[] dest, byte[] source, int size, int destOffset, int sourceOffset)
	{
		for(int i = 0; i < size; i++)
		{
			dest[i+destOffset] = source[i+sourceOffset];
		}
	}

	public static void memcpy(byte[] dest, byte[] source, int size)
	{
		memcpy(dest, source, size, /*destOffset=*/0, /*destOffset=*/0);
	}

	public static void memset(byte[] data, byte value, int size)
	{
		for(int i = 0; i < size; i++)
		{
			data[i] = value;
		}
	}

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

	public static int getIntValue(byte[] data)
	{
		//--- DRK > Have to pad it out from 3 to 4 bytes then flip byte endianness...not required in iOS version.
		byte[] data_padded = new byte[4];
		memcpy(data_padded, data, data.length);
		int value = ByteBuffer.wrap(data_padded).getInt();
		value = Integer.reverseBytes(value);

		return value;
	}

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

	public static short unsignedByte(byte value)
	{
		return (short) (value & 0xff);
	}

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

	public static short bytesToShort(byte[] b)
	{
		short result = 0;
		for( short i = 0; i < 2; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}

		return result;
	}

	public static byte boolToByte(final boolean value)
	{
		return (byte) (value ? 0x1 : 0x0);
	}

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

	public static int bytesToInt(byte[] b)
	{
		int result = 0;
		for( int i = 0; i < 4; i++ )
		{
			result <<= 8;

			if( i < b.length )
			{
				result |= (b[i] & 0xFF);
			}
		}

		return result;
	}

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

	public static long bytesToLong(byte[] b)
	{
		long result = 0;
		for( int i = 0; i < 8; i++ )
		{
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}
}
