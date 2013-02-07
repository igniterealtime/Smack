/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
