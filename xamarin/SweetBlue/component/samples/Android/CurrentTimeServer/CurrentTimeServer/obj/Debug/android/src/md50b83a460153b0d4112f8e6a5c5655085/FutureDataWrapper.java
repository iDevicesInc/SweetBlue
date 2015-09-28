package md50b83a460153b0d4112f8e6a5c5655085;


public class FutureDataWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.utils.FutureData
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_getData:()[B:GetGetDataHandler:Idevices.Sweetblue.Util.IFutureDataInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.FutureDataWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", FutureDataWrapper.class, __md_methods);
	}


	public FutureDataWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == FutureDataWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.FutureDataWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public byte[] getData ()
	{
		return n_getData ();
	}

	private native byte[] n_getData ();

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
