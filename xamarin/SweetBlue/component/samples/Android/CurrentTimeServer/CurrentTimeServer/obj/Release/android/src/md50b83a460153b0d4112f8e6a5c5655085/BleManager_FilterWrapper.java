package md50b83a460153b0d4112f8e6a5c5655085;


public class BleManager_FilterWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleManagerConfig$ScanFilter$ScanEvent;)Lcom/idevicesinc/sweetblue/BleManagerConfig$ScanFilter$Please;:GetOnEvent_Lcom_idevicesinc_sweetblue_BleManagerConfig_ScanFilter_ScanEvent_Handler:Idevices.Sweetblue.BleManagerConfig/IScanFilterInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleManager/FilterWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleManager_FilterWrapper.class, __md_methods);
	}


	public BleManager_FilterWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_FilterWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/FilterWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please onEvent (com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent p0)
	{
		return n_onEvent (p0);
	}

	private native com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.Please n_onEvent (com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter.ScanEvent p0);

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
