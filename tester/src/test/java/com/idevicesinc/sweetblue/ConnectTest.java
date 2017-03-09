package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{

    private BleDevice m_device;


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

    @Test(timeout = 16000)
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

    @Test(timeout = 12000)
    public void connectFailTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGattLayer(device);
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

    @Test(timeout = 30000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DisconnectBeforeServiceDiscoveryGattLayer(device);
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

    @Test(timeout = 12000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DiscoverServicesFailGattLayer(device);
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
                return new TimeOutGattLayer(device);
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

    @Test(timeout = 14000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_device = null;

        m_config.runOnMainThread = false;
        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ReadFailGattLayer(device);
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
                return new DisconnectGattLayer(device);
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
                                m_device.disconnect();
                                ((DisconnectGattLayer) m_device.layerManager().getGattLayer()).disconnectCalled = true;
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



    private class DisconnectGattLayer extends UnitTestGatt
    {

        public boolean disconnectCalled = false;

        public DisconnectGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            setGattNull(false);
            ((UnitTestManagerLayer) m_device.layerManager().getManagerLayer()).updateDeviceState(m_device, BluetoothGatt.STATE_CONNECTING);
            m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    if (!disconnectCalled)
                    {
                        setToConnecting();
                    }
                }
            }, 50);
            m_device.getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    if (!disconnectCalled)
                    {
                        setToConnected();
                    }
                }
            }, 150);
            return device.connect(context, useAutoConnect, callback);
        }
    }


    private class TimeOutGattLayer extends UnitTestGatt
    {

        public TimeOutGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            List<BluetoothGattService> list = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
            service.addCharacteristic(ch);
            return list;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(Uuids.BATTERY_SERVICE_UUID))
            {
                BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
                service.addCharacteristic(ch);
                return service;
            }
            else
            {
                return null;
            }
        }

        @Override public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, 14500);
            return true;
        }
    }

    private class ReadFailGattLayer extends UnitTestGatt
    {

        public ReadFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            List<BluetoothGattService> list = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
            service.addCharacteristic(ch);
            return list;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(Uuids.BATTERY_SERVICE_UUID))
            {
                BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
                service.addCharacteristic(ch);
                return service;
            }
            else
            {
                return null;
            }
        }

        @Override public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            UnitTestUtils.readError(getBleDevice(), characteristic, BleStatuses.GATT_ERROR, 150);
            return true;
        }
    }

    private class ConnectFailGattLayer extends UnitTestGatt
    {

        public ConnectFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setToConnecting()
        {
            super.setToConnecting();
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, 50);
        }

        @Override public void setToConnected()
        {
        }
    }

    private class DisconnectBeforeServiceDiscoveryGattLayer extends UnitTestGatt
    {

        public DisconnectBeforeServiceDiscoveryGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, false, 175);
            return super.connect(device, context, useAutoConnect, callback);
        }
    }

    private class DiscoverServicesFailGattLayer extends UnitTestGatt
    {

        public DiscoverServicesFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setServicesDiscovered()
        {
            UnitTestUtils.failDiscoverServices(getBleDevice(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        }
    }

}
