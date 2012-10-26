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

package org.jivesoftware.smackx.debugger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.provider.ProviderManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

/**
 * The EnhancedDebuggerWindow is the main debug window that will show all the EnhancedDebuggers.
 * For each connection to debug there will be an EnhancedDebugger that will be shown in the
 * EnhancedDebuggerWindow.<p>
 * <p/>
 * This class also provides information about Smack like for example the Smack version and the
 * installed providers.
 *
 * @author Gaston Dombiak
 */
public class EnhancedDebuggerWindow {

    private static EnhancedDebuggerWindow instance;

    private static ImageIcon connectionCreatedIcon;
    private static ImageIcon connectionActiveIcon;
    private static ImageIcon connectionClosedIcon;
    private static ImageIcon connectionClosedOnErrorIcon;

    public static boolean PERSISTED_DEBUGGER = false;
    /**
     * Keeps the max number of rows to keep in the tables. A value less than 0 means that packets
     * will never be removed. If you are planning to use this debugger in a
     * production environment then you should set a lower value (e.g. 50) to prevent the debugger
     * from consuming all the JVM memory.
     */
    public static int MAX_TABLE_ROWS = 150;

    {
        URL url;

        url =
                Thread.currentThread().getContextClassLoader().getResource(
                        "images/trafficlight_off.png");
        if (url != null) {
            connectionCreatedIcon = new ImageIcon(url);
        }
        url =
                Thread.currentThread().getContextClassLoader().getResource(
                        "images/trafficlight_green.png");
        if (url != null) {
            connectionActiveIcon = new ImageIcon(url);
        }
        url =
                Thread.currentThread().getContextClassLoader().getResource(
                        "images/trafficlight_red.png");
        if (url != null) {
            connectionClosedIcon = new ImageIcon(url);
        }
        url = Thread.currentThread().getContextClassLoader().getResource("images/warning.png");
        if (url != null) {
            connectionClosedOnErrorIcon = new ImageIcon(url);
        }

    }

    private JFrame frame = null;
    private JTabbedPane tabbedPane = null;
    private java.util.List<EnhancedDebugger> debuggers = new ArrayList<EnhancedDebugger>();

    private EnhancedDebuggerWindow() {
    }

    /**
     * Returns the unique EnhancedDebuggerWindow instance available in the system.
     *
     * @return the unique EnhancedDebuggerWindow instance
     */
    public static EnhancedDebuggerWindow getInstance() {
        if (instance == null) {
            instance = new EnhancedDebuggerWindow();
        }
        return instance;
    }

    /**
     * Adds the new specified debugger to the list of debuggers to show in the main window.
     *
     * @param debugger the new debugger to show in the debug window
     */
    synchronized static void addDebugger(EnhancedDebugger debugger) {
        getInstance().showNewDebugger(debugger);
    }

    /**
     * Shows the new debugger in the debug window.
     *
     * @param debugger the new debugger to show
     */
    private void showNewDebugger(EnhancedDebugger debugger) {
        if (frame == null) {
            createDebug();
        }
        debugger.tabbedPane.setName("Connection_" + tabbedPane.getComponentCount());
        tabbedPane.add(debugger.tabbedPane, tabbedPane.getComponentCount() - 1);
        tabbedPane.setIconAt(tabbedPane.indexOfComponent(debugger.tabbedPane), connectionCreatedIcon);
        frame.setTitle(
                "Smack Debug Window -- Total connections: " + (tabbedPane.getComponentCount() - 1));
        // Keep the added debugger for later access
        debuggers.add(debugger);
    }

    /**
     * Notification that a user has logged in to the server. A new title will be set
     * to the tab of the given debugger.
     *
     * @param debugger the debugger whose connection logged in to the server
     * @param user     the user@host/resource that has just logged in
     */
    synchronized static void userHasLogged(EnhancedDebugger debugger, String user) {
        int index = getInstance().tabbedPane.indexOfComponent(debugger.tabbedPane);
        getInstance().tabbedPane.setTitleAt(
                index,
                user);
        getInstance().tabbedPane.setIconAt(
                index,
                connectionActiveIcon);
    }

    /**
     * Notification that the connection was properly closed.
     *
     * @param debugger the debugger whose connection was properly closed.
     */
    synchronized static void connectionClosed(EnhancedDebugger debugger) {
        getInstance().tabbedPane.setIconAt(
                getInstance().tabbedPane.indexOfComponent(debugger.tabbedPane),
                connectionClosedIcon);
    }

    /**
     * Notification that the connection was closed due to an exception.
     *
     * @param debugger the debugger whose connection was closed due to an exception.
     * @param e        the exception.
     */
    synchronized static void connectionClosedOnError(EnhancedDebugger debugger, Exception e) {
        int index = getInstance().tabbedPane.indexOfComponent(debugger.tabbedPane);
        getInstance().tabbedPane.setToolTipTextAt(
                index,
                "Connection closed due to the exception: " + e.getMessage());
        getInstance().tabbedPane.setIconAt(
                index,
                connectionClosedOnErrorIcon);
    }

    synchronized static void connectionEstablished(EnhancedDebugger debugger) {
        getInstance().tabbedPane.setIconAt(
                getInstance().tabbedPane.indexOfComponent(debugger.tabbedPane),
                connectionActiveIcon);
    }
    
    /**
     * Creates the main debug window that provides information about Smack and also shows
     * a tab panel for each connection that is being debugged.
     */
    private void createDebug() {

        frame = new JFrame("Smack Debug Window");

        if (!PERSISTED_DEBUGGER) {
            // Add listener for window closing event
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    rootWindowClosing(evt);
                }
            });
        }

        // We'll arrange the UI into tabs. The last tab contains Smack's information.
        // All the connection debugger tabs will be shown before the Smack info tab. 
        tabbedPane = new JTabbedPane();

        // Create the Smack info panel 
        JPanel informationPanel = new JPanel();
        informationPanel.setLayout(new BoxLayout(informationPanel, BoxLayout.Y_AXIS));

        // Add the Smack version label
        JPanel versionPanel = new JPanel();
        versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.X_AXIS));
        versionPanel.setMaximumSize(new Dimension(2000, 31));
        versionPanel.add(new JLabel(" Smack version: "));
        JFormattedTextField field = new JFormattedTextField(SmackConfiguration.getVersion());
        field.setEditable(false);
        field.setBorder(null);
        versionPanel.add(field);
        informationPanel.add(versionPanel);

        // Add the list of installed IQ Providers
        JPanel iqProvidersPanel = new JPanel();
        iqProvidersPanel.setLayout(new GridLayout(1, 1));
        iqProvidersPanel.setBorder(BorderFactory.createTitledBorder("Installed IQ Providers"));
        Vector<String> providers = new Vector<String>();
        for (Object provider : ProviderManager.getInstance().getIQProviders()) {
            if (provider.getClass() == Class.class) {
                providers.add(((Class<?>) provider).getName());
            }
            else {
                providers.add(provider.getClass().getName());
            }
        }
        // Sort the collection of providers
        Collections.sort(providers);
        JList list = new JList(providers);
        iqProvidersPanel.add(new JScrollPane(list));
        informationPanel.add(iqProvidersPanel);

        // Add the list of installed Extension Providers
        JPanel extensionProvidersPanel = new JPanel();
        extensionProvidersPanel.setLayout(new GridLayout(1, 1));
        extensionProvidersPanel.setBorder(BorderFactory.createTitledBorder("Installed Extension Providers"));
        providers = new Vector<String>();
        for (Object provider : ProviderManager.getInstance().getExtensionProviders()) {
            if (provider.getClass() == Class.class) {
                providers.add(((Class<?>) provider).getName());
            }
            else {
                providers.add(provider.getClass().getName());
            }
        }
        // Sort the collection of providers
        Collections.sort(providers);
        list = new JList(providers);
        extensionProvidersPanel.add(new JScrollPane(list));
        informationPanel.add(extensionProvidersPanel);

        tabbedPane.add("Smack Info", informationPanel);

        // Add pop-up menu.
        JPopupMenu menu = new JPopupMenu();
        // Add a menu item that allows to close the current selected tab
        JMenuItem menuItem = new JMenuItem("Close");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Remove the selected tab pane if it's not the Smack info pane
                if (tabbedPane.getSelectedIndex() < tabbedPane.getComponentCount() - 1) {
                    int index = tabbedPane.getSelectedIndex();
                    // Notify to the debugger to stop debugging
                    EnhancedDebugger debugger = debuggers.get(index);
                    debugger.cancel();
                    // Remove the debugger from the root window
                    tabbedPane.remove(debugger.tabbedPane);
                    debuggers.remove(debugger);
                    // Update the root window title
                    frame.setTitle(
                            "Smack Debug Window -- Total connections: "
                                    + (tabbedPane.getComponentCount() - 1));
                }
            }
        });
        menu.add(menuItem);
        // Add a menu item that allows to close all the tabs that have their connections closed
        menuItem = new JMenuItem("Close All Not Active");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<EnhancedDebugger> debuggersToRemove = new ArrayList<EnhancedDebugger>();
                // Remove all the debuggers of which their connections are no longer valid
                for (int index = 0; index < tabbedPane.getComponentCount() - 1; index++) {
                    EnhancedDebugger debugger = debuggers.get(index);
                    if (!debugger.isConnectionActive()) {
                        // Notify to the debugger to stop debugging
                        debugger.cancel();
                        debuggersToRemove.add(debugger);
                    }
                }
                for (EnhancedDebugger debugger : debuggersToRemove) {
                    // Remove the debugger from the root window
                    tabbedPane.remove(debugger.tabbedPane);
                    debuggers.remove(debugger);
                }
                // Update the root window title
                frame.setTitle(
                        "Smack Debug Window -- Total connections: "
                                + (tabbedPane.getComponentCount() - 1));
            }
        });
        menu.add(menuItem);
        // Add listener to the text area so the popup menu can come up.
        tabbedPane.addMouseListener(new PopupListener(menu));

        frame.getContentPane().add(tabbedPane);

        frame.setSize(650, 400);

        if (!PERSISTED_DEBUGGER) {
            frame.setVisible(true);
        }
    }

    /**
     * Notification that the root window is closing. Stop listening for received and
     * transmitted packets in all the debugged connections.
     *
     * @param evt the event that indicates that the root window is closing
     */
    public void rootWindowClosing(WindowEvent evt) {
        // Notify to all the debuggers to stop debugging
        for (EnhancedDebugger debugger : debuggers) {
            debugger.cancel();
        }
        // Release any reference to the debuggers
        debuggers.removeAll(debuggers);
        // Release the default instance
        instance = null;
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

    public void setVisible(boolean visible) {
        if (frame != null) {
            frame.setVisible(visible);
        }
    }

    public boolean isVisible() {
        return frame != null && frame.isVisible();
    }
}
