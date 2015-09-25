namespace Idevices.Sweetblue {

	public delegate void OnForEach_Void(Java.Lang.Object obj);

	class ForEach_VoidWrapper : Java.Lang.Object, Util.IForEach_Void {
		private OnForEach_Void method;

		public ForEach_VoidWrapper(OnForEach_Void meth) {
			method = meth;
		}

		public void Next(Java.Lang.Object item) {
			method.Invoke (item);
		}

	}
}

