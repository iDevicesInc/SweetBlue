package mono.com.idevicesinc.sweetblue;


public class BleNode_ConnectionFailListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleNode.ConnectionFailListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleNode/IConnectionFailListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleNode_ConnectionFailListenerImplementor.class, __md_methods);
	}


	public BleNode_ConnectionFailListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleNode_ConnectionFailListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleNode/IConnectionFailListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}

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
