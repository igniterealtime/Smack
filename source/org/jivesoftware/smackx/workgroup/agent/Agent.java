/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
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

package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smack.packet.Presence;

/**
 * An agent represents the agent role in a workgroup queue.
 *
 * @author Matt Tucker
 */
public class Agent {
    
    private String user;
    private int maxChats = -1;
    private int currentChats = -1;
    private Presence presence = null;
  
  /**
   * Creates an Agent.
   *
   * @param user the agent's JID.
   * @param currentChats the number of chats the agent is currently in.
   * @param maxChats the maximum number of chats the agent is allowed in.
   * @param presence the agent's presence.
   */
    public Agent(String user, int currentChats, int maxChats, Presence presence) {
        this.user = user;
        this.currentChats = currentChats;
        this.maxChats = maxChats;
        this.presence = presence;
    }
  
   /**
    * Return this agent's JID.
    *
    * @return this agent's JID.
   */
    public String getUser() {
        return user;
    }

   /**
    * Return the maximum number of chats this agent can participate in.
    *
    * @return the maximum number of chats this agent can participate in.
    */
    public int getMaxChats() {
        return maxChats;
    }
  
   /**
    * Return the number of chats this agent is currently in.
    *
    * @return the number of chats this agent is currently in.
    */
    public int getCurrentChats() {
        return currentChats;
    }
  
   /**
    * Return this agent's presence.
    *
    * @return this agent's presence.
    */
    public Presence getPresence() {
        return presence;
    }
}
