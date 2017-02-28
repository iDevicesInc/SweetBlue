package com.idevicesinc.sweetblue.utils;

/*
 * NOTE:
 * Some of the static methods here (as of now, {@link #calcDistance(int, int)} and
 * {@link #calcDistance(int, int, double, double, double)}) are adapted from a
 * StackOverflow post (http://stackoverflow.com/questions/20416218/understanding-ibeacon-distancing/20434019#20434019).
 * Further investigation lead to https://github.com/AltBeacon/android-beacon-library which hosts code by the StackOverflow
 * post author. It is licensed under the following:
 * 
 * Copyright 2014 Radius Networks
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Some static utility methods for RSSI-related calculations.
 */
public final class Utils_Rssi extends Utils
{
	private Utils_Rssi(){super();}

	public static double percent(final int rssi, final int rssi_min, final int rssi_max)
	{
		return (((double)(rssi-rssi_min)) / ((double)(rssi_max-rssi_min))) * 100.0;
	}
	
	public static double distance(final int txPower, final int rssi)
	{
		return distance(txPower, rssi, 0.89976, 7.7095, 0.111);
	}
	
	public static double distance(final int txPower, final int rssi, double a, double b, double c)
	{
		if (rssi == 0)
		{
			return -1.0;
		}

		final double ratio = ((double)rssi) * (1.0 / ((double)txPower));
		
		if (ratio < 1.0)
		{
			return Math.pow(ratio, 10);
		}
		else
		{
			final double accuracy = (a) * Math.pow(ratio, b) + c;
			
			return accuracy;
		}
	}
}
