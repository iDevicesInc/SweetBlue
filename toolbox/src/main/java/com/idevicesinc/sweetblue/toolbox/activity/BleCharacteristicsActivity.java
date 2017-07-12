package com.idevicesinc.sweetblue.toolbox.activity;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.toolbox.view.CharacteristicAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BleCharacteristicsActivity extends BaseActivity implements BleDevice.ReadWriteListener
{
    private static final int WRITE_VALUE_RESULT_CODE = 101;

    private BleDevice m_device;
    private BluetoothGattService m_service;
    private List<BluetoothGattCharacteristic> m_characteristicList;
    private TextView m_noCharacteristicsTextView;

    private CharacteristicAdapter m_adapter;
    private ExpandableListView m_charListView;
    private SwipeRefreshLayout m_swipeRefreshLayout;

    private TestTransaction mTransaction = null;

    public class TestTransaction extends BleTransaction
    {
        private List<Object> mPendingReadQueue = new ArrayList<>();
        private AtomicBoolean mTransactionLock = new AtomicBoolean(false);
        private BleDevice.ReadWriteListener mListener = null;

        TestTransaction(BleDevice.ReadWriteListener listener, List<BluetoothGattCharacteristic> characteristicList)
        {
            mListener = listener;

            buildQueue(characteristicList, false);
        }

        public void refreshAll(List<BluetoothGattCharacteristic> list)
        {
            mPendingReadQueue.clear();  //FIXME:  synchronize
            buildQueue(list, true);
        }

        private void buildQueue(List<BluetoothGattCharacteristic> list, boolean wipeValues)
        {
            // Populate initial queue
            for (BluetoothGattCharacteristic bgc : list)
            {
                if (wipeValues)
                    bgc.setValue((byte[])null);
                mPendingReadQueue.add(bgc);
                List<BluetoothGattDescriptor> descriptorList = bgc.getDescriptors();
                for (BluetoothGattDescriptor bgd : descriptorList)
                {
                    if (wipeValues)
                        bgd.setValue(null);
                    mPendingReadQueue.add(bgd);
                }
            }
        }

        @Override
        protected void start(BleDevice device)
        {
            // Do nothing, we handle everything in the update
        }

        @Override
        protected void update(double timeStep)
        {
            Log.d("++tag", "Running with " + getQueueSize());
            if (!mTransactionLock.compareAndSet(false, true))
                return;

            if (mPendingReadQueue.size() < 1)
            {
                mTransactionLock.set(false);
                return;
            }

            Object next = mPendingReadQueue.remove(0);

            // Send a transaction to read next
            if (next instanceof BluetoothGattCharacteristic)
            {
                BluetoothGattCharacteristic bgc = (BluetoothGattCharacteristic) next;
                getDevice().read(bgc.getUuid(), new BleDevice.ReadWriteListener()
                {
                    @Override
                    public void onEvent(ReadWriteEvent e)
                    {
                        mTransactionLock.set(false);

                        if (mListener != null)
                            mListener.onEvent(e);
                    }
                });
            }
            else if (next instanceof BluetoothGattDescriptor)
            {
                BluetoothGattDescriptor bgd = (BluetoothGattDescriptor) next;
                getDevice().readDescriptor(bgd.getUuid(), new BleDevice.ReadWriteListener()
                {
                    @Override
                    public void onEvent(ReadWriteEvent e)
                    {
                        mTransactionLock.set(false);

                        if (mListener != null)
                            mListener.onEvent(e);
                    }
                });
            }
            else
            {
                // Hmm, a bad object was queued?  Unlock our boolean
                mTransactionLock.set(false);
            }
        }

        public int getQueueSize()
        {
            return mPendingReadQueue.size() + (mTransactionLock.get() ? 1 : 0);
        }

        public void finishUp()
        {
            cancel();
        }
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characteristics);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hide logo
        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final String mac = getIntent().getStringExtra("mac");
        final String uuid = getIntent().getStringExtra("uuid");

        m_device = BleManager.get(this).getDevice(mac);
        m_service = m_device.getNativeService(UUID.fromString(uuid));
        m_characteristicList = m_service.getCharacteristics();

        String title = UuidUtil.getServiceName(m_service);
        setTitle(title);

        m_swipeRefreshLayout = find(R.id.swipeRefreshLayout);

        m_adapter = new CharacteristicAdapter(this, m_device, m_service, m_characteristicList);

        m_charListView = find(R.id.expandingListView);

        m_charListView.setAdapter(m_adapter);

        // Disable native indicator, we will use our own
        m_charListView.setGroupIndicator(null);

        boolean haveCharacteristics = !m_characteristicList.isEmpty();

        m_charListView.setVisibility(haveCharacteristics ? View.VISIBLE : View.GONE);

        m_noCharacteristicsTextView = find(R.id.noCharacteristicsTextView);

        m_noCharacteristicsTextView.setVisibility(haveCharacteristics ? View.GONE : View.VISIBLE);

        m_swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                mTransaction.refreshAll(m_characteristicList);
                m_adapter.notifyDataSetChanged();
            }
        });

        mTransaction = new TestTransaction(this, m_characteristicList);
        m_device.performTransaction(mTransaction);
    }

    @Override
    public void onEvent(ReadWriteEvent e)
    {
        // If successful, tell our adapter to refresh
        if (e.wasSuccess())
            m_adapter.notifyDataSetChanged();

        if (mTransaction.getQueueSize() <= 0)
            m_swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void finish()
    {
        mTransaction.finishUp();
        super.finish();
    }

    public void openWriteCharacteristicActivity(UUID serviceUUID, UUID characteristicUUID)
    {
        // Navigate to the write activity
        //final BluetoothGattService service = m_serviceList.get(position);
        Intent intent = new Intent(this, WriteValueActivity.class);
        intent.putExtra("mac", m_device.getMacAddress());
        intent.putExtra("serviceUUID", serviceUUID.toString());
        intent.putExtra("characteristicUUID", characteristicUUID.toString());
        startActivityForResult(intent, WRITE_VALUE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WRITE_VALUE_RESULT_CODE && resultCode == RESULT_OK)
            m_adapter.notifyDataSetChanged();
    }
}
