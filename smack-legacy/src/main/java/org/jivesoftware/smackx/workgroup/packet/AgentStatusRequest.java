/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Agent status request packet. This packet is used by agents to request the list of
 * agents in a workgroup. The response packet contains a list of packets. Presence
 * packets from individual agents follow.
 *
 * @author Matt Tucker
 */
public class AgentStatusRequest extends IQ {

     /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "agent-status-request";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private Set<Item> agents;

    public AgentStatusRequest() {
        super(ELEMENT_NAME, NAMESPACE);
        agents = new HashSet<Item>();
    }

    public int getAgentCount() {
        return agents.size();
    }

    public Set<Item> getAgents() {
        return Collections.unmodifiableSet(agents);
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        synchronized (agents) {
            for (Iterator<Item> i=agents.iterator(); i.hasNext(); ) {
                Item item = i.next();
                buf.append("<agent jid=\"").append(item.getJID()).append("\">");
                if (item.getName() != null) {
                    buf.append("<name xmlns=\""+ AgentInfo.NAMESPACE + "\">");
                    buf.append(item.getName());
                    buf.append("</name>");
                }
                buf.append("</agent>");
            }
        }
        return buf;
    }

    public static class Item {

        private String jid;
        private String type;
        private String name;

        public Item(String jid, String type, String name) {
            this.jid = jid;
            this.type = type;
            this.name = name;
        }

        public String getJID() {
            return jid;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Packet extension provider for AgentStatusRequest packets.
     */
    public static class Provider extends IQProvider<AgentStatusRequest> {

        @Override
        public AgentStatusRequest parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            AgentStatusRequest statusRequest = new AgentStatusRequest();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("agent".equals(parser.getName()))) {
                    statusRequest.agents.add(parseAgent(parser));
                }
                else if (eventType == XmlPullParser.END_TAG &&
                        "agent-status-request".equals(parser.getName()))
                {
                    done = true;
                }
            }
            return statusRequest;
        }

        private Item parseAgent(XmlPullParser parser) throws XmlPullParserException, IOException {

            boolean done = false;
            String jid = parser.getAttributeValue("", "jid");
            String type = parser.getAttributeValue("", "type");
            String name = null;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("name".equals(parser.getName()))) {
                    name = parser.nextText();
                }
                else if (eventType == XmlPullParser.END_TAG &&
                        "agent".equals(parser.getName()))
                {
                    done = true;
                }
            }
            return new Item(jid, type, name);
        }
    }
}
