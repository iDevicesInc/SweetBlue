package com.idevicesinc.sweetblue.tests;


import android.os.Handler;
import com.idevicesinc.sweetblue.PI_UpdateLoop;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class UnitLoop implements PI_UpdateLoop
{

    private boolean m_running = false;
    private final ThreadPoolExecutor m_executor;
    private Operator m_operator = new Operator();
    private long m_lastAutoUpdateTime = 0;
    private long m_autoUpdateRate = 0;
    private PI_UpdateLoop.Callback m_callback;
    private final Runnable m_autoUpdateRunnable = new Runnable()
    {
        @Override public void run()
        {
            long currentTime = System.currentTimeMillis();
            double timeStep = ((double) currentTime - m_lastAutoUpdateTime)/1000.0;

            timeStep = timeStep <= 0.0 ? .00001 : timeStep;
            timeStep = timeStep > 1.0 ? 1.0 : timeStep;

            m_callback.onUpdate(timeStep);

            m_lastAutoUpdateTime = currentTime;

        }
    };

    public UnitLoop(Callback action)
    {
        this.m_callback = action;
        m_executor = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    }

    public void start()
    {
        m_running = true;
        m_executor.execute(m_operator);
    }

    @Override public boolean isRunning()
    {
        return m_running;
    }

    @Override public void start(double updateRate)
    {
        if( updateRate == 0.0 )  return;

        if( /*already*/m_running )
        {
            stop();
        }

        m_running = true;

        m_autoUpdateRate = (long) (updateRate * 1000);
        m_lastAutoUpdateTime = System.currentTimeMillis();
        start();
    }

    public void stop()
    {
        m_running = false;
    }

    @Override public void forcePost(Runnable runnable)
    {
        runnable.run();
    }

    @Override public Handler getHandler()
    {
        return null;
    }

    @Override public boolean postNeeded()
    {
        return false;
    }

    @Override public void postIfNeeded(Runnable runnable)
    {
        runnable.run();
    }

    private void postUpdate() {
        if (m_autoUpdateRunnable != null)
        {
            m_autoUpdateRunnable.run();
        }
    }

    private class Operator implements Runnable {

        @Override public void run()
        {
            int sleep;
            long begin;
            long diff;
            while (m_running)
            {
                if (Thread.currentThread().isInterrupted())
                {
                    m_running = false;
                    return;
                }
                begin = System.currentTimeMillis();
                postUpdate();
                diff = System.currentTimeMillis() - begin;
                sleep = (int) (m_autoUpdateRate - diff);
                if (sleep > 0)
                {
                    try
                    {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e)
                    {
                    }
                }
            }
        }
    }

}
