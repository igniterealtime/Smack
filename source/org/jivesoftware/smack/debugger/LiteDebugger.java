/**
 * $RCSfile$
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

package org.jivesoftware.smack.debugger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;

/**
 * The LiteDebugger is a very simple debugger that allows to debug sent, received and 
 * interpreted messages.
 * 
 * @author Gaston Dombiak
 */
public class LiteDebugger implements SmackDebugger {

    private static final String NEWLINE = "\n";

    private JFrame frame = null;
    private Connection connection = null;

    private PacketListener listener = null;

    private Writer writer;
    private Reader reader;
    private ReaderListener readerListener;
    private WriterListener writerListener;

    public LiteDebugger(Connection connection, Writer writer, Reader reader) {
        this.connection = connection;
        this.writer = writer;
        this.reader = reader;
        createDebug();
    }

    /**
     * Creates the debug process, which is a GUI window that displays XML traffic.
     */
    private void createDebug() {
        frame = new JFrame("Smack Debug Window -- " + connection.getServiceName() + ":" +
                connection.getPort());

        // Add listener for window closing event 
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                rootWindowClosing(evt);
            }
        });

        // We'll arrange the UI into four tabs. The first tab contains all data, the second
        // client generated XML, the third server generated XML, and the fourth is packet
        // data from the server as seen by Smack.
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel allPane = new JPanel();
        allPane.setLayout(new GridLayout(3, 1));
        tabbedPane.add("All", allPane);

        // Create UI elements for client generated XML traffic.
        final JTextArea sentText1 = new JTextArea();
        final JTextArea sentText2 = new JTextArea();
        sentText1.setEditable(false);
        sentText2.setEditable(false);
        sentText1.setForeground(new Color(112, 3, 3));
        sentText2.setForeground(new Color(112, 3, 3));
        allPane.add(new JScrollPane(sentText1));
        tabbedPane.add("Sent", new JScrollPane(sentText2));

        // Add pop-up menu.
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(sentText1.getText()), null);
            }
        });

        JMenuItem menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sentText1.setText("");
                sentText2.setText("");
            }
        });

        // Add listener to the text area so the popup menu can come up.
        MouseListener popupListener = new PopupListener(menu);
        sentText1.addMouseListener(popupListener);
        sentText2.addMouseListener(popupListener);
        menu.add(menuItem1);
        menu.add(menuItem2);

        // Create UI elements for server generated XML traffic.
        final JTextArea receivedText1 = new JTextArea();
        final JTextArea receivedText2 = new JTextArea();
        receivedText1.setEditable(false);
        receivedText2.setEditable(false);
        receivedText1.setForeground(new Color(6, 76, 133));
        receivedText2.setForeground(new Color(6, 76, 133));
        allPane.add(new JScrollPane(receivedText1));
        tabbedPane.add("Received", new JScrollPane(receivedText2));

        // Add pop-up menu.
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(receivedText1.getText()), null);
            }
        });

        menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                receivedText1.setText("");
                receivedText2.setText("");
            }
        });

        // Add listener to the text area so the popup menu can come up.
        popupListener = new PopupListener(menu);
        receivedText1.addMouseListener(popupListener);
        receivedText2.addMouseListener(popupListener);
        menu.add(menuItem1);
        menu.add(menuItem2);

        // Create UI elements for interpreted XML traffic.
        final JTextArea interpretedText1 = new JTextArea();
        final JTextArea interpretedText2 = new JTextArea();
        interpretedText1.setEditable(false);
        interpretedText2.setEditable(false);
        interpretedText1.setForeground(new Color(1, 94, 35));
        interpretedText2.setForeground(new Color(1, 94, 35));
        allPane.add(new JScrollPane(interpretedText1));
        tabbedPane.add("Interpreted", new JScrollPane(interpretedText2));

        // Add pop-up menu.
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(interpretedText1.getText()), null);
            }
        });

        menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                interpretedText1.setText("");
                interpretedText2.setText("");
            }
        });

        // Add listener to the text area so the popup menu can come up.
        popupListener = new PopupListener(menu);
        interpretedText1.addMouseListener(popupListener);
        interpretedText2.addMouseListener(popupListener);
        menu.add(menuItem1);
        menu.add(menuItem2);

        frame.getContentPane().add(tabbedPane);

        frame.setSize(550, 400);
        frame.setVisible(true);

        // Create a special Reader that wraps the main Reader and logs data to the GUI.
        ObservableReader debugReader = new ObservableReader(reader);
        readerListener = new ReaderListener() {
                    public void read(String str) {
                        int index = str.lastIndexOf(">");
                        if (index != -1) {
                            receivedText1.append(str.substring(0, index + 1));
                            receivedText2.append(str.substring(0, index + 1));
                            receivedText1.append(NEWLINE);
                            receivedText2.append(NEWLINE);
                            if (str.length() > index) {
                                receivedText1.append(str.substring(index + 1));
                                receivedText2.append(str.substring(index + 1));
                            }
                        }
                        else {
                            receivedText1.append(str);
                            receivedText2.append(str);
                        }
                    }
                };
        debugReader.addReaderListener(readerListener);

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        ObservableWriter debugWriter = new ObservableWriter(writer);
        writerListener = new WriterListener() {
                    public void write(String str) {
                        sentText1.append(str);
                        sentText2.append(str);
                        if (str.endsWith(">")) {
                            sentText1.append(NEWLINE);
                            sentText2.append(NEWLINE);
                        }
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
                interpretedText1.append(packet.toXML());
                interpretedText2.append(packet.toXML());
                interpretedText1.append(NEWLINE);
                interpretedText2.append(NEWLINE);
            }
        };
    }

    /**
     * Notification that the root window is closing. Stop listening for received and 
     * transmitted packets.
     * 
     * @param evt the event that indicates that the root window is closing 
     */
    public void rootWindowClosing(WindowEvent evt) {
        connection.removePacketListener(listener);
        ((ObservableReader)reader).removeReaderListener(readerListener);
        ((ObservableWriter)writer).removeWriterListener(writerListener);
    }

    /**
     * Listens for debug window popup dialog events.
     */
    private class PopupListener extends MouseAdapter {
        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
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
        boolean isAnonymous = "".equals(StringUtils.parseName(user));
        String title =
            "Smack Debug Window -- "
                + (isAnonymous ? "" : StringUtils.parseBareAddress(user))
                + "@"
                + connection.getServiceName()
                + ":"
                + connection.getPort();
        title += "/" + StringUtils.parseResource(user);
        frame.setTitle(title);
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
