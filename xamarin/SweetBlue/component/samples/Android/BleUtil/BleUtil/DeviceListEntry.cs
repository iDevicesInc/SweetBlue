using System;
using Android.Views;
using Android.Widget;
using Android.Content;
using Android.Text;
using Android.Util;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;


namespace BleUtil
{
	public class DeviceListEntry : FrameLayout
	{

		private BleDevice m_device;
		private Button m_connect;
		private Button m_disconnect;
		private Button m_bond;
		private Button m_unbond;
		private TextView m_status;
		private TextView m_name;

		public DeviceListEntry (Context context, BleDevice device) : base(context)
		{
			m_device = device;
			m_device.State += (sender, e) => {				
				updateStatus(e.StateEvent.NewStateBits());
			};

			m_device.SetListener_ConnectionFail(new Fail (context));

			m_device.Bonded += (sender, args) => {				
				String toast = args.BondEvent.Device().Name_debug + " bond attempt finished with status " + args.BondEvent.Status();
				Toast.MakeText(context, toast, ToastLength.Long).Show();
			};

			View inner = LayoutInflater.From (context).Inflate (Resource.Layout.Device_Entry, null);
			m_connect = inner.FindViewById<Button> (Resource.Id.connect_button);
			m_disconnect = inner.FindViewById<Button> (Resource.Id.disconnect_button);
			m_bond = inner.FindViewById<Button> (Resource.Id.bond_button);
			m_unbond = inner.FindViewById<Button> (Resource.Id.unbond_button);
			m_status = inner.FindViewById<TextView> (Resource.Id.device_status);
			m_name = inner.FindViewById<TextView> (Resource.Id.device_name);

			m_connect.Click += delegate {
				m_device.Connect();
			};

			m_disconnect.Click += delegate {
				m_device.Disconnect();
			};

			m_bond.Click += delegate {
				m_device.Bond();
			};

			m_unbond.Click += delegate {
				m_device.Unbond();
			};

			updateStatus (m_device.StateMask);

			String name = m_device.Name_normalized;
			if (name.Length == 0) {
				name = m_device.MacAddress;
			} else {
				name += "(" + m_device.MacAddress + ")";
			}
			m_name.Text = name;

			AddView (inner);

			if (device.LastDisconnectIntent == StateChangeIntent.Unintentional) {
				device.Connect ();
			}
		}

		public BleDevice getDevice() {
			return m_device;
		}

		private void updateStatus(int deviceStateMask) {
			SpannableString status = Utils_String.MakeStateString (BleDeviceState.Values(), deviceStateMask);
			m_status.TextFormatted = status;
		}


		public class Fail : BleDevice.DefaultConnectionFailListener {

			private Context m_context;

			public Fail(Context context) : base()
			{
				m_context = context;
			}

			public override BleDevice.ConnectionFailListenerPlease OnEvent (BleDevice.ConnectionFailListenerConnectionFailEvent ev)
			{
				BleDevice.ConnectionFailListenerPlease please = base.OnEvent (ev);
				if (!please.IsRetry) {					
					String toast = ev.Device ().Name_debug + " connection failed with " + ev.FailureCountSoFar() + " retries - " + ev.Status ().ToString();
					Toast.MakeText (m_context, toast, ToastLength.Long).Show ();

				}
				return please;
			}
		}

	}
}

