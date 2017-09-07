package com.idevicesinc.sweetblue;


import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;


public final class BleWriteTransaction extends BleTransaction.Ota
{

    /**
     * Interface for handling failures when writing. This also specifies how the transaction should
     * proceed on error.
     *
     * @see com.idevicesinc.sweetblue.BleWriteTransaction.FailListener.Please
     */
    public interface FailListener
    {

        /**
         * Class used to dictate what should happen if a write fail happens.
         * Use static methods to specify what to do on a write fail; {@link #next()}, {@link #retry()}, or
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

        public Please onWriteFail(BleDevice.ReadWriteListener.ReadWriteEvent e);
    }

    /**
     * Interface for listening between each write made in the queue. This allows you to perform
     * operations between each write. You then supply {@link Please#proceed()} to continue the transaction,
     * or {@link Please#cancel()} to cancel the transaction.
     */
    public interface WriteQueueListener
    {

        /**
         * Class used to tell this {@link BleWriteTransaction} to either proceed to the next write,
         * or cancel this transaction.
         */
        public static class Please
        {

            private final boolean proceed;

            private Please(boolean proceed)
            {
                this.proceed = proceed;
            }

            /**
             * Proceed to the next write in the queue.
             */
            public static Please proceed()
            {
                return new Please(true);
            }

            /**
             * Cancels this {@link BleWriteTransaction}.
             */
            public static Please cancel()
            {
                return new Please(false);
            }

        }

        public Please onWriteComplete(BleDevice.ReadWriteListener.ReadWriteEvent e);

    }

    private final BleDevice.ReadWriteListener mListener = new BleDevice.ReadWriteListener()
    {
        @Override public void onEvent(ReadWriteEvent e)
        {
            if (e.wasSuccess())
            {
                writeQueue.remove(0);
                if (hasMore())
                {
                    if (mWriteListener != null)
                    {
                        if (mWriteListener.onWriteComplete(e).proceed)
                        {
                            performNextWrite();
                        }
                        else
                        {
                            fail();
                        }
                    }
                    else
                    {
                        performNextWrite();
                    }
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
                    FailListener.Please please = mfailListener.onWriteFail(e);
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

    private final ArrayList<BleDevice.WriteBuilder> writeQueue = new ArrayList<BleDevice.WriteBuilder>();
    private final FailListener mfailListener;
    private final WriteQueueListener mWriteListener;


    /**
     * Use this constructor if you don't plan on setting a {@link FailListener} to listen for
     * write fails in the queue. The transaction will abort if any writes fail.
     */
    public BleWriteTransaction()
    {
        this(null, null);
    }

    /**
     * Overload of {@link #BleWriteTransaction(WriteQueueListener, FailListener)}.
     * Instantiate a new BleWriteTransaction with a {@link FailListener} to tell the transaction how
     * to proceed on write fails.
     */
    public BleWriteTransaction(FailListener failListener)
    {
        this(null, failListener);
    }

    /**
     * Instantiate a new {@link BleWriteTransaction} with a {@link com.idevicesinc.sweetblue.BleWriteTransaction.WriteQueueListener}, which
     * allows you to perform operations between each write in the queue, and a {@link com.idevicesinc.sweetblue.BleWriteTransaction.FailListener} to
     * tell the Transaction what to do on a write failure.
     */
    public BleWriteTransaction(WriteQueueListener writeListener, FailListener failListener)
    {
        super();
        mWriteListener = writeListener;
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
     * Add an {@link BleDevice.WriteBuilder} to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(BleDevice.WriteBuilder write)
    {
        writeQueue.add(write);
        return this;
    }

    /**
     * Add a Collection of writes to the write queue.
     */
    public BleWriteTransaction addAll(Collection<BleDevice.WriteBuilder> writes)
    {
        writeQueue.addAll(writes);
        return this;
    }

    /**
     * Add a write to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(UUID charUuid, byte[] data)
    {
        writeQueue.add(new BleDevice.WriteBuilder(charUuid).setBytes(data));
        return this;
    }

    /**
     * Add a write to the write queue. You can chain this method, to make it easier to add multiple
     * writes.
     */
    public BleWriteTransaction add(UUID serviceUuid, UUID charUuid, byte[] data)
    {
        writeQueue.add(new BleDevice.WriteBuilder(serviceUuid, charUuid).setBytes(data));
        return this;
    }

    /**
     * Returns how many write operations are left in the queue.
     */
    public int remaining()
    {
        return writeQueue.size();
    }

    private boolean hasMore()
    {
        return remaining() > 0;
    }

    private void performNextWrite()
    {
        final BleDevice.WriteBuilder mCurWrite = writeQueue.get(0);
        getDevice().write(mCurWrite, mListener);
    }

}
