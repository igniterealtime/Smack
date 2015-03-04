/**
 *
 * Copyright 2014 Vyacheslav Blinov
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

package org.jivesoftware.smackx.debugger.slf4j;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;
import org.jxmpp.jid.FullJid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Implementation of SmackDebugger that writes log messages using SLF4J API.
 * Use in conjunction with your SLF4J bindings of choice.
 * See SLF4J manual for more details about bindings usage.
 */
public class SLF4JSmackDebugger implements SmackDebugger  {
    public static final String LOGGER_NAME = "SMACK";
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    public static final AtomicBoolean printInterpreted = new AtomicBoolean(true);

    public static final String SENT_TAG = "SENT";
    public static final String RECEIVED_TAG = "RECV";

    private final XMPPConnection connection;

    private final StanzaListener receivedListener = new SLF4JLoggingPacketListener(logger, RECEIVED_TAG);
    private final StanzaListener sentListener = new SLF4JLoggingPacketListener(logger, SENT_TAG);
    private final SLF4JRawXmlListener slf4JRawXmlListener = new SLF4JRawXmlListener(logger);

    private ObservableWriter writer;
    private ObservableReader reader;

    /**
     * Makes Smack use this Debugger
     */
    public static void enable() {
        SmackConfiguration.setDebuggerFactory(new SLF4JDebuggerFactory());
    }

    /**
     * Create new SLF4J Smack Debugger instance
     * @param connection Smack connection to debug
     * @param writer connection data writer to observe
     * @param reader connection data reader to observe
     */
    public SLF4JSmackDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        this.connection = connection;
        this.writer = new ObservableWriter(writer);
        this.writer.addWriterListener(slf4JRawXmlListener);
        this.reader = new ObservableReader(Validate.notNull(reader));
        this.reader.addReaderListener(slf4JRawXmlListener);
        this.connection.addConnectionListener(new SLF4JLoggingConnectionListener(connection, logger));
    }

    @Override
    public Reader newConnectionReader(Reader newReader) {
        reader.removeReaderListener(slf4JRawXmlListener);
        reader = new ObservableReader(newReader);
        reader.addReaderListener(slf4JRawXmlListener);
        return reader;
    }

    @Override
    public Writer newConnectionWriter(Writer newWriter) {
        writer.removeWriterListener(slf4JRawXmlListener);
        writer = new ObservableWriter(newWriter);
        writer.addWriterListener(slf4JRawXmlListener);
        return writer;
    }

    @Override
    public void userHasLogged(FullJid user) {
        if (logger.isDebugEnabled()) {
            logger.debug("({}) User logged in {}", connection.hashCode(), user.toString());
        }
    }

    @Override
    public Reader getReader() {
        return reader;
    }

    @Override
    public Writer getWriter() {
        return writer;
    }

    @Override
    public StanzaListener getReaderListener() {
        return receivedListener;
    }

    @Override
    public StanzaListener getWriterListener() {
        return sentListener;
    }
}
