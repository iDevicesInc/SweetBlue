using System;
using Android.App;
using Android.Content;
using Android.Text;
using Android.Util;
using Android.Views;
using Android.Widget;
using Java.Util;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;

namespace BleUtil
{
	public class BleBar : LinearLayout
	{

		private static Interval SCAN_TIMEOUT = Interval.FiveSecs;

		private Button m_enable;
		private Button m_disable;
		private Button m_unbondAll;
		private Button m_nuke;
		private Button m_infiniteScan;
		private Button m_xSec;
		private Button m_xSecRepeated;
		private Button m_stop;

		private BleManager m_bleMngr;


		public BleBar (Context context, BleManager mgr) : base(context)
		{
			m_bleMngr = mgr;

			Orientation = Orientation.Vertical;
			int padding = context.Resources.GetDimensionPixelSize (Resource.Dimension.default_padding);
			SetPadding (padding, padding, padding, padding);

			View inner = LayoutInflater.From (context).Inflate(Resource.Layout.Ble_Button_Bar, null);

			m_enable = inner.FindViewById<Button> (Resource.Id.ble_enable_button);
			m_disable = inner.FindViewById<Button> (Resource.Id.ble_disable_button);
			m_unbondAll = inner.FindViewById<Button> (Resource.Id.ble_unbond_all_button);
			m_nuke = inner.FindViewById<Button> (Resource.Id.ble_nuke_button);

			m_enable.Click += delegate {
				m_bleMngr.TurnOn ();
			};

			m_disable.Click += delegate {
				m_bleMngr.TurnOff ();
			};

			m_unbondAll.Click += delegate {
				m_bleMngr.UnbondAll();
			};

			m_nuke.Click += delegate {
				m_bleMngr.Reset();
			};

			m_infiniteScan = inner.FindViewById<Button> (Resource.Id.scan_infinite_button);
			m_xSec = inner.FindViewById<Button> (Resource.Id.scan_for_x_sec_button);
			m_xSecRepeated = inner.FindViewById<Button> (Resource.Id.scan_for_x_sec_repeated_button);
			m_stop = inner.FindViewById<Button> (Resource.Id.scan_stop_button);

			m_infiniteScan.Click += delegate {
				m_bleMngr.StartScan();
			};

			m_xSec.Click += delegate {
				m_bleMngr.StartScan(SCAN_TIMEOUT);
			};

			m_xSecRepeated.Click += delegate {
				m_bleMngr.StartPeriodicScan(SCAN_TIMEOUT, SCAN_TIMEOUT);
			};

			m_stop.Click += delegate {
				// Generic catch all to stop all scan types
				m_bleMngr.StopPeriodicScan();
				m_bleMngr.StopScan();
			};

			int timeout = (int)SCAN_TIMEOUT.Secs ();

			m_xSec.Text = m_xSec.Text.Replace("{{seconds}}", timeout + "");
			String repeatedText = m_xSecRepeated.Text.Replace ("{{seconds}}", timeout + "");
			m_xSecRepeated.Text = repeatedText;

			AddView (inner);

			BleStatusBar scanBar = new BleStatusBar (context, m_bleMngr);
			scanBar.LayoutParameters = new LinearLayout.LayoutParams (LayoutParams.MatchParent, LayoutParams.WrapContent);
			AddView (scanBar);
		}

	}
}

