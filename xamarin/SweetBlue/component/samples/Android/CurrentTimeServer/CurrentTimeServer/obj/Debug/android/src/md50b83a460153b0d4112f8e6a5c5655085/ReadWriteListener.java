package md50b83a460153b0d4112f8e6a5c5655085;


public abstract class ReadWriteListener
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleDevice.ReadWriteListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleDevice$ReadWriteListener$ReadWriteEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleDevice_ReadWriteListener_ReadWriteEvent_Handler:Idevices.Sweetblue.BleDevice/IReadWriteListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.ReadWriteListener, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", ReadWriteListener.class, __md_methods);
	}


	public ReadWriteListener () throws java.lang.Throwable
	{
		super ();
		if (getClass () == ReadWriteListener.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.ReadWriteListener, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent p0);

	java.util.ArrayList refList;
	public void monodroidAddReference (java.lang.Object obj)
	{
		if (refList == null)
			refList = new java.util.ArrayList ();
		refList.add (obj);
	}

	public void monodroidClearReferences ()
	{
		if (refList != null)
			refList.clear ();
	}
}
