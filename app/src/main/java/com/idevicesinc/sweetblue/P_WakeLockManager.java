package com.idevicesinc.sweetblue;


import android.Manifest;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import com.idevicesinc.sweetblue.utils.Utils;


public class P_WakeLockManager
{

    private static final String WAKE_LOCK_TAG = "SWEETBLUE_WAKE_LOCK";

    private int mCount;
    private final PowerManager.WakeLock mWakeLock;
    private final BleManager mManager;


    public P_WakeLockManager(BleManager mgr, boolean enabled)
    {
        mManager = mgr;

        if( enabled )
        {
            if( !Utils.hasPermission(mManager.getAppContext(), Manifest.permission.WAKE_LOCK) )
            {
                Log.e(P_WakeLockManager.class.getSimpleName(), "PERMISSION REQUIRED: " + Manifest.permission.WAKE_LOCK + ". Or set BleManagerConfig#manageCpuWakeLock to false to disable wake lock management.");

                mWakeLock = null;

                return;
            }

            final PowerManager powerMngr = (PowerManager) mManager.getAppContext().getSystemService(Context.POWER_SERVICE);

            mWakeLock = powerMngr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        }
        else
        {
            mWakeLock = null;
        }
    }

    public void clear()
    {
        if( mCount >= 1 )
        {
            releaseLock();
        }

        mCount = 0;
    }

    public void push()
    {
        mCount++;

        if( mCount == 1 )
        {
            if( mWakeLock != null )
            {
                mWakeLock.acquire();
            }
        }
    }

    private void releaseLock()
    {
        if( mWakeLock == null )  return;

        try
        {
            mWakeLock.release();
        }

        //--- DRK > Just looking at the source for release(), it can throw a RuntimeException if it's somehow
        //---		overreleased, like maybe app mismanages it. Just being defensive here.
        catch(RuntimeException e)
        {
            mManager.getLogger().e(e.getMessage());
        }
    }

    public void pop()
    {
        mCount--;

        if( mCount == 0 )
        {
            releaseLock();
        }
        else if( mCount < 0 )
        {
            mCount = 0;
        }
    }

}
