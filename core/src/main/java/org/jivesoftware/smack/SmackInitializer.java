package org.jivesoftware.smack;

/**
 * Defines an initialization class that will be instantiated and invoked by the {@link SmackConfiguration} class during initialization.
 * 
 * <p>
 * Any implementation of this class MUST have a default constructor.
 * 
 * @author Robin Collier
 *
 */
public interface SmackInitializer {
    void initialize();
}
