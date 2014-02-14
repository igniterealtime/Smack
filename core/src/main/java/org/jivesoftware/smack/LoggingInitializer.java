package org.jivesoftware.smack;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.FileUtils;

/**
 * Initializes the Java logging system.
 *  
 * @author Robin Collier
 *
 */
public class LoggingInitializer implements SmackInitializer {

    private static Logger log = Logger.getLogger(LoggingInitializer.class.getName());

    @Override
    public void initialize() {
        try {
            LogManager.getLogManager().readConfiguration(FileUtils.getStreamForUrl("classpath:META-INF/jul.properties", null));
        } 
        catch (Exception e) {
            log .log(Level.WARNING, "Could not initialize Java Logging from default file.", e);
        }
    }
}
