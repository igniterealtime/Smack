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
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * An IQProvider for transcripts summaries.
 *
 * @author Gaston Dombiak
 */
public class TranscriptsProvider implements IQProvider {

    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    public TranscriptsProvider() {
        super();
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        String userID = parser.getAttributeValue("", "userID");
        List<Transcripts.TranscriptSummary> summaries = new ArrayList<Transcripts.TranscriptSummary>();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("transcript")) {
                    summaries.add(parseSummary(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("transcripts")) {
                    done = true;
                }
            }
        }

        return new Transcripts(userID, summaries);
    }

    private Transcripts.TranscriptSummary parseSummary(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        String sessionID =  parser.getAttributeValue("", "sessionID");
        Date joinTime = null;
        Date leftTime = null;
        List<Transcripts.AgentDetail> agents = new ArrayList<Transcripts.AgentDetail>();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("joinTime")) {
                    try {
                        joinTime = UTC_FORMAT.parse(parser.nextText());
                    } catch (ParseException e) {}
                }
                else if (parser.getName().equals("leftTime")) {
                    try {
                        leftTime = UTC_FORMAT.parse(parser.nextText());
                    } catch (ParseException e) {}
                }
                else if (parser.getName().equals("agents")) {
                    agents = parseAgents(parser);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("transcript")) {
                    done = true;
                }
            }
        }

        return new Transcripts.TranscriptSummary(sessionID, joinTime, leftTime, agents);
    }

    private List<Transcripts.AgentDetail> parseAgents(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<Transcripts.AgentDetail> agents = new ArrayList<Transcripts.AgentDetail>();
        String agentJID =  null;
        Date joinTime = null;
        Date leftTime = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("agentJID")) {
                    agentJID = parser.nextText();
                }
                else if (parser.getName().equals("joinTime")) {
                    try {
                        joinTime = UTC_FORMAT.parse(parser.nextText());
                    } catch (ParseException e) {}
                }
                else if (parser.getName().equals("leftTime")) {
                    try {
                        leftTime = UTC_FORMAT.parse(parser.nextText());
                    } catch (ParseException e) {}
                }
                else if (parser.getName().equals("agent")) {
                    agentJID =  null;
                    joinTime = null;
                    leftTime = null;
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("agents")) {
                    done = true;
                }
                else if (parser.getName().equals("agent")) {
                    agents.add(new Transcripts.AgentDetail(agentJID, joinTime, leftTime));
                }
            }
        }
        return agents;
    }
}
