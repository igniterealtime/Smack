package org.jivesoftware.smack.initializer;

import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.initializer.CoreInitializer;
import org.junit.Test;

public class CoreInitializerTest {

    @Test
    public void testCoreInitializer() {
        CoreInitializer ci = new CoreInitializer();
        ci.initialize();
        assertTrue(ci.getExceptions().size() == 0);
    }

}
