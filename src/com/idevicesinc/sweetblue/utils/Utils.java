package com.idevicesinc.sweetblue.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.util.Log;

/**
 * Some static utility methods that are probably not very useful outside the library.
 */
public class Utils
{
	private static final String TAG = Utils.class.getName();

	static boolean requiresBonding(BluetoothGattCharacteristic characteristic)
	{
		return false;
	}
	
	/**
	 * For now returns true if and only if {@link Build#MANUFACTURER} is Sony.
	 */
	public static boolean isSony()
	{
		return Build.MANUFACTURER.equalsIgnoreCase("sony");
	}

	public static boolean isOnMainThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	public static boolean isSuccess(int gattStatus)
	{
		return gattStatus == 0;// || gattStatus == 1;
	}
	
	public static String normalizeDeviceName(String deviceName)
	{
		if( deviceName == null )  return "";
		
		String[] nameParts = deviceName.split("-");
		String consistentName = nameParts[0];
		consistentName = consistentName.toLowerCase();
		consistentName = consistentName.replace(" ", "_");
		
		return consistentName;
	}

	public static boolean haveMatchingIds(List<UUID> advertisedIds, Collection<UUID> lookedForIds)
	{
		if(lookedForIds != null && !lookedForIds.isEmpty())
		{
			boolean match = false;

			for(int i = 0; i < advertisedIds.size(); i++)
			{
				if(lookedForIds.contains(advertisedIds.get(i)))
				{
					match = true;
					break;
				}
			}

			if(!match)
				return false;
		}

		return true;
	}

	public static List<UUID> parseServiceUuids(final byte[] advertisedData)
	{
		List<UUID> uuids = new ArrayList<UUID>();
		 
		 if( advertisedData == null )  return uuids;

		int offset = 0;
		while(offset < (advertisedData.length - 2))
		{
			int len = advertisedData[offset++];
			if(len == 0)
				break;

			int type = advertisedData[offset++];
			switch(type)
			{
				case 0x02: // Partial list of 16-bit UUIDs
				case 0x03: // Complete list of 16-bit UUIDs
					while(len > 1)
					{
						int uuid16 = advertisedData[offset++];
						uuid16 += (advertisedData[offset++] << 8);
						len -= 2;
						uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
					}
					break;
				case 0x06:// Partial list of 128-bit UUIDs
				case 0x07:// Complete list of 128-bit UUIDs
					  // Loop through the advertised 128-bit UUID's.
					while(len >= 16)
					{
						try
						{
							// Wrap the advertised bits and order them.
							ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
							long mostSignificantBit = buffer.getLong();
							long leastSignificantBit = buffer.getLong();
							uuids.add(new UUID(leastSignificantBit, mostSignificantBit));
						}
						catch(IndexOutOfBoundsException e)
						{
							// Defensive programming.
							Log.e(TAG, e.toString());
							continue;
						}
						finally
						{
							// Move the offset to read the next uuid.
							offset += 15;
							len -= 16;
						}
					}
					break;
				default:
					offset += (len - 1);
					break;
			}
		}

		return uuids;
	}

	private static final String SHORTHAND_UUID_TEMPLATE = "00000000-0000-1000-8000-00805f9b34fb";
	private static final int SHORTHAND_LENGTH = 8;

	/**
	 * Get a UUID string by transforming shorthand ids into long form ids.
	 * 
	 * @param shorthandIf Short of long form UUID string for a service/characteristic
	 * @return UUID string.
	 */
	public static String uuidStringFromShorthand(String shorthandUuid)
	{
		// Check for shorthand form
		if(shorthandUuid.length() <= SHORTHAND_LENGTH)
		{
			// Pad with any missing zeros
			long l = Long.parseLong(shorthandUuid, 16);
			shorthandUuid = String.format("%0" + SHORTHAND_LENGTH + "x", l);
			// Overwrite the shorthand places in the template with those provided
			String result = shorthandUuid + SHORTHAND_UUID_TEMPLATE.substring(SHORTHAND_LENGTH);
			return result;
		}
		else
		{
			return shorthandUuid;
		}
	}

	/**
	 * Get a UUID by transforming shorthand ids into long form ids.
	 * 
	 * @param id Short of long form UUID string for a service/characteristic
	 * @return UUID.
	 */
	static public UUID uuidFromShorthand(String shorthandUuid)
	{
		return UUID.fromString(uuidStringFromShorthand(shorthandUuid));
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

	public static byte[] subBytes(byte[] source, int sourceBegin, int sourceEnd)
	{
		byte[] destination = new byte[sourceEnd - sourceBegin];
		System.arraycopy(source, sourceBegin, destination, 0, sourceEnd - sourceBegin);
		return destination;
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

	public static String getStringValue(byte[] data)
	{
		String string = null;
		byte[] value = data;

		if(value != null && value.length > 0)
		{
			try
			{
				string = new String(value, "UTF-8");
			}
			catch(UnsupportedEncodingException e)
			{
				return "";
			}

			string = string.trim();
		}

		return string;
	}

	private static class FlagOnStyle extends CharacterStyle
	{
		@Override public void updateDrawState(TextPaint tp)
		{
			tp.setColor(0xFF006400);
		}
	};

	private static class FlagOffStyle extends CharacterStyle
	{
		@Override public void updateDrawState(TextPaint tp)
		{
			tp.setColor(0xFFFF0000);
			tp.setStrikeThruText(true);
		}
	};
	
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

	public static SpannableString makeStateString(BitwiseEnum[] states, int stateMask)
	{
		String rawString = "";
		String spacer = "  ";

		for(int i = 0; i < states.length; i++)
		{
			String name = ((Enum) states[i]).name();
			rawString += name + spacer;
		}

		SpannableString spannableString = new SpannableString(rawString);

		int position = 0;
		for(int i = 0; i < states.length; i++)
		{
			String name = ((Enum) states[i]).name();

			if(states[i].overlaps(stateMask))
			{
				spannableString.setSpan(new FlagOnStyle(), position, position + name.length(), 0x0);
			}
			else
			{
				spannableString.setSpan(new FlagOffStyle(), position, position + name.length(), 0x0);
			}

			position += name.length() + spacer.length();
		}

		return spannableString;
	}
	
	public static boolean hasPermission(Context context, String permission)
	{
		int res = context.checkCallingOrSelfPermission(permission);
		
	    return (res == PackageManager.PERMISSION_GRANTED);
	}
}