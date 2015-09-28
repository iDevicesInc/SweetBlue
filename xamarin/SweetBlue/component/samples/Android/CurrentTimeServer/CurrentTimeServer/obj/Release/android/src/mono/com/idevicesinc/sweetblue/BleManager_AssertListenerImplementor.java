package mono.com.idevicesinc.sweetblue;


public class BleManager_AssertListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleManager.AssertListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleManager$AssertListener$AssertEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleManager_AssertListener_AssertEvent_Handler:Idevices.Sweetblue.BleManager/IAssertListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleManager/IAssertListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleManager_AssertListenerImplementor.class, __md_methods);
	}


	public BleManager_AssertListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_AssertListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/IAssertListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleManager.AssertListener.AssertEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleManager.AssertListener.AssertEvent p0);

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
