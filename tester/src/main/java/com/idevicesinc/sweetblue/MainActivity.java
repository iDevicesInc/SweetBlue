package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.DebugLogger;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.idevicesinc.sweetblue.tester.R;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;


public class MainActivity extends Activity
{

    private final static int STATE_CHANGE_MIN_TIME = 50;

    BleManager mgr;
    private ListView mListView;
    private Button mStartScan;
    private Button mStopScan;
    private ScanAdaptor mAdaptor;
    private ArrayList<BleDevice> mDevices;
    private DebugLogger mLogger;
    private long mLastStateChange;


    private final static UUID tempUuid = UUID.fromString("47495078-0002-491E-B9A4-F85CD01C3698");
//    private final static UUID tempUuid = UUID.fromString("1234666b-1000-2000-8000-001199334455");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.listView);
        mDevices = new ArrayList<>(0);
        mAdaptor = new ScanAdaptor(this, mDevices);
        mListView.setAdapter(mAdaptor);
        mListView.setOnItemClickListener((parent, view, position, id) ->
        {
            final BleDevice device = mDevices.get(position);
            device.setListener_State((BleDevice.StateListener) e ->
            {
                if (e.didEnter(BleDeviceState.INITIALIZED))
                {
//                            byte[] fakeData = new byte[100];
//                            new Random().nextBytes(fakeData);
//                            device.write(tempUuid, fakeData, null);
                    device.read(Uuids.BATTERY_LEVEL);
                }
            });
            device.connect();
        });
//        mListView.setOnItemLongClickListener((parent, view, position, id) ->
//        {
//            BleDevice device = mDevices.get(position);
//            if (device.is(BleDeviceState.CONNECTED))
//            {
//                device.disconnect();
//                return true;
//            }
//            return false;
//        });

        registerForContextMenu(mListView);

        mStartScan = findViewById(R.id.startScan);
        mStartScan.setOnClickListener(v -> mgr.startPeriodicScan(Interval.TEN_SECS, Interval.ONE_SEC));
        mStopScan = findViewById(R.id.stopScan);
        mStopScan.setOnClickListener(v -> mgr.stopAllScanning());

        mLogger = new DebugLogger(250);

        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        config.logger = mLogger;
        config.bondRetryFilter = new BondRetryFilter.DefaultBondRetryFilter(5);
        config.scanApi = BleScanApi.AUTO;
        config.runOnMainThread = false;
//        config.delayBetweenTasks = Interval.secs(2.0);
//        config.defaultInitFactory = () -> new BleTransaction.Init()
//        {
//            @Override
//            protected void start(BleDevice device)
//            {
//                device.read(Uuids.BATTERY_LEVEL, e ->
//                {
//                    if (e.wasSuccess())
//                    {
//                        succeed();
//                    }
//                    else
//                    {
//                        fail();
//                    }
//                });
//            }
//        };
        config.forceBondDialog = true;
        config.reconnectFilter = new BleNodeConfig.DefaultReconnectFilter(Interval.ONE_SEC, Interval.secs(3.0), Interval.FIVE_SECS, Interval.secs(45));
        config.uhOhCallbackThrottle = Interval.secs(60.0);

        config.defaultScanFilter = e -> BleManagerConfig.ScanFilter.Please.acknowledgeIf(e.name_normalized().contains("wall"));

        mgr = BleManager.get(this, config);

        mgr.setListener_UhOh(e -> Log.e("UhOhs", "Got " + e.uhOh() + " with remedy " + e.remedy()));

        mgr.setListener_State((BleManager.StateListener) event ->
        {
            boolean scanning = mgr.isScanning();
            mStartScan.setEnabled(!scanning);

        });
        mgr.setListener_DeviceState((DeviceStateListener) e ->
        {
            if (System.currentTimeMillis() - mLastStateChange > STATE_CHANGE_MIN_TIME)
                mAdaptor.notifyDataSetChanged();
        });
        mgr.setListener_Discovery(e ->
        {
            if (e.was(BleManager.DiscoveryListener.LifeCycle.DISCOVERED))
            {
                if (!mDevices.contains(e.device()))
                {
                    mDevices.add(e.device());
                    mAdaptor.notifyDataSetChanged();

                }
            }
            else if (e.was(BleManager.DiscoveryListener.LifeCycle.REDISCOVERED))
            {

            }
        });

        mStartScan.setEnabled(false);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter()
                {
                    @Override
                    public Please onEvent(BluetoothEnablerEvent e)
                    {
                        if (e.isDone())
                        {
                            mStartScan.setEnabled(true);
                        }
                        return super.onEvent(e);
                    }
                }
        );

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.print_pretty_log:
                Log.e("Logs!", Utils_String.prettyFormatLogList(mLogger.getLogList()));
                return true;
            case R.id.nukeBle:
                mgr.nukeBle();
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final BleDevice device = mDevices.get(info.position);
            boolean isBonded = device.is(BleDeviceState.BONDED);
            boolean connected = device.is(BleDeviceState.CONNECTED);

            menu.add(0, 0, 0, "Remove Bond");

            if (!isBonded)
            {
                menu.add(1, 1, 0, "Bond");
            }
            if (connected)
            {
                menu.add(2, 2, 0, "Disconnect");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == 0)
        {
            mDevices.get(info.position).unbond();
            return true;
        }
        else if (item.getItemId() == 1)
        {
            mDevices.get(info.position).bond(e -> Log.e("Bonding Event", e.toString()));
            return true;
        }
        else if (item.getItemId() == 2)
        {
            mDevices.get(info.position).disconnect();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private class ScanAdaptor extends ArrayAdapter<BleDevice>
    {

        private List<BleDevice> mDevices;


        public ScanAdaptor(Context context, List<BleDevice> objects)
        {
            super(context, R.layout.scan_listitem_layout, objects);
            mDevices = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder v;
            final BleDevice device = mDevices.get(position);
            if (convertView == null)
            {
                convertView = View.inflate(getContext(), R.layout.scan_listitem_layout, null);
                v = new ViewHolder();
                v.name = convertView.findViewById(R.id.name);
                v.rssi = convertView.findViewById(R.id.rssi);
                convertView.setTag(v);
            }
            else
            {
                v = (ViewHolder) convertView.getTag();
            }
            v.name.setText(Utils_String.concatStrings(device.toString(), "\nNative Name: ", device.getName_native()));
            //v.rssi.setText(String.valueOf(mDevices.get(position).getRssi()));
            return convertView;
        }

    }

    private static class ViewHolder
    {
        private TextView name;
        private TextView rssi;
    }
}
