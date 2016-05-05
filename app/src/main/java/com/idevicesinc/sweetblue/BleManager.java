package com.idevicesinc.sweetblue;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;


public class BleManager
{

    private static BleManager sInstance;

    private Context mContext;
    BleManagerConfig mConfig;
    private SweetBlueHandlerThread mThread;
    private UpdateRunnable mUpdateRunnable;
    P_PostManager mPostManager;
    private P_TaskManager mTaskManager;


    private BleManager(Context context)
    {
        this(context, new BleManagerConfig());
    }

    private BleManager(Context context, BleManagerConfig config)
    {
        mContext = context.getApplicationContext();
        mConfig = config;
        mTaskManager = new P_TaskManager(this);
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

    private void initConfigDependantMembers()
    {

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
        mPostManager.postToUpdateThreadDelayed(mUpdateRunnable, mConfig.updateThreadInterval);
    }

    private void update(long curTimeMs)
    {
        mTaskManager.update(curTimeMs);
    }



    private class UpdateRunnable implements Runnable
    {

        @Override public void run()
        {
            update(System.currentTimeMillis());
            if (mPostManager != null && mConfig != null)
            {
                mPostManager.postToUpdateThreadDelayed(this, mConfig.updateThreadInterval);
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
