package com.idevicesinc.sweetblue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.idevicesinc.sweetblue.utils.ReflectionUuidNameMap;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.UuidNameMap;
import com.idevicesinc.sweetblue.utils.UuidNameMap_ListWrapper;

final class P_Logger
{

	private final static String MAIN = "MAIN(%d)";
	private final static String UPDATE = "UPDATE(%d)";
	private String[] m_debugThreadNamePool;
	private int m_poolIndex = 0;
	private final HashMap<Integer, String> m_threadNames = new HashMap<>();
	private HashMap<Integer, String> m_gattStatusCodes = null;
	private HashMap<Integer, String> m_gattConnStatusCodes = null;
	private HashMap<Integer, String> m_gattConnStates = null;
	private HashMap<Integer, String> m_gattBleStates = null;
	private HashMap<Integer, String> m_gattBondStates = null;
	private HashMap<Integer, String> m_unbondReasonCodes = null;
	private boolean m_enabled;
	private final UuidNameMap_ListWrapper m_nameMap;
	private SweetLogger m_logger = null;
	private final BleManager m_mgr;


	public P_Logger(final BleManager manager, String[] debugThreadNamePool, List<UuidNameMap> debugUuidNameDicts, boolean enabled, SweetLogger logger)
	{
		m_mgr = manager;
		m_logger = logger;
		m_debugThreadNamePool = debugThreadNamePool;
		m_nameMap = new UuidNameMap_ListWrapper(debugUuidNameDicts);
		m_enabled = enabled;
	}

	public void printBuildInfo()
	{
		if (!m_enabled) return;

		int level = Log.DEBUG;

		for (Field field : Build.class.getFields())
		{
			String fieldName = field.getName();
			String fieldValue = Utils_Reflection.fieldStringValue(field);

			this.log(level, fieldName + ": " + fieldValue);
		}
	}

	public boolean isEnabled()
	{
		return m_enabled;
	}

	public synchronized String getDebugAction(String action)
	{
		String[] action_split = action.split("\\.");
		String action_debug = action_split[action_split.length - 1];

		return action_debug;
	}

	public synchronized String getThreadName(int threadId)
	{
		String threadName = null;

		if (m_threadNames != null)
		{
			threadName = m_threadNames.get(threadId);

			if (threadName == null)
			{
				threadName = m_debugThreadNamePool[m_poolIndex % m_debugThreadNamePool.length];
				threadName = threadName + "(" + threadId + ")";

				m_threadNames.put(threadId, threadName);

				m_poolIndex++;
			}
		}

		return threadName == null ? "" : threadName;
	}

	public void setMainThread(int threadId)
	{
		m_threadNames.put(threadId, String.format(MAIN, threadId));
	}

	public void setUpdateThread(int threadId)
	{
		m_threadNames.put(threadId, String.format(UPDATE, threadId));
	}
	
	private StackTraceElement getSoonestTrace()
	{
		StackTraceElement[] trace = new Exception().getStackTrace();
		return getSoonestTrace(trace);
	}
	
	private StackTraceElement getSoonestTrace(StackTraceElement[] trace)
	{
		for(int i = 0; i < trace.length; i++ )
		{
			if( !trace[i].getClassName().equals(this.getClass().getName()) )
			{
				return trace[i];
			}
		}
		
		return null;
	}
	
	private String prefixMessage(String methodName, String message)
	{
		String threadName = getThreadName(Process.myTid());
		message = threadName + " " + methodName + "() - " + message;
		
		return message;
	}
	
	public void log(int level, String message)
	{
		if( !m_enabled )  return;
		
		StackTraceElement trace = getSoonestTrace();
		String className = trace.getClassName();
		String[] className_split = className.split("\\.");
		className = className_split[className_split.length-1];
		log_private(level, className, message, trace);
	}
	
	public void log_status(int gattStatus)
	{
		log_status(gattStatus, "");
	}
	
	public void log_status(int gattStatus, String message)
	{
		if( !m_enabled )  return;
		
		int level = Utils.isSuccess(gattStatus) ? Log.INFO : Log.WARN;
		message = gattStatus(gattStatus) + " " + message;
		
		log(level, message);
	}

	public void log_conn_status(int gattStatus)
	{
		log_conn_status(gattStatus, "");
	}

	public void log_conn_status(int gattStatus, String message)
	{
		if (!m_enabled)	return;

		int level = Utils.isSuccess(gattStatus) ? Log.INFO : Log.WARN;
		message = gattConnStatus(gattStatus) + " " + message;

		log(level, message);
	}
	
	public void log(int level, String tag, String message)
	{
		if( !m_enabled )  return;
		
		StackTraceElement trace = getSoonestTrace();
		log_private(level, tag, message, trace); 
	}
	
	private void log_private(int level, String tag, String message, StackTraceElement trace)
	{
		message = prefixMessage(trace.getMethodName(), message);
		if (m_logger != null)
		{
			m_logger.onLogEntry(level, tag, message);
		}
		else
		{
			Log.println(level, tag, message);
		}
	}
	
	public void d(String tag, String message)
	{
		log(Log.DEBUG, tag, message);
	}
	
	public void i(String tag, String message)
	{
		log(Log.INFO, tag, message);
	}
	
	public void v(String tag, String message)
	{
		log(Log.VERBOSE, tag, message);
	}
	
	public void e(String tag, String message)
	{
		log(Log.ERROR, tag, message);
	}
	
	public void w(String tag, String message)
	{
		log(Log.WARN, tag, message);
	}
	
	
	public void d(String message)
	{
		log(Log.DEBUG, message);
	}
	
	public void i(String message)
	{
		log(Log.INFO, message);
	}
	
	public void v(String message)
	{
		log(Log.VERBOSE, message);
	}
	
	public void e(String message)
	{
		log(Log.ERROR, message);
	}
	
	public void w(String message)
	{
		log(Log.WARN, message);
	}
	
	
	
	
	public String gattConn(int code)
	{
		String name = "NO_NAME";
		
		if( m_gattConnStates == null && m_enabled )
		{
			initConnStates();
		}
		
		if( m_gattConnStates != null )
		{
			String actualName = m_gattConnStates.get(code);
			name = actualName != null ? actualName : name;
		}
		
		return name+"("+code+")";
	}
	
	private synchronized void initConnStates()
	{
		if( m_gattConnStates != null )  return;
		
		m_gattConnStates = new HashMap<>();
		
		initFromReflection(BluetoothProfile.class, "STATE_", m_gattConnStates);
	}

	
	
	public String gattStatus(int code)
	{
		String errorName = "GATT_STATUS_NOT_APPLICABLE";
		
		if( m_gattStatusCodes == null && m_enabled )
		{
			initGattStatusCodes();
		}
		
		if( m_gattStatusCodes != null )
		{
			String actualErrorName = m_gattStatusCodes != null ? m_gattStatusCodes.get(code) : null;
			errorName = actualErrorName != null ? actualErrorName : errorName;
		}
		
		return Utils_String.makeString(errorName, "(", code, ")");
	}
	
	private synchronized void initGattStatusCodes()
	{
		if( m_gattStatusCodes != null )  return;
		
		m_gattStatusCodes = new HashMap<Integer, String>();

		initFromReflection(BluetoothGatt.class, "GATT_", m_gattStatusCodes);
		initFromReflection(BleDeviceConfig.class, "GATT_", m_gattStatusCodes);
		initFromReflection(BleStatuses.class, "GATT_", m_gattStatusCodes);
	}


	public String gattConnStatus(int code)
	{
		String errorName = "GATT_STATUS_NOT_APPLICABLE";

		if( m_gattConnStatusCodes == null && m_enabled )
		{
			initGattConnStatusCodes();
		}

		if( m_gattConnStatusCodes != null )
		{
			String actualErrorName = m_gattConnStatusCodes != null ? m_gattConnStatusCodes.get(code) : null;
			// If we couldn't find a relevant gatt conn code, then we'll fall back to the old gattStatus checks
			if (actualErrorName == null)
				return gattStatus(code);
			errorName = "GATT_" + actualErrorName;
		}

		return Utils_String.makeString(errorName, "(", code, ")");
	}


	private synchronized void initGattConnStatusCodes()
	{
		if (m_gattConnStatusCodes != null)	return;

		m_gattConnStatusCodes = new HashMap<>();

		initFromReflection(BleStatuses.class, "CONN_", m_gattConnStatusCodes);
	}
	
	
	public String gattBleState(int code)
	{
		String name = "NO_NAME";
		
		if( m_gattBleStates == null && m_enabled )
		{
			initGattBleStates();
		}
		
		if( m_gattBleStates != null )
		{
			String actualName = m_gattBleStates.get(code);
			name = actualName != null ? actualName : name;
		}
		
		return name+"("+code+")";
	}
	
	private synchronized void initGattBleStates()
	{
		if( m_gattBleStates != null )  return;
		
		m_gattBleStates = new HashMap<Integer, String>();
		
		initFromReflection(BluetoothAdapter.class, "STATE_", m_gattBleStates);
		
		m_gattBleStates.put(BluetoothAdapter.ERROR, "ERROR");
	}
	
	
	
	
	public String gattUnbondReason(int code)
	{
		String name = "NO_NAME";
		
		if( m_unbondReasonCodes == null && m_enabled )
		{
			initUnbondReasonCodes();
		}
		
		if( m_unbondReasonCodes != null )
		{
			String actualName = m_unbondReasonCodes.get(code);
			name = actualName != null ? actualName : name;
		}
		
		return name+"("+code+")";
	}
	
	private synchronized void initUnbondReasonCodes()
	{
		if( m_unbondReasonCodes != null )  return;
		
		m_unbondReasonCodes = new HashMap<Integer, String>();
		
		initFromReflection(BluetoothDevice.class, "UNBOND_REASON_", m_unbondReasonCodes);
		initFromReflection(BleStatuses.class, "BOND_FAIL_REASON", m_unbondReasonCodes);
	}
	
	
	
	
	
	
	
	
	public String gattBondState(int code)
	{
		String name = "NO_NAME";
		
		if( m_gattBondStates == null && m_enabled )
		{
			initGattBondStates();
		}
		
		if( m_gattBondStates != null )
		{
			String actualName = m_gattBondStates.get(code);
			name = actualName != null ? actualName : name;
		}
		
		return name+"("+code+")";
	}
	
	private synchronized void initGattBondStates()
	{
		if( m_gattBondStates != null )  return;
		
		m_gattBondStates = new HashMap<Integer, String>();
		
		initFromReflection(BluetoothDevice.class, "BOND_", m_gattBondStates);
	}
	
	
	private static void initFromReflection(Class<?> clazz, String fieldSuffix, HashMap<Integer, String> map)
	{
		for( Field field : clazz.getFields() )
		{
			String fieldName = field.getName();
			
			if( !fieldName.contains(fieldSuffix) )  continue;
			
			Integer fieldValue = -1;
			
			try {
				fieldValue = field.getInt(null);
			} catch (IllegalAccessException e) {
//				e.printStackTrace();
			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
			}
			
			if( !map.containsKey(fieldValue) )
			{
				map.put(fieldValue, fieldName);
			}
		}
	}
	
	private String uuidToString(UUID uuid_nullable)
	{
		return uuid_nullable == null ? "null-uuid" : uuid_nullable.toString();
	}
	
	public String descriptorName(UUID uuid)
	{
		return uuidName(uuidToString(uuid), "descriptor");
	}
	
	public String charName(UUID uuid)
	{
		return uuidName(uuidToString(uuid), "char");
	}
	
	public String serviceName(UUID uuid)
	{
		return uuidName(uuidToString(uuid), "service");
	}
	
	public String uuidName(UUID uuid)
	{
		return uuidName(uuidToString(uuid));
	}
	
	public String uuidName(String uuid)
	{
		return uuidName(uuid, null);
	}
	
	public String uuidName(String uuid, String type)
	{
		String debugName = m_nameMap.getUuidName(uuid);
		
		return (type == null ? debugName : type+"="+debugName);
	}
	
	<T> void checkPlease(final T please_nullable, final Class<T> please_class)
	{
		final Class<? extends Object> class_Listener = please_class.getEnclosingClass();
		
		if( please_nullable == null )
		{
			w("WARNING: The " +please_class.getSimpleName() + " returned from " +class_Listener.getSimpleName() +".onEvent() is null. Consider returning a valid instance using static constructor methods.");
		}
	}
}
