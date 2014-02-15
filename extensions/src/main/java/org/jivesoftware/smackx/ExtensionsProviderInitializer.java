package org.jivesoftware.smackx;

import org.jivesoftware.smack.initializer.UrlProviderFileInitializer;

/**
 * Loads the default provider file for the Smack extensions on initialization.
 * 
 * @author Robin Collier
 *
 */
public class ExtensionsProviderInitializer extends UrlProviderFileInitializer {
    @Override
    protected String getFilePath() {
        return "classpath:org.jivesoftware.smackx/extensions.providers";
    }
}