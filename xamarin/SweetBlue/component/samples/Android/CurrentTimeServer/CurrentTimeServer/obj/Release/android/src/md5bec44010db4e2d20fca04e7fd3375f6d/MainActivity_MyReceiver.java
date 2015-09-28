package md5bec44010db4e2d20fca04e7fd3375f6d;


public class MainActivity_MyReceiver
	extends android.content.BroadcastReceiver
	implements
		mono.android.IGCUserPeer
{
	static final String __md_methods;
	static {
		__md_methods = 
			"n_onReceive:(Landroid/content/Context;Landroid/content/Intent;)V:GetOnReceive_Landroid_content_Context_Landroid_content_Intent_Handler\n" +
			"";
		mono.android.Runtime.register ("CurrentTimeServer.MainActivity/MyReceiver, CurrentTimeServer, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", MainActivity_MyReceiver.class, __md_methods);
	}


	public MainActivity_MyReceiver () throws java.lang.Throwable
	{
		super ();
		if (getClass () == MainActivity_MyReceiver.class)
			mono.android.TypeManager.Activate ("CurrentTimeServer.MainActivity/MyReceiver, CurrentTimeServer, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "", this, new java.lang.Object[] {  });
	}

	public MainActivity_MyReceiver (com.idevicesinc.sweetblue.BleServer p0) throws java.lang.Throwable
	{
		super ();
		if (getClass () == MainActivity_MyReceiver.class)
			mono.android.TypeManager.Activate ("CurrentTimeServer.MainActivity/MyReceiver, CurrentTimeServer, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "Idevices.Sweetblue.BleServer, SweetBlue, Version=2.6.10.0, Culture=neutral, PublicKeyToken=null", this, new java.lang.Object[] { p0 });
	}


	public void onReceive (android.content.Context p0, android.content.Intent p1)
	{
		n_onReceive (p0, p1);
	}

	private native void n_onReceive (android.content.Context p0, android.content.Intent p1);

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
