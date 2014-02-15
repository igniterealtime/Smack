package org.jivesoftware.smack.initializer;


/**
 * Loads the default provider file for the Smack core on initialization.
 * 
 * @author Robin Collier
 *
 */
public class CoreInitializer extends UrlProviderFileInitializer implements SmackInitializer {
    protected String getFilePath() {
        return "classpath:org.jivesoftware.smack/core.providers";
    }
}
