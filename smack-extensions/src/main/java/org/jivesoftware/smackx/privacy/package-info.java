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
 * Smacks implementation of XEP-0016: Privacy Lists.
 * <h2 id="what-is-it">What is it?</h2>
 * <p>
 * <code>Privacy</code> is a method for users to block communications from particular other users. In XMPP this is done
 * by managing one’s privacy lists.
 * </p>
 * <p>
 * Server-side privacy lists enable successful completion of the following use cases:
 * </p>
 * <ul>
 * <li>Retrieving one’s privacy lists.</li>
 * <li>Adding, removing, and editing one’s privacy lists.</li>
 * <li>Setting, changing, or declining active lists.</li>
 * <li>Setting, changing, or declining the default list (i.e., the list that is active by default).</li>
 * <li>Allowing or blocking messages based on JID, group, or subscription type (or globally).</li>
 * <li>Allowing or blocking inbound presence notifications based on JID, group, or subscription type (or globally).</li>
 * <li>Allowing or blocking outbound presence notifications based on JID, group, or subscription type (or
 * globally).</li>
 * <li>Allowing or blocking IQ stanzas based on JID, group, or subscription type (or globally).</li>
 * <li>Allowing or blocking all communications based on JID, group, or subscription type (or globally).</li>
 * </ul>
 * <h2 id="how-can-i-use-it">How can I use it?</h2>
 * <p>
 * The API implementation releases three main public classes:
 * </p>
 * <ul>
 * <li><code>PrivacyListManager</code>: this is the main API class to retrieve and handle server privacy lists.</li>
 * <li><code>PrivacyList</code>: witch represents one privacy list, with a name, a set of privacy items. For example,
 * the list with visible or invisible.</li>
 * <li><code>PrivacyItem</code>: block or allow one aspect of privacy. For example, to allow my friend to see my
 * presence.</li>
 * </ul>
 * <ol type="1">
 * <li>Right from the start, a client MAY <strong>get his/her privacy list</strong> that is stored in the server:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Create a privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Retrieve server privacy lists_
 * PrivacyList[] lists = privacyManager.getPrivacyLists();
 * </code>
 * </pre>
 * <p>
 * Now the client is able to show every <code>PrivacyItem</code> of the server and also for every list if it is active,
 * default or none of them. The client is a listener of privacy changes.
 * </p>
 * <ol start="2" type="1">
 * <li>In order to <strong>add a new list in the server</strong>, the client MAY implement something like:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Set the name of the list_
 * String listName = &quot;newList&quot;;
 *
 * // Create the list of PrivacyItem that will allow or deny some privacy aspect_
 * String user = &quot;tybalt@example.com&quot;;
 * String groupName = &quot;enemies&quot;;
 * ArrayList privacyItems = new ArrayList();
 *
 * PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid, user, true, 1);
 * privacyItems.add(item);
 *
 * item = new PrivacyItem(PrivacyItem.Type.subscription, PrivacyItem.SUBSCRIPTION_BOTH, true, 2);
 * privacyItems.add(item);
 *
 * item = new PrivacyItem(PrivacyItem.Type.group, groupName, false, 3);
 * item.setFilterMessage(true);
 * privacyItems.add(item);
 *
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Create the new list._
 * privacyManager.createPrivacyList(listName, privacyItems);
 * </code>
 * </pre>
 * <ol start="3" type="1">
 * <li>To <strong>modify an existent list</strong>, the client code MAY be like:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Set the name of the list_
 * String listName = &quot;existingList&quot;;
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Sent the new list to the server._
 * privacyManager.updatePrivacyList(listName, items);
 * </code>
 * </pre>
 * <p>
 * Notice <code>items</code> was defined at the example 2 and MUST contain all the elements in the list (not the
 * “delta”).
 * </p>
 * <ol start="4" type="1">
 * <li>In order to <strong>delete an existing list</strong>, the client MAY perform something like:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Set the name of the list_
 * String listName = &quot;existingList&quot;;
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Remove the list._
 * privacyManager.deletePrivacyList(listName);
 * </code>
 * </pre>
 * <ol start="5" type="1">
 * <li>In order to <strong>decline the use of an active list</strong>, the client MAY perform something like:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Decline the use of the active list._
 * privacyManager.declineActiveList();
 * </code>
 * </pre>
 * <ol start="6" type="1">
 * <li>In order to <strong>decline the use of a default list</strong>, the client MAY perform something like:</li>
 * </ol>
 *
 * <pre>
 * <code>
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Decline the use of the default list._
 * privacyManager.declineDefaultList();
 * </code>
 * </pre>
 * <p>
 * Listening for Privacy Changes
 * </p>
 * <p>
 * In order to handle privacy changes, clients SHOULD listen manager’s updates. When a list is changed the manager
 * notifies every added listener. Listeners MUST implement the <code>PrivacyListListener</code> interface. Clients may
 * need to react when a privacy list is modified. The <code>PrivacyListManager</code> lets you add listerners that will
 * be notified when a list has been changed. Listeners should implement the <code>PrivacyListListener</code> interface.
 * </p>
 * <p>
 * The most important notification is <code>updatedPrivacyList</code> that is performed when a privacy list changes its
 * privacy items.
 * </p>
 * <p>
 * The listener becomes notified after performing:
 * </p>
 *
 * <pre>
 * <code>
 * // Get the privacy manager for the current connection._
 * PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
 * // Add the listener (this) to get notified_
 * privacyManager.addListener(this);
 * </code>
 * </pre>
 */
package org.jivesoftware.smackx.privacy;
