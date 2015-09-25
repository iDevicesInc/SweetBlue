using System;

using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Android.Util;
using Idevices.Sweetblue;
using Java.Util;


namespace SimpleWrite
{
	[Activity (Label = "SimpleWrite", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		
		private static readonly UUID MY_UUID = UUID.RandomUUID();		// NOTE: Replace with your actual UUID.
		private static readonly byte[] MY_DATA = { 0xC0, 0xFF, 0xEE };	// NOTE: Replace with your actual data, not 0xC0FFEE

		private BleManager m_bleManager;


		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);
			m_bleManager = BleManager.Get (this);
			m_bleManager.StartScan((e) => {
				m_bleManager.StopScan ();
				if (e.Was (BleManager.DiscoveryListenerLifeCycle.Discovered)) {					
					e.Device().Connect((ev) => {
						if (ev.DidEnter (BleDeviceState.Initialized)) {
							Log.Info ("SweetBlueExample", ev.Device().Name_debug + " just initialized!");
							ev.Device().Write(MY_UUID, MY_DATA, (eve) => {
								if (eve.WasSuccess ()) {
									Log.Info ("", "Write Successful");
								} else {
									Log.Error ("", eve.Status().ToString()); // Logs the reason why it failed.
								}
							});
						}
					});
				}
			});
		}

		protected override void OnResume() {
			base.OnResume ();
			m_bleManager.OnResume ();
		}

		protected override void OnPause() {
			base.OnPause ();
			m_bleManager.OnPause ();
		}

	}
}


