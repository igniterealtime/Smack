/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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
