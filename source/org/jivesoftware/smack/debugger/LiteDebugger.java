/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smack.debugger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;

/**
 * The LiteDebugger is a very simple debugger that allows to debug sent, received and 
 * interpreted messages.
 * 
 * @author Gaston Dombiak
 */
public class LiteDebugger implements SmackDebugger {

    private static final String NEWLINE = "\n";

    private JFrame frame = null;
    private XMPPConnection connection = null;

    private PacketListener listener = null;

    private Writer writer;
    private Reader reader;

    public LiteDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        this.connection = connection;
        this.writer = writer;
        this.reader = reader;
        createDebug();
    }

    /**
     * Creates the debug process, which is a GUI window that displays XML traffic.
     */
    private void createDebug() {
        // Use the native look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        frame =
            new JFrame(
                "Smack Debug Window -- " + connection.getHost() + ":" + connection.getPort());

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
        Reader debugReader = new Reader() {

            Reader myReader = reader;

            public int read(char cbuf[], int off, int len) throws IOException {
                int count = myReader.read(cbuf, off, len);
                if (count > 0) {
                    String str = new String(cbuf, off, count);
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
                return count;
            }

            public void close() throws IOException {
                myReader.close();
            }

            public int read() throws IOException {
                return myReader.read();
            }

            public int read(char cbuf[]) throws IOException {
                return myReader.read(cbuf);
            }

            public long skip(long n) throws IOException {
                return myReader.skip(n);
            }

            public boolean ready() throws IOException {
                return myReader.ready();
            }

            public boolean markSupported() {
                return myReader.markSupported();
            }

            public void mark(int readAheadLimit) throws IOException {
                myReader.mark(readAheadLimit);
            }

            public void reset() throws IOException {
                myReader.reset();
            }
        };

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        Writer debugWriter = new Writer() {

            Writer myWriter = writer;

            public void write(char cbuf[], int off, int len) throws IOException {
                myWriter.write(cbuf, off, len);
                String str = new String(cbuf, off, len);
                sentText1.append(str);
                sentText2.append(str);
                if (str.endsWith(">")) {
                    sentText1.append(NEWLINE);
                    sentText2.append(NEWLINE);
                }
            }

            public void flush() throws IOException {
                myWriter.flush();
            }

            public void close() throws IOException {
                myWriter.close();
            }

            public void write(int c) throws IOException {
                myWriter.write(c);
            }

            public void write(char cbuf[]) throws IOException {
                myWriter.write(cbuf);
                String str = new String(cbuf);
                sentText1.append(str);
                sentText2.append(str);
                if (str.endsWith(">")) {
                    sentText1.append(NEWLINE);
                    sentText2.append(NEWLINE);
                }
            }

            public void write(String str) throws IOException {
                myWriter.write(str);
                sentText1.append(str);
                sentText2.append(str);
                if (str.endsWith(">")) {
                    sentText1.append(NEWLINE);
                    sentText2.append(NEWLINE);
                }
            }

            public void write(String str, int off, int len) throws IOException {
                myWriter.write(str, off, len);
                str = str.substring(off, off + len);
                sentText1.append(str);
                sentText2.append(str);
                if (str.endsWith(">")) {
                    sentText1.append(NEWLINE);
                    sentText2.append(NEWLINE);
                }
            }
        };

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

    public void userHasLogged(String user) {
        boolean isAnonymous = "".equals(StringUtils.parseName(user));
        String title =
            "Smack Debug Window -- "
                + (isAnonymous ? "" : StringUtils.parseBareAddress(user))
                + "@"
                + connection.getHost()
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
