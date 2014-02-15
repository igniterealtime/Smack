package org.jivesoftware.smack.initializer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
    
    private List<Exception> exceptions = new LinkedList<Exception>();

    @Override
    public void initialize() {
        try {
            LogManager.getLogManager().readConfiguration(FileUtils.getStreamForUrl("classpath:org.jivesofware.smack/jul.properties", null));
        } 
        catch (Exception e) {
            log .log(Level.WARNING, "Could not initialize Java Logging from default file.", e);
            exceptions.add(e);
        }
    }

	@Override
	public List<Exception> getExceptions() {
		return Collections.unmodifiableList(exceptions);
	}
}
