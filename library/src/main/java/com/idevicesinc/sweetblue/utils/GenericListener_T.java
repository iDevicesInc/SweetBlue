package com.idevicesinc.sweetblue.utils;


public interface GenericListener_T<T_Event extends Event, T_Return>
{
    T_Return onEvent(T_Event e);
}
