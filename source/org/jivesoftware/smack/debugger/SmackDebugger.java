/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smack.debugger;

import java.io.*;

import org.jivesoftware.smack.*;

/**
 * Interface that allows for implementing classes to debug XML traffic. That is a GUI window that 
 * displays XML traffic.<p>
 * 
 * Every implementation of this interface <b>must</b> have a public constructor with the following 
 * arguments: XMPPConnection, Writer, Reader.
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