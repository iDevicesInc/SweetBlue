package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import java.util.UUID;


/**
 * Callback used to test out an MTU size adjustment. Some phones (OnePlus 2, Moto Pure X are 2 we know of for sure) will report a success on getting
 * a larger MTU, but fail when trying to write with the requested MTU size. This callback exists to test the MTU before saying things are fine. Once the
 * MTU request comes back as a success, this callback will be invoked to see if a test should be performed. The {@link #onResult(TestResult)} method will
 * be called if it either fails, or succeeds. If the write fails, the device will be disconnected, as once a bluetooth operation times out in this manner, nothing
 * else will work until the device gets re-connected.
 */
public interface MtuTestCallback
{

    /**
     * This gets called after {@link BleDevice#setMtu(int, BleDevice.ReadWriteListener)} or any of it's overloads is called, and it returns a success. Return a
     * {@link Please} here to tell SweetBlue if it should test the new MTU setting or not.
     */
    @Nullable(Nullable.Prevalence.NEVER) Please onTestRequest(MtuTestEvent event);

    /**
     * This gets called if the MTU test succeeds, or fails. Note that if the test fails, the device will automatically be disconnected, as nothing else will work
     * at this point, until re-connected.
     */
    void onResult(@Nullable(Nullable.Prevalence.NEVER) TestResult result);


    /**
     * Class passed in to {@link #onTestRequest(MtuTestEvent)} when an MTU has been successfully negotiated to see if SweetBlue should now test it to make
     * sure it works as it's supposed to.
     */
    class MtuTestEvent extends Event
    {

        private final int m_negotiatedMtuSize;
        private final BleDevice m_device;


        MtuTestEvent(BleDevice device, int mtuSize)
        {
            m_negotiatedMtuSize = mtuSize;
            m_device = device;
        }

        /**
         * The {@link BleDevice} which just got it's MTU size negotiated with
         */
        public BleDevice device()
        {
            return m_device;
        }

        /**
         * The MTU size that was negotiated with the peripheral. NOTE: This may not be the same value as what you passed in to the
         * {@link BleDevice#setMtu(int)} method.
         */
        public int getNegotiatedMtuSize()
        {
            return m_negotiatedMtuSize;
        }
    }

    /**
     * Enumeration reporting the result of an MTU test.
     */
    class TestResult
    {

        private final BleDevice m_device;
        private final Result m_result;
        private final BleDevice.ReadWriteListener.Status m_writeStatus;


        TestResult(BleDevice device, Result result, BleDevice.ReadWriteListener.Status writeStatus)
        {
            m_device = device;
            m_result = result;
            m_writeStatus = writeStatus;
        }

        /**
         * The {@link BleDevice} the test was run on.
         */
        public BleDevice device()
        {
            return m_device;
        }

        /**
         * The result of the MTU test -- you can also just call {@link #wasSuccess()} if you only care if it worked or not.
         */
        public Result result()
        {
            return m_result;
        }

        /**
         * If {@link #wasSuccess()} returns <code>false</code>, and {@link #result()} is {@link Result#OTHER_FAILURE}, this value will hold
         * the {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status} from the write which was performed as the test.
         */
        public BleDevice.ReadWriteListener.Status write_status()
        {
            return m_writeStatus;
        }

        /**
         * Convenience method to see if the MTU test worked or not.
         */
        public boolean wasSuccess()
        {
            return m_result.success();
        }

        enum Result
        {
            /**
             * The MTU test succeeded, so this android device should be good to go with the newly negotiated MTU size.
             */
            SUCCESS,

            /**
             * This means that {@link Please#doNothing()} was returned in the method {@link MtuTestCallback#onTestRequest(MtuTestEvent)}.
             */
            NO_OP,

            /**
             * The write timed out when testing the new MTU size. The device will get disconnected, as nothing will work on this device until it's reconnected.
             * This isn't 100% accurate, as the write could have just legitimately just timed out, so it's best not to persist this (the check will run again
             * on the next connection).
             */
            WRITE_TIMED_OUT,

            /**
             * The write to test the MTU size failed for a reason other than a time out.
             */
            OTHER_FAILURE;

            public boolean success()
            {
                return this == SUCCESS;
            }
        }
    }

    /**
     * Please class used to tell SweetBlue if it should test the new MTU size, and if so, which service uuid, and char uuid to use, and the data to write (the data
     * should be the size of the MTU size requested to properly test).
     */
    class Please
    {

        private final UUID m_serviceUuid;
        private final UUID m_charUuid;
        private final byte[] m_data;
        private BleDevice.ReadWriteListener.Type m_writeType = BleDevice.ReadWriteListener.Type.WRITE;


        private Please(@Nullable(Nullable.Prevalence.NORMAL) UUID serviceUuid, @Nullable(Nullable.Prevalence.NEVER) UUID charUuid, @Nullable(Nullable.Prevalence.NEVER) byte[] data)
        {
            m_serviceUuid = serviceUuid;
            m_charUuid = charUuid;
            m_data = data;
        }


        boolean doTest()
        {
            // If the char uuid, and data are not null, then that means we have to test (service uuid can be null, as we can get the char without it, assuming
            // there's only one char with that uuid.
            return m_charUuid != null && m_data != null;
        }

        UUID serviceUuid()
        {
            return m_serviceUuid;
        }

        UUID charUuid()
        {
            return m_charUuid;
        }

        byte[] data()
        {
            return m_data;
        }

        BleDevice.ReadWriteListener.Type writeType()
        {
            return m_writeType;
        }

        /**
         * Don't perform any test for the new MTU size.
         */
        public static Please doNothing()
        {
            return new Please(null, null, null);
        }

        /**
         * Tell SweetBlue what characteristic it should write to, and provide the data to write. The byte array size should ideally be the size of the newly
         * negotiated MTU size.
         */
        public static Please doWriteTest(@Nullable(Nullable.Prevalence.NORMAL) UUID serviceUuid, @Nullable(Nullable.Prevalence.NEVER) UUID charUuid, @Nullable(Nullable.Prevalence.NEVER) byte[] data)
        {
            return new Please(serviceUuid, charUuid, data);
        }

        /**
         * Tell SweetBlue what characteristic it should write to, and provide the data to write. The byte array size should ideally be the size of the newly
         * negotiated MTU size. This also allows you to set the write type -- one of {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#WRITE} (this is the default),
         * {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#WRITE_NO_RESPONSE}, or {@link com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type#WRITE_SIGNED}.
         */
        public static Please doWriteTest(@Nullable(Nullable.Prevalence.NORMAL) UUID serviceUuid, @Nullable(Nullable.Prevalence.NEVER) UUID charUuid, @Nullable(Nullable.Prevalence.NEVER) byte[] data, BleDevice.ReadWriteListener.Type writeType)
        {
            Please please = new Please(serviceUuid, charUuid, data);
            please.m_writeType = writeType;
            return please;
        }

    }

}
