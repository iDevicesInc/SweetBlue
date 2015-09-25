namespace Idevices.Sweetblue {
	public partial class BleManager
	{
		
		public delegate void OnDiscover(BleManager.DiscoveryListenerDiscoveryEvent ev);
		public delegate BleManagerConfig.ScanFilterPlease OnFilter(BleManagerConfig.ScanFilterScanEvent ev);
		public delegate void OnReset(BleManager.ResetListenerResetEvent ev);
		public delegate BleServer.IncomingListenerPlease OnIncoming(BleServer.IncomingListenerIncomingEvent ev);


		public void StartScan(OnDiscover discover) {
			StartScan (new DiscoveryWrapper (discover));
		}

		public void StartScan(Util.Interval interval, OnDiscover discover) {
			StartScan (interval, new DiscoveryWrapper (discover));
		}

		public void StartScan(OnFilter filter) {
			StartScan (new FilterWrapper (filter));
		}

		public void StartScan(Util.Interval interval, OnFilter filter) {
			StartScan (interval, new FilterWrapper (filter));
		}

		public void StartScan(OnFilter filter, OnDiscover discover) {
			StartScan (new FilterWrapper (filter), new DiscoveryWrapper (discover));
		}

		public void StartScan(Util.Interval interval, OnFilter filter, OnDiscover discover) {
			StartScan (interval, new FilterWrapper (filter), new DiscoveryWrapper (discover));
		}

		public void StartPeriodicScan(Util.Interval scanActiveTime, Util.Interval scanPauseTime, OnDiscover listener) {
			StartPeriodicScan (scanActiveTime, scanPauseTime, new DiscoveryWrapper (listener));
		}

		public void StartPeriodicScan(Util.Interval scanActiveTime, Util.Interval scanPauseTime, OnFilter filter) {
			StartPeriodicScan (scanActiveTime, scanPauseTime, new FilterWrapper (filter));
		}

		public void StartPeriodicScan(Util.Interval scanActiveTime, Util.Interval scanPauseTime, OnFilter filter, OnDiscover listener) {
			StartPeriodicScan (scanActiveTime, scanPauseTime, new FilterWrapper (filter), new DiscoveryWrapper (listener));
		}

		public void Reset(OnReset listener) {
			Reset (new ResetWrapper (listener));
		}

		public BleServer GetServer(OnIncoming listener) {
			return GetServer (new IncomingWrapper (listener));
		}

		private class IncomingWrapper : Java.Lang.Object, BleServer.IIncomingListener {
			private OnIncoming incoming;

			public IncomingWrapper(OnIncoming inc) {
				incoming = inc;
			}

			public BleServer.IncomingListenerPlease OnEvent(BleServer.IncomingListenerIncomingEvent ev) {
				return incoming.Invoke (ev);
			}
		}

		private class ResetWrapper : Java.Lang.Object, BleManager.IResetListener {
			private OnReset reset;

			public ResetWrapper(OnReset reset) {
				this.reset = reset;
			}

			public void OnEvent(BleManager.ResetListenerResetEvent ev) {
				reset.Invoke (ev);
			}
		}

		private class FilterWrapper : Java.Lang.Object, BleManagerConfig.IScanFilter {
			private OnFilter filter;

			public FilterWrapper(OnFilter filter) {
				this.filter = filter;
			}

			public BleManagerConfig.ScanFilterPlease OnEvent(BleManagerConfig.ScanFilterScanEvent ev) {
				return filter.Invoke (ev);
			}
		}

		private class DiscoveryWrapper : Java.Lang.Object, BleManager.IDiscoveryListener {
			private OnDiscover method;

			public DiscoveryWrapper(OnDiscover meth) {
				method = meth;
			}

			public void OnEvent(BleManager.DiscoveryListenerDiscoveryEvent ev) {
				method.Invoke(ev);
			}
		}

		System.WeakReference dispatcher;
		OutgoingEventDispatcher EventDispatcher
		{
			get
			{
				if (dispatcher == null || !dispatcher.IsAlive) {
					var d = new OutgoingEventDispatcher (this);
					SetListener_Outgoing (d);
					dispatcher = new System.WeakReference (d);
				}
				return (OutgoingEventDispatcher)dispatcher.Target;
			}
		}

		public event System.EventHandler<OutgoingEventArgs> Outgoing
		{
			add
			{
				EventDispatcher.OutGoingEvent += value;
			}
			remove
			{
				EventDispatcher.OutGoingEvent -= value;
			}
		}

		public class OutgoingEventArgs : System.EventArgs {
			public BleServer.OutgoingListenerOutgoingEvent OutgoingEvent { get; internal set; }
		}

		internal partial class OutgoingEventDispatcher : Java.Lang.Object, BleServer.IOutgoingListener {
			private BleManager man;

			public OutgoingEventDispatcher(BleManager man) {
				this.man = man;
			}

			internal System.EventHandler<OutgoingEventArgs> OutGoingEvent;

			public void OnEvent(BleServer.OutgoingListenerOutgoingEvent ev) {
				var h = OutGoingEvent;
				if (h != null) {
					h (man, new OutgoingEventArgs () {
						OutgoingEvent = ev
					});
				}
			}
		}
	}
}

