/**
 *
 * Copyright 2003-2007 Jive Software, 2017 Florian Schmaus.
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

package org.jivesoftware.smack.debugger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;

import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.xml.splitter.XmlPrettyPrinter;
import org.jxmpp.xml.splitter.XmppXmlSplitter;

/**
 * Interface that allows for implementing classes to debug XML traffic. That is a GUI window that
 * displays XML traffic.<p>
 *
 * Every implementation of this interface <b>must</b> have a public constructor with the following
 * arguments: XMPPConnection, Writer, Reader.
 *
 * @author Gaston Dombiak
 */
public abstract class SmackDebugger {

    protected final XMPPConnection connection;

    private XmppXmlSplitter outgoingStreamSplitterForPrettyPrinting;
    private XmppXmlSplitter incomingStreamSplitterForPrettyPrinting;

    protected SmackDebugger(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Called when a user has logged in to the server. The user could be an anonymous user, this
     * means that the user would be of the form host/resource instead of the form
     * user@host/resource.
     *
     * @param user the user@host/resource that has just logged in
     */
    // TODO: Should be replaced with a connection listener authenticed().
    public abstract void userHasLogged(EntityFullJid user);

    /**
     * Note that the sequence of characters may be pretty printed.
     *
     * @param outgoingCharSequence the outgoing character sequence.
     */
    public abstract void outgoingStreamSink(CharSequence outgoingCharSequence);

    public void onOutgoingElementCompleted() {
    }

    public abstract void incomingStreamSink(CharSequence incomingCharSequence);

    public void onIncomingElementCompleted() {
    }

    /**
     * Returns a new special Reader that wraps the new connection Reader. The connection
     * has been secured so the connection is using a new reader and writer. The debugger
     * needs to wrap the new reader and writer to keep being notified of the connection
     * traffic.
     *
     * @param reader connection reader.
     * @return a new special Reader that wraps the new connection Reader.
     */
    public final Reader newConnectionReader(Reader reader) {
        XmlPrettyPrinter xmlPrettyPrinter = XmlPrettyPrinter.builder()
                        .setPrettyWriter((sb) -> incomingStreamSink(sb))
                        .build();
        incomingStreamSplitterForPrettyPrinting = new XmppXmlSplitter(xmlPrettyPrinter);

        ObservableReader observableReader = new ObservableReader(reader);
        observableReader.addReaderListener((readString) -> {
            try {
                incomingStreamSplitterForPrettyPrinting.append(readString);
            }
            catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        return observableReader;
    }

    /**
     * Returns a new special Writer that wraps the new connection Writer. The connection
     * has been secured so the connection is using a new reader and writer. The debugger
     * needs to wrap the new reader and writer to keep being notified of the connection
     * traffic.
     *
     * @param writer connection writer.
     * @return a new special Writer that wraps the new connection Writer.
     */
    public final Writer newConnectionWriter(Writer writer) {
        XmlPrettyPrinter xmlPrettyPrinter = XmlPrettyPrinter.builder()
                        .setPrettyWriter((sb) -> outgoingStreamSink(sb))
                        .build();
        outgoingStreamSplitterForPrettyPrinting = new XmppXmlSplitter(xmlPrettyPrinter);

        ObservableWriter observableWriter = new ObservableWriter(writer);
        observableWriter.addWriterListener((writtenString) -> {
            try {
                outgoingStreamSplitterForPrettyPrinting.append(writtenString);
            }
            catch (IOException e) {
                throw new AssertionError(e);
            }
        });
        return observableWriter;
    }

    /**
     * Used by the connection to notify about an incoming top level stream element.
     * <p>
     * This method is invoked right after the incoming stream was parsed.
     * </p>
     *
     * @param streamElement the incoming top level stream element.
     */
    public abstract void onIncomingStreamElement(TopLevelStreamElement streamElement);

    /**
     * Used by the connection to notify about a outgoing top level stream element.
     * <p>
     * This method is invoked right before the element is serialized to XML and put into the outgoing stream.
     * </p>
     *
     * @param streamElement the outgoing top level stream element.
     */
    public abstract void onOutgoingStreamElement(TopLevelStreamElement streamElement);

}
