package com.idevicesinc.sweetblue.tests;

import com.idevicesinc.sweetblue.utils.Distance;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class DistanceTest
{

    @Test
    public void smokeTests() {
        Distance d = Distance.meters(4.0);
        assertEquals(4.0, d.meters(), 0);
        assertEquals(4.0 * 3.28084, d.feet(), 0);
        assertEquals(0.0, Distance.ZERO.meters(), 0);
        assertEquals(-1.0, Distance.INVALID.meters(), 0);
    }

    @Test
    public void meterToFeetTest() {
        double meters = 4.0;
        double feet = meters * 3.28084;
        assertEquals(feet, Distance.meters(meters).feet(), 0);
    }

    @Test
    public void feetToMeterTest() {
        double feet = 25;
        double meters = feet / 3.28084;
        assertEquals(meters, Distance.feet(feet).meters(), 0);
    }

}
