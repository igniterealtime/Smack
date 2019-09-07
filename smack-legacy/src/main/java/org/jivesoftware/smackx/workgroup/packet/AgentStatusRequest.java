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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.EntityBareJid;

/**
 * Agent status request packet. This stanza is used by agents to request the list of
 * agents in a workgroup. The response stanza contains a list of packets. Presence
 * packets from individual agents follow.
 *
 * @author Matt Tucker
 */
public class AgentStatusRequest extends IQ {

     /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "agent-status-request";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private final Set<Item> agents = new HashSet<>();

    public AgentStatusRequest() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public int getAgentCount() {
        return agents.size();
    }

    public Set<Item> getAgents() {
        return Collections.unmodifiableSet(agents);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        synchronized (agents) {
            for (Iterator<Item> i = agents.iterator(); i.hasNext(); ) {
                Item item = i.next();
                buf.append("<agent jid=\"").append(item.getJID()).append("\">");
                if (item.getName() != null) {
                    buf.append("<name xmlns=\"" + AgentInfo.NAMESPACE + "\">");
                    buf.append(item.getName());
                    buf.append("</name>");
                }
                buf.append("</agent>");
            }
        }
        return buf;
    }

    public static class Item {

        private final EntityBareJid jid;
        private final String type;
        private final String name;

        public Item(EntityBareJid jid, String type, String name) {
            this.jid = jid;
            this.type = type;
            this.name = name;
        }

        public EntityBareJid getJID() {
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
     * Stanza extension provider for AgentStatusRequest packets.
     */
    public static class Provider extends IQProvider<AgentStatusRequest> {

        @Override
        public AgentStatusRequest parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
            AgentStatusRequest statusRequest = new AgentStatusRequest();

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && "agent".equals(parser.getName())) {
                    statusRequest.agents.add(parseAgent(parser));
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT &&
                        "agent-status-request".equals(parser.getName())) {
                    done = true;
                }
            }
            return statusRequest;
        }

        private static Item parseAgent(XmlPullParser parser) throws XmlPullParserException, IOException {

            boolean done = false;
            EntityBareJid jid = ParserUtils.getBareJidAttribute(parser);
            String type = parser.getAttributeValue("", "type");
            String name = null;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && "name".equals(parser.getName())) {
                    name = parser.nextText();
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT &&
                        "agent".equals(parser.getName())) {
                    done = true;
                }
            }
            return new Item(jid, type, name);
        }
    }
}
