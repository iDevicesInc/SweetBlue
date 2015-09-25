namespace Idevices.Sweetblue {

	public delegate byte[] OnFutureData();

	class FutureDataWrapper : Java.Lang.Object, Util.IFutureData
	{
		private OnFutureData future;

		public FutureDataWrapper(OnFutureData onFuture) {
			future = onFuture;
		}

		public byte[] GetData() {
			return future.Invoke ();
		}
	}
}

