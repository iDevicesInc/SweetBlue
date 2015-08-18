package com.idevicesinc.sweetblue;

import android.bluetooth.BluetoothDevice;


abstract class PA_Task_ConnectOrDisconnectServer extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	final BluetoothDevice m_nativeDevice;

	protected int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

	PA_Task_ConnectOrDisconnectServer(final BleServer server, final BluetoothDevice device, final I_StateListener listener, final boolean explicit, final PE_TaskPriority priority)
	{
		super(server, listener);

		m_explicit = explicit;
		m_priority = priority;
		m_nativeDevice = device;
	}

	public boolean isFor(final BleServer server, final String macAddress)
	{
		return server.equals(getServer()) && macAddress.equals(m_nativeDevice.getAddress());
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}

	@Override public boolean isExplicit()
	{
		return m_explicit;
	}

	public int getGattStatus()
	{
		return m_gattStatus;
	}
}
