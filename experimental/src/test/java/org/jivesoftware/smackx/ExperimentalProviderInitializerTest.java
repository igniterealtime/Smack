package org.jivesoftware.smackx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExperimentalProviderInitializerTest {

    @Test
    public void testExperimentalProviderInitialzer() {
        ExperimentalProviderInitializer epi = new ExperimentalProviderInitializer();
        epi.initialize();
        assertTrue(epi.getExceptions().size() == 0);
    }
}
