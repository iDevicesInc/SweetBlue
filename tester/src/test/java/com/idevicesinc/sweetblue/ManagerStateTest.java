package com.idevicesinc.sweetblue;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class ManagerStateTest extends BaseBleUnitTest
{

    @Test(timeout = 20000)
    public void onToOffTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_config.nativeManagerLayer = new UnitTestManagerLayer();

        m_mgr.setConfig(m_config);

        assertTrue(m_mgr.is(BleManagerState.ON));

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.TURNING_OFF))
                {
                    System.out.println("Bluetooth is turning off...");

                    if (e.didEnter(BleManagerState.OFF))
                    {
                        succeed();
                    }
                }
                else if (e.didEnter(BleManagerState.OFF))
                {
                    succeed();
                }
            }
        });

        m_mgr.turnOff();

        startTest();
    }

    @Test(timeout = 20000)
    public void onToOffToOnTest() throws Exception
    {
        m_config.loggingEnabled = true;

        m_config.nativeManagerLayer = new UnitTestManagerLayer();

        m_mgr.setConfig(m_config);

        assertTrue(m_mgr.is(BleManagerState.ON));

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.TURNING_OFF))
                {
                    System.out.println("Bluetooth is turning off...");

                    if (e.didEnter(BleManagerState.OFF))
                    {
                        m_mgr.turnOn();
                    }
                }
                else if (e.didEnter(BleManagerState.OFF))
                {
                    m_mgr.turnOn();
                }
                else if (e.didEnter(BleManagerState.TURNING_ON))
                {
                    System.out.println("Bluetooth is turning on...");
                }
                else if (e.didEnter(BleManagerState.ON))
                {
                    succeed();
                }
            }
        });

        m_mgr.turnOff();

        startTest();
    }

//    @Test
    public void turningOffToTurningOnTest() throws Exception
    {
        m_config.loggingEnabled = true;

        final DontTurnOffManagerLayer layer = new DontTurnOffManagerLayer();
        m_config.nativeManagerLayer = layer;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.TURNING_OFF))
                {
                    System.out.println("Bluetooth is turning off...");
//                    UnitTestUtils.sendBluetoothStateBroadcast(m_activity, BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                    layer.manuallySetState(BleStatuses.STATE_TURNING_ON);
                }
                else if (e.didEnter(BleManagerState.TURNING_ON))
                {
                    System.out.print("Bluetooth is turning on...");
                }
                else if (e.didEnter(BleManagerState.ON))
                {
                    s.release();
                }

            }
        });

        m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
        {
            @Override public void run()
            {
                m_mgr.turnOff();
            }
        }, 50);

        s.acquire();
    }

    private class DontTurnOffManagerLayer extends UnitTestManagerLayer
    {
        // Don't turn the state to off, so we stay in the turning off state to test going into turning on/ble turning on from here
        @Override protected void setToOff()
        {
        }
    }

}
