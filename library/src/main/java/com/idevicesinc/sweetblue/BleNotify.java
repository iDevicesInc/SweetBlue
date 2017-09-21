package com.idevicesinc.sweetblue;



import com.idevicesinc.sweetblue.utils.Interval;
import java.util.UUID;


public class BleNotify extends BleOp<BleNotify>
{

    Interval m_forceReadTimeout = Interval.INFINITE;


    public BleNotify()
    {
    }

    public BleNotify(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleNotify(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleNotify} from the one given. Note that this only copies over the service, and characteristic UUIDs, and the
     * forceReadTimeout value. No interfaces or filters are copied over.
     */
    public BleNotify(BleNotify notify)
    {
        super(notify.serviceUuid, notify.charUuid);
        m_forceReadTimeout = Interval.secs(notify.m_forceReadTimeout.secs());
    }


    @Override
    public final boolean isValid()
    {
        return charUuid != null;
    }

    @Override
    final BleNotify createDuplicate()
    {
        BleNotify notify = getDuplicateOp();
        notify.m_forceReadTimeout = m_forceReadTimeout;
        return notify;
    }

    @Override
    final BleNotify createNewOp()
    {
        return new BleNotify();
    }

    public final BleNotify setForceReadTimeout(Interval timeout)
    {
        m_forceReadTimeout = timeout;
        return this;
    }


    /**
     * Builder class to build out a list (or array) of {@link BleNotify} instances.
     */
    public final static class Builder extends BleOp.Builder<Builder, BleNotify>
    {

        public Builder()
        {
            this(null, null);
        }

        public Builder(UUID characteristicUuid)
        {
            this(null, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid)
        {
            currentOp = new BleNotify(serviceUuid, characteristicUuid);
        }


        public final Builder setForceReadTimeout(Interval timeout)
        {
            currentOp.setForceReadTimeout(timeout);
            return this;
        }

    }
}
