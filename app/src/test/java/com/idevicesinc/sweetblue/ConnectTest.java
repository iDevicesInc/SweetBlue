package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
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

    private int m_currentState = BluetoothGatt.STATE_DISCONNECTED;
    private BleDevice m_device;


    @Test(timeout = 6000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

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
                    });
                }
            }
        });

        m_mgr.newDevice(P_UnitUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().equals("Test Device"));
            }
        };

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

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

        final byte[] scanRecord = P_UnitUtils.newScanRecord("Test Device");

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    m_mgr.getScanManager().postScanResult(null, 90, scanRecord);
                }
            }
        });

        m_mgr.startScan();
        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectFailTest() throws Exception
    {
        m_currentState = 0;
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGattLayer(device);
            }
        };
        m_mgr.setConfig(m_config);

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

        m_mgr.newDevice(P_UnitUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DisconnectBeforeServiceDiscoveryGattLayer(device);
            }
        };

        m_mgr.setConfig(m_config);

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

        m_mgr.newDevice(P_UnitUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DiscoverServicesFailGattLayer(device);
            }
        };

        m_config.connectFailRetryConnectingOverall = true;
        m_mgr.setConfig(m_config);

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

        m_mgr.newDevice(P_UnitUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 7000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ReadFailGattLayer(device);
            }
        };
        m_config.connectFailRetryConnectingOverall = true;
        m_mgr.setConfig(m_config);

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

        m_mgr.newDevice(P_UnitUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Override public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.unitTest = true;
        m_config.nativeManagerLayer = new P_UnitTestManagerLayer();
        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory<P_UnitDevice>()
        {
            @Override public P_UnitDevice newInstance(BleDevice device)
            {
                return new P_UnitDevice(device);
            }
        };
        m_config.gattLayerFactory = new P_GattLayerFactory<P_UnitGatt>()
        {
            @Override public P_UnitGatt newInstance(BleDevice device)
            {
                return new P_UnitGatt(device);
            }
        };
        m_config.logger = new P_UnitLogger();
        m_config.runOnMainThread = false;
        return m_config;
    }

    private class ReadFailGattLayer extends P_UnitGatt
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
            getBleDevice().getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onCharacteristicRead(null, characteristic, BleStatuses.GATT_ERROR);
                }
            }, 150);
            return true;
        }
    }

    private class ConnectFailGattLayer extends P_UnitGatt
    {

        public ConnectFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setToConnecting()
        {
            super.setToConnecting();
            getBleDevice().getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    ((P_UnitTestManagerLayer) getBleDevice().layerManager().getManagerLayer()).updateDeviceState(getBleDevice(), BluetoothGatt.STATE_DISCONNECTED);
                    m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_ERROR, m_currentState);
                }
            }, 50);
        }

        @Override public void setToConnected()
        {
        }
    }

    private class DisconnectBeforeServiceDiscoveryGattLayer extends P_UnitGatt
    {

        public DisconnectBeforeServiceDiscoveryGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            getBleDevice().getManager().getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                    getBleDevice().m_listeners.onConnectionStateChange(null, BleStatuses.GATT_ERROR, BluetoothGatt.STATE_DISCONNECTED);
                }
            }, 175);
            return super.connect(device, context, useAutoConnect, callback);
        }
    }

    private class DiscoverServicesFailGattLayer extends P_UnitGatt
    {

        public DiscoverServicesFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setServicesDiscovered()
        {
            m_device.m_listeners.onServicesDiscovered(null, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        }
    }

}
