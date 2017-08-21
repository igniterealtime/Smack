/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b.provider;

import static org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.Mode.tcp;
import static org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.Mode.udp;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_CID;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_HOST;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_JID;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_PORT;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_PRIORITY;
import static org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement.ATTR_TYPE;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;

import org.xmlpull.v1.XmlPullParser;


/**
 * Provider for JingleSocks5BytestreamTransport elements.
 */
public class JingleS5BTransportProvider extends JingleContentTransportProvider<JingleS5BTransportElement> {

    @Override
    public JingleS5BTransportElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        JingleS5BTransportElement.Builder builder = JingleS5BTransportElement.getBuilder();

        String streamId = parser.getAttributeValue(null, JingleS5BTransportElement.ATTR_SID);
        builder.setStreamId(streamId);

        String dstAddr = parser.getAttributeValue(null, JingleS5BTransportElement.ATTR_DSTADDR);
        builder.setDestinationAddress(dstAddr);

        String mode = parser.getAttributeValue(null, JingleS5BTransportElement.ATTR_MODE);
        if (mode != null) {
            builder.setMode(mode.equals(udp.toString()) ? udp : tcp);
        }

        JingleS5BTransportCandidateElement.Builder cb;
        outerloop: while (true) {
            int tag = parser.nextTag();
            String name = parser.getName();
            switch (tag) {
                case START_TAG: {
                    switch (name) {

                        case JingleS5BTransportCandidateElement.ELEMENT:
                            cb = JingleS5BTransportCandidateElement.getBuilder();
                            cb.setCandidateId(parser.getAttributeValue(null, ATTR_CID));
                            cb.setHost(parser.getAttributeValue(null, ATTR_HOST));
                            cb.setJid(parser.getAttributeValue(null, ATTR_JID));
                            cb.setPriority(Integer.parseInt(parser.getAttributeValue(null, ATTR_PRIORITY)));

                            String portString = parser.getAttributeValue(null, ATTR_PORT);
                            if (portString != null) {
                                cb.setPort(Integer.parseInt(portString));
                            }

                            String typeString = parser.getAttributeValue(null, ATTR_TYPE);
                            if (typeString != null) {
                                cb.setType(JingleS5BTransportCandidateElement.Type.fromString(typeString));
                            }
                            builder.addTransportCandidate(cb.build());
                            break;

                        case JingleS5BTransportInfoElement.CandidateActivated.ELEMENT:
                            builder.setTransportInfo(new JingleS5BTransportInfoElement.CandidateActivated(
                                    parser.getAttributeValue(null,
                                            JingleS5BTransportInfoElement.JingleS5BCandidateTransportInfoElement.ATTR_CID)));
                            break;

                        case JingleS5BTransportInfoElement.CandidateUsed.ELEMENT:
                            builder.setTransportInfo(new JingleS5BTransportInfoElement.CandidateUsed(
                                    parser.getAttributeValue(null,
                                            JingleS5BTransportInfoElement.JingleS5BCandidateTransportInfoElement.ATTR_CID)));
                            break;

                        case JingleS5BTransportInfoElement.CandidateError.ELEMENT:
                            builder.setTransportInfo(JingleS5BTransportInfoElement.CandidateError.INSTANCE);
                            break;

                        case JingleS5BTransportInfoElement.ProxyError.ELEMENT:
                            builder.setTransportInfo(JingleS5BTransportInfoElement.ProxyError.INSTANCE);
                            break;
                    }
                }
                break;

                case END_TAG: {
                    switch (name) {
                        case JingleContentTransportElement.ELEMENT:
                            break outerloop;
                    }
                }
            }
        }
        return builder.build();
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }
}
