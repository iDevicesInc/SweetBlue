package mono.com.idevicesinc.sweetblue;


public class BleServer_ExchangeListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleServer.ExchangeListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleServer/IExchangeListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleServer_ExchangeListenerImplementor.class, __md_methods);
	}


	public BleServer_ExchangeListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleServer_ExchangeListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleServer/IExchangeListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
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
