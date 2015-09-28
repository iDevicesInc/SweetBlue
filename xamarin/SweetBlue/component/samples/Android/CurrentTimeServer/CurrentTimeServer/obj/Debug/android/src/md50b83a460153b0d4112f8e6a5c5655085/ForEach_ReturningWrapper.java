package md50b83a460153b0d4112f8e6a5c5655085;


public class ForEach_ReturningWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.utils.ForEach_Returning
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_next:(I)Ljava/lang/Object;:GetNext_IHandler:Idevices.Sweetblue.Util.IForEach_ReturningInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.ForEach_ReturningWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", ForEach_ReturningWrapper.class, __md_methods);
	}


	public ForEach_ReturningWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == ForEach_ReturningWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.ForEach_ReturningWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public java.lang.Object next (int p0)
	{
		return n_next (p0);
	}

	private native java.lang.Object n_next (int p0);

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
