package org.jivesoftware.smackx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExtensionsProviderInitializerTest {

    @Test
    public void testExtensionProviderInitializer() {
        ExtensionsProviderInitializer ei = new ExtensionsProviderInitializer();
        ei.initialize();
        assertTrue(ei.getExceptions().size() == 0);
    }

}
