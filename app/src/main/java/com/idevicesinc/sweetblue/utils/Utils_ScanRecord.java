/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.idevicesinc.sweetblue.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.le.*;
import android.nfc.Tag;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.SweetLogger;
import com.idevicesinc.sweetblue.annotations.Nullable;

/**
 * Some utilities for dealing with raw byte array scan records.
 * Most of the code herein is slightly modified from Android's 21-and-up scan record parsing API,
 * with code copy/pasted from various static methods of {@link ScanRecord} and android.bluetooth.BluetoothUuid.
 *
 * Why not use these classes directly? So that the same code can be used if you're targeting < 21.
 */
public final class Utils_ScanRecord extends Utils
{
	private Utils_ScanRecord(){super();}

	private static final String TAG = Utils_ScanRecord.class.getName();

	private static final int DATA_TYPE_FLAGS = 0x01;
	private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
	private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
	private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
	private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
	private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
	private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
	private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
	private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
	private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
	private static final int DATA_TYPE_SERVICE_DATA = 0x16;
	private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

	/** Length of bytes for 16 bit UUID */
	private static final int UUID_BYTES_16_BIT = 2;
	/** Length of bytes for 32 bit UUID */
	private static final int UUID_BYTES_32_BIT = 4;
	/** Length of bytes for 128 bit UUID */
	private static final int UUID_BYTES_128_BIT = 16;

	private static final ParcelUuid BASE_UUID = ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

	/**
	 *
	 */
	@Deprecated  public static @Nullable(Nullable.Prevalence.NEVER) List<UUID> parseServiceUuids(final byte[] scanRecord)
	{
		final ArrayList<UUID> serviceUuids = new ArrayList<UUID>();

		parseScanRecord(scanRecord, null, null, serviceUuids, null, null);

		return serviceUuids;
	}

	public static BleScanInfo parseScanRecord(final byte[] scanRecord)
	{
		Pointer<Integer> txPower = new Pointer<>();
		txPower.value = BleNodeConfig.INVALID_TX_POWER;

		Pointer<Integer> advFlags = new Pointer<>();
		advFlags.value = -1;

		Map<UUID, byte[]> serviceData = new HashMap<UUID, byte[]>();
		serviceData.clear();

		List<UUID> serviceUUIDs =  new ArrayList<UUID>();
		serviceUUIDs.clear();

		int mfgId = -1;
		byte[] mfgData = new byte[34];

		if(scanRecord == null)
		{
			return null;
		}

		int currentPos = 0;
		String localName = null;
		while( currentPos < scanRecord.length ) {
			// length is unsigned int.
			int length = scanRecord[currentPos++] & 0xFF;
			if (length == 0) {
				break;
			}
			// Note the length includes the length of the field type itself.
			int dataLength = length - 1;
			// fieldType is unsigned int.
			int fieldType = scanRecord[currentPos++] & 0xFF;
			switch (fieldType) {
				case DATA_TYPE_FLAGS:
					advFlags.value = scanRecord[currentPos] & 0xFF;
					break;
				case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
				case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
					parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_16_BIT, serviceUUIDs);
					break;
				case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
				case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
					parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_32_BIT, serviceUUIDs);
					break;
				case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
				case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
					parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_128_BIT, serviceUUIDs);
					break;
				case DATA_TYPE_LOCAL_NAME_SHORT:
				case DATA_TYPE_LOCAL_NAME_COMPLETE:
					try
					{
						localName = new String(extractBytes(scanRecord, currentPos, dataLength));
					}
					catch(Exception e)
					{
						Log.e(TAG, "unable to parse name");
					}
					break;
				case DATA_TYPE_TX_POWER_LEVEL:
					txPower.value = (int) scanRecord[currentPos];
					break;
				case DATA_TYPE_SERVICE_DATA:
					// The first two bytes of the service data are service data UUID in little
					// endian. The rest bytes are service data.
					try
					{
						int serviceUuidLength = UUID_BYTES_16_BIT;
						byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos, serviceUuidLength);
						UUID serviceDataUuid = parseUuidFrom(serviceDataUuidBytes);
						byte[] serviceDataArray = extractBytes(scanRecord, currentPos + serviceUuidLength, dataLength - serviceUuidLength);
						serviceData.put(serviceDataUuid, serviceDataArray);
					}
					catch(Exception e)
					{
						Log.e(TAG, "unable to parse service data");
					}

					break;
				case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
					// The first two bytes of the manufacturer specific data are
					// manufacturer ids in little endian.
					try
					{
						mfgId = ((scanRecord[currentPos + 1] & 0xFF) << 8) + (scanRecord[currentPos] & 0xFF);
						mfgData = extractBytes(scanRecord, currentPos + 2, dataLength - 2);
					}
					catch(Exception e)
					{
						Log.e(TAG, "unable to parse manufacturer data");
					}

					break;
				default:
					// Just ignore, we don't handle such data type.
					break;
			}
			currentPos += dataLength;
		}
		return new BleScanInfo(advFlags, txPower, serviceUUIDs, mfgId, mfgData, serviceData, localName);
	}

	@Deprecated public static void parseScanRecord(final byte[] scanRecord, final Pointer<Integer> advFlags_out_nullable, final Pointer<Integer> txPower_nullable, final List<UUID> serviceUuids_out_nullable, final SparseArray<byte[]> manufacturerData_out_nullable, final Map<UUID, byte[]> serviceData_out_nullable)
	{
		if( txPower_nullable != null )
		{
			txPower_nullable.value = BleNodeConfig.INVALID_TX_POWER;
		}

		if( advFlags_out_nullable != null )
		{
			advFlags_out_nullable.value = -1;
		}

		if( serviceData_out_nullable != null )
		{
			serviceData_out_nullable.clear();
		}

		if( serviceUuids_out_nullable != null )
		{
			serviceUuids_out_nullable.clear();
		}

		if( manufacturerData_out_nullable != null )
		{
			manufacturerData_out_nullable.clear();
		}

		if (scanRecord == null)
		{
			return;
		}

		int currentPos = 0;
		String localName = null;

		try
		{
			while( currentPos < scanRecord.length )
			{
				// length is unsigned int.
				int length = scanRecord[currentPos++] & 0xFF;
				if( length == 0 )
				{
					break;
				}
				// Note the length includes the length of the field type itself.
				int dataLength = length - 1;
				// fieldType is unsigned int.
				int fieldType = scanRecord[currentPos++] & 0xFF;
				switch( fieldType )
				{
					case DATA_TYPE_FLAGS:
						if( advFlags_out_nullable != null )
						{
							advFlags_out_nullable.value = scanRecord[currentPos] & 0xFF;
						}
						break;
					case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
					case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
						parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_16_BIT, serviceUuids_out_nullable);
						break;
					case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
					case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
						parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_32_BIT, serviceUuids_out_nullable);
						break;
					case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
					case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
						parseServiceUuid(scanRecord, currentPos, dataLength, UUID_BYTES_128_BIT, serviceUuids_out_nullable);
						break;
					case DATA_TYPE_LOCAL_NAME_SHORT:
					case DATA_TYPE_LOCAL_NAME_COMPLETE:
						localName = new String(extractBytes(scanRecord, currentPos, dataLength));
						break;
					case DATA_TYPE_TX_POWER_LEVEL:
						if( txPower_nullable != null )
						{
							txPower_nullable.value = (int) scanRecord[currentPos];
						}
						break;
					case DATA_TYPE_SERVICE_DATA:
						// The first two bytes of the service data are service data UUID in little
						// endian. The rest bytes are service data.
						int serviceUuidLength = UUID_BYTES_16_BIT;
						byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos, serviceUuidLength);
						UUID serviceDataUuid = parseUuidFrom(serviceDataUuidBytes);
						byte[] serviceDataArray = extractBytes(scanRecord, currentPos + serviceUuidLength, dataLength - serviceUuidLength);

						if( serviceData_out_nullable != null )
						{
							serviceData_out_nullable.put(serviceDataUuid, serviceDataArray);
						}
						break;
					case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
						// The first two bytes of the manufacturer specific data are
						// manufacturer ids in little endian.
						int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) + (scanRecord[currentPos] & 0xFF);
						byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2, dataLength - 2);

						if( manufacturerData_out_nullable != null )
						{
							manufacturerData_out_nullable.put(manufacturerId, manufacturerDataBytes);
						}
						break;
					default:
						// Just ignore, we don't handle such data type.
						break;
				}
				currentPos += dataLength;
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
		}
	}

	public static String parseName(byte[] scanRecord) {
		String name = "<NO_NAME>";
		if (scanRecord == null)
		{
			return name;
		}
		int currentPos = 0;
		try
		{
			while( currentPos < scanRecord.length )
			{
				// length is unsigned int.
				int length = scanRecord[currentPos++] & 0xFF;
				if( length == 0 )
				{
					break;
				}
				// Note the length includes the length of the field type itself.
				int dataLength = length - 1;
				// fieldType is unsigned int.
				int fieldType = scanRecord[currentPos++] & 0xFF;
				switch( fieldType )
				{
					case DATA_TYPE_LOCAL_NAME_COMPLETE:
						String n = new String(extractBytes(scanRecord, currentPos, dataLength));
						if (!TextUtils.isEmpty(n)) {
							name = n;
						}
						break;
					default:
						// Just ignore, we don't handle such data type.
						break;
				}
				currentPos += dataLength;
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
		}
		return name;
	}

	// Parse service UUIDs.
	private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength, int uuidLength, final List<UUID> serviceUuids_nullable)
	{
		try
		{
			while( dataLength > 0 )
			{
				byte[] uuidBytes = extractBytes(scanRecord, currentPos, uuidLength);

				if( serviceUuids_nullable != null )
				{
					serviceUuids_nullable.add(parseUuidFrom(uuidBytes));
				}

				dataLength -= uuidLength;
				currentPos += uuidLength;
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "unable to parse service uuid of length " + dataLength);
		}

		return currentPos;
	}

	// Helper method to extract bytes from byte array.
	private static byte[] extractBytes(byte[] scanRecord, int start, int length)
	{
		byte[] bytes = new byte[length];
		System.arraycopy(scanRecord, start, bytes, 0, length);
		return bytes;
	}

	/**
	 * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
	 * but the returned UUID is always in 128-bit format.
	 * Note UUID is little endian in Bluetooth.
	 *
	 * @param uuidBytes Byte representation of uuid.
	 * @return {@link ParcelUuid} parsed from bytes.
	 * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
	 */
	private static UUID parseUuidFrom(byte[] uuidBytes)
	{
		if( uuidBytes == null )
		{
			throw new IllegalArgumentException("uuidBytes cannot be null");
		}
		int length = uuidBytes.length;
		if( length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT &&
				length != UUID_BYTES_128_BIT )
		{
			throw new IllegalArgumentException("uuidBytes length invalid - " + length);
		}

		// Construct a 128 bit UUID.
		if( length == UUID_BYTES_128_BIT )
		{
			ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
			long msb = buf.getLong(8);
			long lsb = buf.getLong(0);
			return new UUID(msb, lsb);
		}

		// For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
		// 128_bit_value = uuid * 2^96 + BASE_UUID
		long shortUuid;
		if( length == UUID_BYTES_16_BIT )
		{
			shortUuid = uuidBytes[0] & 0xFF;
			shortUuid += (uuidBytes[1] & 0xFF) << 8;
		}
		else
		{
			shortUuid = uuidBytes[0] & 0xFF;
			shortUuid += (uuidBytes[1] & 0xFF) << 8;
			shortUuid += (uuidBytes[2] & 0xFF) << 16;
			shortUuid += (uuidBytes[3] & 0xFF) << 24;
		}
		long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
		long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
		return new UUID(msb, lsb);

	}
}
