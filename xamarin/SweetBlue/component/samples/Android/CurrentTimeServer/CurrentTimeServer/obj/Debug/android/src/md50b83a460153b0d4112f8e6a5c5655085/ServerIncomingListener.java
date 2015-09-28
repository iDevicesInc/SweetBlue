package md50b83a460153b0d4112f8e6a5c5655085;


public abstract class ServerIncomingListener
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleServer.IncomingListener,
		com.idevicesinc.sweetblue.BleServer.ExchangeListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleServer$IncomingListener$IncomingEvent;)Lcom/idevicesinc/sweetblue/BleServer$IncomingListener$Please;:GetOnEvent_Lcom_idevicesinc_sweetblue_BleServer_IncomingListener_IncomingEvent_Handler:Idevices.Sweetblue.BleServer/IIncomingListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.ServerIncomingListener, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", ServerIncomingListener.class, __md_methods);
	}


	public ServerIncomingListener () throws java.lang.Throwable
	{
		super ();
		if (getClass () == ServerIncomingListener.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.ServerIncomingListener, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public com.idevicesinc.sweetblue.BleServer.IncomingListener.Please onEvent (com.idevicesinc.sweetblue.BleServer.IncomingListener.IncomingEvent p0)
	{
		return n_onEvent (p0);
	}

	private native com.idevicesinc.sweetblue.BleServer.IncomingListener.Please n_onEvent (com.idevicesinc.sweetblue.BleServer.IncomingListener.IncomingEvent p0);

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
