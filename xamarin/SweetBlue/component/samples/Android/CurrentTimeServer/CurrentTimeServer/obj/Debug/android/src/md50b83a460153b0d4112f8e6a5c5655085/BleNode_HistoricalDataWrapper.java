package md50b83a460153b0d4112f8e6a5c5655085;


public class BleNode_HistoricalDataWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleNode.HistoricalDataQueryListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleNode$HistoricalDataQueryListener$HistoricalDataQueryEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleNode_HistoricalDataQueryListener_HistoricalDataQueryEvent_Handler:Idevices.Sweetblue.BleNode/IHistoricalDataQueryListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleNode/HistoricalDataWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleNode_HistoricalDataWrapper.class, __md_methods);
	}


	public BleNode_HistoricalDataWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleNode_HistoricalDataWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleNode/HistoricalDataWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleNode.HistoricalDataQueryListener.HistoricalDataQueryEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleNode.HistoricalDataQueryListener.HistoricalDataQueryEvent p0);

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
