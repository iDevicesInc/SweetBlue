package com.idevicesinc.sweetblue.tests;

import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class IntervalTest
{

    @Test
    public void smokeTest() {
        assertEquals(-1.0, Interval.DISABLED.secs(), 0);
        assertEquals(Double.POSITIVE_INFINITY, Interval.INFINITE.secs(), 0);
        assertEquals(0.0, Interval.ZERO.secs(), 0);
        assertEquals(1000, Interval.ONE_SEC.millis(), 0);
        assertEquals(5.0, Interval.FIVE_SECS.secs(), 0);
        assertEquals(10000, Interval.TEN_SECS.millis(), 0);
    }

    @Test
    public void enablingTests() {
        Interval in = Interval.secs(400);
        assert Interval.isEnabled(in);
        in = Interval.secs(-400);
        assert Interval.isDisabled(in);
    }

    @Test
    public void secsToMillisTest() {
        Interval in = Interval.secs(400);
        assertEquals(400 * 1000, in.millis());
    }

    @Test
    public void millisToSecsTest() {
        Interval in = Interval.millis(500000);
        assertEquals(500000 / 1000, in.secs(), 0);
    }

    @Test
    public void deltaTest() {
        Interval in = Interval.delta(1000, 10000);
        assertEquals(10000 - 1000, in.millis());
        assertEquals((10000 - 1000) / 1000, in.secs(), 0);
    }

}
