using System;

using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Android.Util;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;


namespace HelloBle
{
	[Activity (Label = "HelloBle", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		private BleManager m_bleManager;

		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

			m_bleManager = BleManager.Get (this);
			m_bleManager.StartScan ((e) => {
				if (e.Was(BleManager.DiscoveryListenerLifeCycle.Discovered)) {					
					m_bleManager.StopScan();
					e.Device().Connect((ev) => {						
						if (ev.DidEnter (BleDeviceState.Initialized)) {
							Log.Info ("SweetBlueExample", ev.Device().Name_debug + " just initialized!");
							ev.Device().Read(Uuids.BatteryLevel, (eve) => {
								String name = eve.Device().Name_debug;
								if (eve.WasSuccess ()) {
									Log.Info ("SweetBlueExample", "Battery level is " + eve.Data()[0] + "%");
								}
							});
						}
					});
				}
			});
		}
	}
}


