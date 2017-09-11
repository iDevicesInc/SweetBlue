package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.UsesCustomNull;


abstract class P_NativeGattObject<T> implements UsesCustomNull
{

    private final UhOhListener.UhOh m_uhOh;
    private final T m_gattObject;


    P_NativeGattObject()
    {
        this(null, null);
    }

    P_NativeGattObject(T gattObject)
    {
        this(gattObject, null);
    }

    P_NativeGattObject(UhOhListener.UhOh uhOh)
    {
        this(null, uhOh);
    }

    P_NativeGattObject(T gattObject, UhOhListener.UhOh uhOh)
    {
        m_gattObject = gattObject;
        m_uhOh = uhOh;
    }


    /**
     * Mostly used internally, but if there was a particular issue when retrieving a gatt object, it will have an {@link com.idevicesinc.sweetblue.UhOhListener.UhOh}
     * with a status of what went wrong.
     */
    public UhOhListener.UhOh getUhOh()
    {
        return m_uhOh;
    }

    public boolean hasUhOh()
    {
        return m_uhOh != null;
    }

    /**
     * Returns <code>true</code> if the gatt object held in this class is <code>null</code> or not.
     */
    @Override
    public boolean isNull()
    {
        return m_gattObject == null;
    }



    T getGattObject()
    {
        return m_gattObject;
    }
}
