package com.idevicesinc.sweetblue;


import android.os.Parcelable;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;


abstract class P_NativeGattObject<T> implements UsesCustomNull
{

    private final BleManager.UhOhListener.UhOh m_uhOh;
    private final T m_gattObject;


    P_NativeGattObject()
    {
        this(null, null);
    }

    P_NativeGattObject(T gattObject)
    {
        this(gattObject, null);
    }

    P_NativeGattObject(BleManager.UhOhListener.UhOh uhOh)
    {
        this(null, uhOh);
    }

    P_NativeGattObject(T gattObject, BleManager.UhOhListener.UhOh uhOh)
    {
        m_gattObject = gattObject;
        m_uhOh = uhOh;
    }


    /**
     * Mostly used internally, but if there was a particular issue when retrieving a gatt object, it will have an {@link com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOh}
     * with a status of what went wrong.
     */
    public BleManager.UhOhListener.UhOh getUhOh()
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
