package com.idevicesinc.sweetblue.utils;

import com.idevicesinc.sweetblue.BleServices;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Utilities for dealing with time with an emphasis on BLE.
 */
public class Utils_Time extends Utils
{
	/**
	 * Returns the current time as a byte array, useful for implementing {@link BleServices#currentTime()} for example.
	 */
	private static byte[] getCurrentTime()
	{
		final byte[] time = new byte[10];
		final byte adjustReason = 0;
		GregorianCalendar timestamp = new GregorianCalendar();

		short year = (short) timestamp.get( Calendar.YEAR );
		final byte[] year_bytes = Utils.shortToBytes(year);
		Utils.reverseBytes(year_bytes);

		System.arraycopy(year_bytes, 0, time, 0, 2);

		time[2] = (byte)(timestamp.get( Calendar.MONTH ) + 1);
		time[3] = (byte)timestamp.get( Calendar.DAY_OF_MONTH );
		time[4] = (byte)timestamp.get( Calendar.HOUR_OF_DAY );
		time[5] = (byte)timestamp.get( Calendar.MINUTE );
		time[6] = (byte)timestamp.get( Calendar.SECOND );
		time[7] = (byte)timestamp.get( Calendar.DAY_OF_WEEK );
		time[8] = 0; // 1/256 of a second
		time[9] = adjustReason;

		return time;
	}

	/**
	 * Returns the local time info as a byte array, useful for implementing {@link BleServices#currentTime()} for example.
	 */
	private static byte[] getLocalTimeInfo()
	{
		final byte[] info = new byte[2];
		TimeZone timeZone = TimeZone.getDefault();
		int offset = timeZone.getOffset( System.currentTimeMillis() );
		offset /= 1800000; // see CTS spec for why this is like this: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.time_zone.xml
		offset *= 2;
		info[0] = (byte)offset;
		final byte dst;

		if ( timeZone.useDaylightTime() && timeZone.inDaylightTime( new GregorianCalendar().getTime() ) )
		{
			final int savings = timeZone.getDSTSavings();
			dst = (byte)(savings / 900000);
		}
		else
		{
			dst = 0;
		}

		info[1] = dst;

		return info;
	}
}
