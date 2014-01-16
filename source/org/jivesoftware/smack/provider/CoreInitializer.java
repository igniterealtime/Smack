package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.SmackInitializer;

/**
 * Loads the default provider file for the Smack core on initialization.
 * 
 * @author Robin Collier
 *
 */
public class CoreInitializer extends UrlProviderFileInitializer implements SmackInitializer {
    protected String getFilePath() {
        return "classpath:META-INF/core.providers";
    }
}
