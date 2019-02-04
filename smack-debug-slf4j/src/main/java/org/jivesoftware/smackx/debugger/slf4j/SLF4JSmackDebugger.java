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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;

import org.jxmpp.jid.EntityFullJid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of SmackDebugger that writes log messages using SLF4J API.
 * Use in conjunction with your SLF4J bindings of choice.
 * See SLF4J manual for more details about bindings usage.
 */
public class SLF4JSmackDebugger extends SmackDebugger  {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(SLF4JSmackDebugger.class.getName());

    public static final String LOGGER_NAME = "SMACK";
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    public static final AtomicBoolean printInterpreted = new AtomicBoolean(true);

    public static final String SENT_TAG = "SENT";
    public static final String RECEIVED_TAG = "RECV";

    private final SLF4JRawXmlListener slf4JRawXmlListener = new SLF4JRawXmlListener(logger);

    private ObservableWriter writer;
    private ObservableReader reader;

    /**
     * Makes Smack use this Debugger.
     */
    public static void enable() {
        SmackConfiguration.DEBUG = true;
        SmackConfiguration.setDefaultSmackDebuggerFactory(SLF4JDebuggerFactory.INSTANCE);
    }

    /**
     * Create new SLF4J Smack Debugger instance.
     * @param connection Smack connection to debug
     */
    SLF4JSmackDebugger(XMPPConnection connection) {
        super(connection);
        this.writer = new ObservableWriter(writer);
        this.writer.addWriterListener(slf4JRawXmlListener);
        this.reader = new ObservableReader(Validate.notNull(reader));
        this.reader.addReaderListener(slf4JRawXmlListener);

        final SLF4JLoggingConnectionListener loggingConnectionListener = new SLF4JLoggingConnectionListener(connection, logger);
        this.connection.addConnectionListener(loggingConnectionListener);

        if (connection instanceof AbstractXMPPConnection) {
            AbstractXMPPConnection abstractXmppConnection = (AbstractXMPPConnection) connection;
            ReconnectionManager.getInstanceFor(abstractXmppConnection).addReconnectionListener(loggingConnectionListener);
        } else {
            LOGGER.info("The connection instance " + connection
                            + " is not an instance of AbstractXMPPConnection, thus we can not install the ReconnectionListener");
        }
    }

    @Override
    public void outgoingStreamSink(CharSequence outgoingCharSequence) {
        slf4JRawXmlListener.write(outgoingCharSequence.toString());
    }

    @Override
    public void incomingStreamSink(CharSequence incomingCharSequence) {
        slf4JRawXmlListener.read(incomingCharSequence.toString());
    }

    @Override
    public void userHasLogged(EntityFullJid user) {
        if (logger.isDebugEnabled()) {
            logger.debug("({}) User logged in {}", connection.hashCode(), user.toString());
        }
    }

    @Override
    public void onIncomingStreamElement(TopLevelStreamElement streamElement) {
        if (SLF4JSmackDebugger.printInterpreted.get() && logger.isDebugEnabled()) {
            logger.debug("IN {}: {}", streamElement.getClass().getName(), streamElement.toXML(null));
        }
    }

    @Override
    public void onOutgoingStreamElement(TopLevelStreamElement streamElement) {
        if (SLF4JSmackDebugger.printInterpreted.get() && logger.isDebugEnabled()) {
            logger.debug("OUT {}: {}", streamElement.getClass().getName(), streamElement.toXML(null));
        }
    }

}
