package com.idevicesinc.sweetblue;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;


final class P_SweetBlueThread implements P_SweetHandler
{

    private final LinkedBlockingQueue<SweetRunnable> m_runnables;

    private Thread thread;
    private boolean m_running;


    P_SweetBlueThread()
    {
        m_runnables = new LinkedBlockingQueue<>();
        m_running = true;
        thread = new Thread(new HandlerRunner());
        thread.start();
    }


    @Override public void post(Runnable action)
    {
        m_runnables.add(new SweetRunnable(action, System.currentTimeMillis(), 0));
    }

    @Override public void postDelayed(Runnable action, long delay)
    {
        m_runnables.add(new SweetRunnable(action, System.currentTimeMillis(), delay));
    }

    @Override public void removeCallbacks(Runnable action)
    {
        Iterator<SweetRunnable> it = m_runnables.iterator();
        while (it.hasNext())
        {
            SweetRunnable run = it.next();
            if (run.m_runnable == action)
            {
                run.cancel();
                it.remove();
            }
        }
    }

    @Override public Thread getThread()
    {
        return thread;
    }

    public void quit()
    {
        m_running = false;
        if (Thread.currentThread() != thread)
        {
            try
            {
                thread.join();
            } catch (Exception e)
            {
            }
        }
    }

    private final static class SweetRunnable
    {
        private final Runnable m_runnable;
        private final long m_postedTime;
        private final long m_delay;
        private boolean m_canceled;


        public SweetRunnable(Runnable action, long postedTime, long delay)
        {
            m_runnable = action;
            m_postedTime = postedTime;
            m_delay = delay;
        }

        public void run()
        {
            if (!m_canceled)
            {
                m_runnable.run();
            }
        }

        public void cancel()
        {
            m_canceled = true;
        }

        public boolean canceled()
        {
            return m_canceled;
        }

        public boolean ready(long curTime)
        {
            return m_canceled || (curTime - m_postedTime) > m_delay;
        }

        public boolean ready()
        {
            return m_canceled || ready(System.currentTimeMillis());
        }
    }

    private final class HandlerRunner implements Runnable
    {
        @Override public void run()
        {
            while (m_running)
            {
                if (!m_runnables.isEmpty())
                {
                    long curTime = System.currentTimeMillis();
                    Iterator<SweetRunnable> it = m_runnables.iterator();
                    while (it.hasNext())
                    {
                        SweetRunnable run = it.next();
                        if (run.ready(curTime))
                        {
                            it.remove();
                            run.run();
                        }
                    }
                }
                // Sleep for a short period, so we don't hog the cpu
                try
                {
                    Thread.sleep(5);
                } catch (Exception e)
                {
                }
            }
        }
    }
}
