package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;

/**
 * Type-safe parallel of various static final int members of {@link android.bluetooth.le.ScanSettings} and a way to
 * force pre-Lollipop scanning mode. Provide an option to {@link BleManagerConfig#scanMode}.
 */
public enum BleScanMode
{
	/**
	 * This option is recommended and will let SweetBlue automatically choose what scanning mode to use
	 * based on whether the app is backgrounded, if we're doing a long-term scan, polling scan, etc.
	 */
	AUTO(-1),

	/**
	 * Will force SweetBlue to use {@link BluetoothAdapter#startDiscovery()}, which is so-called "Bluetooth Classic" discovery.
	 * This is the scanning mode used on the Android Bluetooth Settings screen. It only returns the mac address and name of your
	 * device through a {@link com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent}, as opposed to full LE scanning packets
	 * which usually have a service {@link java.util.UUID} (at the least) as well.
	 */
	CLASSIC(-1),

	/**
	 * This forces the use of the pre-Lollipop scanning API {@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)},
	 * which was deprecated in Lollipop.
	 */
	PRE_LOLLIPOP(-1),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_POWER}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 */
	LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_BALANCED}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 */
	MEDIUM_POWER(ScanSettings.SCAN_MODE_BALANCED),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_LATENCY}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 */
	HIGH_POWER(ScanSettings.SCAN_MODE_LOW_LATENCY);

	private final int m_nativeMode;

	private BleScanMode(final int nativeMode)
	{
		m_nativeMode = nativeMode;
	}

	/**
	 * Returns one of the static final int members of {@link ScanSettings}, or -1 for {@link #AUTO}.
	 */
	public int getNativeMode()
	{
		return m_nativeMode;
	}

	public boolean isLollipopScanMode()
	{
		return this == LOW_POWER || this == MEDIUM_POWER || this == HIGH_POWER;
	}
}
