namespace Idevices.Sweetblue {
	public partial class BleServer
	{
		public delegate void OnServiceAdd(BleServer.ServiceAddListenerServiceAddEvent ev);
		public delegate void OnOutgoing(BleServer.OutgoingListenerOutgoingEvent ev);
		public delegate void OnState(BleServer.ServerStateListenerStateEvent ev);
		public delegate BleServer.ConnectionFailListenerPlease OnSrvrConnectFail(BleServer.ConnectionFailListenerConnectionFailEvent ev);


		public void AddService(BleService service, OnServiceAdd listener) {
			AddService (service, new ServiceAddWrapper (listener));
		}

		public void GetClients(OnForEach_Void onfor) {
			GetClients (new ForEach_VoidWrapper (onfor));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendIndication(string macAddress, Java.Util.UUID charUuid, byte[] data, OnOutgoing listener) {
			return SendIndication (macAddress, charUuid, data, new OutGoingWrapper (listener));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendIndication(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, byte[] data, OnOutgoing listener) {
			return SendIndication (macAddress, serviceUuid, charUuid, data, new OutGoingWrapper (listener));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendIndication(string macAddress, Java.Util.UUID charUuid, OnFutureData data) {
			return SendIndication (macAddress, charUuid, new FutureDataWrapper(data));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendIndication(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, OnFutureData data) {
			return SendIndication (macAddress, serviceUuid, charUuid, new FutureDataWrapper(data));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendIndication(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, OnFutureData data, OnOutgoing listener) {
			return SendIndication (macAddress, serviceUuid, charUuid, new FutureDataWrapper(data), new OutGoingWrapper (listener));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID charUuid, byte[] data, OnOutgoing listener) {
			return SendNotification (macAddress, charUuid, data, new OutGoingWrapper (listener));	
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, byte[] data, OnOutgoing listener) {
			return SendNotification (macAddress, serviceUuid, charUuid, data, new OutGoingWrapper (listener));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID charUuid, OnFutureData data) {
			return SendNotification (macAddress, charUuid, new FutureDataWrapper (data));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID charUuid, OnFutureData data, OnOutgoing listener) {
			return SendNotification (macAddress, charUuid, new FutureDataWrapper (data), new OutGoingWrapper (listener));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, OnFutureData data) {
			return SendNotification (macAddress, serviceUuid, charUuid, new FutureDataWrapper (data));
		}

		public BleServer.OutgoingListenerOutgoingEvent SendNotification(string macAddress, Java.Util.UUID serviceUuid, Java.Util.UUID charUuid, OnFutureData data, OnOutgoing listener) {
			return SendNotification (macAddress, serviceUuid, charUuid, new FutureDataWrapper (data), new OutGoingWrapper (listener));
		}

		public BleServer.ConnectionFailListenerConnectionFailEvent Connect(string macAddress, OnState listener) {
			return Connect (macAddress, new ServerStateWrapper (listener));
		}

		public BleServer.ConnectionFailListenerConnectionFailEvent Connect(string macAddress, OnSrvrConnectFail onFail) {
			return Connect (macAddress, new SConnectFailWrapper (onFail));
		}

		public BleServer.ConnectionFailListenerConnectionFailEvent Connect(string macAddress, OnState stListen, OnSrvrConnectFail onFail) {
			return Connect (macAddress, new ServerStateWrapper (stListen), new SConnectFailWrapper (onFail));
		}


		private class SConnectFailWrapper : Java.Lang.Object, BleServer.IConnectionFailListener {
			private OnSrvrConnectFail fail;

			public SConnectFailWrapper(OnSrvrConnectFail failer) {
				fail = failer;
			}

			public BleServer.ConnectionFailListenerPlease OnEvent(BleServer.ConnectionFailListenerConnectionFailEvent ev) {
				return fail.Invoke (ev);
			}
		}

		private class ServerStateWrapper : Java.Lang.Object, BleServer.IServerStateListener {
			private OnState state;

			public ServerStateWrapper(OnState state) {
				this.state = state;
			}

			public void OnEvent(BleServer.ServerStateListenerStateEvent ev) {
				state.Invoke (ev);
			}
		}

		private class OutGoingWrapper : Java.Lang.Object, BleServer.IOutgoingListener {
			private OnOutgoing outgoing;

			public OutGoingWrapper(OnOutgoing outt) {
				outgoing = outt;
			}

			public void OnEvent(BleServer.OutgoingListenerOutgoingEvent ev) {
				outgoing.Invoke (ev);
			}
		}

		private class ServiceAddWrapper : Java.Lang.Object, BleServer.IServiceAddListener {
			private OnServiceAdd serviceAdd;

			public ServiceAddWrapper(OnServiceAdd sadd) {
				serviceAdd = sadd;
			}

			public void OnEvent(BleServer.ServiceAddListenerServiceAddEvent ev) {
				serviceAdd.Invoke (ev);
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
			public OutgoingListenerOutgoingEvent OutgoingEvent { get; internal set; }
		}

		internal partial class OutgoingEventDispatcher : Java.Lang.Object, IOutgoingListener {
			private BleServer sender;

			public OutgoingEventDispatcher(BleServer serv) {
				sender = serv;
			}

			internal System.EventHandler<OutgoingEventArgs> OutGoingEvent;

			public void OnEvent(OutgoingListenerOutgoingEvent ev) {
				var h = OutGoingEvent;
				if (h != null) {
					h (sender, new OutgoingEventArgs () {
						OutgoingEvent = ev
					});
				}
			}
		}
	}
}

