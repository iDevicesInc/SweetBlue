package com.idevicesinc.sweetblue.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleDeviceConfig;

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
 * See subclasses for more specific groups of utility methods.
 */
public class Utils
{
	protected Utils(){}

	static boolean requiresBonding(BluetoothGattCharacteristic characteristic)
	{
		return false;
	}

	public static boolean isLollipop()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	/**
	 * Returns true for certain Sony and Motorola products, which may have problems managing bonding state
	 * and so this method is used in {@link com.idevicesinc.sweetblue.BleDeviceConfig.DefaultBondFilter}. 
	 */ 
	public static boolean phoneHasBondingIssues()
	{
		return
			Utils.isManufacturer("sony")																		||
			Utils.isManufacturer("motorola") && (Utils.isProduct("ghost") || Utils.isProduct("victara"))		||
			Utils.isManufacturer("samsung") && (Utils.isProduct("degaswifiue"))									 ;
	}
	
	public static boolean isManufacturer(String manufacturer)
	{
		return Build.MANUFACTURER != null && Build.MANUFACTURER.equalsIgnoreCase(manufacturer);
	}
	
	public static boolean isProduct(String product)
	{
		return Build.PRODUCT != null && Build.PRODUCT.contains(product);
	}

	public static boolean isOnMainThread()
	{
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	public static void enforceMainThread()
	{
		if( !isOnMainThread() )
		{
			throw new WrongThreadException();
		}
	}

	public static boolean isSuccess(int gattStatus)
	{
		return gattStatus == 0;// || gattStatus == 1;
	}

	public static boolean contains(final Object[] uuids, final Object uuid)
	{
		for( int i = 0; i < uuids.length; i++ )
		{
			final Object ith = uuids[i];

			if( ith.equals(uuid) )
			{
				return true;
			}
		}

		return false;
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

	public static boolean hasPermission(Context context, String permission)
	{
		int res = context.checkCallingOrSelfPermission(permission);
		
	    return (res == PackageManager.PERMISSION_GRANTED);
	}
}
