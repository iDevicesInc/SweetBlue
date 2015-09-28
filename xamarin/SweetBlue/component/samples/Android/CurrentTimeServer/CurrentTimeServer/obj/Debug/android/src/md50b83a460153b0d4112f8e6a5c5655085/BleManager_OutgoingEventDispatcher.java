package md50b83a460153b0d4112f8e6a5c5655085;


public class BleManager_OutgoingEventDispatcher
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleServer.OutgoingListener,
		com.idevicesinc.sweetblue.BleServer.ExchangeListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleServer$OutgoingListener$OutgoingEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleServer_OutgoingListener_OutgoingEvent_Handler:Idevices.Sweetblue.BleServer/IOutgoingListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleManager/OutgoingEventDispatcher, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleManager_OutgoingEventDispatcher.class, __md_methods);
	}


	public BleManager_OutgoingEventDispatcher () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_OutgoingEventDispatcher.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/OutgoingEventDispatcher, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}

	public BleManager_OutgoingEventDispatcher (com.idevicesinc.sweetblue.BleManager p0) throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_OutgoingEventDispatcher.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/OutgoingEventDispatcher, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "Idevices.Sweetblue.BleManager, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", this, new java.lang.Object[] { p0 });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleServer.OutgoingListener.OutgoingEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleServer.OutgoingListener.OutgoingEvent p0);

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
