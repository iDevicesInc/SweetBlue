package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.utils.Percent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class PercentTest extends BaseTest
{

    @Test
    public void smokeTests() throws Exception
    {
        startTest(false);
        assertEquals(0, Percent.ZERO.toDouble(), 0);
        assertEquals(100, Percent.HUNDRED.toDouble(), 0);
        succeed();
    }

    @Test
    public void percentToFractionTest() throws Exception
    {
        startTest(false);
        Percent p = Percent.fromDouble(79.5);
        assertEquals(.795, p.toFraction(), 0);
        succeed();
    }

    @Test
    public void clampTest() throws Exception
    {
        startTest(false);
        Percent p = Percent.fromDouble_clamped(125);
        assertEquals(100, p.toDouble(), 0);
        p = Percent.fromDouble(125).clamp();
        assertEquals(100, p.toDouble(), 0);
        p = Percent.fromDouble_clamped(-25);
        assertEquals(0, p.toDouble(), 0);
        p = Percent.fromDouble(-25).clamp();
        assertEquals(0, p.toDouble(), 0);
        succeed();
    }

    @Test
    public void ceilingFloorTest() throws Exception
    {
        startTest(false);
        Percent p = Percent.fromDouble(71.87);
        assertEquals(71, p.toInt_floor(), 0);
        assertEquals(72, p.toInt_ceil(), 0);
        succeed();
    }
}
