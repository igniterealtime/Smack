package org.jivesoftware.smackx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExtensionsStartupClassesTest {

    @Test
    public void testExtensiosnStartupClasses() {
        ExtensionsStartupClasses esc = new ExtensionsStartupClasses();
        esc.initialize();
        assertTrue(esc.getExceptions().size() == 0);
    }

}
