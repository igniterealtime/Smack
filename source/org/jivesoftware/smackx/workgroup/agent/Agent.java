package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smack.packet.Presence;

/**
 * An Agent represents the agent role in a Workgroup Queue.
 */
public class Agent {
    
  private String user;
  private int maxChats = -1;
  private int currentChats = -1;
  private Presence presence = null;
  
  /**
   * Creates an Agent
   * @param user - the current agents JID
   * @param currentChats - the number of chats the agent is in.
   * @param maxChats - the maximum number of chats the agent is allowed.
   * @param presence - the agents presence
   */
  public Agent( String user, int currentChats, int maxChats, Presence presence ) {
    this.user = user;
    this.currentChats = currentChats;
    this.maxChats = maxChats;
    this.presence = presence;
  }
  
  /**
   * Return the agents JID
   * @return - the agents JID.
   */
  public String getUser() {
    return user;
  }

  /**
   * Return the maximum number of chats for this agent.
   * @return - maximum number of chats allowed.
   */
  public int getMaxChats() {
    return maxChats;
  }
  
  /**
   * Return the current chat count.
   * @return - the current chat count.
   */
  public int getCurrentChats() {
    return currentChats;
  }
  
  /**
   * Return the agents <code>Presence</code>
   * @return - the agents presence.
   */
  public Presence getPresence() {
    return presence;
  }
}
