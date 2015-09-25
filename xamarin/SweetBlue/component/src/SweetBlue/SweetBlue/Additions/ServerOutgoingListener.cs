namespace Idevices.Sweetblue {
	public abstract class ServerOutgoingListener : Java.Lang.Object, Idevices.Sweetblue.BleServer.IOutgoingListener
	{
		public ServerOutgoingListener ()
		{
		}

		public abstract void OnEvent(Idevices.Sweetblue.BleServer.OutgoingListenerOutgoingEvent ev);
	}
}

