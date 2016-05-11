package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.idevicesinc.sweetblue.listeners.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.P_StateTracker.E_Intent;

import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.TURNING_ON;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


public class BleManager
{

    private static BleManager sInstance;

    private static final int SPINDOWN_BUFFER_TIME = 5000;

    private Context mContext;
    BleManagerConfig mConfig;
    private SweetBlueHandlerThread mThread;
    private UpdateRunnable mUpdateRunnable;
    P_PostManager mPostManager;
    P_TaskManager mTaskManager;
    BleServer mServer;
    private P_Logger mLogger;
    private P_NativeBleStateTracker mNativeStateTracker;
    private P_BleStateTracker mStateTracker;
    private ManagerStateListener mStateListener;
    private long mLastTaskExecution;
    private long mUpdateInterval;
    private BluetoothManager mNativeManager;
    private P_BleReceiverManager mReceiverManager;
    private P_WakeLockManager mWakeLockManager;


    private BleManager(Context context)
    {
        this(context, new BleManagerConfig());
    }

    private BleManager(Context context, BleManagerConfig config)
    {
        mContext = context.getApplicationContext();
        mConfig = config;
        mNativeManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);

        BleManagerState nativeState;
        if( mNativeManager == null )
        {
            nativeState = BleManagerState.get(BluetoothAdapter.STATE_ON);
        }
        else
        {
            nativeState = BleManagerState.get(mNativeManager.getAdapter().getState());
        }
        mNativeStateTracker = new P_NativeBleStateTracker(this);
        mNativeStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        mStateTracker = new P_BleStateTracker(this);
        mStateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        mTaskManager = new P_TaskManager(this);
        mReceiverManager = new P_BleReceiverManager(this);


        initConfigDependantMembers();
    }

    static BleManager get()
    {
        return sInstance;
    }

    public static BleManager get(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new BleManager(context);
        }
        return sInstance;
    }

    public static BleManager get(Context context, BleManagerConfig config)
    {
        if (sInstance == null)
        {
            sInstance = new BleManager(context, config);
        }
        else
        {
            sInstance.setConfig(config);
        }
        return sInstance;
    }

    public void setManagerStateListener(ManagerStateListener listener)
    {
        mStateTracker.setListener(listener);
    }

    public void turnOn()
    {
        if (isAny(TURNING_ON, ON))
        {
            return;
        }

        if (is(BleManagerState.OFF))
        {
            mStateTracker.update(P_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_ON, true, OFF, false);
        }

        mTaskManager.add(new P_Task_TurnBleOn(mTaskManager));
    }

    public boolean is(BleManagerState state)
    {
        return state.overlaps(getStateMask());
    }

    public boolean isAny(BleManagerState... states)
    {
        for( int i = 0; i < states.length; i++ )
        {
            if( is(states[i]) )  return true;
        }

        return false;
    }

    int getStateMask()
    {
        return mStateTracker.getState();
    }

    int getNativeStateMask()
    {
        return mNativeStateTracker.getState();
    }

    P_NativeBleStateTracker getNativeStateTracker()
    {
        return mNativeStateTracker;
    }

    P_BleStateTracker getStateTracker()
    {
        return mStateTracker;
    }

    public BluetoothManager getNativeManager()
    {
        return mNativeManager;
    }

    public BluetoothAdapter getNativeAdapter()
    {
        return mNativeManager.getAdapter();
    }

    public Context getAppContext()
    {
        return mContext;
    }

    public void setConfig(BleManagerConfig config)
    {
        mConfig = config;
        initConfigDependantMembers();
    }

    public void shutdown()
    {
        mPostManager.removeCallbacks(mUpdateRunnable);
        mReceiverManager.onDestroy();
        if (!mConfig.runOnUIThread && mThread != null)
        {
            mThread.quit();
        }
    }

    boolean isOnSweetBlueThread()
    {
        return mPostManager.isOnSweetBlueThread();
    }

    private void initConfigDependantMembers()
    {

        mConfig.initMaps();

        Handler mUpdateHandler;
        Handler mUIHandler;

        if (mUpdateRunnable != null)
        {
            mPostManager.removeCallbacks(mUpdateRunnable);
        }
        else
        {
            mUpdateRunnable = new UpdateRunnable();
        }

        if( mWakeLockManager == null )
        {
            mWakeLockManager = new P_WakeLockManager(this, mConfig.manageCpuWakeLock);
        }
        else if( mWakeLockManager != null && mConfig.manageCpuWakeLock == false )
        {
            mWakeLockManager.clear();
            mWakeLockManager = new P_WakeLockManager(this, mConfig.manageCpuWakeLock);
        }

        if (mConfig.runOnUIThread)
        {
            mUpdateHandler = new Handler(Looper.getMainLooper());
            mUIHandler = mUpdateHandler;
        }
        else
        {
            mUIHandler = new Handler(Looper.getMainLooper());
            if (mConfig.updateLooper == null)
            {
                mThread = new SweetBlueHandlerThread();
                mThread.start();
                mUpdateHandler = mThread.prepareHandler();
            }
            else
            {
                mUpdateHandler = new Handler(mConfig.updateLooper);
            }
        }
        mPostManager = new P_PostManager(mUIHandler, mUpdateHandler);
        mUpdateInterval = mConfig.updateThreadIntervalMs;
        mPostManager.postToUpdateThreadDelayed(mUpdateRunnable, mUpdateInterval);
        mLogger = new P_Logger(mPostManager, mConfig.debugThreadNames, mConfig.uuidNameMaps, mConfig.logger, mConfig.loggingEnabled);
    }

    P_Logger getLogger()
    {
        return mLogger;
    }

    /*package*/ void update(long curTimeMs)
    {
        if (mTaskManager.update(curTimeMs))
        {
            // The task manager reported that there are tasks to process (or are being processed)
            // So, we record this time, and make sure the update interval is at full speed.
            mLastTaskExecution = curTimeMs;
            mUpdateInterval = mConfig.updateThreadIntervalMs;
        }

        if (mConfig.updateCallback != null)
        {
            mConfig.updateCallback.onUpdate(curTimeMs);
        }

        if (mLastTaskExecution + SPINDOWN_BUFFER_TIME < curTimeMs)
        {
            // If the last task execution happened more than the spindown buffer time ago, then we spin down
            // the update cycle, so we're not chewing up CPU/battery power unnecessarily.
            mUpdateInterval = mConfig.updateThreadIdleIntervalMs;
        }
    }



    private class UpdateRunnable implements Runnable
    {

        @Override public void run()
        {
            update(System.currentTimeMillis());
            if (mPostManager != null && mConfig != null)
            {
                mPostManager.postToUpdateThreadDelayed(this, mUpdateInterval);
            }
        }
    }

    private static class SweetBlueHandlerThread extends HandlerThread
    {

        private Handler mHandler;

        public SweetBlueHandlerThread()
        {
            super("SweetBlue");
        }

        public Handler prepareHandler()
        {
            mHandler = new Handler(getLooper()){
                @Override public void handleMessage(Message msg)
                {
                    super.handleMessage(msg);
                }
            };
            return mHandler;
        }
    }

}
