package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.OtaWrite;
import java.util.ArrayList;
import java.util.UUID;


public class BleWriteTransaction extends BleTransaction.Ota
{

    /**
     * Class used to dictate what should happen if a write fail happens. This class is only used
     * if a {@link FailListener} has been set. Otherwise, {@link #fail()} is called on a write fail.
     * Use static methods to specify what to do on fail; {@link #next()}, {@link #retry()}, or
     * {@link #stop()}.
     */
    public static class Please
    {

        private enum Action
        {
            NEXT,
            RETRY,
            STOP
        }

        private Action action;

        private Please(Action action)
        {
            this.action = action;
        }

        /**
         * Tells the transaction to proceed to the next write in the queue, and ignore the one that failed.
         */
        public static Please next()
        {
            return new Please(Action.NEXT);
        }

        /**
         * Tells the transaction to retry the failed write again.
         */
        public static Please retry()
        {
            return new Please(Action.RETRY);
        }

        /**
         * Stops the transaction, and clears all remaining writes from the queue.
         */
        public static Please stop()
        {
            return new Please(Action.STOP);
        }

    }

    /**
     * Interface for handling failures when writing. This also specifies how the transaction should
     * proceed on error.
     *
     * @see com.idevicesinc.sweetblue.BleWriteTransaction.Please
     */
    public interface FailListener
    {
        Please onWriteFail(BleDevice.ReadWriteListener.ReadWriteEvent e);
    }


    private final ArrayList<OtaWrite> writeQueue = new ArrayList<>();
    private FailListener mfailListener;

    private BleDevice.ReadWriteListener mListener = new BleDevice.ReadWriteListener()
    {
        @Override public void onEvent(ReadWriteEvent e)
        {
            if (e.wasSuccess())
            {
                writeQueue.remove(0);
                if (hasMore())
                {
                    performNextWrite();
                }
                else
                {
                    succeed();
                }
            }
            else
            {
                if (mfailListener != null)
                {
                    Please please = mfailListener.onWriteFail(e);
                    switch (please.action)
                    {
                        case NEXT:
                            writeQueue.remove(0);
                            if (hasMore())
                            {
                                performNextWrite();
                            }
                            else
                            {
                                succeed();
                            }
                            break;
                        case RETRY:
                            performNextWrite();
                            break;
                        case STOP:
                            writeQueue.clear();
                            succeed();
                    }
                }
                else
                {
                    fail();
                }
            }
        }
    };

    /**
     * Use this constructor if you don't plan on setting a {@link FailListener} to listen for
     * write fails in the queue. The transaction will abort if any writes fail.
     */
    public BleWriteTransaction() {
        this(null);
    }

    /**
     * Instantiate a new BleWriteTransaction with a {@link FailListener} to tell the transaction how
     * to proceed on write fails.
     */
    public BleWriteTransaction(FailListener failListener) {
        super();
        mfailListener = failListener;
    }


    /**
     * Starts the transaction. If you override this method, be sure to call super.start(), so the writes
     * are actually performed.
     */
    @Override protected void start(BleDevice device)
    {
        if (hasMore())
        {
            performNextWrite();
        }
        else
        {
            fail();
        }
    }

    /**
     * Set the {@link FailListener} for this transaction.
     */
    public void setWriteFailListener(FailListener failListener) {
        mfailListener = failListener;
    }

    /**
     * Add an {@link OtaWrite} to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(OtaWrite write)
    {
        writeQueue.add(write);
        return this;
    }

    /**
     * Add a write to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(UUID charUuid, byte[] data)
    {
        writeQueue.add(new OtaWrite(charUuid, data));
        return this;
    }

    /**
     * Add a write to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(UUID serviceUuid, UUID charUuid, byte[] data)
    {
        writeQueue.add(new OtaWrite(serviceUuid, charUuid, data));
        return this;
    }

    /**
     * Returns the size of the write queue.
     */
    public int size() {
        return writeQueue.size();
    }

    private boolean hasMore()
    {
        return writeQueue.size() > 0;
    }

    private void performNextWrite()
    {
        final OtaWrite mCurWrite = writeQueue.get(0);
        if (mCurWrite.hasServiceUuid())
        {
            getDevice().write(mCurWrite.getServiceUuid(), mCurWrite.getCharUuid(), mCurWrite.getData(), mListener);
        }
        else
        {
            getDevice().write(mCurWrite.getCharUuid(), mCurWrite.getData(), mListener);
        }
    }

}
