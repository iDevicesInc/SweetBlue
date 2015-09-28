package mono.com.idevicesinc.sweetblue;


public class BleServer_ServerStateListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleServer.ServerStateListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleServer$ServerStateListener$StateEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleServer_ServerStateListener_StateEvent_Handler:Idevices.Sweetblue.BleServer/IServerStateListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleServer/IServerStateListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleServer_ServerStateListenerImplementor.class, __md_methods);
	}


	public BleServer_ServerStateListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleServer_ServerStateListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleServer/IServerStateListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleServer.ServerStateListener.StateEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleServer.ServerStateListener.StateEvent p0);

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
