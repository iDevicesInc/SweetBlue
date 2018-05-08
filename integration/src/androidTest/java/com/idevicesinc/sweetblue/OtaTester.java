package com.idevicesinc.sweetblue;

import android.util.Log;

import com.idevicesinc.sweetblue.utils.ByteBuffer;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class OtaTester extends BaseTester<MainActivity>
{

    private final static UUID TRANSFER_SERVICE = Uuids.fromInt("AA00");
    private final static UUID RX_CHAR = Uuids.fromInt("AA01");
    private final static UUID TX_CHAR = Uuids.fromInt("AA02");
    private final static UUID CONTROL_SERVICE = Uuids.fromInt("BB01");
    private final static UUID CONTROL_CHAR = Uuids.fromInt("BB02");

    private final static int CONTROL_NORMAL = 0;
    private final static int CONTROL_OTA_START = 1;
    private final static int CONTROL_OTA_END = 2;


    @Test
    public void otaTest() throws Exception
    {
        final Semaphore s = new Semaphore(0);

        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    mgr.stopAllScanning();
                    e.device().connect(new BleDevice.StateListener()
                    {
                        @Override
                        public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                e.device().negotiateMtu(512, new BleDevice.ReadWriteListener()
                                {
                                    @Override
                                    public void onEvent(ReadWriteEvent e)
                                    {
                                        if (e.wasSuccess())
                                            Log.e("MTU", "It worked!!");
                                        else
                                            Log.e("MTU", "It didn't work :(");
                                        WriteBuilder write = new WriteBuilder(CONTROL_SERVICE, CONTROL_CHAR);
                                        write.setBytes(new byte[] { CONTROL_OTA_START } );
                                        write.setReadWriteListener(new ReadWriteListener()
                                        {
                                            @Override
                                            public void onEvent(ReadWriteEvent e)
                                            {
                                                assertTrue(e.wasSuccess());
                                                try
                                                {
                                                    doOta(e.device(), s);
                                                } catch (Exception e1)
                                                {
                                                    e1.printStackTrace();
                                                }
                                            }
                                        });
                                        e.device().write(write);
                                    }
                                });

                            }
                        }
                    });
                }
            }
        });

        mgr.startScan();

        s.acquire();
    }

    private void doOta(final BleDevice device, final Semaphore s) throws Exception
    {
        final int mtuSize = device.getEffectiveWriteMtuSize();

        BufferedInputStream in = new BufferedInputStream(activity.getAssets().open("OtaFile.bin"));
        byte[] buff = new byte[mtuSize];
        final ByteBuffer bb = new ByteBuffer();
        int len;
        while ((len = in.read(buff)) != -1)
        {
            bb.append(buff, len);
        }
        in.close();

        final long startTime = System.currentTimeMillis();
        final Pointer<Integer> curOffest = new Pointer<>(0);

        final WriteBuilder write = new WriteBuilder(TRANSFER_SERVICE, TX_CHAR);
        write.setBytes(bb.subData(curOffest.value, mtuSize));
        curOffest.value += mtuSize;
        write.setReadWriteListener(new ReadWriteListener()
        {
            @Override
            public void onEvent(BleDevice.ReadWriteListener.ReadWriteEvent e)
            {
                if (e.wasSuccess())
                {
                    if (curOffest.value < bb.length())
                    {
                        int length = mtuSize;
                        if (curOffest.value + mtuSize > bb.length())
                            length = bb.length() - curOffest.value;

                        write.setBytes(bb.subData(curOffest.value, length));
                        curOffest.value += length;
                        device.write(write);
                    }
                    else
                    {
                        WriteBuilder w = new WriteBuilder(CONTROL_SERVICE, CONTROL_CHAR);
                        w.setBytes(new byte[] { CONTROL_OTA_END });
                        w.setReadWriteListener(new ReadWriteListener()
                        {
                            @Override
                            public void onEvent(BleDevice.ReadWriteListener.ReadWriteEvent e)
                            {
                                assertTrue(e.wasSuccess());
                                device.disconnect();
                                long diff = (System.currentTimeMillis() - startTime) / 1000;
                                Log.e("OTATEST", "OTA took " + diff + " seconds.");
                                System.out.println("OTA took " + diff + " seconds.");
                                s.release();
                            }
                        });
                        device.write(w);
                    }
                }
                else
                    assertFalse("Read failed! Event: " + e, true);
            }
        });
        device.write(write);
    }

    @Test
    public void createOtaFile()
    {

    }

    @Override
    Class<MainActivity> getActivityClass()
    {
        return MainActivity.class;
    }

    @Override
    BleManagerConfig getInitialConfig()
    {
        BleManagerConfig config = new BleManagerConfig();
        config.runOnMainThread = false;
        config.loggingEnabled = true;
        config.connectFailRetryConnectingOverall = true;
        config.scanReportDelay = Interval.ZERO;
        config.autoUpdateRate = Interval.millis(10);
        config.defaultScanFilter = new BleManagerConfig.DefaultScanFilter(TRANSFER_SERVICE);
        return config;
    }
}
