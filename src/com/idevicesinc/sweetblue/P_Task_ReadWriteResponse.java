package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleServer.ReadOrWriteRequestListener.Response;
import com.idevicesinc.sweetblue.PA_Task.I_StateListener;

public class P_Task_ReadWriteResponse extends PA_Task_RequiresConnection implements I_StateListener {

	private final BleServer m_server;
	private final Response m_response;
	public P_Task_ReadWriteResponse( BleServer server, I_StateListener listener, Response response ) {
		super( server.getDevice(), BleDeviceConfig.DEFAULT_TASK_TIMEOUT, listener);
		m_server = server;
		m_response = response;
	}

	@Override
	public void onStateChange( PA_Task task, PE_TaskState state ) {
		// TODO Auto-generated method stub

	}

	@Override
	void execute() {
		if ( !m_server.getNative().sendResponse( m_response.device, m_response.requestId, m_response.status, m_response.offset, m_response.data ) ) {
			this.fail();
		}
	}

	@Override
	public PE_TaskPriority getPriority() {
		// TODO Auto-generated method stub
		return PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING; //PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}

	@Override 
	protected boolean isExecutable()
	{
		return true;
	}
}
