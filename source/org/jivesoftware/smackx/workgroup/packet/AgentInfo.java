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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * IQ packet for retrieving and changing the Agent personal information.
 */
public class AgentInfo extends IQ {

    /**
    * Element name of the packet extension.
    */
   public static final String ELEMENT_NAME = "agent-info";

   /**
    * Namespace of the packet extension.
    */
   public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private String jid;
    private String name;

    /**
     * Returns the Agent's jid.
     *
     * @return the Agent's jid.
     */
    public String getJid() {
        return jid;
    }

    /**
     * Sets the Agent's jid.
     *
     * @param jid the jid of the agent.
     */
    public void setJid(String jid) {
        this.jid = jid;
    }

    /**
     * Returns the Agent's name. The name of the agent may be different than the user's name.
     * This property may be shown in the webchat client.
     *
     * @return the Agent's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the Agent's name. The name of the agent may be different than the user's name.
     * This property may be shown in the webchat client.
     *
     * @param name the new name of the agent.
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");
        if (jid != null) {
            buf.append("<jid>").append(getJid()).append("</jid>");
        }
        if (name != null) {
            buf.append("<name>").append(getName()).append("</name>");
        }
        buf.append("</").append(ELEMENT_NAME).append("> ");

        return buf.toString();
    }

    /**
     * An IQProvider for AgentInfo packets.
     *
     * @author Gaston Dombiak
     */
    public static class Provider implements IQProvider {

        public Provider() {
            super();
        }

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            AgentInfo answer = new AgentInfo();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("jid")) {
                        answer.setJid(parser.nextText());
                    }
                    else if (parser.getName().equals("name")) {
                        answer.setName(parser.nextText());
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return answer;
        }
    }
}
