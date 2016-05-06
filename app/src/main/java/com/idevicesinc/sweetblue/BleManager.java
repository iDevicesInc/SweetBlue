package com.idevicesinc.sweetblue;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;


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
    private P_Logger mLogger;
    private P_NativeBleStateTracker mNativeStateTracker;
    private long mLastTaskExecution;
    private long mUpdateInterval;


    private BleManager(Context context)
    {
        this(context, new BleManagerConfig());
    }

    private BleManager(Context context, BleManagerConfig config)
    {
        mContext = context.getApplicationContext();
        mConfig = config;
        mTaskManager = new P_TaskManager(this);
        mNativeStateTracker = new P_NativeBleStateTracker(this);
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



    public void setConfig(BleManagerConfig config)
    {
        mConfig = config;
        initConfigDependantMembers();
    }

    public void shutdown()
    {
        mPostManager.removeCallbacks(mUpdateRunnable);
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

        if (mConfig.runOnUIThread)
        {
            mUpdateHandler = new Handler(Looper.getMainLooper());
            mUIHandler = mUpdateHandler;
        }
        else
        {
            mUIHandler = new Handler(Looper.getMainLooper());
            mThread = new SweetBlueHandlerThread();
            mThread.start();
            mUpdateHandler = mThread.prepareHandler();
        }
        mPostManager = new P_PostManager(mUIHandler, mUpdateHandler);
        mUpdateInterval = mConfig.updateThreadIntervalMs;
        mPostManager.postToUpdateThreadDelayed(mUpdateRunnable, mUpdateInterval);
        mLogger = new P_Logger(mConfig.debugThreadNames, mConfig.uuidNameMaps, mConfig.logger, mConfig.loggingEnabled);
    }

    P_Logger getLogger()
    {
        return mLogger;
    }

    private void update(long curTimeMs)
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
            mHandler = new Handler(getLooper());
            return mHandler;
        }
    }

}
