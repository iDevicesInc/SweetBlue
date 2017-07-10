package com.idevicesinc.sweetblue;


import android.util.Log;
import org.junit.Test;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertFalse;


public class OnOffBlitzTest extends BaseTester<MainActivity>
{


    @Test
    public void onOffSequentialTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        final AtomicInteger counter = new AtomicInteger(0);

        mgr.setListener_State(new ManagerStateListener()
        {
            @Override
            public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (counter.incrementAndGet() >= 200)
                {
                    s.release();
                    return;
                }
                assertFalse("BleManager is in both the OFF AND ON states!", mgr.is(BleManagerState.ON) && mgr.is(BleManagerState.OFF));
                if (e.didEnter(BleManagerState.OFF))
                {
                    mgr.turnOn();
                }
                else if (e.didEnter(BleManagerState.ON))
                {
                    mgr.turnOff();
                }
            }
        });

        if (mgr.is(BleManagerState.OFF))
            mgr.turnOn();
        else
            mgr.turnOff();

        s.acquire();
    }

    @Test
    public void onOffRandomTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);
        final AtomicInteger counter = new AtomicInteger(0);

        // Turn off calls
        mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (counter.incrementAndGet() >= 200)
                {
                    s.release();
                    return;
                }
                assertFalse("BleManager is in both the OFF AND ON states!", mgr.is(BleManagerState.ON) && mgr.is(BleManagerState.OFF));
                mgr.turnOff();
                mgr.getPostManager().postToUpdateThreadDelayed(this, getRandomMillis());
            }
        }, getRandomMillis());

        // Turn on calls
        mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (counter.incrementAndGet() >= 200)
                {
                    s.release();
                    return;
                }
                assertFalse("BleManager is in both the OFF AND ON states!", mgr.is(BleManagerState.ON) && mgr.is(BleManagerState.OFF));
                mgr.turnOn();
                mgr.getPostManager().postToUpdateThreadDelayed(this, getRandomMillis());
            }
        }, getRandomMillis());

        s.acquire();

    }

    int getRandomMillis()
    {
        return getRandomMillis(100, 2000);
    }

    int getRandomMillis(int min, int max)
    {
        return (new Random().nextInt((max - min)) + min);
    }


    @Override
    Class<MainActivity> getActivityClass()
    {
        return MainActivity.class;
    }

    @Override
    BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig(true);
        config.runOnMainThread = false;
        config.logger = new SweetLogger()
        {
            @Override
            public void onLogEntry(int level, String tag, String msg)
            {
                StringBuilder b = new StringBuilder();
                switch (level)
                {
                    case Log.ASSERT:
                        b.append("A");
                        break;
                    case Log.DEBUG:
                        b.append("D");
                        break;
                    case Log.ERROR:
                        b.append("E");
                        break;
                    case Log.VERBOSE:
                        b.append("V");
                        break;
                    case Log.WARN:
                        b.append("W");
                        break;
                    default:
                        b.append("I");
                        break;
                }
                b.append("/").append(tag).append(" : ").append(msg);
                System.out.println(b.toString());
            }
        };
        return config;
    }

}
