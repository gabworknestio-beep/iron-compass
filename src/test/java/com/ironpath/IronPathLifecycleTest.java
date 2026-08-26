package com.ironpath;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IronPathLifecycleTest
{
    @Test
    public void firstCaptureIsDueWithoutOverflow()
    {
        assertTrue(IronPathPlugin.isCaptureDue(1, Integer.MIN_VALUE));
    }

    @Test
    public void subsequentCapturesRespectTwoTickInterval()
    {
        assertFalse(IronPathPlugin.isCaptureDue(10, 9));
        assertTrue(IronPathPlugin.isCaptureDue(10, 8));
    }
}
