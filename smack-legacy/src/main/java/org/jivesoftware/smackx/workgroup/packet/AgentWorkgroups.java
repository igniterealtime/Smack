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
import org.jivesoftware.smack.util.ParserUtils;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a request for getting the jid of the workgroups where an agent can work or could
 * represent the result of such request which will contain the list of workgroups JIDs where the
 * agent can work.
 *
 * @author Gaston Dombiak
 */
public class AgentWorkgroups extends IQ {

    private Jid agentJID;
    private List<String> workgroups;

    private AgentWorkgroups() {
        super("workgroups", "http://jabber.org/protocol/workgroup");
    }

    /**
     * Creates an AgentWorkgroups request for the given agent. This IQ will be sent and an answer
     * will be received with the jid of the workgroups where the agent can work.
     *
     * @param agentJID the id of the agent to get his workgroups.
     */
    public AgentWorkgroups(Jid agentJID) {
        this();
        this.agentJID = agentJID;
        this.workgroups = new ArrayList<String>();
    }

    /**
     * Creates an AgentWorkgroups which will contain the JIDs of the workgroups where an agent can
     * work.
     *
     * @param agentJID the id of the agent that can work in the list of workgroups.
     * @param workgroups the list of workgroup JIDs where the agent can work.
     */
    public AgentWorkgroups(Jid agentJID, List<String> workgroups) {
        this();
        this.agentJID = agentJID;
        this.workgroups = workgroups;
    }

    public Jid getAgentJID() {
        return agentJID;
    }

    /**
     * Returns a list of workgroup JIDs where the agent can work.
     *
     * @return a list of workgroup JIDs where the agent can work.
     */
    public List<String> getWorkgroups() {
        return Collections.unmodifiableList(workgroups);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.attribute("jid", agentJID).rightAngleBracket();

        for (Iterator<String> it=workgroups.iterator(); it.hasNext();) {
            String workgroupJID = it.next();
            buf.append("<workgroup jid=\"" + workgroupJID + "\"/>");
        }

        return buf;
    }

    /**
     * An IQProvider for AgentWorkgroups packets.
     *
     * @author Gaston Dombiak
     */
    public static class Provider extends IQProvider<AgentWorkgroups> {

        @Override
        public AgentWorkgroups parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            final Jid agentJID = ParserUtils.getJidAttribute(parser);
            List<String> workgroups = new ArrayList<String>();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("workgroup")) {
                        workgroups.add(parser.getAttributeValue("", "jid"));
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("workgroups")) {
                        done = true;
                    }
                }
            }

            return new AgentWorkgroups(agentJID, workgroups);
        }
    }
}
