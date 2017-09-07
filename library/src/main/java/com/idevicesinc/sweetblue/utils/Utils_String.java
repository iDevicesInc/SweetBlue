package com.idevicesinc.sweetblue.utils;

import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Utility methods for string manipulation and creation needed by SweetBlue, mostly for debug purposes.
 */
public class Utils_String extends Utils
{
	private Utils_String(){super();}

	private static final int FRACTION_DIGITS = 2;

	private static final DecimalFormat s_toFixedFormat = new DecimalFormat();
	{
		s_toFixedFormat.setMaximumFractionDigits(FRACTION_DIGITS);
		s_toFixedFormat.setMinimumFractionDigits(FRACTION_DIGITS);
	}

	public static String toFixed(final double value)
	{
		return s_toFixedFormat.format(value);
	}

	public static String bytesToMacAddress(final byte[] raw)
	{
		return String.format("%02X:%02X:%02X:%02X:%02X:%02X", raw[0],raw[1],raw[2],raw[3],raw[4],raw[5]);
	}

	public static String normalizeMacAddress(final String macAddress)
	{
		return normalizeMacAddress_replaceDelimiters(macAddress.toUpperCase());
	}

	public static String normalizeMacAddress_replaceDelimiters(final String macAddress)
	{
		final char[] commonDelimiters = {'-', '.', ' ', '_'};

		if( macAddress == null )
		{
			return "";
		}
		else if( macAddress.length() == 0 )
		{
			return "";
		}
		else
		{
			for( int i = 0; i < commonDelimiters.length; i++ )
			{
				final String commonDelimiter_ith = String.valueOf(commonDelimiters[i]);

				if( macAddress.contains(commonDelimiter_ith) )
				{
					return macAddress.replace(commonDelimiter_ith, ":");
				}
			}
		}

		return macAddress;
	}

	public static String normalizeDeviceName(String deviceName)
	{
		if( deviceName == null || deviceName.length() == 0 )  return "";

		String[] nameParts = deviceName.split("-");
		String consistentName = nameParts[0];
		consistentName = consistentName.toLowerCase();
		consistentName = consistentName.trim();
		consistentName = consistentName.replace(" ", "_");

		return consistentName;
	}

	public static String debugizeDeviceName(String macAddress, String normalizedName, boolean isNativeDeviceNull)
	{
		String[] address_split = macAddress.split(":");
		StringBuilder b = new StringBuilder();
		b.append(normalizedName.length() == 0 ? "<no_name>" : normalizedName);
		if (!isNativeDeviceNull)
		{
			b.append("_").append(address_split[address_split.length - 2]).append(address_split[address_split.length - 1]);
		}
		return b.toString();
	}

	public static String getStringValue(final byte[] data, final String charset)
	{
		String string = "";
		byte[] value = data;

		if(value != null && value.length > 0)
		{
			try
			{
				string = new String(value, charset);
			}
			catch(UnsupportedEncodingException e)
			{
				return "";
			}

			string = string.trim();
		}

		return string;
	}

	public static String getStringValue(final byte[] data)
	{
		return getStringValue(data, "UTF-8");
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

	public static SpannableString makeStateString(State[] states, int stateMask)
	{
		String rawString = "";
		String spacer = "  ";

		for(int i = 0; i < states.length; i++)
		{
			if( states[i].isNull() )  continue;

			String name = ((Enum) states[i]).name();
			rawString += name + spacer;
		}

		SpannableString spannableString = new SpannableString(rawString);

		int position = 0;
		for(int i = 0; i < states.length; i++)
		{
			if( states[i].isNull() )  continue;

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

	public static String concatStrings(String... strings)
	{
		StringBuilder b = new StringBuilder();
		for (String s : strings)
		{
			b.append(s);
		}
		return b.toString();
	}

	public static String makeString(Object... objects)
	{
		StringBuilder builder = new StringBuilder();
		if (objects != null)
		{
			for (Object o : objects)
			{
				builder.append(o);
			}
		}
		return builder.toString();
	}

	public static String prettyFormatLogList(List<String> logEntries)
	{
		int size = logEntries == null ? 0 : logEntries.size();
		StringBuilder b = new StringBuilder();
		b.append("Log entry count: ").append(size);
		if (size > 0)
		{
			b.append("\n\n");
			b.append("Entries:\n\n[\n");
			for (int i = 0; i < size; i++)
			{
				b.append(logEntries.get(i)).append("\n\n");
			}
			b.append("]\n");
		}
		return b.toString();
	}

	public static String toString(int mask, State[] values)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");

		boolean foundFirst = false;

		for( int i = 0; i < values.length; i++ )
		{
			if( values[i].overlaps(mask) )
			{
				if( foundFirst )
				{
					builder.append(", ");
				}
				else
				{
					foundFirst = true;
				}

				builder.append(values[i]);
			}
		}

		builder.append("]");

		return builder.toString();
	}

	public static String toString(Class<?> type, Object ... values)
	{
		StringBuilder builder = new StringBuilder();

		builder.append(type.getSimpleName());

		int length_highest = 0;
		for( int i = 0; i < values.length; i+=2 )
		{
			int length_ith = values[i].toString().length();

			if( length_ith > length_highest )
			{
				length_highest = length_ith;
			}
		}

		for( int i = 0; i < values.length; i+=2 )
		{
			builder.append("\n   ");

			final int length_ith = values[i].toString().length();
			final int spaceCount = length_highest - length_ith;

			builder.append(values[i]);

			for( int j = 0; j < spaceCount; j++ )
			{
				builder.append(" ");
			}
			builder.append(" = ");
			builder.append(values[i+1]);
		}

		return builder.toString();
	}
}
