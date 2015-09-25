namespace Idevices.Sweetblue {
	public abstract class DiscoveryListener : Java.Lang.Object, Idevices.Sweetblue.BleManager.IDiscoveryListener
	{
		public DiscoveryListener ()
		{
		}

		public abstract void OnEvent (Idevices.Sweetblue.BleManager.DiscoveryListenerDiscoveryEvent ev);
	}
}


