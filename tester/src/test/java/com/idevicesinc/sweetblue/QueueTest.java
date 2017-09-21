package com.idevicesinc.sweetblue;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;

@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class QueueTest extends BaseBleUnitTest
{
    private Integer mRemainingTasks = null;
    private long mStartTimestamp;

    private void onExecute(TestTask tt)
    {

        if (mRemainingTasks == null)
        {
            // Fail
        }
        else
        {
            mRemainingTasks--;

            System.out.println("Executing task of type " + tt.getClass().getSimpleName() + " with priority " + tt.mPriority + " and metadata " + tt.getMetadata() + ".  There are " + mRemainingTasks + " remaining tasks");

            if (mRemainingTasks == 0)
            {
                long dt = System.currentTimeMillis() - mStartTimestamp;
                System.out.println("All tasks complete after " + dt + "ms");
                succeed();
            }
        }
    }

    public class TestTask extends PA_Task
    {
        private Object mMetadata = null;
        private PE_TaskPriority mPriority = PE_TaskPriority.MEDIUM;

        public Object getMetadata()
        {
            return mMetadata;
        }

        public TestTask(BleServer server, Object metadata)
        {
            super(server, null);
            mMetadata = metadata;
        }

        public TestTask(BleDevice device, Object metadata)
        {
            super(device, null);
            mMetadata = metadata;
        }

        public TestTask(BleManager manager, Object metadata)
        {
            super(manager, null);
            mMetadata = metadata;
        }

        @Override
        protected BleTask getTaskType()
        {
            return BleTask.READ;
        }

        void setPriority(PE_TaskPriority priority)
        {
            mPriority = priority;
        }

        @Override
        void execute()
        {
            onExecute(this);
            succeed();
        }

        @Override
        public PE_TaskPriority getPriority()
        {
            return mPriority;
        }
    }

    public class TestTaskA extends TestTask
    {

        public TestTaskA(BleServer server, Object metadata)
        {
            super(server, metadata);
        }

        public TestTaskA(BleDevice device, Object metadata)
        {
            super(device, metadata);
        }

        public TestTaskA(BleManager manager, Object metadata)
        {
            super(manager, metadata);
        }
    }

    public class TestTaskB extends TestTask
    {

        public TestTaskB(BleServer server, Object metadata)
        {
            super(server, metadata);
        }

        public TestTaskB(BleDevice device, Object metadata)
        {
            super(device, metadata);
        }

        public TestTaskB(BleManager manager, Object metadata)
        {
            super(manager, metadata);
        }
    }

    private int populateQueue(int numTasks, double distributionRatio)
    {
        Random r = new Random();

        P_TaskManager tm = m_mgr.getTaskManager();

        int countA = 0;
        int countB = 0;

        for (int i = 0; i < numTasks; ++i)
        {
            TestTask tt = null;

            if (r.nextDouble() < .5)
            {
                tt = new TestTaskA(m_mgr, i);
                ++countA;
            }
            else
            {
                tt = new TestTaskB(m_mgr, i);
                ++countB;
            }

            // Assign a random priority to force the inserts to walk the list
            tt.setPriority(PE_TaskPriority.values()[r.nextInt(PE_TaskPriority.values().length)]);

            tm.add(tt);
        }

        return countA;
    }


    @Test
    public void queueTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_mgr.setConfig(m_config);

        P_TaskManager tm = m_mgr.getTaskManager();

        // Suspend the queue so we can cram it full of stuff w/o it doing anything just yet
        tm.setSuspended(true);

        final int kTaskCount = 10000;

        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Starting queue operations at " + mStartTimestamp);

        int countA = populateQueue(kTaskCount, .5);
        int countB = kTaskCount - countA;

        long dt = System.currentTimeMillis() - mStartTimestamp;
        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Queue adds complete after " + dt + " ms.  Queue size is now " + tm.getSize() + " with " + countA + " task A and " + countB + " task B.  Starting queue clear at " + mStartTimestamp);

        tm.clearQueueOf(TestTaskA.class, m_mgr);

        dt = System.currentTimeMillis() - mStartTimestamp;
        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Queue operations complete after " + dt + " ms.  Queue size is now " + tm.getSize() + ".  Starting task processing at " + mStartTimestamp);

        mRemainingTasks = tm.getSize();

        tm.setSuspended(false);

        startAsyncTest();
    }
}
