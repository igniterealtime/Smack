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

package org.jivesoftware.smackx.debugger;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.provider.ProviderManager;

/**
 * The EnhancedDebuggerWindow is the main debug window that will show all the EnhancedDebuggers. 
 * For each connection to debug there will be an EnhancedDebugger that will be shown in the 
 * EnhancedDebuggerWindow.<p>
 * 
 * This class also provides information about Smack like for example the Smack version and the
 * installed providers. 
 * 
 * @author Gaston Dombiak
 */
class EnhancedDebuggerWindow {

    private static EnhancedDebuggerWindow instance;

    private static ImageIcon connectionCreatedIcon;
    private static ImageIcon connectionActiveIcon;
    private static ImageIcon connectionClosedIcon;
    private static ImageIcon connectionClosedOnErrorIcon;
    
    {
        URL url;
        
        url = ClassLoader.getSystemClassLoader().getResource("images/trafficlight_off.png");
        if (url != null) {
            connectionCreatedIcon = new ImageIcon(url);
        }            
        url = ClassLoader.getSystemClassLoader().getResource("images/trafficlight_green.png");
        if (url != null) {
            connectionActiveIcon = new ImageIcon(url);
        }
        url = ClassLoader.getSystemClassLoader().getResource("images/trafficlight_red.png");
        if (url != null) {
            connectionClosedIcon = new ImageIcon(url);
        }
        url = ClassLoader.getSystemClassLoader().getResource("images/warning.png");
        if (url != null) {
            connectionClosedOnErrorIcon = new ImageIcon(url);
        }

    }

    private JFrame frame = null;
    private JTabbedPane tabbedPane = null;

    private EnhancedDebuggerWindow() {
    }

    /**
     * Returns the unique EnhancedDebuggerWindow instance available in the system. 
     * 
     * @return the unique EnhancedDebuggerWindow instance 
     */
    private static EnhancedDebuggerWindow getInstance() {
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
    }

    /**
     * Notification that a user has logged in to the server. A new title will be set 
     * to the tab of the given debugger. 
     * 
     * @param debugger the debugger whose connection logged in to the server
     * @param user the user@host/resource that has just logged in
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
     * @param e the exception.
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

    /**
     * Creates the main debug window that provides information about Smack and also shows
     * a tab panel for each connection that is being debugged. 
     */
    private void createDebug() {

        frame = new JFrame("Smack Debug Window");

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
        JFormattedTextField field = new JFormattedTextField(SmackConfiguration.getVersionNumber());
        field.setEditable(false);
        field.setBorder(null);
        versionPanel.add(field);
        informationPanel.add(versionPanel);
        //informationPanel.add(Box.createVerticalGlue());

        // Add the list of installed IQ Providers
        Vector providers = new Vector();
        for (Iterator it = ProviderManager.getIQProviders(); it.hasNext();) {
            Object provider = it.next();
            providers.add(
                (provider.getClass() == Class.class ? provider : provider.getClass().getName()));
        }
        JList list = new JList(providers);
        list.setBorder(BorderFactory.createTitledBorder("Installed IQ Providers"));
        informationPanel.add(new JScrollPane(list));

        // Add the list of installed Extension Providers
        providers = new Vector();
        for (Iterator it = ProviderManager.getExtensionProviders(); it.hasNext();) {
            Object provider = it.next();
            providers.add(
                (provider.getClass() == Class.class ? provider : provider.getClass().getName()));
        }
        list = new JList(providers);
        list.setBorder(BorderFactory.createTitledBorder("Installed Extension Providers"));
        informationPanel.add(new JScrollPane(list));

        tabbedPane.add("Smack Info", informationPanel);

        // Add pop-up menu.
        JPopupMenu menu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Close");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Remove the selected tab pane if it's not the Smack info pane
                if (tabbedPane.getSelectedIndex() < tabbedPane.getComponentCount() - 1) {
                    tabbedPane.remove(tabbedPane.getSelectedComponent());
                    frame.setTitle(
                        "Smack Debug Window -- Total connections: "
                            + (tabbedPane.getComponentCount() - 1));
                }
            }
        });
        menu.add(menuItem);
        // Add listener to the text area so the popup menu can come up.
        tabbedPane.addMouseListener(new PopupListener(menu));

        frame.getContentPane().add(tabbedPane);

        frame.setSize(650, 400);
        frame.setVisible(true);

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
}
