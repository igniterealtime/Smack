/**
 *
 * Copyright 2020 Aditya Borikar, 2021 Florian Schmaus
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
package org.jivesoftware.smack.websocket.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.debugger.SmackDebugger;

import org.jxmpp.xml.splitter.XmlPrettyPrinter;
import org.jxmpp.xml.splitter.XmppXmlSplitter;

public class SmackWebSocketDebugger {

    private static final Logger LOGGER = Logger.getLogger(SmackWebSocketDebugger.class.getName());

    private final SmackDebugger debugger;
    private final XmppXmlSplitter incomingXmlSplitter;
    private final XmppXmlSplitter outgoingXmlSplitter;

    SmackWebSocketDebugger(SmackDebugger smackDebugger) {
        this.debugger = smackDebugger;

        XmlPrettyPrinter incomingTextPrinter = XmlPrettyPrinter.builder()
                        .setPrettyWriter(sb -> debugger.incomingStreamSink(sb))
                        .setTabWidth(4)
                        .build();
        incomingXmlSplitter = new XmppXmlSplitter(incomingTextPrinter);

        XmlPrettyPrinter outgoingTextPrinter = XmlPrettyPrinter.builder()
                        .setPrettyWriter(sb -> debugger.outgoingStreamSink(sb))
                        .setTabWidth(4)
                        .build();
        outgoingXmlSplitter = new XmppXmlSplitter(outgoingTextPrinter);
    }

    void incoming(String text) {
        try {
            incomingXmlSplitter.write(text);
        } catch (IOException e) {
            // Connections shouldn't be terminated due to exceptions encountered during debugging. hence only log them.
            LOGGER.log(Level.WARNING, "IOException encountered while parsing received text: " + text, e);
        }
    }

    void outgoing(String text) {
        try {
            outgoingXmlSplitter.write(text);
        } catch (IOException e) {
            // Connections shouldn't be terminated due to exceptions encountered during debugging, hence only log them.
            LOGGER.log(Level.WARNING, "IOException encountered while parsing outgoing text: " + text, e);
        }
    }
}
