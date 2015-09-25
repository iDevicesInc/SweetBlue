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
using IncomingPlease = Idevices.Sweetblue.BleServer.IncomingListenerPlease;
using Target = Idevices.Sweetblue.BleServer.ExchangeListenerTarget;

namespace CurrentTimeServer
{
	/**
 	* This sample demonstrates setting up a server, scanning for a peripheral, connecting to that peripheral, then 
 	* connecting our local time server to the peripheral (treating it as a "client" as far as time 
 	* synchronization is concerned), and providing the time to the peripheral when it changes or when 
 	* it asks for it. It will work with 4.3 and up.
 	*/
	[Activity (Label = "CurrentTimeServer", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		private const String MY_DEVICE_NAME = "my_device"; // CHANGE to your device name or a substring thereof.

		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

			BleManager mngr = BleManager.Get (this);
			BleServer server = mngr.Server;

			// Set up a broadcast receiver to get updates to the phone's time and forward them to the 
			// client through BLE notifications.
			RegisterReceiver (new MyReceiver(server), newTimeChangeIntentFilter());

			// Set up our incoming listener to listen for explicit read/write requests and respond accordingly.
			server.Listener_Incoming = new InListener();

			// In a real app you can use this listener to confirm that data was sent -
			// maybe pop up a toast or something to user depending on requirements.
			server.Outgoing += (sender, e) => {
				if (e.OutgoingEvent.WasSuccess ()) {
					if (e.OutgoingEvent.Type ().IsNotificationOrIndication) {
						Log.Info ("", "Current time change sent!");
					} else {
						Log.Info ("", "Current time or local info request successfully responded to!");
					}
				} else {
					Log.Error ("", "Problem sending time change or read request thereof.");
				}
			};


			// Set a listener so we know when the server has finished connecting.
			server.State += (sender, e) => {
				if (e.StateEvent.DidEnter(BleServerState.Connected)) {
					Log.Info("", "Server connected!");
				}
			};

			// Kick things off...from here it's a flow of a bunch of async callbacks...obviously you may 
			// want to structure this differently for your actual app.
			server.AddService(BleServices.CurrentTime(), (e) => {
				if (e.WasSuccess ()) {					
					mngr.StartScan((ev) => {
						// Filter discovered devices by device name
						return BleManagerConfig.ScanFilterPlease.AcknowledgeIf (ev.Name_normalized().Contains(MY_DEVICE_NAME));

					}, (ev) => {
						// Device discover event
						if (ev.Was (BleManager.DiscoveryListenerLifeCycle.Discovered)) {
							ev.Device ().Connect ((eve) => {
								if (eve.DidEnter (BleDeviceState.Initialized)) {
									server.Connect (eve.Device().MacAddress);
								}
							});
						}
					});
				}
			});
		}

		private IntentFilter newTimeChangeIntentFilter() {
			IntentFilter filter = new IntentFilter ();
			filter.AddAction (Intent.ActionDateChanged);
			filter.AddAction (Intent.ActionTimeChanged);
			filter.AddAction (Intent.ActionTimezoneChanged);
			return filter;
		}

		protected override void OnResume ()
		{
			base.OnResume ();
			BleManager.Get (this).OnResume ();
		}

		protected override void OnPause() {
			base.OnPause();
			BleManager.Get (this).OnPause();
		}

		private class InListener : ServerIncomingListener {
			public override IncomingPlease OnEvent(BleServer.IncomingListenerIncomingEvent ev) {
				if (ev.Target () == Target.Characteristic) {
					if (ev.CharUuid ().Equals (Uuids.CurrentTimeServiceCurrentTime)) {
						return IncomingPlease.RespondWithSuccess (Utils_Time.FutureTime);
					} else if (ev.CharUuid ().Equals (Uuids.CurrentTimeServiceLocalTimeInfo)) {
						return IncomingPlease.RespondWithSuccess (Utils_Time.FutureLocalTimeInfo);
					}
				} else if (ev.Target () == Target.Descriptor) {
					return IncomingPlease.RespondWithSuccess ();
				}
				return IncomingPlease.RespondWithError (BleStatuses.GattError);
			}
		}

		private class MyReceiver : BroadcastReceiver {

			private BleServer server;

			public MyReceiver(BleServer srvr) {
				server = srvr;
			}

			public override void OnReceive(Context context, Intent intent) {
				//server.GetClients (new ClientForEach(server));
				server.GetClients((macAddress) => {
					// We use the "future data" construct here because SweetBlue's job queue might force
					// this operation to wait (absolute worst case second or two if you're really pounding 
					// SweetBlue, but still) a bit before it actually gets sent out over the air, and we
					// want to send the most recent time.
					server.SendNotification (macAddress.ToString(), Uuids.CurrentTimeServiceCurrentTime, Utils_Time.FutureTime);
				});
			}
		}
	}
}


