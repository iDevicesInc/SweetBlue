namespace Idevices.Sweetblue {
	public partial class BleNode
	{
		public delegate void OnHistoricalDataQuery(HistoricalDataQueryListenerHistoricalDataQueryEvent ev);

		public void QueryHistoricalData(string query, OnHistoricalDataQuery listener) {
			QueryHistoricalData (query, new HistoricalDataWrapper (listener));
		}

		private class HistoricalDataWrapper : Java.Lang.Object, IHistoricalDataQueryListener {
			private OnHistoricalDataQuery query;

			public HistoricalDataWrapper(OnHistoricalDataQuery query) {
				this.query = query;
			}

			public void OnEvent(HistoricalDataQueryListenerHistoricalDataQueryEvent ev) {
				query.Invoke (ev);
			}
		}
	}
}

