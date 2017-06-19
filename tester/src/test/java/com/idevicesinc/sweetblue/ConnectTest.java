package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 24)
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
        m_config.loggingEnabled = true;

        doConnectCreatedDeviceTest(m_config);

        m_mgr.disconnectAll();

        m_config.runOnMainThread = true;

        doConnectCreatedDeviceTest(m_config);

    }

    private void doConnectCreatedDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED) || e.was(LifeCycle.REDISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 40000)
    public void retryConnectionTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override
            public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);
            }
        };

        m_mgr.setConfig(m_config);

        m_device = m_mgr.newDevice(UnitTestUtils.randomMacAddress());

        m_device.connect(new BleDevice.StateListener() {

            int timesTried = 0;

            @Override
            public void onEvent(BleDevice.StateListener.StateEvent e)
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
                        s.release();
                    }
                }
            }
        }, new BleDevice.DefaultConnectionFailListener(3, 3));

        s.acquire();
    }

    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_device = null;

        m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().equals("Test Device"));
            }
        };

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;

        doConnectDiscoveredDeviceTest(m_config);

        m_mgr.disconnectAll();

        m_config.runOnMainThread = true;

        doConnectDiscoveredDeviceTest(m_config);
    }

    private void doConnectDiscoveredDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED) || e.was(LifeCycle.REDISCOVERED))
                {
                    m_mgr.stopScan();
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    UnitTestUtils.advertiseNewDevice(m_mgr, -45, "Test Device");
                }
            }
        });

        m_mgr.startScan();
        s.acquire();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().contains("Test Device"));
            }
        };

        m_config.loggingEnabled = true;

        doConnectDiscoveredMultipleDeviceTest(m_config);

        m_mgr.stopScan();
        m_mgr.disconnectAll();

        m_config.runOnMainThread = true;

        doConnectDiscoveredMultipleDeviceTest(m_config);

    }

    private void doConnectDiscoveredMultipleDeviceTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            final Pointer<Integer> connected = new Pointer(0);

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED) || e.was(LifeCycle.REDISCOVERED))
                {
                    e.device().connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                connected.value++;
                                System.out.println(e.device().getName_override() + " connected. #" + connected.value);
                                if (connected.value == 3)
                                {
                                    s.release();
                                }
                            }
                        }
                    });
                }
            }
        });

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    UnitTestUtils.advertiseNewDevice(m_mgr, -45, "Test Device #1");
                    UnitTestUtils.advertiseNewDevice(m_mgr, -35, "Test Device #2");
                    UnitTestUtils.advertiseNewDevice(m_mgr, -60, "Test Device #3");
                }
            }
        });

        m_mgr.startScan();
        s.acquire();
    }

    @Test(timeout = 40000)
    public void connectFailTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);
            }
        };

        doConnectFailTest(m_config);

        m_config.runOnMainThread = true;

        doConnectFailTest(m_config);

    }

    private void doConnectFailTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    }, new BleDevice.DefaultConnectionFailListener()
                    {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    } );

                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 40000)
    public void connectFailManagerTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);
            }
        };

        doConnectFailManagerTest(m_config);

        m_config.runOnMainThread = true;

        doConnectFailManagerTest(m_config);

    }

    private void doConnectFailManagerTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_ConnectionFail(new BleDevice.DefaultConnectionFailListener()
        {
            @Override public Please onEvent(ConnectionFailEvent e)
            {
                System.out.println("Connection fail event: " + e.toString());
                if (e.failureCountSoFar() == 3)
                {
                    s.release();
                }
                return super.onEvent(e);
            }
        });

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 40000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.DISCONNECT_GATT_ERROR);
            }
        };

        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);

        m_config.runOnMainThread = true;

        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);

    }

    private void doConnectThenDisconnectBeforeServiceDiscoveryTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                        }
                    }, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGatt(device, ConnectFailGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailGatt.FailureType.SERVICE_DISCOVERY_FAILED);
            }
        };

        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailDiscoverServicesTest(m_config);

        m_config.runOnMainThread = true;

        doConnectThenFailDiscoverServicesTest(m_config);

    }

    private void doConnectThenFailDiscoverServicesTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                        }
                    }, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 90000)
    public void connectThenTimeoutThenFailTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.TIME_OUT);
            }
        };

        m_config.connectFailRetryConnectingOverall = false;

        doconnectThenTimeoutThenFailTest(m_config);

        m_config.runOnMainThread = true;

        doconnectThenTimeoutThenFailTest(m_config);

    }

    private void doconnectThenTimeoutThenFailTest(BleManagerConfig config) throws Exception
    {
        m_device = null;

        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override protected void start(BleDevice device)
            {
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
//                            fail();
                        }
                    }
                });
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
                            fail();
                        }
                    }
                });
            }
        };

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(init, null, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            assertTrue(e.status() == Status.INITIALIZATION_FAILED);
                            System.out.println("Connection fail event: " + e.toString());
                            s.release();
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 40000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ReadWriteFailGatt(device, batteryDb, ReadWriteFailGatt.FailType.GATT_ERROR);
            }
        };
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailInitTxnTest(m_config);

        m_config.runOnMainThread = true;

        doConnectThenFailInitTxnTest(m_config);

    }

    private void doConnectThenFailInitTxnTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override protected void start(BleDevice device)
            {
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
                            fail();
                        }
                    }
                });
            }
        };

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(init, null, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                //return new DisconnectGattLayer(device);
                return new UnitTestGatt(device);
            }
        };

        doDisconnectDuringConnectTest(m_config);

        m_config.runOnMainThread = true;

        doDisconnectDuringConnectTest(m_config);

    }

    private void doDisconnectDuringConnectTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            System.out.print(e);
                            if (e.didEnter(BleDeviceState.CONNECTING))
                            {
                                hasConnected = true;
                                //((DisconnectGattLayer) m_device.layerManager().getGattLayer()).disconnectCalled = true;
                                m_device.disconnect();
                            }
                            else if (hasConnected && e.didEnter(BleDeviceState.DISCONNECTED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 12000)
    public void disconnectDuringServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;

        doDisconnectDuringServiceDiscoveryTest(m_config);

        m_config.runOnMainThread = true;

        doDisconnectDuringServiceDiscoveryTest(m_config);

    }

    private void doDisconnectDuringServiceDiscoveryTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.DISCOVERING_SERVICES))
                            {
                                hasConnected = true;
                                m_device.disconnect();
                            }
                            else if (hasConnected && e.didEnter(BleDeviceState.DISCONNECTED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }


    @Test(timeout = 12000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;

        doConnectThenDisconnectTest(m_config);

        m_config.runOnMainThread = true;

        doConnectThenDisconnectTest(m_config);

    }

    private void doConnectThenDisconnectTest(BleManagerConfig config) throws Exception
    {
        m_mgr.setConfig(config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                hasConnected = true;
                                m_device.disconnect();
                            }
                            else if (hasConnected && e.didEnter(BleDeviceState.DISCONNECTED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }



    @Override public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.nativeManagerLayer = new UnitTestManagerLayer();
        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory<UnitTestDevice>()
        {
            @Override public UnitTestDevice newInstance(BleDevice device)
            {
                return new UnitTestDevice(device);
            }
        };
        m_config.gattLayerFactory = new P_GattLayerFactory<UnitTestGatt>()
        {
            @Override public UnitTestGatt newInstance(BleDevice device)
            {
                return new UnitTestGatt(device);
            }
        };
        m_config.logger = new UnitTestLogger();
        m_config.runOnMainThread = false;
        return m_config;
    }

}
