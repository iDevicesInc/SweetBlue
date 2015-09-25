namespace Idevices.Sweetblue {

	public delegate Java.Lang.Object OnForEach_Returning(Java.Lang.Object obj);

	class ForEach_ReturningWrapper : Java.Lang.Object, Util.IForEach_Returning
	{
		private OnForEach_Returning onreturn;

		public ForEach_ReturningWrapper (OnForEach_Returning ret)
		{
			onreturn = ret;
		}

		public Java.Lang.Object Next(int index) {
			return onreturn.Invoke(index);
		}

	}
}

