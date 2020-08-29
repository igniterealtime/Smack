/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket.implementations.okhttp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.debugger.SmackDebugger;

import okhttp3.Headers;
import okhttp3.Response;

import org.jxmpp.xml.splitter.Utf8ByteXmppXmlSplitter;
import org.jxmpp.xml.splitter.XmlPrettyPrinter;
import org.jxmpp.xml.splitter.XmppXmlSplitter;

public final class LoggingInterceptor {
    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final int MAX_ELEMENT_SIZE = 64 * 1024;
    private final SmackDebugger debugger;
    private final Utf8ByteXmppXmlSplitter incomingTextSplitter;
    private final Utf8ByteXmppXmlSplitter outgoingTextSplitter;

    public LoggingInterceptor(SmackDebugger smackDebugger) {
        this.debugger = smackDebugger;

        XmlPrettyPrinter incomingTextPrinter = XmlPrettyPrinter.builder()
                                    .setPrettyWriter(sb -> debugger.incomingStreamSink(sb))
                                    .setTabWidth(4)
                                    .build();
        XmppXmlSplitter incomingXmlSplitter = new XmppXmlSplitter(MAX_ELEMENT_SIZE, null,
                incomingTextPrinter);
        incomingTextSplitter = new Utf8ByteXmppXmlSplitter(incomingXmlSplitter);

        XmlPrettyPrinter outgoingTextPrinter = XmlPrettyPrinter.builder()
                .setPrettyWriter(sb -> debugger.outgoingStreamSink(sb))
                .setTabWidth(4)
                .build();
        XmppXmlSplitter outgoingXmlSplitter = new XmppXmlSplitter(MAX_ELEMENT_SIZE, null,
                outgoingTextPrinter);
        outgoingTextSplitter = new Utf8ByteXmppXmlSplitter(outgoingXmlSplitter);
    }

    // Open response received here isn't in the form of an Xml an so, there isn't much to format.
    public void interceptOpenResponse(Response response) {
       Headers headers = response.headers();
       Iterator<?> iterator = headers.iterator();
       StringBuilder sb = new StringBuilder();
       sb.append("Received headers:");
       while (iterator.hasNext()) {
           sb.append("\n\t" + iterator.next());
       }
       debugger.incomingStreamSink(sb);
    }

    public void interceptReceivedText(String text) {
        try {
            incomingTextSplitter.write(text.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            // Connections shouldn't be terminated due to exceptions encountered during debugging. hence only log them.
            LOGGER.log(Level.WARNING, "IOException encountered while parsing received text: " + text, e);
        }
    }

    public void interceptSentText(String text) {
        try {
            outgoingTextSplitter.write(text.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            // Connections shouldn't be terminated due to exceptions encountered during debugging, hence only log them.
            LOGGER.log(Level.WARNING, "IOException encountered while parsing outgoing text: " + text, e);
        }
    }
}
