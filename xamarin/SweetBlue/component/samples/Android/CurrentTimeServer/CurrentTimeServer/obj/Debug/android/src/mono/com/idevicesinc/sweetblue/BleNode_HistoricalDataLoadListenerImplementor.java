package mono.com.idevicesinc.sweetblue;


public class BleNode_HistoricalDataLoadListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleNode.HistoricalDataLoadListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleNode$HistoricalDataLoadListener$HistoricalDataLoadEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleNode_HistoricalDataLoadListener_HistoricalDataLoadEvent_Handler:Idevices.Sweetblue.BleNode/IHistoricalDataLoadListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleNode/IHistoricalDataLoadListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleNode_HistoricalDataLoadListenerImplementor.class, __md_methods);
	}


	public BleNode_HistoricalDataLoadListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleNode_HistoricalDataLoadListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleNode/IHistoricalDataLoadListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleNode.HistoricalDataLoadListener.HistoricalDataLoadEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleNode.HistoricalDataLoadListener.HistoricalDataLoadEvent p0);

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
