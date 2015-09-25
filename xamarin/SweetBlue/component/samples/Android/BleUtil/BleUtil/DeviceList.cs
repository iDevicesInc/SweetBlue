using System;
using Android.Views;
using Android.Content;
using Android.Widget;
using Idevices.Sweetblue;
using Idevices.Sweetblue.Util;

namespace BleUtil
{
	public class DeviceList : ScrollView
	{

		private const int BASE_COLOR = 0x00115395;
		private const int LIGHT_ALPHA = 0x33000000;
		private const int DARK_ALPHA = 0x44000000;

		private LinearLayout m_list;
		private BleManager m_bleMngr;

		public DeviceList (Context context, BleManager mgr) : base(context)
		{
			m_bleMngr = mgr;
			m_bleMngr.Discovery += (sender, e) => {
				handleEvent(e.DiscoveryEvent);
			};

			m_list = new LinearLayout (context);
			m_list.LayoutParameters = new LayoutParams (LayoutParams.MatchParent, LayoutParams.MatchParent);
			m_list.Orientation = Orientation.Vertical;
			AddView (m_list);
		}

		private void colorList()
		{
			for( int i = 0; i < m_list.ChildCount; i++ )
			{
				View ithView = m_list.GetChildAt(i);

				int alphaMask = i%2 == 0 ? LIGHT_ALPHA : DARK_ALPHA;
				int color = BASE_COLOR | alphaMask;

				ithView.SetBackgroundColor(new Android.Graphics.Color(color));
			}
		}

		void handleEvent(BleManager.DiscoveryListenerDiscoveryEvent ev)
		{
			if( ev.Was(BleManager.DiscoveryListenerLifeCycle.Discovered) )
			{
				DeviceListEntry entry = new DeviceListEntry(base.Context, ev.Device());
				LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MatchParent, LinearLayout.LayoutParams.WrapContent);
				entry.LayoutParameters = p;
				entry.SetBackgroundColor(new Android.Graphics.Color(0x00ff00));

				m_list.AddView(entry);

				colorList();
			}
			else if( ev.Was(BleManager.DiscoveryListenerLifeCycle.Undiscovered) )
			{
				for( int i = 0; i < m_list.ChildCount; i++ )
				{
					DeviceListEntry entry = (DeviceListEntry) m_list.GetChildAt(i);

					if( entry.getDevice().Equals(ev.Device()) )
					{
						m_list.RemoveViewAt(i);
						colorList();

						return;
					}
				}
			}
		}
	}
}

