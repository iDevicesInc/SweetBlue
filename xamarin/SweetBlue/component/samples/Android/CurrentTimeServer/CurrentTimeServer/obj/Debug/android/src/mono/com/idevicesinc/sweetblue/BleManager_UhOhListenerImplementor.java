package mono.com.idevicesinc.sweetblue;


public class BleManager_UhOhListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleManager.UhOhListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleManager$UhOhListener$UhOhEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleManager_UhOhListener_UhOhEvent_Handler:Idevices.Sweetblue.BleManager/IUhOhListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleManager/IUhOhListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleManager_UhOhListenerImplementor.class, __md_methods);
	}


	public BleManager_UhOhListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleManager_UhOhListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleManager/IUhOhListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOhEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleManager.UhOhListener.UhOhEvent p0);

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
