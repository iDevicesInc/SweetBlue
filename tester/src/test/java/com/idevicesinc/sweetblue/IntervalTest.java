package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class IntervalTest extends BaseTest
{

    @Test
    public void smokeTest() throws Exception
    {
        startTest(false);
        assertEquals(-1.0, Interval.DISABLED.secs(), 0);
        assertEquals(Double.POSITIVE_INFINITY, Interval.INFINITE.secs(), 0);
        assertEquals(0.0, Interval.ZERO.secs(), 0);
        assertEquals(1000, Interval.ONE_SEC.millis(), 0);
        assertEquals(5.0, Interval.FIVE_SECS.secs(), 0);
        assertEquals(10000, Interval.TEN_SECS.millis(), 0);
        succeed();
    }

    @Test
    public void enablingTests() throws Exception
    {
        startTest(false);
        Interval in = Interval.secs(400);
        assert Interval.isEnabled(in);
        in = Interval.secs(-400);
        assert Interval.isDisabled(in);
        succeed();
    }

    @Test
    public void secsToMillisTest() throws Exception
    {
        startTest(false);
        Interval in = Interval.secs(400);
        assertEquals(400 * 1000, in.millis());
        succeed();
    }

    @Test
    public void millisToSecsTest() throws Exception
    {
        startTest(false);
        Interval in = Interval.millis(500000);
        assertEquals(500000 / 1000, in.secs(), 0);
        succeed();
    }

    @Test
    public void deltaTest() throws Exception
    {
        startTest(false);
        Interval in = Interval.delta(1000, 10000);
        assertEquals(10000 - 1000, in.millis());
        assertEquals((10000 - 1000) / 1000, in.secs(), 0);
        succeed();
    }

}
