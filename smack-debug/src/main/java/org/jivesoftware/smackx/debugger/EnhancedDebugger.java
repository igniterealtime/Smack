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

package org.jivesoftware.smackx.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;
import org.jivesoftware.smack.util.ReaderListener;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.WriterListener;
import org.jivesoftware.smack.util.XmlUtil;

import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

/**
 * The EnhancedDebugger is a debugger that allows to debug sent, received and interpreted messages
 * but also provides the ability to send ad-hoc messages composed by the user.
 * <p>
 * A new EnhancedDebugger will be created for each connection to debug. All the EnhancedDebuggers
 * will be shown in the same debug window provided by the class EnhancedDebuggerWindow.
 * </p>
 *
 * @author Gaston Dombiak
 */
public class EnhancedDebugger extends SmackDebugger {

    private static final Logger LOGGER = Logger.getLogger(EnhancedDebugger.class.getName());

    private static final String NEWLINE = "\n";

    private static ImageIcon packetReceivedIcon;
    private static ImageIcon packetSentIcon;
    private static ImageIcon presencePacketIcon;
    private static ImageIcon iqPacketIcon;
    private static ImageIcon messagePacketIcon;
    private static ImageIcon unknownPacketTypeIcon;

    {
        URL url;
        // Load the image icons
        url =
                Thread.currentThread().getContextClassLoader().getResource("images/nav_left_blue.png");
        if (url != null) {
            packetReceivedIcon = new ImageIcon(url);
        }
        url =
                Thread.currentThread().getContextClassLoader().getResource("images/nav_right_red.png");
        if (url != null) {
            packetSentIcon = new ImageIcon(url);
        }
        url =
                Thread.currentThread().getContextClassLoader().getResource("images/photo_portrait.png");
        if (url != null) {
            presencePacketIcon = new ImageIcon(url);
        }
        url =
                Thread.currentThread().getContextClassLoader().getResource(
                        "images/question_and_answer.png");
        if (url != null) {
            iqPacketIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/message.png");
        if (url != null) {
            messagePacketIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/unknown.png");
        if (url != null) {
            unknownPacketTypeIcon = new ImageIcon(url);
        }
    }

    private DefaultTableModel messagesTable = null;
    private JTextArea messageTextArea = null;
    private JFormattedTextField userField = null;
    private JFormattedTextField statusField = null;

    private ConnectionListener connListener = null;
    private final ReconnectionListener reconnectionListener;

    private Writer writer;
    private Reader reader;
    private ReaderListener readerListener;
    private WriterListener writerListener;

    private Date creationTime = new Date();

    // Statistics variables
    private DefaultTableModel statisticsTable = null;
    private int sentPackets = 0;
    private int receivedPackets = 0;
    private int sentIQPackets = 0;
    private int receivedIQPackets = 0;
    private int sentMessagePackets = 0;
    private int receivedMessagePackets = 0;
    private int sentPresencePackets = 0;
    private int receivedPresencePackets = 0;
    private int sentOtherPackets = 0;
    private int receivedOtherPackets = 0;

    JTabbedPane tabbedPane;

    public EnhancedDebugger(XMPPConnection connection) {
        super(connection);

        reconnectionListener = new ReconnectionListener() {
            @Override
            public void reconnectingIn(final int seconds) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setValue("Attempt to reconnect in " + seconds + " seconds");
                    }
                });
            }

            @Override
            public void reconnectionFailed(Exception e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setValue("Reconnection failed");
                    }
                });
            }
        };

        if (connection instanceof AbstractXMPPConnection) {
            AbstractXMPPConnection abstractXmppConnection = (AbstractXMPPConnection) connection;
            ReconnectionManager.getInstanceFor(abstractXmppConnection).addReconnectionListener(reconnectionListener);
        } else {
            LOGGER.info("The connection instance " + connection
                            + " is not an instance of AbstractXMPPConnection, thus we can not install the ReconnectionListener");
        }

        // We'll arrange the UI into six tabs. The first tab contains all data, the second
        // client generated XML, the third server generated XML, the fourth allows to send
        // ad-hoc messages and the fifth contains connection information.
        tabbedPane = new JTabbedPane();

        // Add the All Packets, Sent, Received and Interpreted panels
        addBasicPanels();

        // Add the panel to send ad-hoc messages
        addAdhocPacketPanel();

        // Add the connection information panel
        addInformationPanel();

        // Create a thread that will listen for any connection closed event
        connListener = new ConnectionListener() {
            @Override
            public void connectionClosed() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setValue("Closed");
                        EnhancedDebuggerWindow.connectionClosed(EnhancedDebugger.this);
                    }
                });

            }

            @Override
            public void connectionClosedOnError(final Exception e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        statusField.setValue("Closed due to an exception");
                        EnhancedDebuggerWindow.connectionClosedOnError(EnhancedDebugger.this, e);
                    }
                });

            }
        };

        EnhancedDebuggerWindow.addDebugger(this);
    }

    private void addBasicPanels() {
        JSplitPane allPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        allPane.setOneTouchExpandable(true);

        messagesTable =
                new DefaultTableModel(
                        new Object[] {"Hide", "Timestamp", "", "", "Message", "Id", "Type", "To", "From"},
                        0) {
                    private static final long serialVersionUID = 8136121224474217264L;
                    @Override
                    public boolean isCellEditable(int rowIndex, int mColIndex) {
                        return false;
                    }

                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        if (columnIndex == 2 || columnIndex == 3) {
                            return Icon.class;
                        }
                        return super.getColumnClass(columnIndex);
                    }

                };
        JTable table = new JTable(messagesTable);
        // Allow only single a selection
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Hide the first column
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMaxWidth(0);
        table.getTableHeader().getColumnModel().getColumn(0).setMinWidth(0);
        // Set the column "timestamp" size
        table.getColumnModel().getColumn(1).setMaxWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        // Set the column "direction" icon size
        table.getColumnModel().getColumn(2).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(30);
        // Set the column "packet type" icon size
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(30);
        // Set the column "Id" size
        table.getColumnModel().getColumn(5).setMaxWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(55);
        // Set the column "type" size
        table.getColumnModel().getColumn(6).setMaxWidth(200);
        table.getColumnModel().getColumn(6).setPreferredWidth(50);
        // Set the column "to" size
        table.getColumnModel().getColumn(7).setMaxWidth(300);
        table.getColumnModel().getColumn(7).setPreferredWidth(90);
        // Set the column "from" size
        table.getColumnModel().getColumn(8).setMaxWidth(300);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);
        // Create a table listener that listen for row selection events
        SelectionListener selectionListener = new SelectionListener(table);
        table.getSelectionModel().addListSelectionListener(selectionListener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
        allPane.setTopComponent(new JScrollPane(table));
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        // Add pop-up menu.
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(messageTextArea.getText()), null);
            }
        });
        menu.add(menuItem1);
        // Add listener to the text area so the popup menu can come up.
        messageTextArea.addMouseListener(new PopupListener(menu));
        JPanel sublayout = new JPanel(new BorderLayout());
        sublayout.add(new JScrollPane(messageTextArea), BorderLayout.CENTER);

        JButton clearb = new JButton("Clear All Packets");

        clearb.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -8576045822764763613L;

            @Override
            public void actionPerformed(ActionEvent e) {
                messagesTable.setRowCount(0);
            }
        });

        sublayout.add(clearb, BorderLayout.NORTH);
        allPane.setBottomComponent(sublayout);

        allPane.setDividerLocation(150);

        tabbedPane.add("All Packets", allPane);
        tabbedPane.setToolTipTextAt(0, "Sent and received packets processed by Smack");

        // Create UI elements for client generated XML traffic.
        final JTextArea sentText = new JTextArea();
        sentText.setWrapStyleWord(true);
        sentText.setLineWrap(true);
        sentText.setEditable(false);
        sentText.setForeground(new Color(112, 3, 3));
        tabbedPane.add("Raw Sent Packets", new JScrollPane(sentText));
        tabbedPane.setToolTipTextAt(1, "Raw text of the sent packets");

        // Add pop-up menu.
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(sentText.getText()), null);
            }
        });

        JMenuItem menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sentText.setText("");
            }
        });

        // Add listener to the text area so the popup menu can come up.
        sentText.addMouseListener(new PopupListener(menu));
        menu.add(menuItem1);
        menu.add(menuItem2);

        // Create UI elements for server generated XML traffic.
        final JTextArea receivedText = new JTextArea();
        receivedText.setWrapStyleWord(true);
        receivedText.setLineWrap(true);
        receivedText.setEditable(false);
        receivedText.setForeground(new Color(6, 76, 133));
        tabbedPane.add("Raw Received Packets", new JScrollPane(receivedText));
        tabbedPane.setToolTipTextAt(
                2,
                "Raw text of the received packets before Smack process them");

        // Add pop-up menu.
        menu = new JPopupMenu();
        menuItem1 = new JMenuItem("Copy");
        menuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the clipboard
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                // Set the sent text as the new content of the clipboard
                clipboard.setContents(new StringSelection(receivedText.getText()), null);
            }
        });

        menuItem2 = new JMenuItem("Clear");
        menuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                receivedText.setText("");
            }
        });

        // Add listener to the text area so the popup menu can come up.
        receivedText.addMouseListener(new PopupListener(menu));
        menu.add(menuItem1);
        menu.add(menuItem2);

        // Create a special Reader that wraps the main Reader and logs data to the GUI.
        ObservableReader debugReader = new ObservableReader(reader);
        readerListener = new ReaderListener() {
            private final PriorityBlockingQueue<String> buffer = new PriorityBlockingQueue<>();

            @Override
            public void read(final String string) {
                addBatched(string, buffer, receivedText);
            }
        };
        debugReader.addReaderListener(readerListener);

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        ObservableWriter debugWriter = new ObservableWriter(writer);
        writerListener = new WriterListener() {
            private final PriorityBlockingQueue<String> buffer = new PriorityBlockingQueue<>();

            @Override
            public void write(final String string) {
                addBatched(string, buffer, sentText);
            }
        };
        debugWriter.addWriterListener(writerListener);

        // Assign the reader/writer objects to use the debug versions. The packet reader
        // and writer will use the debug versions when they are created.
        reader = debugReader;
        writer = debugWriter;

    }

    private static void addBatched(String string, PriorityBlockingQueue<String> buffer, JTextArea jTextArea) {
        buffer.add(string);

        SwingUtilities.invokeLater(() -> {
            List<String> linesToAdd = new ArrayList<>();
            String data;
            Instant start = Instant.now();
            try {
                // To reduce overhead/increase performance, try to process up to a certain amount of lines at the
                // same time, when they arrive in rapid succession.
                while (linesToAdd.size() < 50
                                && Duration.between(start, Instant.now()).compareTo(Duration.ofMillis(100)) < 0
                                && (data = buffer.poll(10, TimeUnit.MILLISECONDS)) != null) {
                    linesToAdd.add(data);
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.FINER, "Interrupted wait-for-poll in addBatched(). Process all data now.", e);
            }

            if (linesToAdd.isEmpty()) {
                return;
            }

            if (EnhancedDebuggerWindow.PERSISTED_DEBUGGER && !EnhancedDebuggerWindow.getInstance().isVisible()) {
                // Do not add content if the parent is not visible
                return;
            }

            // Delete lines from the top, if lines to be added will exceed the maximum.
            int linesToDelete = jTextArea.getLineCount() + linesToAdd.size() - EnhancedDebuggerWindow.MAX_TABLE_ROWS;
            if (linesToDelete > 0) {
                try {
                    jTextArea.replaceRange("", 0, jTextArea.getLineEndOffset(linesToDelete - 1));
                } catch (BadLocationException e) {
                    LOGGER.log(Level.SEVERE, "Error with line offset, MAX_TABLE_ROWS is set too low: "
                                    + EnhancedDebuggerWindow.MAX_TABLE_ROWS, e);
                }
            }

            // Add the new content.
            jTextArea.append(String.join(NEWLINE, linesToAdd));
        });
    }

    private void addAdhocPacketPanel() {
        // Create UI elements for sending ad-hoc messages.
        final JTextArea adhocMessages = new JTextArea();
        adhocMessages.setEditable(true);
        adhocMessages.setForeground(new Color(1, 94, 35));
        tabbedPane.add("Ad-hoc message", new JScrollPane(adhocMessages));
        tabbedPane.setToolTipTextAt(3, "Panel that allows you to send adhoc packets");

        // Add pop-up menu.
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Message");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(
                        "<message to=\"\" id=\""
                                + StringUtils.randomString(5)
                                + "-X\"><body></body></message>");
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("IQ Get");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(
                        "<iq type=\"get\" to=\"\" id=\""
                                + StringUtils.randomString(5)
                                + "-X\"><query xmlns=\"\"></query></iq>");
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("IQ Set");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(
                        "<iq type=\"set\" to=\"\" id=\""
                                + StringUtils.randomString(5)
                                + "-X\"><query xmlns=\"\"></query></iq>");
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Presence");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(
                        "<presence to=\"\" id=\"" + StringUtils.randomString(5) + "-X\"/>");
            }
        });
        menu.add(menuItem);
        menu.addSeparator();

        menuItem = new JMenuItem("Send");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!"".equals(adhocMessages.getText())) {
                    AdHocPacket packetToSend = new AdHocPacket(adhocMessages.getText());
                    try {
                        connection.sendStanza(packetToSend);
                    }
                    catch (InterruptedException | NotConnectedException e1) {
                        LOGGER.log(Level.WARNING, "exception", e);
                    }
                }
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Clear");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adhocMessages.setText(null);
            }
        });
        menu.add(menuItem);

        // Add listener to the text area so the popup menu can come up.
        adhocMessages.addMouseListener(new PopupListener(menu));
    }

    private void addInformationPanel() {
        // Create UI elements for connection information.
        JPanel informationPanel = new JPanel();
        informationPanel.setLayout(new BorderLayout());

        // Add the Host information
        JPanel connPanel = new JPanel();
        connPanel.setLayout(new GridBagLayout());
        connPanel.setBorder(BorderFactory.createTitledBorder("XMPPConnection information"));

        JLabel label = new JLabel("Host: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(
                label,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        JFormattedTextField field = new JFormattedTextField(connection.getXMPPServiceDomain());
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(
                field,
                new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));

        // Add the Port information
        label = new JLabel("Port: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(
                label,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        field = new JFormattedTextField(connection.getPort());
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(
                field,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));

        // Add the connection's User information
        label = new JLabel("User: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(
                label,
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        userField = new JFormattedTextField();
        userField.setMinimumSize(new java.awt.Dimension(150, 20));
        userField.setMaximumSize(new java.awt.Dimension(150, 20));
        userField.setEditable(false);
        userField.setBorder(null);
        connPanel.add(
                userField,
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));

        // Add the connection's creationTime information
        label = new JLabel("Creation time: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(
                label,
                new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        field = new JFormattedTextField(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SS"));
        field.setMinimumSize(new java.awt.Dimension(150, 20));
        field.setMaximumSize(new java.awt.Dimension(150, 20));
        field.setValue(creationTime);
        field.setEditable(false);
        field.setBorder(null);
        connPanel.add(
                field,
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));

        // Add the connection's creationTime information
        label = new JLabel("Status: ");
        label.setMinimumSize(new java.awt.Dimension(150, 14));
        label.setMaximumSize(new java.awt.Dimension(150, 14));
        connPanel.add(
                label,
                new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 21, 0, new Insets(0, 0, 0, 0), 0, 0));
        statusField = new JFormattedTextField();
        statusField.setMinimumSize(new java.awt.Dimension(150, 20));
        statusField.setMaximumSize(new java.awt.Dimension(150, 20));
        statusField.setValue("Active");
        statusField.setEditable(false);
        statusField.setBorder(null);
        connPanel.add(
                statusField,
                new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        // Add the connection panel to the information panel
        informationPanel.add(connPanel, BorderLayout.NORTH);

        // Add the Number of sent packets information
        JPanel packetsPanel = new JPanel();
        packetsPanel.setLayout(new GridLayout(1, 1));
        packetsPanel.setBorder(BorderFactory.createTitledBorder("Transmitted Packets"));

        statisticsTable =
                new DefaultTableModel(new Object[][] { {"IQ", 0, 0}, {"Message", 0, 0},
                        {"Presence", 0, 0}, {"Other", 0, 0}, {"Total", 0, 0}},
                        new Object[] {"Type", "Received", "Sent"}) {
                    private static final long serialVersionUID = -6793886085109589269L;
                    @Override
                    public boolean isCellEditable(int rowIndex, int mColIndex) {
                        return false;
                    }
                };
        JTable table = new JTable(statisticsTable);
        // Allow only single a selection
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        packetsPanel.add(new JScrollPane(table));

        // Add the packets panel to the information panel
        informationPanel.add(packetsPanel, BorderLayout.CENTER);

        tabbedPane.add("Information", new JScrollPane(informationPanel));
        tabbedPane.setToolTipTextAt(4, "Information and statistics about the debugged connection");
    }

    @Override
    public final void outgoingStreamSink(CharSequence outgoingCharSequence) {
        writerListener.write(outgoingCharSequence.toString());
    }

    @Override
    public final void incomingStreamSink(CharSequence incomingCharSequence) {
        readerListener.read(incomingCharSequence.toString());
    }

    @Override
    public void userHasLogged(final EntityFullJid user) {
        final EnhancedDebugger debugger = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                userField.setText(user.toString());
                EnhancedDebuggerWindow.userHasLogged(debugger, user.toString());
                // Add the connection listener to the connection so that the debugger can be notified
                // whenever the connection is closed.
                connection.addConnectionListener(connListener);
            }
        });

    }

    /**
     * Updates the statistics table
     */
    private void updateStatistics() {
        statisticsTable.setValueAt(Integer.valueOf(receivedIQPackets), 0, 1);
        statisticsTable.setValueAt(Integer.valueOf(sentIQPackets), 0, 2);

        statisticsTable.setValueAt(Integer.valueOf(receivedMessagePackets), 1, 1);
        statisticsTable.setValueAt(Integer.valueOf(sentMessagePackets), 1, 2);

        statisticsTable.setValueAt(Integer.valueOf(receivedPresencePackets), 2, 1);
        statisticsTable.setValueAt(Integer.valueOf(sentPresencePackets), 2, 2);

        statisticsTable.setValueAt(Integer.valueOf(receivedOtherPackets), 3, 1);
        statisticsTable.setValueAt(Integer.valueOf(sentOtherPackets), 3, 2);

        statisticsTable.setValueAt(Integer.valueOf(receivedPackets), 4, 1);
        statisticsTable.setValueAt(Integer.valueOf(sentPackets), 4, 2);
    }

    /**
     * Adds the received stanza detail to the messages table.
     *
     * @param dateFormatter the SimpleDateFormat to use to format Dates
     * @param packet        the read stanza to add to the table
     */
    private void addReadPacketToTable(final SimpleDateFormat dateFormatter, final TopLevelStreamElement packet) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String messageType;
                Jid from;
                String stanzaId;
                if (packet instanceof Stanza) {
                    Stanza stanza = (Stanza) packet;
                    from = stanza.getFrom();
                    stanzaId = stanza.getStanzaId();
                } else {
                    from = null;
                    stanzaId = "(Nonza)";
                }
                String type = "";
                Icon packetTypeIcon;
                receivedPackets++;
                if (packet instanceof IQ) {
                    packetTypeIcon = iqPacketIcon;
                    messageType = "IQ Received (class=" + packet.getClass().getName() + ")";
                    type = ((IQ) packet).getType().toString();
                    receivedIQPackets++;
                }
                else if (packet instanceof Message) {
                    packetTypeIcon = messagePacketIcon;
                    messageType = "Message Received";
                    type = ((Message) packet).getType().toString();
                    receivedMessagePackets++;
                }
                else if (packet instanceof Presence) {
                    packetTypeIcon = presencePacketIcon;
                    messageType = "Presence Received";
                    type = ((Presence) packet).getType().toString();
                    receivedPresencePackets++;
                }
                else {
                    packetTypeIcon = unknownPacketTypeIcon;
                    messageType = packet.getClass().getName() + " Received";
                    receivedOtherPackets++;
                }

                // Check if we need to remove old rows from the table to keep memory consumption low
                if (EnhancedDebuggerWindow.MAX_TABLE_ROWS > 0 &&
                        messagesTable.getRowCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                    messagesTable.removeRow(0);
                }

                messagesTable.addRow(
                        new Object[] {
                                XmlUtil.prettyFormatXml(packet.toXML().toString()),
                                dateFormatter.format(new Date()),
                                packetReceivedIcon,
                                packetTypeIcon,
                                messageType,
                                stanzaId,
                                type,
                                "",
                                from});
                // Update the statistics table
                updateStatistics();
            }
        });
    }

    /**
     * Adds the sent stanza detail to the messages table.
     *
     * @param dateFormatter the SimpleDateFormat to use to format Dates
     * @param packet        the sent stanza to add to the table
     */
    private void addSentPacketToTable(final SimpleDateFormat dateFormatter, final TopLevelStreamElement packet) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String messageType;
                Jid to;
                String stanzaId;
                if (packet instanceof Stanza) {
                    Stanza stanza = (Stanza) packet;
                    to = stanza.getTo();
                    stanzaId = stanza.getStanzaId();
                } else {
                    to = null;
                    stanzaId = "(Nonza)";
                }
                String type = "";
                Icon packetTypeIcon;
                sentPackets++;
                if (packet instanceof IQ) {
                    packetTypeIcon = iqPacketIcon;
                    messageType = "IQ Sent (class=" + packet.getClass().getName() + ")";
                    type = ((IQ) packet).getType().toString();
                    sentIQPackets++;
                }
                else if (packet instanceof Message) {
                    packetTypeIcon = messagePacketIcon;
                    messageType = "Message Sent";
                    type = ((Message) packet).getType().toString();
                    sentMessagePackets++;
                }
                else if (packet instanceof Presence) {
                    packetTypeIcon = presencePacketIcon;
                    messageType = "Presence Sent";
                    type = ((Presence) packet).getType().toString();
                    sentPresencePackets++;
                }
                else {
                    packetTypeIcon = unknownPacketTypeIcon;
                    messageType = packet.getClass().getName() + " Sent";
                    sentOtherPackets++;
                }

                // Check if we need to remove old rows from the table to keep memory consumption low
                if (EnhancedDebuggerWindow.MAX_TABLE_ROWS > 0 &&
                        messagesTable.getRowCount() >= EnhancedDebuggerWindow.MAX_TABLE_ROWS) {
                    messagesTable.removeRow(0);
                }

                messagesTable.addRow(
                        new Object[] {
                                XmlUtil.prettyFormatXml(packet.toXML().toString()),
                                dateFormatter.format(new Date()),
                                packetSentIcon,
                                packetTypeIcon,
                                messageType,
                                stanzaId,
                                type,
                                to,
                                ""});

                // Update the statistics table
                updateStatistics();
            }
        });
    }

    /**
     * Returns true if the debugger's connection with the server is up and running.
     *
     * @return true if the connection with the server is active.
     */
    boolean isConnectionActive() {
        return connection.isConnected();
    }

    /**
     * Stops debugging the connection. Removes any listener on the connection.
     */
    void cancel() {
        connection.removeConnectionListener(connListener);
        ((ObservableReader) reader).removeReaderListener(readerListener);
        ((ObservableWriter) writer).removeWriterListener(writerListener);
        messagesTable = null;
    }

    /**
     * An ad-hoc stanza is like any regular stanza but with the exception that it's intention is
     * to be used only <b>to send packets</b>.<p>
     * <p/>
     * The whole text to send must be passed to the constructor. This implies that the client of
     * this class is responsible for sending a valid text to the constructor.
     */
    private static final class AdHocPacket extends Stanza {

        private final String text;

        /**
         * Create a new AdHocPacket with the text to send. The passed text must be a valid text to
         * send to the server, no validation will be done on the passed text.
         *
         * @param text the whole text of the stanza to send
         */
        private AdHocPacket(String text) {
            this.text = text;
        }

        @Override
        public String toXML(XmlEnvironment enclosingNamespace) {
            return text;
        }

        @Override
        public String toString() {
            return toXML((XmlEnvironment) null);
        }

        @Override
        public String getElementName() {
            return null;
        }
    }

    /**
     * Listens for debug window popup dialog events.
     */
    private static class PopupListener extends MouseAdapter {

        JPopupMenu popup;

        PopupListener(JPopupMenu popupMenu) {
            popup = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class SelectionListener implements ListSelectionListener {

        JTable table;

        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(JTable table) {
            this.table = table;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (table.getSelectedRow() == -1) {
                // Clear the messageTextArea since there is none packet selected
                messageTextArea.setText(null);
            }
            else {
                // Set the detail of the packet in the messageTextArea
                messageTextArea.setText(
                        (String) table.getModel().getValueAt(table.getSelectedRow(), 0));
                // Scroll up to the top
                messageTextArea.setCaretPosition(0);
            }
        }
    }

    @Override
    public void onIncomingStreamElement(final TopLevelStreamElement streamElement) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss:SS");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addReadPacketToTable(dateFormatter, streamElement);
            }
        });
    }

    @Override
    public void onOutgoingStreamElement(final TopLevelStreamElement streamElement) {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss:SS");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addSentPacketToTable(dateFormatter, streamElement);
            }
        });
    }

    public static final class Factory implements SmackDebuggerFactory {

        public static final SmackDebuggerFactory INSTANCE = new Factory();

        private Factory() {
        }

        @Override
        public SmackDebugger create(XMPPConnection connection) throws IllegalArgumentException {
            return new EnhancedDebugger(connection);
        }

    }
}
