namespace Idevices.Sweetblue {
	public abstract class ServerIncomingListener : Java.Lang.Object, Idevices.Sweetblue.BleServer.IIncomingListener
	{
		public ServerIncomingListener ()
		{
		}

		public abstract Idevices.Sweetblue.BleServer.IncomingListenerPlease OnEvent(Idevices.Sweetblue.BleServer.IncomingListenerIncomingEvent ev);
	}
}

