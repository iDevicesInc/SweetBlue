package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;

import java.util.List;

/**
 * Type-safe parallel of various static final int members of {@link android.bluetooth.le.ScanSettings} and a way to
 * force pre-Lollipop scanning mode. Provide an option to {@link BleManagerConfig#scanMode}.
 *
 * @deprecated This is deprecated in favor of {@link BleScanApi}.
 */
@Deprecated
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
	 * This will tell SweetBlue to use the newer scanning API introduced in Lollipop
	 * ({@link android.bluetooth.le.BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}) . We've found that this API is
	 * not yet as good as it's predecessor. It may be better for battery life, as you have more control over the scanning power (using
	 * {@link BleScanPower}), however, even at {@link BleScanPower#HIGH_POWER}, we've found that it doesn't discover devices
	 * as reliably as the pre-lollipop scan API.
	 */
	POST_LOLLIPOP(-1),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_POWER}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 * @deprecated - This will be removed in v3. Use {@link BleScanPower#LOW_POWER} instead.
	 */
	@Deprecated
	LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_BALANCED}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 * @deprecated - This will be removed in v3. Use {@link BleScanPower#MEDIUM_POWER} instead.
	 */
	@Deprecated
	MEDIUM_POWER(ScanSettings.SCAN_MODE_BALANCED),

	/**
	 * Lollipop-and-up-relevant-only, this is strict typing for {@link ScanSettings#SCAN_MODE_LOW_LATENCY}.
	 * For phones lower than Lollipop, {@link #PRE_LOLLIPOP} will automatically be used instead.
	 * @deprecated - This will be removed in v3. Use {@link BleScanPower#HIGH_POWER} instead.
	 */
	@Deprecated
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

	@Deprecated
	public boolean isLollipopScanMode()
	{
		return this == LOW_POWER || this == MEDIUM_POWER || this == HIGH_POWER;
	}
}
