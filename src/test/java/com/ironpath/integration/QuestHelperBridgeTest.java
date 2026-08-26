package com.ironpath.integration;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class QuestHelperBridgeTest
{
    @Test
    public void absenceIsAnExplicitSupportedFallback()
    {
        QuestHelperBridge bridge = new QuestHelperBridge();
        assertFalse(bridge.canLaunch());
        assertEquals(IntegrationStatus.PARTIAL, bridge.status());
    }
}
