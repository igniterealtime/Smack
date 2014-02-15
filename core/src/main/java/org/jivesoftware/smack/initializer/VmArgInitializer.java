package org.jivesoftware.smack.initializer;

import org.jivesoftware.smack.provider.ProviderManager;


/**
 * Looks for a provider file location based on the VM argument <i>smack.provider.file</>.  If it is supplied, its value will 
 * be used as a file location for a providers file and loaded into the {@link ProviderManager} on Smack initialization.
 *  
 * @author Robin Collier
 *
 */
public class VmArgInitializer extends UrlProviderFileInitializer {

    protected String getFilePath() {
        return System.getProperty("smack.provider.file");
    }

    @Override
    public void initialize() {
        if (getFilePath() != null) {
            super.initialize();
        }
    }
}
