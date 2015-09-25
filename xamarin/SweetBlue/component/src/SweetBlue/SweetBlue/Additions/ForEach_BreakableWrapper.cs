namespace Idevices.Sweetblue {

	public delegate Util.ForEach_BreakablePlease OnForEach_Breakable(Java.Lang.Object obj);

	class ForEach_BreakableWrapper : Java.Lang.Object, Util.IForEach_Breakable
	{
		private OnForEach_Breakable onFor;

		public ForEach_BreakableWrapper (OnForEach_Breakable onFor)
		{
			this.onFor = onFor;
		}

		public Util.ForEach_BreakablePlease Next(Java.Lang.Object item) {
			return onFor.Invoke (item);
		}
	}
}

