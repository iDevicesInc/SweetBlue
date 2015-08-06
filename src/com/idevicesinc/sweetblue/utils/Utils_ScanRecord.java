package com.idevicesinc.sweetblue.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.util.Log;

/**
 * Some utilities for dealing with raw byte array scan records.
 */
public class Utils_ScanRecord extends Utils
{
	private static final String TAG = Utils_ScanRecord.class.getName();
	
	/**
	 * Adapted from a StackOverflow post (http://stackoverflow.com/a/21986475/4248895).
	 */
	public static List<UUID> parseServiceUuids(final byte[] scanRecord)
	{
		List<UUID> uuids = new ArrayList<UUID>();
		 
		if( scanRecord == null )  return uuids;

		int offset = 0;
		while(offset < (scanRecord.length - 2))
		{
			int len = scanRecord[offset++];
			if(len == 0)
				break;

			int type = scanRecord[offset++];
			switch(type)
			{
				case 0x02: // Partial list of 16-bit UUIDs
				case 0x03: // Complete list of 16-bit UUIDs
					while(len > 1)
					{
						int uuid16 = scanRecord[offset++];
						uuid16 += (scanRecord[offset++] << 8);
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
							ByteBuffer buffer = ByteBuffer.wrap(scanRecord, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
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
}
