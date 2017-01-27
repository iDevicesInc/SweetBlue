package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Semaphore;



@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{

    private int m_currentState = BluetoothGatt.STATE_DISCONNECTED;
    private BleDevice m_device;


    @Override public P_NativeManagerLayer getManagerLayer()
    {
        return new P_UnitTestManagerLayer();
    }


    @Test(timeout = 5000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_currentState = 0;
        m_device = null;



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
                            if (e.didEnter(BleDeviceState.CONNECTING))
                            {
                                // Push the "connected" state to the native wrapper
                                m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
                                {
                                    @Override public void run()
                                    {
                                        m_currentState = BluetoothGatt.STATE_CONNECTED;
                                        m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_SUCCESS, m_currentState);
                                    }
                                }, 50);
                            }
                            else if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        BleDevice device = m_mgr.newDevice("00:11:22:33:44:55", "Test Device");

        s.acquire();
    }

    @Test(timeout = 5000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_currentState = 0;
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_mgr.m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().equals("Test Device"));
            }
        };

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
                            if (e.didEnter(BleDeviceState.CONNECTING))
                            {
                                // Push the "connected" state to the native wrapper
                                m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
                                {
                                    @Override public void run()
                                    {
                                        m_currentState = BluetoothGatt.STATE_CONNECTED;
                                        m_device.m_listeners.onConnectionStateChange(null, BleStatuses.GATT_SUCCESS, m_currentState);
                                    }
                                }, 50);
                            }
                            else if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        final byte[] scanRecord = getNameRecord("Test Device");

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

    private byte[] getNameRecord(String name)
    {
        byte[] nameBytes = name.getBytes();

        byte[] record = new byte[nameBytes.length + 1];

        record[0] = 0x09; // FULL Name type

        for (int i = 0; i < record.length; i++)
        {
            if (i == 0)
            {
                record[i] = 0x09;
            }
            else
            {
                record[i] = nameBytes[i - 1];
            }
        }

        return record;
    }

    @Override public BleManagerConfig getConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.unitTest = true;
        config.nativeManagerLayer = new ManagerLayer();
        config.nativeDeviceFactory = new P_NativeDeviceLayerFactory<DeviceLayer>()
        {
            @Override public DeviceLayer newInstance()
            {
                return new DeviceLayer();
            }
        };
        config.gattLayerFactory = new P_GattLayerFactory<GattLayer>()
        {
            @Override public GattLayer newInstance()
            {
                return new GattLayer();
            }
        };
        config.runOnMainThread = false;
        return config;
    }

    private class GattLayer extends P_UnitGatt
    {
        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            return super.connect(device, context, useAutoConnect, callback);
        }

        @Override public void disconnect()
        {
            super.disconnect();
            m_currentState = BluetoothGatt.STATE_DISCONNECTED;
        }

        @Override public boolean discoverServices()
        {
            m_mgr.getPostManager().postToUpdateThreadDelayed(new Runnable()
            {
                @Override public void run()
                {
                      m_device.m_listeners.onServicesDiscovered(null, BleStatuses.GATT_SUCCESS);
                }
            }, 250);
            return true;
        }
    }


    private class DeviceLayer extends P_UnitDevice
    {
        @Override public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            m_currentState = BluetoothGatt.STATE_CONNECTING;
            return super.connect(context, useAutoConnect, callback);
        }

        @Override public String getName()
        {
            return "Test Device";
        }
    }

    private class ManagerLayer extends P_UnitTestManagerLayer
    {

        @Override public int getConnectionState(P_NativeDeviceLayer device, int profile)
        {
            return m_currentState;
        }
    }

}
