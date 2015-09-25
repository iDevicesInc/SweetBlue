using System;
using Android.Views;
using Android.Widget;
using Android.Content;
using Idevices.Sweetblue;


namespace BleUtil
{
	public class ViewController : LinearLayout
	{

		private enum State {
			DEVICE_LIST,
			DEVICE_DETAIL,
			NULL
		}

		private BleManager m_bleMgr;
		private FrameLayout m_inner;
		private State m_state = State.NULL;


		public ViewController (Context context, BleManager mgr) : base(context)
		{
			m_bleMgr = mgr;

			this.LayoutParameters = new LayoutParams (LayoutParams.MatchParent, LayoutParams.MatchParent);
			this.Orientation = Orientation.Vertical;

			BleBar bleStateBar = new BleBar (context, mgr);
			bleStateBar.LayoutParameters = new LayoutParams (LayoutParams.MatchParent, LayoutParams.WrapContent);
			bleStateBar.SetBackgroundColor (new Android.Graphics.Color(0x33A21615));
			AddView (bleStateBar);

			m_inner = new FrameLayout (context);
			m_inner.LayoutParameters = new LayoutParams (LayoutParams.MatchParent, LayoutParams.MatchParent);
			AddView (m_inner);

			setState (State.DEVICE_LIST);
		}

		private void setState(State state) {
			if (m_state != State.NULL) {
				m_inner.RemoveAllViews ();
			}

			m_state = state;

			View newView = null;

			if (m_state == State.DEVICE_LIST) {
				newView = new DeviceList (Context, m_bleMgr);
			} else if (m_state == State.DEVICE_DETAIL) {				
			}

			if (newView != null) {
				newView.LayoutParameters = new LayoutParams (LayoutParams.MatchParent, LayoutParams.MatchParent);
				m_inner.AddView (newView);
			}				
		}

	}
}

