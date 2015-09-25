using System;
using Android.App;
using Android.Text;
using Android.Widget;
using Android.Views;
using Android.Content;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;


namespace BleUtil
{
	public class BleStatusBar : FrameLayout
	{

		private BleManager m_bleMgr;
		private TextView m_status;
		private TextView m_nativeStatus;


		public BleStatusBar (Context context, BleManager mgr) : base(context)
		{
			m_bleMgr = mgr;
			m_bleMgr.State += delegate {
				updateStatus();		
			};
			m_bleMgr.NativeState += delegate {
				updateStatus();	
			};

			View inner = LayoutInflater.From (context).Inflate (Resource.Layout.Ble_Status_Bar, null);

			m_status = inner.FindViewById<TextView> (Resource.Id.ble_status);
			m_nativeStatus = inner.FindViewById<TextView> (Resource.Id.native_ble_status);

			updateStatus ();

			AddView (inner);
		}

		private void updateStatus()
		{
			SpannableString status = Utils_String.MakeStateString (BleManagerState.Values(), m_bleMgr.StateMask);
			m_status.TextFormatted = status;

			status = Utils_String.MakeStateString (BleManagerState.Values (), m_bleMgr.NativeStateMask);
			m_nativeStatus.TextFormatted = status;
		}

	}
}

