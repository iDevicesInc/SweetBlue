namespace Idevices.Sweetblue {
	public abstract class ReadWriteListener : Java.Lang.Object, Idevices.Sweetblue.BleDevice.IReadWriteListener
	{
		public ReadWriteListener()
		{			
		}

		public abstract void OnEvent(Idevices.Sweetblue.BleDevice.ReadWriteListenerReadWriteEvent ev);
	}
}


