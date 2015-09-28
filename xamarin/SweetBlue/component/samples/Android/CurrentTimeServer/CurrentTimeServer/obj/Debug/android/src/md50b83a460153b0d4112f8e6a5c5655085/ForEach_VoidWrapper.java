package md50b83a460153b0d4112f8e6a5c5655085;


public class ForEach_VoidWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.utils.ForEach_Void
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_next:(Ljava/lang/Object;)V:GetNext_Ljava_lang_Object_Handler:Idevices.Sweetblue.Util.IForEach_VoidInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.ForEach_VoidWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", ForEach_VoidWrapper.class, __md_methods);
	}


	public ForEach_VoidWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == ForEach_VoidWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.ForEach_VoidWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public void next (java.lang.Object p0)
	{
		n_next (p0);
	}

	private native void n_next (java.lang.Object p0);

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
