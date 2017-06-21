package com.idevicesinc.sweetblue.toolbox.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idevicesinc.sweetblue.PI_UpdateLoop;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateManager implements PI_UpdateLoop.Callback
{
    public interface UpdateListener
    {
        void onUpdate();  //TODO:  Add some parameters
    }

    private class ListenerInfo
    {
        private WeakReference<UpdateListener> m_listenerWR;
        private double m_updateFrequency;
        private double m_countdown;

        ListenerInfo(UpdateListener listener, double updateFrequency)
        {
            m_listenerWR = new WeakReference<UpdateListener>(listener);
            m_updateFrequency = updateFrequency;
            m_countdown = m_updateFrequency;
        }

        UpdateListener getListener()
        {
            return m_listenerWR != null ? m_listenerWR.get() : null;
        }

        void update(double dt, Handler handler)
        {
            m_countdown -= dt;
            if (m_countdown <= 0.0)
            {
                final UpdateListener ul = getListener();
                if (ul != null)
                {
                    handler.post(new Runnable()
                    {
                        @Override public void run()
                        {
                            ul.onUpdate();
                        }
                    });
                }

                m_countdown = m_updateFrequency;
            }
        }
    }

    // Our list of subscribed listeners
    List<ListenerInfo> m_updateListenerList = new ArrayList<>();

    // Static instance
    private static UpdateManager s_instance;

    // Static getter/creator
    public static UpdateManager getInstance()
    {
        if (s_instance == null)
            s_instance = new UpdateManager();

        return s_instance;
    }

    static void shutdownInstance()
    {
        //TODO
    }

    private UpdateManager()
    {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask()
        {
            long m_lastTimestamp = System.currentTimeMillis();

            @Override public void run()
            {
                long timeNow = System.currentTimeMillis();
                long delta = timeNow - m_lastTimestamp;
                m_lastTimestamp = timeNow;

                double dt = delta / 1000.0;

                onUpdate(dt);
            }
        }, 0, 20);
    }

    @Override
    public void onUpdate(double dt)
    {
        Handler handler = new android.os.Handler(Looper.getMainLooper());

        //Log.d("upd", "update manger starting");
        synchronized (m_updateListenerList)
        {
            Iterator<ListenerInfo> it = m_updateListenerList.iterator();

            while (it.hasNext())
            {
                ListenerInfo li = it.next();

                // Purge dead listeners
                if (li.getListener() == null)
                {
                    it.remove();
                    continue;
                }

                // Update listener
                li.update(dt, handler);
            }
        }
        //Log.d("upd", "update manger done");
    }

    public void subscribe(UpdateListener listener, double updateFrequency)
    {
        if (listener == null)
            return;

        synchronized (m_updateListenerList)
        {
            ListenerInfo existing = null;
            for (ListenerInfo li : m_updateListenerList)
            {
                UpdateListener ul = li.getListener();
                if (ul == listener)
                {
                    existing = li;
                    break;
                }
            }

            if (existing == null)
            {
                // Add a new listener
                existing = new ListenerInfo(listener, updateFrequency);
                m_updateListenerList.add(existing);
            }
            else
            {
                // Just update the frequency
                existing.m_updateFrequency = updateFrequency;
            }
        }
    }

    public void unsubscribe(UpdateListener listener)
    {
        synchronized (m_updateListenerList)
        {
            Iterator<ListenerInfo> it = m_updateListenerList.iterator();
            while (it.hasNext())
            {
                ListenerInfo li = it.next();
                if (li.getListener() == listener)
                    it.remove();
            }
        }
    }
}
