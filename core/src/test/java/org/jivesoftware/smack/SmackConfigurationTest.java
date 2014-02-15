package org.jivesoftware.smack;

import static org.junit.Assert.fail;

import org.junit.Test;

public class SmackConfigurationTest {

    @Test
    public void testSmackConfiguration() {
        try {
            SmackConfiguration.getPacketReplyTimeout();
        } catch (Throwable t) {
            fail("SmackConfiguration threw Throwable");
        }
    }
}
