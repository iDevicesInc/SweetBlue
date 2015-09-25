using System;
using System.Collections.Generic;
using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Android.Util;
using Java.Util;
using Idevices.Sweetblue;

namespace SimpleOta
{
	[Activity (Label = "SimpleOta", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		private static readonly UUID MY_UUID = UUID.RandomUUID();		// NOTE: Replace with your actual UUID.
		private static readonly byte[] MY_DATA = { 0xC0, 0xFF, 0xEE };	// NOTE: Replace with your actual data, not 0xC0FFEE

		private BleManager m_bleManager;



		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

			m_bleManager = BleManager.Get (this);
			m_bleManager.StartScan ((e) => {
				m_bleManager.StopScan();
				if (e.Was (BleManager.DiscoveryListenerLifeCycle.Discovered)) {					
					e.Device().Connect((ev) => {
						if (ev.DidEnter (BleDeviceState.Initialized)) {
							Log.Info ("SweetBlueExample", ev.Device().Name_debug + " just initialized!");
							List<byte[]> writeQueue = new List<byte[]> ();
							writeQueue.Add (MY_DATA);
							writeQueue.Add (MY_DATA);
							writeQueue.Add (MY_DATA);
							writeQueue.Add (MY_DATA);
							ev.Device ().PerformOta (new MyOtaTransaction (writeQueue));
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

		private class MyOtaTransaction : BleTransaction.Ota 
		{
			private List<byte[]> m_dataQueue;
			private int m_currentIndex = 0;
			private OtaReadWriteListener m_readWriteListener;


			public MyOtaTransaction(List<byte[]> dataQueue) {
				m_readWriteListener = new OtaReadWriteListener(this);
				m_dataQueue = dataQueue;
			}

			protected override void Start (BleDevice device)
			{
				doNextWrite ();
			}

			private void doNextWrite() {
				if (m_currentIndex == m_dataQueue.Count) {
					Succeed ();
				} else {
					byte[] nextdata = m_dataQueue[m_currentIndex];
					Device.Write (MY_UUID, nextdata, m_readWriteListener);
					m_currentIndex++;
				}
			}

			private class OtaReadWriteListener : ReadWriteListener {

				public MyOtaTransaction tran;

				public OtaReadWriteListener(MyOtaTransaction trans) {
					tran = trans;
				}

				public override void OnEvent(BleDevice.ReadWriteListenerReadWriteEvent ev) {
					if (ev.WasSuccess ()) {
						tran.doNextWrite ();
					} else {
						tran.Fail ();
					}
				}
			}
		}
	}
}


