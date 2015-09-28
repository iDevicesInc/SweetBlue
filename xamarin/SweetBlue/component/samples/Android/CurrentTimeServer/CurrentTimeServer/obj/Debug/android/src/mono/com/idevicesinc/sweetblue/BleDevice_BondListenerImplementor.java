package mono.com.idevicesinc.sweetblue;


public class BleDevice_BondListenerImplementor
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.BleDevice.BondListener
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onEvent:(Lcom/idevicesinc/sweetblue/BleDevice$BondListener$BondEvent;)V:GetOnEvent_Lcom_idevicesinc_sweetblue_BleDevice_BondListener_BondEvent_Handler:Idevices.Sweetblue.BleDevice/IBondListenerInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.BleDevice/IBondListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", BleDevice_BondListenerImplementor.class, __md_methods);
	}


	public BleDevice_BondListenerImplementor () throws java.lang.Throwable
	{
		super ();
		if (getClass () == BleDevice_BondListenerImplementor.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.BleDevice/IBondListenerImplementor, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void onEvent (com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent p0)
	{
		n_onEvent (p0);
	}

	private native void n_onEvent (com.idevicesinc.sweetblue.BleDevice.BondListener.BondEvent p0);

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
