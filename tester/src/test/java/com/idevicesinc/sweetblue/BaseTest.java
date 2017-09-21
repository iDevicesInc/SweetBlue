package com.idevicesinc.sweetblue;


import org.junit.Assert;

import java.util.concurrent.Semaphore;

public class BaseTest
{

    private Semaphore s;
    private String className;
    private String methodName;
    private Error exception;
    private boolean useSemaphore;


    private void startTest(boolean useSemaphore) throws Exception
    {
        exception = null;
        className = this.getClass().getName();
        methodName = getSoonestTrace().getMethodName();
        this.useSemaphore = useSemaphore;
        if (useSemaphore)
        {
            s = new Semaphore(0);
            s.acquire();

            if (exception != null)
                throw exception;
        }
    }

    /**
     * Call this method if the test is synchronous. This should be the first method called in the test method.
     */
    public void startSynchronousTest() throws Exception
    {
        startTest(false);
    }

    /**
     * Call this method if the test is asynchronous. This should be the last method in the test method, as it acquires a {@link Semaphore}.
     * You then need to call {@link #succeed()} to , well, succeed the test. Otherwise, you should be using any of the assert methods in this class
     * to catch errors (and will throw exceptions if there are -- and properly kill the test if this is the case)
     */
    public void startAsyncTest() throws Exception
    {
        startTest(true);
    }

    public void reacquire() throws Exception
    {
        useSemaphore = true;
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

    public void assertTrue(boolean condition)
    {
        try
        {
            Assert.assertTrue(condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertTrue(String msg, boolean condition)
    {
        try
        {
            Assert.assertTrue(msg, condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertFalse(boolean condition)
    {
        try
        {
            Assert.assertFalse(condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertFalse(String msg, boolean condition)
    {
        try
        {
            Assert.assertFalse(msg, condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertNotNull(Object object)
    {
        try
        {
            Assert.assertNotNull(object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertNotNull(String msg, Object object)
    {
        try
        {
            Assert.assertNotNull(msg, object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertNull(Object object)
    {
        try
        {
            Assert.assertNull(object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertNull(String msg, Object object)
    {
        try
        {
            Assert.assertNull(msg, object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertArrayEquals(byte[] a1, byte[] a2)
    {
        try
        {
            Assert.assertArrayEquals(a1, a2);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    public void assertArrayEquals(String msg, byte[] a1, byte[] a2)
    {
        try
        {
            Assert.assertArrayEquals(msg, a1, a2);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }



    private void handleException(Error e)
    {
        exception = e;
        if (useSemaphore)
            release();
        else
            throw exception;
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
