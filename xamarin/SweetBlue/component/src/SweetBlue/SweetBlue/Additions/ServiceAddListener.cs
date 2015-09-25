namespace Idevices.Sweetblue {
	public abstract class ServiceAddListener : Java.Lang.Object, Idevices.Sweetblue.BleServer.IServiceAddListener
	{
		public ServiceAddListener()
		{
		}

		public abstract void OnEvent (Idevices.Sweetblue.BleServer.ServiceAddListenerServiceAddEvent ev);
	}
}

