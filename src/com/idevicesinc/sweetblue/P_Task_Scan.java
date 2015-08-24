package com.idevicesinc.sweetblue;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;

import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class P_Task_Scan extends PA_Task_RequiresBleOn
{	
	static enum E_Mode
	{
		BLE, CLASSIC;
	}
	
	private E_Mode m_mode = null;
	
	//TODO
	private final boolean m_explicit = true;
	private final boolean m_isPoll;
	private final double m_scanTime;

	private final int m_retryCountMax = 3;

	private final ScanCallback m_scanCallback_postLollipop = !Utils.isLollipop() ? null : new ScanCallback()
	{
		public void onScanResult(final int callbackType, final ScanResult result)
		{
			getManager().m_nativeStateTracker.append(BleManagerState.SCANNING, getIntent(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			getManager().getUpdateLoop().postIfNeeded(new Runnable()
			{
				@Override public void run()
				{
					final ScanRecord scanRecord = result.getScanRecord();

					getManager().onDiscovered(result.getDevice(), result.getRssi(), scanRecord.getBytes());
				}
			});
		}

		public void onBatchScanResults(List<ScanResult> results)
		{
		}

		public void onScanFailed(int errorCode)
		{
			if( errorCode != SCAN_FAILED_ALREADY_STARTED )
			{
				fail();
			}
			else
			{
				tryClassicDiscovery(getIntent());

				m_mode = E_Mode.CLASSIC;
			}
		}

	};
	
	public P_Task_Scan(BleManager manager, I_StateListener listener, double scanTime, boolean isPoll)
	{
		super(manager, listener);
		
		m_scanTime = scanTime;
		m_isPoll = isPoll;
	}

	public E_Intent getIntent()
	{
		return m_explicit ? E_Intent.INTENTIONAL : E_Intent.UNINTENTIONAL;
	}

	public ScanCallback getScanCallback_postLollipop()
	{
		return m_scanCallback_postLollipop;
	}
	
	@Override protected double getInitialTimeout()
	{
		return m_scanTime;
	}
	
	@Override public void execute()
	{
		//--- DRK > Because scanning has to be done on a separate thread, isExecutable() can return true
		//---		but then by the time we get here it can be false. isExecutable() is currently not thread-safe
		//---		either, thus we're doing the manual check in the native stack. Before 5.0 the scan would just fail
		//---		so we'd fail as we do below, but Android 5.0 makes this an exception for at least some phones (OnePlus One (A0001)).
		if( !getManager().getNative().getAdapter().isEnabled() )
		{
			fail();

			return;
		}

		if( Utils.isLollipop() )
		{
			m_mode = E_Mode.BLE;
			getManager().m_nativeStateTracker.append(BleManagerState.SCANNING, getIntent(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			startNativeScan_postLollipop();
		}
		else
		{
			m_mode = startNativeScan_preLollipop(getIntent());

			if( m_mode == null )
			{
				fail();
			}
		}
	}

	private void startNativeScan_postLollipop()
	{
		final int scanMode;

		if( getManager().isForegrounded() )
		{
			if( m_isPoll || m_scanTime == Double.POSITIVE_INFINITY )
			{
				scanMode = ScanSettings.SCAN_MODE_BALANCED;
			}
			else
			{
				scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
			}
		}
		else
		{
			scanMode = ScanSettings.SCAN_MODE_LOW_POWER;
		}

		final ScanSettings scanSettings = !Utils.isLollipop() ? null : new ScanSettings.Builder().setScanMode(scanMode).build();

		getManager().getNativeAdapter().getBluetoothLeScanner().startScan(null, scanSettings, m_scanCallback_postLollipop);
	}

	private P_Task_Scan.E_Mode startNativeScan_preLollipop(final E_Intent intent)
	{
		//--- DRK > Not sure how useful this retry loop is. I've definitely seen startLeScan
		//---		fail but then work again at a later time (seconds-minutes later), so
		//---		it's possible that it can recover although I haven't observed it in this loop.
		int retryCount = 0;

		while( retryCount <= m_retryCountMax )
		{
			final boolean success = getManager().getNativeAdapter().startLeScan(getManager().m_listeners.m_scanCallback_preLollipop);

			if( success )
			{
				if( retryCount >= 1 )
				{
					//--- DRK > Not really an ASSERT case...rather just really want to know if this can happen
					//---		so if/when it does I want it to be loud.
					//---		UPDATE: Yes, this hits...TODO: Now have to determine if this is my fault or Android's.
					//---		Error message is "09-29 16:37:11.622: E/BluetoothAdapter(16286): LE Scan has already started".
					//---		Calling stopLeScan below "fixes" the issue.
					//---		UPDATE: Seems like it mostly happens on quick restarts of the app while developing, so
					//---		maybe the scan started in the previous app sessions is still lingering in the new session.
//					ASSERT(false, "Started Le scan on attempt number " + retryCount);
				}

				break;
			}

			retryCount++;

			if( retryCount <= m_retryCountMax )
			{
				if( retryCount == 1 )
				{
					m_logger.w("Failed first startLeScan() attempt. Calling stopLeScan() then trying again...");

					//--- DRK > It's been observed that right on app start up startLeScan can fail with a log
					//---		message saying it's already started...not sure if it's my fault or not but throwing
					//---		this in as a last ditch effort to "fix" things.
					//---
					//---		UPDATE: It's been observed through simple test apps that when restarting an app through eclipse,
					//---		Android somehow, sometimes, keeps the same actual BleManager instance in memory, so it's not
					//---		far-fetched to assume that the scan from the previous app run can sometimes still be ongoing.
					//m_btMngr.getAdapter().stopLeScan(m_listeners.m_scanCallback);
					getManager().getNativeAdapter().stopLeScan(getManager().m_listeners.m_scanCallback_preLollipop);
				}
				else
				{
					m_logger.w("Failed startLeScan() attempt number " + retryCount + ". Trying again...");
				}
			}

			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}

		if( retryCount > m_retryCountMax )
		{
			m_logger.w("Pre-Lollipop LeScan totally failed to start!");

			tryClassicDiscovery(intent);

			return E_Mode.CLASSIC;
		}
		else
		{
			if( retryCount > 0 )
			{
				m_logger.w("Started native scan with " + (retryCount+1) + " attempts.");
			}

			if( getManager().m_config.enableCrashResolver )
			{
				getManager().getCrashResolver().start();
			}

			getManager().m_nativeStateTracker.append(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

			return E_Mode.BLE;
		}
	}

	private boolean tryClassicDiscovery(final E_Intent intent)
	{
		if( getManager().m_config.revertToClassicDiscoveryIfNeeded )
		{
			if( !getManager().getNativeAdapter().startDiscovery() )
			{
				m_logger.w("Classic discovery failed to start!");

				fail();

				getManager().uhOh(BleManager.UhOhListener.UhOh.CLASSIC_DISCOVERY_FAILED);

				return false;
			}
			else
			{
				getManager().m_nativeStateTracker.append(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

				getManager().uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED__USING_CLASSIC);

				return true;
			}
		}
		else
		{
			fail();

			getManager().uhOh(BleManager.UhOhListener.UhOh.START_BLE_SCAN_FAILED);

			return false;
		}
	}
	
	private double getMinimumScanTime()
	{
		return Interval.secs(getManager().m_config.idealMinScanTime);
	}
	
	@Override protected void update(double timeStep)
	{
		if( this.getState() == PE_TaskState.EXECUTING && getTimeout() == Interval.INFINITE.secs() )
		{
			if( getTotalTimeExecuting() >= getMinimumScanTime() && getQueue().getSize() > 0 && isSelfInterruptableBy(getQueue().peek()) )
			{
				selfInterrupt();
			}
			else if( m_mode == E_Mode.CLASSIC && getTotalTimeExecuting() >= BleManagerConfig.MAX_CLASSIC_SCAN_TIME )
			{
				selfInterrupt();
			}
		}
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.TRIVIAL;
	}
	
	public E_Mode getMode()
	{
		return m_mode;
	}
	
	@Override public boolean executeOnSeperateThread()
	{
		return true;
	}

	private boolean isSelfInterruptableBy(final PA_Task otherTask)
	{
		//--- DRK > This logic used to be part of isInterruptableBy but removed because scan task being interruptable
		//---		by reads/writes gives a small chance that a bunch of writes could go out of order.
		if( otherTask instanceof P_Task_Read || otherTask instanceof P_Task_Write || otherTask instanceof P_Task_ReadRssi )
		{
			if( otherTask.getPriority().ordinal() > PE_TaskPriority.FOR_NORMAL_READS_WRITES.ordinal() )
			{
				return true;
			}
			else if( otherTask.getPriority().ordinal() >= this.getPriority().ordinal() )
			{
				//--- DRK > Not sure infinite timeout check really matters here.
				return this.getTotalTimeExecuting() >= getMinimumScanTime();
//				return getTimeout() == TIMEOUT_INFINITE && this.getTotalTimeExecuting() >= getManager().m_config.minimumScanTime;
			}
		}

		return false;
	}
	
	@Override public boolean isInterruptableBy(PA_Task otherTask)
	{
		return otherTask.getPriority().ordinal() >= PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING.ordinal();
	}
	
	@Override public boolean isExplicit()
	{
		return m_explicit;
	}
	
	@Override protected BleTask getTaskType()
	{
		return null;
	}
}
