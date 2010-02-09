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

package org.jivesoftware.smack.debugger;

import java.io.*;

import org.jivesoftware.smack.*;

/**
 * Interface that allows for implementing classes to debug XML traffic. That is a GUI window that 
 * displays XML traffic.<p>
 * 
 * Every implementation of this interface <b>must</b> have a public constructor with the following 
 * arguments: Connection, Writer, Reader.
 * 
 * @author Gaston Dombiak
 */
public interface SmackDebugger {

    /**
     * Called when a user has logged in to the server. The user could be an anonymous user, this 
     * means that the user would be of the form host/resource instead of the form 
     * user@host/resource.
     * 
     * @param user the user@host/resource that has just logged in
     */
    public abstract void userHasLogged(String user);

    /**
     * Returns the special Reader that wraps the main Reader and logs data to the GUI.
     * 
     * @return the special Reader that wraps the main Reader and logs data to the GUI.
     */
    public abstract Reader getReader();

    /**
     * Returns the special Writer that wraps the main Writer and logs data to the GUI.
     * 
     * @return the special Writer that wraps the main Writer and logs data to the GUI.
     */
    public abstract Writer getWriter();

    /**
     * Returns a new special Reader that wraps the new connection Reader. The connection
     * has been secured so the connection is using a new reader and writer. The debugger
     * needs to wrap the new reader and writer to keep being notified of the connection
     * traffic.
     *
     * @return a new special Reader that wraps the new connection Reader.
     */
    public abstract Reader newConnectionReader(Reader reader);

    /**
     * Returns a new special Writer that wraps the new connection Writer. The connection
     * has been secured so the connection is using a new reader and writer. The debugger
     * needs to wrap the new reader and writer to keep being notified of the connection
     * traffic.
     *
     * @return a new special Writer that wraps the new connection Writer.
     */
    public abstract Writer newConnectionWriter(Writer writer);

    /**
     * Returns the thread that will listen for all incoming packets and write them to the GUI. 
     * This is what we call "interpreted" packet data, since it's the packet data as Smack sees 
     * it and not as it's coming in as raw XML.
     * 
     * @return the PacketListener that will listen for all incoming packets and write them to 
     * the GUI
     */
    public abstract PacketListener getReaderListener();

    /**
     * Returns the thread that will listen for all outgoing packets and write them to the GUI. 
     * 
     * @return the PacketListener that will listen for all sent packets and write them to 
     * the GUI
     */
    public abstract PacketListener getWriterListener();
}