/**
 *
 * Copyright 2015 Florian Schmaus
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

/**
 * This extension is used to send rosters, roster groups and roster entries from one XMPP Entity to another. It also
 * provides an easy way to hook up custom logic when entries are received from other XMPP clients.
 * <p>
 * Follow these links to learn how to send and receive roster items:
 * </p>
 * <ul>
 * <li>Send a complete roster</li>
 * <li>Send a roster’s group</li>
 * <li>Send a roster’s entry</li>
 * <li>Receive roster entries</li>
 * </ul>
 * <p>
 * <strong>XEP related:</strong> <a href="http://www.xmpp.org/extensions/xep-0093.html">XEP-93</a>
 * </p>
 * <h2 id="send-a-entire-roster">Send a entire roster</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * Sometimes it is useful to send a whole roster to another user. Smack provides a very easy way to send a complete
 * roster to another XMPP client.
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <p>
 * Create an instance of <em><strong>RosterExchangeManager</strong></em> and use the <strong>#send(Roster,
 * String)</strong> message to send a roster to a given user. The first parameter is the roster to send and the second
 * parameter is the id of the user that will receive the roster entries.
 * </p>
 * <p>
 * <strong>Example</strong>
 * </p>
 * <p>
 * In this example we can see how user1 sends his roster to user2.
 * </p>
 *
 * <pre>
 * <code>XMPPConnection conn1 = …

// Create a new roster exchange manager on conn1
RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
// Send user1&#39;s roster to user2
rosterExchangeManager.send(Roster.getInstanceFor(conn1), user2);</code>
 * </pre>
 *
 * <h2 id="send-a-roster-group">Send a roster group</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * It is also possible to send a roster group to another XMPP client. A roster group groups a set of roster entries
 * under a name.
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <p>
 * Create an instance of <em><strong>RosterExchangeManager</strong></em> and use the <strong>#send(RosterGroup,
 * String)</strong> message to send a roster group to a given user. The first parameter is the roster group to send and
 * the second parameter is the id of the user that will receive the roster entries.
 * </p>
 * <p>
 * <strong>Example</strong>
 * </p>
 * <p>
 * In this example we can see how user1 sends his roster groups to user2.
 * </p>
 *
 * <pre>
 * <code>
 * XMPPConnection conn1 = …
 * // Create a new roster exchange manager on conn1
 * RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
 * // Send user1&#39;s RosterGroups to user2
 * for (Iterator it = Roster.getInstanceFor(conn1).getGroups(); it.hasNext(); )
 * rosterExchangeManager.send((RosterGroup)it.next(), user2);
 * </code>
 * </pre>
 *
 * <h2 id="send-a-roster-entry">Send a roster entry</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * Sometimes you may need to send a single roster entry to another XMPP client. Smack also lets you send items at this
 * granularity level.
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <p>
 * Create an instance of <em><strong>RosterExchangeManager</strong></em> and use the <strong>#send(RosterEntry,
 * String)</strong> message to send a roster entry to a given user. The first parameter is the roster entry to send and
 * the second parameter is the id of the user that will receive the roster entries.
 * </p>
 * <p>
 * <strong>Example</strong>
 * </p>
 * <p>
 * In this example we can see how user1 sends a roster entry to user2.
 * </p>
 *
 * <pre>
 * <code>
 * XMPPConnection conn1 = …
 * // Create a new roster exchange manager on conn1
 * RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
 * // Send a roster entry (any) to user2
 * rosterExchangeManager1.send((RosterEntry)Roster.getInstanceFor(conn1).getEntries().next(), user2);</code>
 * </pre>
 *
 * <h2 id="receive-roster-entries">Receive roster entries</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * Since roster items are sent between XMPP clients, it is necessary to listen to possible roster entries receptions.
 * Smack provides a mechanism that you can use to execute custom logic when roster entries are received.
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <ol type="1">
 * <li>Create a class that implements the <em><strong>RosterExchangeListener</strong></em> interface.</li>
 * <li>Implement the method <strong>entriesReceived(String, Iterator)</strong> that will be called when new entries are
 * received with custom logic.</li>
 * <li>Add the listener to the <em>RosterExchangeManager</em> that works on the desired <em>XMPPConnection</em>.</li>
 * </ol>
 * <p>
 * <strong>Example</strong>
 * </p>
 * <p>
 * In this example we can see how user1 sends a roster entry to user2 and user2 adds the received entries to his roster.
 * </p>
 *
 * <pre>
 * <code>
 * // Connect to the server and log in the users
 * XMPPConnection conn1 = …
 * XMPPConnection conn2 = …
 *
 * final Roster user2_roster = Roster.getInstanceFor(conn2);
 *
 * // Create a RosterExchangeManager that will help user2 to listen and accept
 * the entries received
 * RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(conn2);
 * // Create a RosterExchangeListener that will iterate over the received roster entries
 * RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
         * public void entriesReceived(String from, Iterator remoteRosterEntries) {
             * while (remoteRosterEntries.hasNext()) {
                 * try {
                     * // Get the received entry
                         * RemoteRosterEntry remoteRosterEntry = (RemoteRosterEntry) remoteRosterEntries.next();
                     * // Display the remote entry on the console
                         * System.out.println(remoteRosterEntry);
                     * // Add the entry to the user2&#39;s roster
                         * user2_roster.createEntry(
                                                    * remoteRosterEntry.getUser(),
                                                    * remoteRosterEntry.getName(),
                                                    * remoteRosterEntry.getGroupArrayNames());
                     * }
                 * catch (XMPPException e) {
                     * e.printStackTrace();
                     * }
                 * }
             * }
             * };
* // Add the RosterExchangeListener to the RosterExchangeManager that user2 is using
    * rosterExchangeManager2.addRosterListener(rosterExchangeListener);
*
    * // Create a RosterExchangeManager that will help user1 to send his roster
    * RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(conn1);
* // Send user1&#39;s roster to user2
    * rosterExchangeManager1.send(Roster.getInstanceFor(conn1), user2);
    * </code>
 * </pre>
 * @see <a href="https://xmpp.org/extensions/xep-0093.html">XEP-0093: Roster Item Exchange</a>
 */
package org.jivesoftware.smackx.xroster;
