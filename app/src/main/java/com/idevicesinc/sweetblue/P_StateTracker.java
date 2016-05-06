package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.BleStatuses;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_String;


abstract class P_StateTracker
{

    private int mStateMask = 0x0;
    private final long[] mTimesInState;
    private final int mStateCount;

    P_StateTracker(State[] enums, boolean trackTimes)
    {
        mStateCount = enums.length;
        mTimesInState = trackTimes ? new long[mStateCount] : null;
    }

    P_StateTracker(State[] enums)
    {
        this(enums, true);
    }


    public boolean is(State state)
    {
        return checkBitMatch(state, true);
    }

    public int getState()
    {
        return mStateMask;
    }

    boolean checkBitMatch(State flag, boolean value)
    {
        return ((flag.bit() & mStateMask) != 0) == value;
    }


    abstract void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status);


    void append(State newState, E_Intent intent, int status)
    {
        if( newState./*already*/overlaps(mStateMask) )
        {
            return;
        }

//        append_assert(newState);

        setStateMask(mStateMask | newState.bit(), intent == E_Intent.INTENTIONAL ? newState.bit() : 0x0, status);
    }

    void remove(State state, E_Intent intent, int status)
    {
        setStateMask(mStateMask & ~state.bit(), intent == E_Intent.INTENTIONAL ? state.bit() : 0x0, status);
    }

    void set(final E_Intent intent, final int status, final Object ... statesAndValues)
    {
        set(intent.getMask(), status, statesAndValues);
    }

    long getTimeInState(int stateOrdinal)
    {
        if( mTimesInState == null )  return 0;

        int bit = (0x1 << stateOrdinal);

        if( (bit & mStateMask) != 0x0 )
        {
            return System.currentTimeMillis() - mTimesInState[stateOrdinal];
        }
        else
        {
            return mTimesInState[stateOrdinal];
        }
    }

    void copy(P_StateTracker stateTracker)
    {
        this.setStateMask(stateTracker.getState(), 0x0, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    void update(E_Intent intent, int status, Object ... statesAndValues)
    {
        update(intent.getMask(), status, statesAndValues);
    }

    String toString(State[] enums)
    {
        return Utils_String.toString(mStateMask, enums);
    }


    private void set(final int intentMask, final int status, final Object ... statesAndValues)
    {
        final int newStateBits = getMask(0x0, statesAndValues);

        setStateMask(newStateBits, intentMask, status);
    }

    private void update(int intentMask, int status, Object ... statesAndValues)
    {
        int newStateBits = getMask(mStateMask, statesAndValues);

        setStateMask(newStateBits, intentMask, status);
    }

    private void fireStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
    {
        if( oldStateBits == newStateBits )
        {
            //--- DRK > Should sorta have an assert here but there's a few
            //---		valid cases (like implicit connection fail) where it's valid
            //---		so we'd have to be a little more intelligent about asserting.
//			U_Bt.ASSERT(false);
            return;
        }

        onStateChange(oldStateBits, newStateBits, intentMask, status);
    }

    private void setStateMask(final int newStateBits, int intentMask, final int status)
    {
        int oldStateBits = mStateMask;
        mStateMask = newStateBits;

        //--- DRK > Minor skip optimization...shouldn't actually skip (too much) in practice
        //---		if other parts of the library are handling their state tracking sanely.
        if( oldStateBits != newStateBits )
        {
            for( int i = 0, bit = 0x1; i < mStateCount; i++, bit <<= 0x1 )
            {
                //--- DRK > State exited...
                if( (oldStateBits & bit) != 0x0 && (newStateBits & bit) == 0x0 )
                {
                    if( mTimesInState != null )
                    {
                        mTimesInState[i] = System.currentTimeMillis() - mTimesInState[i];
                    }
                }
                //--- DRK > State entered...
                else if( (oldStateBits & bit) == 0x0 && (newStateBits & bit) != 0x0 )
                {
                    if( mTimesInState != null )
                    {
                        mTimesInState[i] = System.currentTimeMillis();
                    }
                }
                else
                {
                    intentMask &= ~bit;
                }
            }
        }
        else
        {
            intentMask = 0x0;
        }

        fireStateChange(oldStateBits, newStateBits, intentMask, status);
    }

    private int getMask(final int currentStateMask, final Object[] statesAndValues)
    {
        int newStateBits = currentStateMask;

        for( int i = 0; i < statesAndValues.length; i++ )
        {
            Object ithValue = statesAndValues[i];

            if( ithValue instanceof Object[] )
            {
                newStateBits = getMask(newStateBits, (Object[])ithValue);

                continue;
            }

            State state = (State) statesAndValues[i];
            boolean append = true;

            if( statesAndValues[i+1] instanceof Boolean )
            {
                append = (Boolean) statesAndValues[i+1];
                i++;
            }

            if( append )
            {
                //append_assert(state);

                newStateBits |= state.bit();
            }
            else
            {
                newStateBits &= ~state.bit();
            }
        }

        return newStateBits;
    }


    enum E_Intent
    {
        INTENTIONAL, UNINTENTIONAL;

        public int getMask()
        {
            return this == INTENTIONAL ? 0xFFFFFFFF : 0x0;
        }

        public State.ChangeIntent convert()
        {
            switch(this)
            {
                case INTENTIONAL:	  return State.ChangeIntent.INTENTIONAL;
                case UNINTENTIONAL:	  return State.ChangeIntent.UNINTENTIONAL;
            }

            return State.ChangeIntent.NULL;
        }
    }

}
