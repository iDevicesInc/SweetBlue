namespace Idevices.Sweetblue {

	public abstract class DeviceStateListener : Java.Lang.Object, Idevices.Sweetblue.BleDevice.IStateListener
	{
		public DeviceStateListener ()
		{
		}

		public abstract void OnEvent(Idevices.Sweetblue.BleDevice.StateListenerStateEvent ev);
	}
}

