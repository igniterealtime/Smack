/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.debugger.android;

import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.*;

import android.util.Log;

import java.io.Reader;
import java.io.Writer;

/**
 * Very simple debugger that prints to the android log the sent and received stanzas. Use
 * this debugger with caution since printing to the console is an expensive operation that may
 * even block the thread since only one thread may print at a time.<p>
 * <p/>
 * It is possible to not only print the raw sent and received stanzas but also the interpreted
 * packets by Smack. By default interpreted packets won't be printed. To enable this feature
 * just change the <tt>printInterpreted</tt> static variable to <tt>true</tt>.
 *
 */
public class AndroidDebugger implements SmackDebugger {

    public static boolean printInterpreted = false;

    private XMPPConnection connection = null;

    private PacketListener listener = null;
    private ConnectionListener connListener = null;

    private Writer writer;
    private Reader reader;
    private ReaderListener readerListener;
    private WriterListener writerListener;

    public AndroidDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        this.connection = connection;
        this.writer = writer;
        this.reader = reader;
        createDebug();
    }

    /**
     * Creates the listeners that will print in the console when new activity is detected.
     */
    private void createDebug() {
        // Create a special Reader that wraps the main Reader and logs data to the GUI.
        ObservableReader debugReader = new ObservableReader(reader);
        readerListener = new ReaderListener() {
            public void read(String str) {
                Log.d("SMACK", "RCV (" + connection.getConnectionCounter() +
                        "): " + str);
            }
        };
        debugReader.addReaderListener(readerListener);

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        ObservableWriter debugWriter = new ObservableWriter(writer);
        writerListener = new WriterListener() {
            public void write(String str) {
                Log.d("SMACK", "SENT (" + connection.getConnectionCounter() +
                        "): " + str);
            }
        };
        debugWriter.addWriterListener(writerListener);

        // Assign the reader/writer objects to use the debug versions. The packet reader
        // and writer will use the debug versions when they are created.
        reader = debugReader;
        writer = debugWriter;

        // Create a thread that will listen for all incoming packets and write them to
        // the GUI. This is what we call "interpreted" packet data, since it's the packet
        // data as Smack sees it and not as it's coming in as raw XML.
        listener = new PacketListener() {
            public void processPacket(Packet packet) {
                if (printInterpreted) {
                    Log.d("SMACK", "RCV PKT (" + connection.getConnectionCounter() +
                            "): " + packet.toXML());
                }
            }
        };

        connListener = new AbstractConnectionListener() {
            public void connectionClosed() {
                Log.d("SMACK", "Connection closed (" + connection.getConnectionCounter() + ")");
            }

            public void connectionClosedOnError(Exception e) {
                Log.d("SMACK", "Connection closed due to an exception (" +
                        connection.getConnectionCounter() + ")");
            }
            public void reconnectionFailed(Exception e) {
                Log.d("SMACK", "Reconnection failed due to an exception (" +
                        connection.getConnectionCounter() + ")");
            }
            public void reconnectionSuccessful() {
                Log.d("SMACK", "Connection reconnected (" +
                        connection.getConnectionCounter() + ")");
            }
            public void reconnectingIn(int seconds) {
                Log.d("SMACK", "Connection (" + connection.getConnectionCounter() +
                        ") will reconnect in " + seconds);
            }
        };
    }

    public Reader newConnectionReader(Reader newReader) {
        ((ObservableReader)reader).removeReaderListener(readerListener);
        ObservableReader debugReader = new ObservableReader(newReader);
        debugReader.addReaderListener(readerListener);
        reader = debugReader;
        return reader;
    }

    public Writer newConnectionWriter(Writer newWriter) {
        ((ObservableWriter)writer).removeWriterListener(writerListener);
        ObservableWriter debugWriter = new ObservableWriter(newWriter);
        debugWriter.addWriterListener(writerListener);
        writer = debugWriter;
        return writer;
    }

    public void userHasLogged(String user) {
        String title =
                "User logged (" + connection.getConnectionCounter() + "): "
                + user
                + "@"
                + connection.getServiceName()
                + ":"
                + connection.getPort();
        Log.d("SMACK", title);
        // Add the connection listener to the connection so that the debugger can be notified
        // whenever the connection is closed.
        connection.addConnectionListener(connListener);
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public PacketListener getReaderListener() {
        return listener;
    }

    public PacketListener getWriterListener() {
        return null;
    }
}

