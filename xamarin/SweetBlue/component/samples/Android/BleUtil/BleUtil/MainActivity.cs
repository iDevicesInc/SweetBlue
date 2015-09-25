using System;

using Android.App;
using Android.Content;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using Android.OS;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;

namespace BleUtil
{
	[Activity (Label = "BleUtil", MainLauncher = true, Icon = "@drawable/icon")]
	public class MainActivity : Activity
	{
		private const int BLE_TURN_ON_REQUEST_CODE = 2;

		private BleManager m_bleMngr;
		private ViewController m_viewController;
		private AlertManager m_alertMngr;
		private BleManagerConfig m_bleManagerConfig = new BleManagerConfig();


		protected override void OnCreate (Bundle bundle)
		{
			base.OnCreate (bundle);

			// Mostly using default options for this demo, but provide overrides here if desired.
			// Disabling undiscovery so the list doesn't jump around...ultimately a UI problem so 
			// should be fixed there eventually.
			m_bleManagerConfig.UndiscoveryKeepAlive = Interval.Disabled;
			m_bleManagerConfig.LoggingEnabled = true;

			m_bleMngr = BleManager.Get(ApplicationContext, m_bleManagerConfig);
			m_alertMngr = new AlertManager(this, m_bleMngr);
			m_viewController = new ViewController(this, m_bleMngr);
			SetContentView(m_viewController);

			if( !m_bleMngr.IsBleSupported )
			{
				m_alertMngr.ShowBleNotSupported();
			}
			else if( !m_bleMngr.Is(BleManagerState.On) )
			{
				m_bleMngr.TurnOnWithIntent(this, BLE_TURN_ON_REQUEST_CODE);
			}
		}

		protected override void OnResume() {
			base.OnResume ();
			m_bleMngr.OnResume ();
		}

		protected override void OnPause() {
			base.OnPause ();
			m_bleMngr.OnPause ();
		}

	}
}