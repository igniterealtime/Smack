package org.jivesoftware.smackx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorkgroupProviderInitializerTest {

    @Test
    public void testWorkgroupProviderInitializer() {
        WorkgroupProviderInitializer wpi = new WorkgroupProviderInitializer();
        wpi.initialize();
        assertTrue(wpi.getExceptions().size() == 0);
    }
}
