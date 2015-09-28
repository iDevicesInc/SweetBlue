package md50b83a460153b0d4112f8e6a5c5655085;


public class BleDevice_ConnectionFailWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener,
		com.idevicesinc.sweetblue.BleNode.ConnectionFailListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleDevice$ConnectionFailListener$ConnectionFailEvent;)Lcom/idevicesinc/sweetblue/BleNode$ConnectionFailListener$Please;:GetOnEvent_Lcom_idevicesinc_sweetblue_BleDevice_ConnectionFailListener_ConnectionFailEvent_Handler:Idevices.Sweetblue.BleDevice/IConnectionFailListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleDevice/ConnectionFailWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleDevice_ConnectionFailWrapper.class, __md_methods);
	}


	public BleDevice_ConnectionFailWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleDevice_ConnectionFailWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleDevice/ConnectionFailWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public com.idevicesinc.sweetblue.BleNode.ConnectionFailListener.Please onEvent (com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent p0)
	{
		return n_onEvent (p0);
	}

	private native com.idevicesinc.sweetblue.BleNode.ConnectionFailListener.Please n_onEvent (com.idevicesinc.sweetblue.BleDevice.ConnectionFailListener.ConnectionFailEvent p0);

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
