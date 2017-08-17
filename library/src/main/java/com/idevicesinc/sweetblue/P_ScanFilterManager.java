package com.idevicesinc.sweetblue;

import java.util.ArrayList;
import java.util.List;

import com.idevicesinc.sweetblue.ScanFilter.Please;
import com.idevicesinc.sweetblue.ScanFilter.ScanEvent;

final class P_ScanFilterManager
{
	private ScanFilter m_default;  // Regular default scan filter
	private ScanFilter m_ephemeral = null;  // Temporary filter for just the current scan
	private ScanFilter.ApplyMode m_ephemeralApplyMode = ScanFilter.ApplyMode.CombineEither;  // How should the ephemeral filter be applied?
	private final BleManager m_mngr;
	
	P_ScanFilterManager(final BleManager mngr, final ScanFilter defaultFilter)
	{
		m_mngr = mngr;
		m_default = defaultFilter;
	}

	void setDefaultFilter(ScanFilter filter)
	{
		m_default = filter;
	}

	void setEphemeralFilter(ScanFilter ephemeral)
	{
		m_ephemeral = ephemeral;
	}

	void setEphemeralFilter(ScanFilter ephemeral, ScanFilter.ApplyMode applyMode)
	{
		m_ephemeral = ephemeral;
		m_ephemeralApplyMode = applyMode;
	}

	void setEphemeralFilterApplyMode(ScanFilter.ApplyMode applyMode)
	{
		m_ephemeralApplyMode = applyMode;
	}

	void clearEphemeralFilter()
	{
		m_ephemeral = null;
		m_ephemeralApplyMode = ScanFilter.ApplyMode.CombineEither;
	}

	private List<ScanFilter> activeFilters()
	{
		List<ScanFilter> l = new ArrayList<>();
		if (m_ephemeralApplyMode == ScanFilter.ApplyMode.Override)
		{
			if (m_ephemeral != null)
				l.add(m_ephemeral);
		}
		else
		{
			if (m_default != null)
				l.add(m_default);
			if (m_ephemeral != null)
				l.add(m_ephemeral);
		}
		return l;
	}

	public boolean makeEvent()
	{
		return activeFilters().size() > 0;
	}
	
	ScanFilter.Please allow(P_Logger logger, final ScanEvent e)
	{
		List<ScanFilter> activeFilters = activeFilters();

		if (activeFilters.size() < 1)
			return Please.acknowledge();

		Please yesPlease = null;
		for (ScanFilter sf : activeFilters)
		{
			final Please please = sf.onEvent(e);

			logger.checkPlease(please, Please.class);

			stopScanningIfNeeded(sf, please);

			boolean accepted = (please != null && please.ack());

			if (accepted && yesPlease == null)
				yesPlease = please;

			if (m_ephemeralApplyMode != ScanFilter.ApplyMode.CombineBoth && accepted)
				return please;

			if (m_ephemeralApplyMode == ScanFilter.ApplyMode.CombineBoth && !accepted)
				return ScanFilter.Please.ignore();
		}

		return m_ephemeralApplyMode == ScanFilter.ApplyMode.CombineBoth && yesPlease != null ? yesPlease : ScanFilter.Please.ignore();
	}

	private void stopScanningIfNeeded(final ScanFilter filter, final ScanFilter.Please please_nullable)
	{
		if( please_nullable != null )
		{
			if( please_nullable.ack() )
			{
				if( (please_nullable.m_stopScanOptions & Please.STOP_PERIODIC_SCAN) != 0x0 )
				{
					m_mngr.stopPeriodicScan(filter);
				}

				if( (please_nullable.m_stopScanOptions & Please.STOP_SCAN) != 0x0 )
				{
					m_mngr.stopScan(filter);
				}
			}
		}
	}
}
