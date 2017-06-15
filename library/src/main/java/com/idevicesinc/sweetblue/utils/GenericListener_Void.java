package com.idevicesinc.sweetblue.utils;

/**
 *
 */
public interface GenericListener_Void<T_Event extends Event>
{
	void onEvent(T_Event e);
}
