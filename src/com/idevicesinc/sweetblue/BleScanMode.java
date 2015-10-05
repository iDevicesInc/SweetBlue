package com.idevicesinc.sweetblue;

import android.bluetooth.le.ScanSettings;

/**
 * Type-safe parallel of various static final int members of {@link android.bluetooth.le.ScanSettings}.
 * Provide an instance to {@link BleManagerConfig#scanMode}. This is only applicable for Android >= 5.0
 * OS levels.
 */
public enum BleScanMode
{
	/**
	 * This option is recommended and will let SweetBlue automatically choose what scanning mode to use
	 * based on whether the app is backgrounded, if we're doing a long-term scan, polling scan, etc.
	 */
	AUTO(-1),

	/**
	 * Strict typing for {@link ScanSettings#SCAN_MODE_LOW_POWER}.
	 */
	LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER),

	/**
	 * Strict typing for {@link ScanSettings#SCAN_MODE_BALANCED}.
	 */
	MEDIUM_POWER(ScanSettings.SCAN_MODE_BALANCED),

	/**
	 * Strict typing for {@link ScanSettings#SCAN_MODE_LOW_LATENCY}.
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
}
