package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class UhOhThrottleTest extends BaseBleUnitTest
{

    @Test(timeout = 20000)
    public void uhOhThrottleTest() throws Exception
    {
        m_config.updateLoopCallback = new PI_UpdateLoop.Callback()
        {
            private double time;

            @Override
            public void onUpdate(double timestep_seconds)
            {
                time += timestep_seconds;
                if (time > 10)
                {
                    succeed();
                }
            }
        };

        m_mgr.setConfig(m_config);
        m_mgr.setListener_UhOh(new BleManager.UhOhListener()
        {
            private long m_lastEvent;

            @Override
            public void onEvent(UhOhEvent e)
            {
                long now = System.currentTimeMillis();
                if (m_lastEvent == 0)
                {
                    m_lastEvent = now;
                }
                else if (now - m_lastEvent < 5000)
                {
                    assertFalse("Didn't honor throttle time! Time diff: " + (now - m_lastEvent) + "ms", true);
                }
            }
        });
        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        startTest();
    }

    @Test(timeout = 30000)
    public void uhOhThrottleShutdownTest() throws Exception
    {
        final UhOhCallback callback = new UhOhCallback();
        m_config.updateLoopCallback = callback;
        m_config.uhOhCallbackThrottle = Interval.secs(20);

        m_mgr.setConfig(m_config);
        m_mgr.setListener_UhOh(new BleManager.UhOhListener()
        {
            private long m_lastEvent;

            @Override
            public void onEvent(UhOhEvent e)
            {
                long now = System.currentTimeMillis();
                if (m_lastEvent == 0)
                {
                    m_lastEvent = now;
                }
                else if (now - m_lastEvent < 5000)
                {
                    assertFalse("Didn't honor throttle time! Time diff: " + (now - m_lastEvent) + "ms", true);
                }
            }
        });
        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        startTest();
    }

    private final class UhOhCallback implements PI_UpdateLoop.Callback
    {

        private double time;
        private boolean secondTime;


        @Override
        public void onUpdate(double timestep_seconds)
        {
            time += timestep_seconds;
            if (time > 5 && time < 10 && !secondTime)
            {
                secondTime = true;
                m_mgr.shutdown();

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        System.out.println("Restarting BleManager...");
                        m_mgr = BleManager.get(m_activity, m_config);
                        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                        m_mgr.uhOh(BleManager.UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                    }
                }).start();

            }
            else if (time >= 10d)
            {
                System.out.println("Time steps at release: " + time);
                succeed();
            }
        }
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.runOnMainThread = false;
        config.loggingEnabled = true;
        config.uhOhCallbackThrottle = Interval.secs(5.0);
        return config;
    }
}
