package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.impl.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Semaphore;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{

    private BleDevice m_device;

    private GattDatabase batteryDb = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setProperties().read().setPermissions().read().completeService();


    @Test(timeout = 12000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        doConnectCreatedDeviceTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectCreatedDeviceTest_main() throws Exception
    {
        m_device = null;

        m_config.loggingOptions = LogOptions.ON;
        m_config.runOnMainThread = true;

        doConnectCreatedDeviceTest(m_config);
        startAsyncTest();
    }

    private void doConnectCreatedDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    succeed();
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void retryConnectionTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        m_mgr.setConfig(m_config);

        m_device = m_mgr.newDevice(Util_Unit.randomMacAddress());

        m_device.setListener_Reconnect(new DefaultDeviceReconnectFilter(3, 3));

        m_device.setListener_State(new DeviceStateListener()
        {
            int timesTried = 0;

            @Override
            public void onEvent(StateEvent e)
            {
                if (e.didEnter(BleDeviceState.DISCONNECTED))
                {
                    if (timesTried < 3)
                    {
                        timesTried++;
                        assertTrue(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                    }
                    else
                    {
                        assertFalse(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                        succeed();
                    }
                }
            }
        });

        m_device.connect();

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_device = null;

        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().equals("Test Device"));

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        doConnectDiscoveredDeviceTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest_main() throws Exception
    {
        m_device = null;

        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().equals("Test Device"));

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;

        doConnectDiscoveredDeviceTest(m_config);
        startAsyncTest();
    }

    private void doConnectDiscoveredDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                m_mgr.stopScan();
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    assertTrue(e1.wasSuccess());
                    succeed();
                });
            }
        });

        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                NativeUtil.advertiseNewDevice(m_mgr, -45, "Test Device");
            }
        });

        m_mgr.startScan();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().contains("Test Device"));

        m_config.loggingOptions = LogOptions.ON;

        doConnectDiscoveredMultipleDeviceTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().contains("Test Device"));

        m_config.loggingOptions = LogOptions.ON;

        doConnectDiscoveredMultipleDeviceTest(m_config);

        startAsyncTest();
    }

    private void doConnectDiscoveredMultipleDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        m_mgr.setListener_Discovery(new DiscoveryListener()
        {
            final Pointer<Integer> connected = new Pointer(0);

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED) || e.was(LifeCycle.REDISCOVERED))
                {
                    e.device().connect(e1 ->
                    {
                        assertTrue(e1.wasSuccess());
                        connected.value++;
                        System.out.println(e1.device().getName_override() + " connected. #" + connected.value);
                        if (connected.value == 3)
                        {
                            succeed();
                        }
                    });
                }
            }
        });

        m_mgr.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                NativeUtil.advertiseNewDevice(m_mgr, -45, "Test Device #1");
                NativeUtil.advertiseNewDevice(m_mgr, -35, "Test Device #2");
                NativeUtil.advertiseNewDevice(m_mgr, -60, "Test Device #3");
            }
        });

        m_mgr.startScan();
    }

    @Test(timeout = 40000)
    public void connectFailTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectFailTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectFailTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectFailTest(m_config);
        startAsyncTest();
    }

    private void doConnectFailTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    if (e1.wasSuccess())
                    {
                        succeed();
                    }
                    else
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            succeed();
                        }
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectFailManagerTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectFailManagerTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectFailManagerTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectFailManagerTest(m_config);
        startAsyncTest();
    }

    private void doConnectFailManagerTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        m_mgr.setListener_DeviceReconnect(new DefaultDeviceReconnectFilter()
        {
            @Override
            public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
            {
                System.out.println("Connection fail event: " + e.toString());
                if (e.failureCountSoFar() == 3)
                {
                    succeed();
                }
                return super.onConnectFailed(e);
            }
        });

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    if (e1.wasSuccess())
                    {
                        succeed();
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);

        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenDisconnectBeforeServiceDiscoveryTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            succeed();
                        }
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.SERVICE_DISCOVERY_FAILED);

        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailDiscoverServicesTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.SERVICE_DISCOVERY_FAILED);

        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailDiscoverServicesTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenFailDiscoverServicesTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 ->
                {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            succeed();
                        }
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 9000000)
    public void connectThenTimeoutThenFailTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.TIME_OUT);

        m_config.connectFailRetryConnectingOverall = false;

        doconnectThenTimeoutThenFailTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 9000000)
    public void connectThenTimeoutThenFailTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.TIME_OUT);

        m_config.connectFailRetryConnectingOverall = false;

        doconnectThenTimeoutThenFailTest(m_config);
        startAsyncTest();
    }

    private void doconnectThenTimeoutThenFailTest(BleManagerConfig config) throws Exception
    {
        m_device = null;

        m_mgr.setConfig(config);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e ->
                {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
//                            fail();
                    }
                });
                device.read(read);
                read.setReadWriteListener(e ->
                {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
                        fail();
                    }
                });
                device.read(read);
            }
        };

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(null, init, e1 ->
                {
                    if (!e1.wasSuccess())
                    {
                        assertTrue(e1.failEvent().status() == DeviceReconnectFilter.Status.INITIALIZATION_FAILED);
                        System.out.println("Connection fail event: " + e1.toString());
                        succeed();
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.GATT_ERROR);
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailInitTxnTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenFailInitTxnTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = device -> new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.GATT_ERROR);
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailInitTxnTest(m_config);

        startAsyncTest();
    }

    private void doConnectThenFailInitTxnTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override
            protected void start(BleDevice device)
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e ->
                {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
                        fail();
                    }
                });
                device.read(read);
            }
        };

        m_mgr.setListener_Discovery(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(null, init, e1 ->
                {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            succeed();
                        }
                    }
                });
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = UnitTestGatt::new;

        doDisconnectDuringConnectTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattLayerFactory = UnitTestGatt::new;

        doDisconnectDuringConnectTest(m_config);
        startAsyncTest();
    }

    private void doDisconnectDuringConnectTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.setListener_State(e1 ->
                    {
                        System.out.print(e1);
                        if (e1.didEnter(BleDeviceState.CONNECTING))
                        {
                            hasConnected = true;
                            m_device.disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    m_device.connect();
                }
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 12000)
    public void disconnectDuringServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        doDisconnectDuringServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void disconnectDuringServiceDiscoveryTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;

        doDisconnectDuringServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    private void doDisconnectDuringServiceDiscoveryTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.setListener_State(e1 ->
                    {
                        if (e1.didEnter(BleDeviceState.DISCOVERING_SERVICES))
                        {
                            hasConnected = true;
                            m_device.disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    m_device.connect();
                }
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }


    @Test(timeout = 12000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingOptions = LogOptions.ON;

        doConnectThenDisconnectTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectThenDisconnectTest_main() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = true;
        m_config.loggingOptions = LogOptions.ON;

        doConnectThenDisconnectTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenDisconnectTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.setListener_State(e1 ->
                    {
                        if (e1.didEnter(BleDeviceState.INITIALIZED))
                        {
                            hasConnected = true;
                            m_device.disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    m_device.connect();
                }
            }
        });

        m_mgr.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }


    @Override
    public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.nativeManagerLayer = new UnitTestManagerLayer();
        m_config.nativeDeviceFactory = (P_NativeDeviceLayerFactory<UnitTestDevice>) device -> new UnitTestDevice(device);
        m_config.gattLayerFactory = (P_GattLayerFactory<UnitTestGatt>) device -> new UnitTestGatt(device);
        m_config.logger = new UnitTestLogger();
        m_config.runOnMainThread = false;
        return m_config;
    }

}
