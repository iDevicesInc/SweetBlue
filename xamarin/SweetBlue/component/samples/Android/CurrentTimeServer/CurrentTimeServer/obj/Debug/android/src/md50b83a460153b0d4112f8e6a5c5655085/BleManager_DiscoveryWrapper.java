package md50b83a460153b0d4112f8e6a5c5655085;


public class BleManager_DiscoveryWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleManager.DiscoveryListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleManager$DiscoveryListener$DiscoveryEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleManager_DiscoveryListener_DiscoveryEvent_Handler:Idevices.Sweetblue.BleManager/IDiscoveryListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleManager/DiscoveryWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleManager_DiscoveryWrapper.class, __md_methods);
	}


	public BleManager_DiscoveryWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_DiscoveryWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/DiscoveryWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleManager.DiscoveryListener.DiscoveryEvent p0);

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
