package com.idevicesinc.sweetblue.utils;

/**
 *
 */
public interface ForEach_Breakable<T>
{
	public static class Please
	{
		private static final Please CONTINUE = new Please(true);
		private static final Please BREAK = new Please(false);

		private final boolean m_continue;

		private Please(final boolean doContinue)
		{
			m_continue = doContinue;
		}

		public boolean shouldContinue()
		{
			return m_continue;
		}

		public boolean shouldBreak()
		{
			return !shouldContinue();
		}

		public static Please doContinue()
		{
			return CONTINUE;
		}

		public static Please doBreak()
		{
			return BREAK;
		}
	}

	Please next(T next);
}
