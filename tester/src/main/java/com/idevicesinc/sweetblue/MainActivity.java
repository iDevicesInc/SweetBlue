package com.idevicesinc.sweetblue;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import com.idevicesinc.sweetblue.tester.R;
import com.idevicesinc.sweetblue.utils.Utils_String;


public class MainActivity extends Activity
{

    BleManager mgr;
    private ListView mListView;
    private Button mStartScan;
    private Button mStopScan;
    private ScanAdaptor mAdaptor;
    private ArrayList<BleDevice> mDevices;


    private final static UUID tempUuid = UUID.fromString("47495078-0002-491E-B9A4-F85CD01C3698");
//    private final static UUID tempUuid = UUID.fromString("1234666b-1000-2000-8000-001199334455");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView);
        mDevices = new ArrayList<>(0);
        mAdaptor = new ScanAdaptor(this, mDevices);
        mListView.setAdapter(mAdaptor);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                final BleDevice device = mDevices.get(position);
                device.setListener_State(new BleDevice.StateListener()
                {
                    @Override public void onEvent(StateEvent e)
                    {
                        if (e.didEnter(BleDeviceState.INITIALIZED))
                        {
                            byte[] fakeData = new byte[100];
                            new Random().nextBytes(fakeData);
                            device.write(tempUuid, fakeData, null);
                        }
                        mAdaptor.notifyDataSetChanged();
                    }
                });
                device.connect();
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                BleDevice device = mDevices.get(position);
                if (device.is(BleDeviceState.CONNECTED))
                {
                    device.disconnect();
                    return true;
                }
                return false;
            }
        });

        registerForContextMenu(mListView);

        mStartScan = (Button) findViewById(R.id.startScan);
        mStartScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                mgr.startPeriodicScan(Interval.FIVE_SECS, Interval.ONE_SEC);
            }
        });
        mStopScan = (Button) findViewById(R.id.stopScan);
        mStopScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                mgr.stopPeriodicScan();
            }
        });

        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        config.scanApi = BleScanApi.PRE_LOLLIPOP;
        config.useGattRefresh = true;
        config.runOnMainThread = false;

        mgr = BleManager.get(this, config);
        mgr.setListener_State(new BleManager.StateListener()
        {
            @Override public void onEvent(StateEvent event)
            {
                if (event.didEnter(BleManagerState.ON))
                {
                    mStartScan.setEnabled(true);
                }
                else if (event.didEnter(BleManagerState.SCANNING))
                {
                    mStartScan.setEnabled(false);
                    mStopScan.setEnabled(true);
                }
                else if (event.didExit(BleManagerState.SCANNING))
                {
                    mStartScan.setEnabled(true);
//                    mStopScan.setEnabled(false);
                }
            }
        });
        mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(BleManager.DiscoveryListener.DiscoveryEvent e)
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
            }
        });


        mStartScan.setEnabled(false);

        BluetoothEnabler.start(this, new BluetoothEnabler.DefaultBluetoothEnablerFilter()
                {
                    @Override public Please onEvent(BluetoothEnablerEvent e)
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

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            boolean isBonded = mDevices.get(info.position).is(BleDeviceState.BONDED);
            boolean connected = mDevices.get(info.position).is(BleDeviceState.CONNECTED);

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

    @Override public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == 0)
        {
            mDevices.get(info.position).unbond();
            return true;
        }
        else if (item.getItemId() == 1)
        {
            mDevices.get(info.position).bond();
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

        @Override public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder v;
            if (convertView == null)
            {
                convertView = View.inflate(getContext(), R.layout.scan_listitem_layout, null);
                v = new ViewHolder();
                v.name = (TextView) convertView.findViewById(R.id.name);
                v.rssi = (TextView) convertView.findViewById(R.id.rssi);
                convertView.setTag(v);
            }
            else
            {
                v = (ViewHolder) convertView.getTag();
            }
            v.name.setText(Utils_String.concatStrings(mDevices.get(position).toString(), "\nNative Name: ", mDevices.get(position).getName_native()));
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
