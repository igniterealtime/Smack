package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.Jingle;

/**
 *  Implement the Jingle Session state using the State Behavioral pattern.
 *  (From the book Design Patterns, AKA GoF.)
 *  These classes also employ the Flyweight and Singleton patterns as recommended for the State pattern by GoF.
 *  
 *  There seems to be three ways to go with the State pattern in Java: interface, abstract class and enums.
 *  Most of the accepted models use abstract classes.  It wasn't clear to me that any of the three models was
 *  superior, so I went with the most common example.
 *  
 *  @author Jeff Williams
 */
public abstract class JingleSessionState {

    /**
     * Called when entering the state.
     */
    public static JingleSessionState getInstance() {
        // Since we can never instantiate this class there is nothing to return (ever).
        return null;
    }

    /**
     * Called when entering the state.
     */
    public abstract void enter();

    /**
     * Called when exiting the state.
     */
    public abstract void exit();

    /**
     * Process an incoming Jingle Packet.
     * When you look at the GoF State pattern this method roughly corresponds to example on p310: ProcessOctect().
     */
    public abstract IQ processJingle(JingleSession session, Jingle jingle, JingleActionEnum action);

    /**
     * For debugging just emit the short name of the class.
     */
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
