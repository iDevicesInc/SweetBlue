namespace Idevices.Sweetblue {
	public partial class BleDevice
	{

		// Delegates
		public delegate void StateListen(BleDevice.StateListenerStateEvent ev);
		public delegate void ReadWriteListen(BleDevice.ReadWriteListenerReadWriteEvent ev);
		public delegate BleDevice.ConnectionFailListenerPlease ConnectFailListen(BleDevice.ConnectionFailListenerConnectionFailEvent ev);
		public delegate void OnHistoricalDataLoad(BleNode.HistoricalDataLoadListenerHistoricalDataLoadEvent ev);
		public delegate void OnBond(BleDevice.BondListenerBondEvent ev);


		// Additional methods, which use the above delegates
		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(StateListen listen) {
			return Connect (new StateWrapper (listen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(ConnectFailListen listen) {
			return Connect (new ConnectionFailWrapper (listen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(StateListen stListen, ConnectFailListen fListen) {
			return Connect (new StateWrapper (stListen), new ConnectionFailWrapper (fListen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect( ConnectFailListen fListen, StateListen stListen) {
			return Connect (new ConnectionFailWrapper (fListen), new StateWrapper (stListen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Auth authenticationTxn, StateListen listen) {
			return Connect (authenticationTxn, new StateWrapper (listen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Auth authenticationTxn, StateListen listen, ConnectFailListen failListen) {
			return Connect (authenticationTxn, new StateWrapper (listen), new ConnectionFailWrapper(failListen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Init initTxn, StateListen listen) {
			return Connect (initTxn, new StateWrapper (listen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Init initTxn, StateListen listen, ConnectFailListen failListen) {
			return Connect (initTxn, new StateWrapper (listen), new ConnectionFailWrapper(failListen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, StateListen listen) {
			return Connect (authenticationTxn, initTxn, new StateWrapper (listen));
		}

		public BleDevice.ConnectionFailListenerConnectionFailEvent Connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, StateListen listen, ConnectFailListen failListen) {
			return Connect (authenticationTxn, initTxn, new StateWrapper (listen), new ConnectionFailWrapper(failListen));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Read(Java.Util.UUID characteristicUuid, ReadWriteListen listener) {
			return Read (characteristicUuid, new ReadWriteWrapper(listener));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Read(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, ReadWriteListen listener) {
			return Read (serviceUuid, characteristicUuid, new ReadWriteWrapper(listener));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID characteristicUuid, byte[] data, ReadWriteListen listen) {
			return Write (characteristicUuid, data, new ReadWriteWrapper (listen));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, byte[] data, ReadWriteListen listen) {
			return Write (serviceUuid, characteristicUuid, data, new ReadWriteWrapper (listen));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID characteristicUuid, OnFutureData data) {
			return Write (characteristicUuid, new FutureDataWrapper(data));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID characteristicUuid, OnFutureData data, ReadWriteListen listen) {
			return Write (characteristicUuid, new FutureDataWrapper(data), new ReadWriteWrapper (listen));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, OnFutureData data) {
			return Write (serviceUuid, characteristicUuid, new FutureDataWrapper(data));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent Write(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, OnFutureData data, ReadWriteListen rwListen) {
			return Write (serviceUuid, characteristicUuid, new FutureDataWrapper(data), new ReadWriteWrapper(rwListen));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent ReadRssi(ReadWriteListen listen) {
			return ReadRssi (new ReadWriteWrapper (listen));
		}

		public void StartRssiPoll(Util.Interval interval, ReadWriteListen listen) {
			StartRssiPoll (interval, new ReadWriteWrapper (listen));
		}

		public void StartPoll(Java.Util.UUID characteristicUuid, Util.Interval interval, ReadWriteListen listen) {
			StartPoll (characteristicUuid, interval, new ReadWriteWrapper (listen));
		}

		public void StartPoll(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, Util.Interval interval, ReadWriteListen listen) {
			StartPoll (serviceUuid, characteristicUuid, interval, new ReadWriteWrapper (listen));
		}

		public void StartPoll(Java.Util.UUID[] uuids, Util.Interval interval, ReadWriteListen listen) {
			StartPoll (uuids, interval, new ReadWriteWrapper (listen));
		}

		public void StartChangeTrackingPoll(Java.Util.UUID characteristicUuid, Util.Interval interval, ReadWriteListen listener) {
			StartChangeTrackingPoll (characteristicUuid, interval, new ReadWriteWrapper (listener));
		}

		public void StartChangeTrackingPoll(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, Util.Interval interval, ReadWriteListen listener) {
			StartChangeTrackingPoll (serviceUuid, characteristicUuid, interval, new ReadWriteWrapper (listener));
		}

		public void LoadHistoricalData(OnHistoricalDataLoad listener) {
			LoadHistoricalData (new HistoricalDataLoadWrapper (listener));
		}

		public void LoadHistoricalData(Java.Util.UUID uuid, OnHistoricalDataLoad listener) {
			LoadHistoricalData (uuid, new HistoricalDataLoadWrapper (listener));
		}

		public bool GetHistoricalData_forEach(Java.Util.UUID uuid, OnForEach_Void forEach) {
			return GetHistoricalData_forEach(uuid, new ForEach_VoidWrapper(forEach));
		}

		public bool GetHistoricalData_forEach(Java.Util.UUID uuid, Util.EpochTimeRange range, OnForEach_Void forEach) {
			return GetHistoricalData_forEach (uuid, range, new ForEach_VoidWrapper (forEach));
		}

		public bool GetHistoricalData_forEach(Java.Util.UUID uuid, OnForEach_Breakable forEach) {
			return GetHistoricalData_forEach (uuid, new ForEach_BreakableWrapper (forEach));
		}

		public bool GetHistoricalData_forEach(Java.Util.UUID uuid, Util.EpochTimeRange range, OnForEach_Breakable forEach) {
			return GetHistoricalData_forEach (uuid, range, new ForEach_BreakableWrapper (forEach));
		}

		public void AddHistoricalData<T>(Java.Util.UUID uuid, OnForEach_Returning data) {
			AddHistoricalData (uuid, new ForEach_ReturningWrapper (data));
		}

		public BleDevice.ReadWriteListenerReadWriteEvent SetName(string name, Java.Util.UUID characteristicUuid, ReadWriteListen listener) {
			return SetName (name, characteristicUuid, new ReadWriteWrapper (listener));
		}

		public BleDevice.BondListenerBondEvent Bond(OnBond listener) {
			return Bond (new BondWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID characteristicUuid, ReadWriteListen listener) {
			EnableNotify (characteristicUuid, new ReadWriteWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, ReadWriteListen listener) {
			EnableNotify (serviceUuid, characteristicUuid, new ReadWriteWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID characteristicUuid, Util.Interval forceReadTimeout, ReadWriteListen listener) {
			EnableNotify (characteristicUuid, forceReadTimeout, new ReadWriteWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID serviceUuid, Java.Util.UUID characteristicUuid, Util.Interval forceReadTimeout, ReadWriteListen listener) {
			EnableNotify (serviceUuid, characteristicUuid, forceReadTimeout, new ReadWriteWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID[] uuids, ReadWriteListen listener) {
			EnableNotify (uuids, new ReadWriteWrapper (listener));
		}

		public void EnableNotify(Java.Util.UUID[] uuids, Util.Interval forceReadTimeout, ReadWriteListen listener) {
			EnableNotify (uuids, forceReadTimeout, new ReadWriteWrapper (listener));
		}

		// Wrapper classes which take a delegate

		private class BondWrapper : Java.Lang.Object, BleDevice.IBondListener {
			private OnBond bond;

			public BondWrapper(OnBond bonder) {
				bond = bonder;
			}

			public void OnEvent(BleDevice.BondListenerBondEvent ev) {
				bond.Invoke (ev);
			}
		}

		private class HistoricalDataLoadWrapper : Java.Lang.Object, BleNode.IHistoricalDataLoadListener {
			private OnHistoricalDataLoad load;

			public HistoricalDataLoadWrapper(OnHistoricalDataLoad loader) {
				load = loader;
			}

			public void OnEvent(BleNode.HistoricalDataLoadListenerHistoricalDataLoadEvent ev) {
				load.Invoke (ev);
			}
		}

		private class ConnectionFailWrapper : Java.Lang.Object, BleDevice.IConnectionFailListener {
			private ConnectFailListen listen;

			public ConnectionFailWrapper(ConnectFailListen listen) {
				this.listen = listen;
			}

			public BleDevice.ConnectionFailListenerPlease OnEvent(BleDevice.ConnectionFailListenerConnectionFailEvent ev) {
				return listen.Invoke (ev);
			}
		}

		private class ReadWriteWrapper : Java.Lang.Object, BleDevice.IReadWriteListener
		{
			private ReadWriteListen listen;

			public ReadWriteWrapper(ReadWriteListen listen) {
				this.listen = listen;
			}

			public void OnEvent(BleDevice.ReadWriteListenerReadWriteEvent ev) {
				listen.Invoke (ev);
			}
		} 

		private class StateWrapper : Java.Lang.Object, BleDevice.IStateListener {
			private StateListen listen;

			public StateWrapper(StateListen listen) {
				this.listen = listen;
			}

			public void OnEvent(BleDevice.StateListenerStateEvent ev) {
				listen.Invoke (ev);
			}
		}

	}
}