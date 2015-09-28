package md5bec44010db4e2d20fca04e7fd3375f6d;


public class MainActivity_InListener
	extends md50b83a460153b0d4112f8e6a5c5655085.ServerIncomingListener
	implements
		mono.android.IGCUserPeer
{
	static final String __md_methods;
	static {
		__md_methods = 
			"";
		mono.android.Runtime.register ("CurrentTimeServer.MainActivity/InListener, CurrentTimeServer, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", MainActivity_InListener.class, __md_methods);
	}


	public MainActivity_InListener () throws java.lang.Throwable
	{
		super ();
		if (getClass () == MainActivity_InListener.class)
			mono.android.TypeManager.Activate ("CurrentTimeServer.MainActivity/InListener, CurrentTimeServer, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}

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
