package md50b83a460153b0d4112f8e6a5c5655085;


public class ForEach_BreakableWrapper
	extends java.lang.Object
	implements
		mono.android.IGCUserPeer,
		com.idevicesinc.sweetblue.utils.ForEach_Breakable
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_next:(Ljava/lang/Object;)Lcom/idevicesinc/sweetblue/utils/ForEach_Breakable$Please;:GetNext_Ljava_lang_Object_Handler:Idevices.Sweetblue.Util.IForEach_BreakableInvoker, SweetBlue\n" +
			"";
		mono.android.Runtime.register ("Idevices.Sweetblue.ForEach_BreakableWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", ForEach_BreakableWrapper.class, __md_methods);
	}


	public ForEach_BreakableWrapper () throws java.lang.Throwable
	{
		super ();
		if (getClass () == ForEach_BreakableWrapper.class)
			mono.android.TypeManager.Activate ("Idevices.Sweetblue.ForEach_BreakableWrapper, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}


	public com.idevicesinc.sweetblue.utils.ForEach_Breakable.Please next (java.lang.Object p0)
	{
		return n_next (p0);
	}

	private native com.idevicesinc.sweetblue.utils.ForEach_Breakable.Please n_next (java.lang.Object p0);

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
