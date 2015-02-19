package com.idevicesinc.sweetblue;

import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATED;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATING;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZING;
import static com.idevicesinc.sweetblue.BleDeviceState.PERFORMING_OTA;

import com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.Reason;
import com.idevicesinc.sweetblue.BleTransaction.EndReason;
import com.idevicesinc.sweetblue.PA_StateTracker.E_Intent;

/**
 * 
 * 
 *
 */
class P_TransactionManager
{
	final BleTransaction.PI_EndListener m_txnEndListener = new BleTransaction.PI_EndListener()
	{
		@Override public void onTransactionEnd(BleTransaction txn, EndReason reason)
		{
			synchronized (m_device.m_threadLock)
			{
				onTransactionEnd_private(txn, reason);
			}
		}
		
		private void onTransactionEnd_private(BleTransaction txn, EndReason reason)
		{
			clearQueueLock();

			m_current = null;
			
			if( !m_device.is(BleDeviceState.CONNECTED) )
			{
				if( reason == EndReason.CANCELLED )
				{
					return;
				}
				else if( reason == EndReason.SUCCEEDED || reason == EndReason.FAILED )
				{
					m_mngr.ASSERT(false, "nativelyConnected=" + m_mngr.getLogger().gattConn(m_device.m_nativeWrapper.getConnectionState()) + " gatt==" + m_device.m_nativeWrapper.getGatt());
					
					return;
				}
			}
			
			if (txn == m_authTxn )
			{
				if (reason == EndReason.SUCCEEDED)
				{
					m_device.getPollManager().enableNotifications();
					
					if ( m_initTxn != null)
					{
						m_device.getStateTracker().update
						(
							E_Intent.EXPLICIT,
							AUTHENTICATING, false, AUTHENTICATED, true, INITIALIZING, true
						);

						start(m_initTxn);
					}
					else
					{
						m_device.onFullyInitialized();
					}
				}
				else
				{
					m_device.disconnectWithReason(Reason.AUTHENTICATION_FAILED, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE);
				}
			}
			else if (txn == m_initTxn )
			{
				if (reason == EndReason.SUCCEEDED)
				{
					m_device.onFullyInitialized();
				}
				else
				{
					m_device.disconnectWithReason(Reason.INITIALIZATION_FAILED, BleDeviceConfig.GATT_STATUS_NOT_APPLICABLE);
				}
			}
			else if (txn == m_device.getFirmwareUpdateTxn())
			{
//				m_device.m_txnMngr.clearFirmwareUpdateTxn();
				E_Intent intent = E_Intent.IMPLICIT;
				m_device.getStateTracker().remove(PERFORMING_OTA, intent);

				//--- DRK > As of now don't care whether this succeeded or failed.
				if (reason == EndReason.SUCCEEDED)
				{
				}
				else
				{
				}
			}
		}
	};
	private final Object m_threadLock = new Object();
	
	private final BleDevice m_device;
	private final BleManager m_mngr;
	
	BleTransaction.Auth m_authTxn;
	BleTransaction.Init m_initTxn;
	BleTransaction.Ota m_firmwareUpdateTxn;
	
	BleTransaction m_current;
	
	P_TransactionManager(BleDevice device)
	{
		m_device = device;
		m_mngr = m_device.getManager();
	}
	
	void start(BleTransaction txn)
	{
		synchronized (m_threadLock)
		{
			if( m_current != null )
			{
				m_mngr.ASSERT(false, "Old: " + m_current.getClass().getSimpleName() + " New: " + txn.getClass().getSimpleName());
			}
			
			m_current = txn;
			if( m_current.needsAtomicity() )
			{
				m_mngr.getTaskQueue().add(new P_Task_TxnLock(m_device, txn));
			}
			
			m_current.start_internal();
		}
	}
	
	BleTransaction getCurrent()
	{
		return m_current;
	}
	
	void clearQueueLock()
	{
		//--- DRK > Kind of a band-aid hack to prevent deadlock when this is called upstream from
		//---		main thread. A queue addition comes in on the heartbeat thread, which takes the
		//---		queue lock. At the same time we call disconnect from the main thread, which takes
		//---		the device lock, which then cascades here and would wait on the queue lock for
		//---		succeed or clear. Meanwhile queue calls device.equals which used to take device lock.
		//---		It doesn't anymore, but still putting this behind a runnabel just in case.
		m_mngr.getUpdateLoop().postIfNeeded(new Runnable()
		{
			@Override public void run()
			{
				if( !m_mngr.getTaskQueue().succeed(P_Task_TxnLock.class, m_device) )
				{
					m_mngr.getTaskQueue().clearQueueOf(P_Task_TxnLock.class, m_device);
				}
			}
		});
	}
	
//	void clearAllTxns()
//	{
//		synchronized (m_threadLock)
//		{
//			if( m_authTxn != null )
//			{
//				m_authTxn.deinit();
//				m_authTxn = null;
//			}
//			
//			if( m_initTxn != null )
//			{
//				m_initTxn.deinit();
//				m_initTxn = null;
//			}
//			
//			clearFirmwareUpdateTxn();
//			
//			m_current = null;
//		}
//	}
	
//	void clearFirmwareUpdateTxn()
//	{
//		synchronized (m_threadLock)
//		{
//			if( m_firmwareUpdateTxn != null )
//			{
//				m_firmwareUpdateTxn.deinit();
//				m_firmwareUpdateTxn = null;
//			}
//		}
//	}
	
	void cancelFirmwareUpdateTxn()
	{
		synchronized (m_threadLock)
		{
			if( m_firmwareUpdateTxn != null && m_firmwareUpdateTxn.isRunning() )
			{
				m_firmwareUpdateTxn.cancel();
			}
		}
	}
	
	void cancelAllTransactions()
	{
		synchronized (m_threadLock)
		{
			if( m_authTxn != null && m_authTxn.isRunning() )
			{
				m_authTxn.cancel();
			}
			
			if( m_initTxn != null && m_initTxn.isRunning() )
			{
				m_initTxn.cancel();
			}
			
			cancelFirmwareUpdateTxn();
		}
	}
	
	void update(double timeStep)
	{
		synchronized (m_threadLock)
		{
			if( m_authTxn != null && m_authTxn.isRunning() )
			{
				m_authTxn.update_internal(timeStep);
			}
			
			if( m_initTxn != null && m_initTxn.isRunning() )
			{
				m_initTxn.update_internal(timeStep);
			}
			
			if( m_firmwareUpdateTxn != null && m_firmwareUpdateTxn.isRunning() )
			{
				m_firmwareUpdateTxn.update_internal(timeStep);
			}
		}
	}
	
	void onConnect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn)
	{
		synchronized (m_threadLock)
		{
			m_authTxn = authenticationTxn;
			m_initTxn = initTxn;
			
			if( m_authTxn != null )
			{
				m_authTxn.init(m_device, m_txnEndListener);
			}
			
			if( m_initTxn != null )
			{
				m_initTxn.init(m_device, m_txnEndListener);
			}
		}
	}
	
	void onOta(BleTransaction.Ota txn)
	{
		synchronized (m_threadLock)
		{
//			m_device.getManager().ASSERT(m_firmwareUpdateTxn == null);
			
			m_firmwareUpdateTxn = txn;
			m_firmwareUpdateTxn.init(m_device, m_txnEndListener);
			
			m_device.getStateTracker().append(PERFORMING_OTA, E_Intent.EXPLICIT);
			
			start(m_firmwareUpdateTxn);
		}
	}
	
	void runAuthOrInitTxnIfNeeded(Object ... extraFlags)
	{
		synchronized (m_threadLock)
		{
			E_Intent intent = m_device.lastConnectDisconnectIntent();
			if( m_authTxn == null && m_initTxn == null )
			{
				m_device.getPollManager().enableNotifications();
				
				m_device.onFullyInitialized(extraFlags);
			}
			else if( m_authTxn != null )
			{
				m_device.getStateTracker().update(intent, extraFlags, AUTHENTICATING, true);
				
				start(m_authTxn);
			}
			else if( m_initTxn != null )
			{
				m_device.getPollManager().enableNotifications();
				
				m_device.getStateTracker().update(intent, extraFlags, AUTHENTICATED, true, INITIALIZING, true);
				
				start(m_initTxn);
			}
		}
	}
}
