using System;
using Android.Content;
using Android.App;
using Android.Views;
using Idevices.Sweetblue;

namespace BleUtil
{
	public class AlertManager : Java.Lang.Object
	{

		private Context m_context;
		public BleManager m_bleMgr;

		public AlertManager (Context context, BleManager mgr)
		{
			m_context = context;
			m_bleMgr = mgr;
			m_bleMgr.UhOh += (sender, e) => {
				handleUhOh(e.UhOhEvent);
			};
		}

		public void ShowBleNotSupported()
		{
			AlertDialog.Builder builder = new AlertDialog.Builder (m_context);

			String dismiss = m_context.GetString (Resource.String.generic_ok);
			String message = m_context.GetString (Resource.String.ble_not_supported);

			builder.SetMessage (message);
			builder.SetNeutralButton (dismiss, closeClick);
			AlertDialog dialog = builder.Create();
			dialog.RequestWindowFeature ((int) WindowFeatures.NoTitle);
			dialog.Show ();
		}

		private void handleUhOh(BleManager.UhOhListenerUhOhEvent ev)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder (m_context);


			String title = m_context.GetString (Resource.String.uhoh_title);
			title = title.Replace ("{{reason}}", ev.UhOh ().Name ());
			builder.SetTitle (title);

			if (ev.Remedy () == BleManager.UhOhListenerRemedy.ResetBle) 
			{
				builder.SetMessage (m_context.GetString (Resource.String.uhoh_message_nuke));
				builder.SetPositiveButton (m_context.GetString (Resource.String.uhoh_message_nuke_drop), resetClick);
				builder.SetNegativeButton (m_context.GetString (Resource.String.uhoh_message_nuke_cancel), closeClick);
			}
			else if (ev.Remedy () == BleManager.UhOhListenerRemedy.RestartPhone) 
			{
				builder.SetMessage (m_context.GetString (Resource.String.uhoh_message_phone_restart));
				builder.SetNeutralButton (m_context.GetString (Resource.String.uhoh_message_phone_restart_ok), closeClick);
			}
			else if (ev.Remedy () == BleManager.UhOhListenerRemedy.WaitAndSee) 
			{
				builder.SetMessage (m_context.GetString (Resource.String.uhoh_message_weirdness));
				builder.SetNeutralButton (m_context.GetString (Resource.String.uhoh_message_weirdness_ok), closeClick);
			}
			AlertDialog dialog = builder.Create ();
			dialog.Show();
		}

		private void closeClick(object dialogObj, DialogClickEventArgs args)
		{			
			AlertDialog dialog = (AlertDialog)dialogObj;
			dialog.Dismiss ();
		}

		private void resetClick(object dialogObj, DialogClickEventArgs args)
		{
			AlertDialog dialog = (AlertDialog)dialogObj;
			dialog.Dismiss ();
			m_bleMgr.Reset ();
		}


	}
}

