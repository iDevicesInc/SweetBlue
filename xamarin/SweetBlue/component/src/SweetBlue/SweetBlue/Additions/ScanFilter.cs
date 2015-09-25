namespace Idevices.Sweetblue {
	public abstract class ScanFilter : Java.Lang.Object, Idevices.Sweetblue.BleManagerConfig.IScanFilter
	{
		public ScanFilter()
		{
		}

		public abstract Idevices.Sweetblue.BleManagerConfig.ScanFilterPlease OnEvent(Idevices.Sweetblue.BleManagerConfig.ScanFilterScanEvent ev);
	}
}

