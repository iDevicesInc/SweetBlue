package com.idevicesinc.sweetblue;


import android.os.Handler;

import com.idevicesinc.sweetblue.annotations.Lambda;
import com.idevicesinc.sweetblue.utils.UpdateLoop;

public interface PI_UpdateLoop
{
    /**
     * A callback where you handle the update time step.
     */
    @Lambda
    public static interface Callback
    {
        /**
         * Gives you the amount of time that has passed in seconds since the last callback.
         */
        void onUpdate(double timestep_seconds);
    }

    boolean isRunning();
    void start(double updateRate);
    void stop();
    void forcePost(Runnable runnable);
    Handler getHandler();
    boolean postNeeded();
    void postIfNeeded(Runnable runnable);


    public interface IUpdateLoopFactory {
        PI_UpdateLoop newAnonThreadLoop();
        PI_UpdateLoop newMainThreadLoop(Callback callback);
        PI_UpdateLoop newAnonThreadLoop(Callback callback);
    }

    public static class DefaultUpdateLoopFactory implements IUpdateLoopFactory {

        @Override public PI_UpdateLoop newAnonThreadLoop()
        {
            return UpdateLoop.newAnonThreadLoop();
        }

        @Override public PI_UpdateLoop newMainThreadLoop(Callback callback)
        {
            return UpdateLoop.newMainThreadLoop(callback);
        }

        @Override public PI_UpdateLoop newAnonThreadLoop(Callback callback)
        {
            return UpdateLoop.newAnonThreadLoop(callback);
        }
    }


}
