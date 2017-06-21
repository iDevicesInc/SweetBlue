package com.idevicesinc.sweetblue;


import java.util.concurrent.Semaphore;

public class BaseTest
{

    private Semaphore s;
    private String className;
    private String methodName;


    public void startTest(boolean useSemaphore) throws Exception
    {
        className = this.getClass().getName();
        methodName = getSoonestTrace().getMethodName();
        if (useSemaphore)
        {
            s = new Semaphore(0);
            s.acquire();
        }
    }

    public void startTest() throws Exception
    {
        startTest(true);
    }

    public void reacquire() throws Exception
    {
        s = new Semaphore(0);
        s.acquire();
    }

    public void release()
    {
        if (s != null)
        {
            s.release();
        }
    }

    public void succeed()
    {
        System.out.println(methodName + " completed successfully.");
        release();
    }

    private StackTraceElement getSoonestTrace()
    {
        StackTraceElement[] trace = new Exception().getStackTrace();
        return getSoonestTrace(trace);
    }

    private StackTraceElement getSoonestTrace(StackTraceElement[] trace)
    {
        for (int i = 0; i < trace.length; i++)
        {
            if (trace[i].getClassName().equals(className))
            {
                return trace[i];
            }
        }
        return trace[getTraceIndex()];
    }

    int getTraceIndex()
    {
        return 2;
    }


}
