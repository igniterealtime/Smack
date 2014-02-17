/**
 *
 * Copyright the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
