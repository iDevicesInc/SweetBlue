package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Utils_Byte;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;


final class PU_File
{
	private static String EXTENSION = "history";
	private static String FILENAME_DELIMITER = "_";

	static byte readByte(final FileInputStream in) throws IOException
	{
		byte toReturn = 0;

		{
			toReturn = (byte) in.read();
		}

		return toReturn;
	}

	static short readShort(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		short toReturn = 0;

		{
			in.read(tempBuffer, 0, 2);
			toReturn = Utils_Byte.bytesToShort(tempBuffer);
		}

		return toReturn;
	}

	static int readInt(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		int toReturn = 0;

		{
			in.read(tempBuffer, 0, 4);
			toReturn = Utils_Byte.bytesToInt(tempBuffer);
		}

		return toReturn;
	}

	static long readLong(final FileInputStream in, final byte[] tempBuffer) throws IOException
	{
		long toReturn = 0;

		{
			in.read(tempBuffer, 0, 8);
			toReturn = Utils_Byte.bytesToLong(tempBuffer);
		}

		return toReturn;
	}

	static String makeHistoryFileName(final String macAddress, final UUID uuid)
	{
		return macAddress + FILENAME_DELIMITER + uuid.toString() + "." + EXTENSION;
	}
}
