package com.ironcompass;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IronCompassLifecycleTest
{
    @Test
    public void firstCaptureIsDueWithoutOverflow()
    {
        assertTrue(IronCompassPlugin.isCaptureDue(1, Integer.MIN_VALUE));
    }

    @Test
    public void subsequentCapturesRespectTwoTickInterval()
    {
        assertFalse(IronCompassPlugin.isCaptureDue(10, 9));
        assertTrue(IronCompassPlugin.isCaptureDue(10, 8));
    }
}
