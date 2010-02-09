/**
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

package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smackx.workgroup.packet.AgentInfo;
import org.jivesoftware.smackx.workgroup.packet.AgentWorkgroups;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;

import java.util.Collection;

/**
 * The <code>Agent</code> class is used to represent one agent in a Workgroup Queue.
 *
 * @author Derek DeMoro
 */
public class Agent {
    private Connection connection;
    private String workgroupJID;

    public static Collection<String> getWorkgroups(String serviceJID, String agentJID, Connection connection) throws XMPPException {
        AgentWorkgroups request = new AgentWorkgroups(agentJID);
        request.setTo(serviceJID);
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        // Send the request
        connection.sendPacket(request);

        AgentWorkgroups response = (AgentWorkgroups)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response.getWorkgroups();
    }

    /**
     * Constructs an Agent.
     */
    Agent(Connection connection, String workgroupJID) {
        this.connection = connection;
        this.workgroupJID = workgroupJID;
    }

    /**
     * Return the agents JID
     *
     * @return - the agents JID.
     */
    public String getUser() {
        return connection.getUser();
    }

    /**
     * Return the agents name.
     *
     * @return - the agents name.
     */
    public String getName() throws XMPPException {
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.setType(IQ.Type.GET);
        agentInfo.setTo(workgroupJID);
        agentInfo.setFrom(getUser());
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(agentInfo.getPacketID()));
        // Send the request
        connection.sendPacket(agentInfo);

        AgentInfo response = (AgentInfo)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response.getName();
    }

    /**
     * Changes the name of the agent in the server. The server may have this functionality
     * disabled for all the agents or for this agent in particular. If the agent is not
     * allowed to change his name then an exception will be thrown with a service_unavailable
     * error code.
     *
     * @param newName the new name of the agent.
     * @throws XMPPException if the agent is not allowed to change his name or no response was
     *                       obtained from the server.
     */
    public void setName(String newName) throws XMPPException {
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.setType(IQ.Type.SET);
        agentInfo.setTo(workgroupJID);
        agentInfo.setFrom(getUser());
        agentInfo.setName(newName);
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(agentInfo.getPacketID()));
        // Send the request
        connection.sendPacket(agentInfo);

        IQ response = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return;
    }
}
