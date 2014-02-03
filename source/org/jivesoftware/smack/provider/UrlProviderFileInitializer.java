package org.jivesoftware.smack.provider;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackInitializer;
import org.jivesoftware.smack.util.FileUtils;

/**
 * Loads the provider file defined by the URL returned by {@link #getFilePath()}.  This file will be loaded on Smack initialization.
 * 
 * @author Robin Collier
 *
 */
public abstract class UrlProviderFileInitializer implements SmackInitializer {
    private static final Logger log = Logger.getLogger(UrlProviderFileInitializer.class.getName());

    @Override
    public void initialize() {
        String filePath = getFilePath();
        
        try {
            InputStream is = FileUtils.getStreamForUrl(filePath, getClassLoader());
            
            if (is != null) {
                log.log(Level.INFO, "Loading providers for file [" + filePath + "]");
                ProviderManager.getInstance().addLoader(new ProviderFileLoader(is));
            }
            else {
                log.log(Level.WARNING, "No input stream created for " + filePath);
            }
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "Error trying to load provider file " + filePath, e);
        }
    }

    protected abstract String getFilePath();
    
    /**
     * Returns an array of class loaders to load resources from.
     *
     * @return an array of ClassLoader instances.
     */
    protected ClassLoader getClassLoader() {
        return null;
    }
}
