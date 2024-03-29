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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.Jid;

/**
 * An IQProvider for transcripts summaries.
 *
 * @author Gaston Dombiak
 */
public class TranscriptsProvider extends IqProvider<Transcripts> {

    @SuppressWarnings("DateFormatConstant")
    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    @Override
    public Transcripts parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, TextParseException, ParseException {
        Jid userID = ParserUtils.getJidAttribute(parser, "userID");
        List<Transcripts.TranscriptSummary> summaries = new ArrayList<>();

        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("transcript")) {
                    Transcripts.TranscriptSummary summary = parseSummary(parser);
                    summaries.add(summary);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("transcripts")) {
                    done = true;
                }
            }
        }

        return new Transcripts(userID, summaries);
    }

    private static Transcripts.TranscriptSummary parseSummary(XmlPullParser parser)
                    throws IOException, XmlPullParserException, ParseException {
        String sessionID =  parser.getAttributeValue("", "sessionID");
        Date joinTime = null;
        Date leftTime = null;
        List<Transcripts.AgentDetail> agents = new ArrayList<>();

        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("joinTime")) {
                    synchronized (UTC_FORMAT) {
                        joinTime = UTC_FORMAT.parse(parser.nextText());
                     }
                }
                else if (parser.getName().equals("leftTime")) {
                    synchronized (UTC_FORMAT) {
                        leftTime = UTC_FORMAT.parse(parser.nextText());
                    }
                }
                else if (parser.getName().equals("agents")) {
                    agents = parseAgents(parser);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("transcript")) {
                    done = true;
                }
            }
        }

        return new Transcripts.TranscriptSummary(sessionID, joinTime, leftTime, agents);
    }

    private static List<Transcripts.AgentDetail> parseAgents(XmlPullParser parser)
                    throws IOException, XmlPullParserException, ParseException {
        List<Transcripts.AgentDetail> agents = new ArrayList<>();
        String agentJID =  null;
        Date joinTime = null;
        Date leftTime = null;

        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("agentJID")) {
                    agentJID = parser.nextText();
                }
                else if (parser.getName().equals("joinTime")) {
                    synchronized (UTC_FORMAT) {
                        joinTime = UTC_FORMAT.parse(parser.nextText());
                    }
                }
                else if (parser.getName().equals("leftTime")) {
                    synchronized (UTC_FORMAT) {
                        leftTime = UTC_FORMAT.parse(parser.nextText());
                    }
                }
                else if (parser.getName().equals("agent")) {
                    agentJID =  null;
                    joinTime = null;
                    leftTime = null;
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
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
