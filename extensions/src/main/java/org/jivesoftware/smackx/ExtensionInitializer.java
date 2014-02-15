package org.jivesoftware.smackx;

import org.jivesoftware.smack.provider.UrlProviderFileInitializer;

/**
 * Loads the default provider file for the Smack extensions on initialization.
 * 
 * @author Robin Collier
 *
 */
public class ExtensionInitializer extends UrlProviderFileInitializer {
    @Override
    protected String getFilePath() {
        return "classpath:META-INF/extension.providers";
    }
}
