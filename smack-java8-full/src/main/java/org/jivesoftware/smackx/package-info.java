/**
 *
 * Copyright 2015-2023 Florian Schmaus
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
 * Smack's API for XMPP extensions.
 * <p>
 * The XMPP protocol includes a base protocol and many optional extensions typically documented as "XEPs". Smack
 * provides the {@link org.jivesoftware.smack} package for the core XMPP protocol and the {@link org.jivesoftware.smackx
 * package} for many of the protocol extensions.
 * </p>
 * <table>
 * <caption>XEPs supported by Smack</caption> <thead>
 * <tr>
 * <th>Name</th>
 * <th>XEP</th>
 * <th>Smack API</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>Data Forms</td>
 * <td><a href="https://xmpp.org/extensions/xep-0004.html">XEP-0004</a></td>
 * <td>{@link org.jivesoftware.smackx.xdata}</td>
 * <td>Allows to gather data using Forms.</td>
 * </tr>
 * <tr>
 * <td>Last Activity</td>
 * <td><a href="https://xmpp.org/extensions/xep-0012.html">XEP-0012</a></td>
 * <td></td>
 * <td>Communicating information about the last activity associated with an XMPP entity.</td>
 * </tr>
 * <tr>
 * <td>Flexible Offline Message Retrieval</td>
 * <td><a href="https://xmpp.org/extensions/xep-0013.html">XEP-0013</a></td>
 * <td></td>
 * <td>Extension for flexible, POP3-like handling of offline messages.</td>
 * </tr>
 * <tr>
 * <td>Privacy Lists</td>
 * <td><a href="https://xmpp.org/extensions/xep-0016.html">XEP-0016</a></td>
 * <td>{@link org.jivesoftware.smackx.privacy}</td>
 * <td>Enabling or disabling communication with other entities.</td>
 * </tr>
 * <tr>
 * <td>Message Events</td>
 * <td><a href="https://xmpp.org/extensions/xep-0022.html">XEP-0022</a></td>
 * <td></td>
 * <td>Requests and responds to message events.</td>
 * </tr>
 * <tr>
 * <td>Service Discovery</td>
 * <td><a href="https://xmpp.org/extensions/xep-0030.html">XEP-0030</a></td>
 * <td></td>
 * <td>Allows to discover services in XMPP entities.</td>
 * </tr>
 * <tr>
 * <td>Extended Stanza Addressing</td>
 * <td><a href="https://xmpp.org/extensions/xep-0033.html">XEP-0033</a></td>
 * <td></td>
 * <td>Allows to include headers in stanzas in order to specifiy multiple recipients or sub-addresses.</td>
 * </tr>
 * <tr>
 * <td>Multi User Chat</td>
 * <td><a href="https://xmpp.org/extensions/xep-0045.html">XEP-0045</a></td>
 * <td>{@link org.jivesoftware.smackx.muc}</td>
 * <td>Allows configuration of, participation in, and administration of individual text-based conference rooms.</td>
 * </tr>
 * <tr>
 * <td>In-Band Bytestreams</td>
 * <td><a href="https://xmpp.org/extensions/xep-0047.html">XEP-0047</a></td>
 * <td></td>
 * <td>Enables any two entities to establish a one-to-one bytestream between themselves using plain XMPP.</td>
 * </tr>
 * <tr>
 * <td>Bookmarks</td>
 * <td><a href="https://xmpp.org/extensions/xep-0048.html">XEP-0048</a></td>
 * <td></td>
 * <td>Bookmarks, for e.g. MUC and web pages.</td>
 * </tr>
 * <tr>
 * <td>Private Data</td>
 * <td><a href="https://xmpp.org/extensions/xep-0049.html">XEP-0049</a></td>
 * <td></td>
 * <td>Manages private data.</td>
 * </tr>
 * <tr>
 * <td>Ad-Hoc Commands</td>
 * <td><a href="https://xmpp.org/extensions/xep-0050.html">XEP-0050</a></td>
 * <td></td>
 * <td>Advertising and executing application-specific commands.</td>
 * </tr>
 * <tr>
 * <td>vcard-temp</td>
 * <td><a href="https://xmpp.org/extensions/xep-0054.html">XEP-0054</a></td>
 * <td></td>
 * <td>The vCard-XML format currently in use.</td>
 * </tr>
 * <tr>
 * <td>Jabber Search</td>
 * <td><a href="https://xmpp.org/extensions/xep-0055.html">XEP-0055</a></td>
 * <td></td>
 * <td>Search information repositories on the XMPP network.</td>
 * </tr>
 * <tr>
 * <td>Result Set Management</td>
 * <td><a href="https://xmpp.org/extensions/xep-0059.html">XEP-0059</a></td>
 * <td></td>
 * <td>Page through and otherwise manage the receipt of large result sets</td>
 * </tr>
 * <tr>
 * <td>PubSub</td>
 * <td><a href="https://xmpp.org/extensions/xep-0060.html">XEP-0060</a></td>
 * <td></td>
 * <td>Generic publish and subscribe functionality.</td>
 * </tr>
 * <tr>
 * <td>SOCKS5 Bytestreams</td>
 * <td><a href="https://xmpp.org/extensions/xep-0065.html">XEP-0065</a></td>
 * <td></td>
 * <td>Out-of-band bytestream between any two XMPP entities.</td>
 * </tr>
 * <tr>
 * <td>Field Standardization for Data Forms</td>
 * <td><a href="https://xmpp.org/extensions/xep-0068.html">XEP-0068</a></td>
 * <td></td>
 * <td>Standardized field variables used in the context of jabber:x:data forms.</td>
 * </tr>
 * <tr>
 * <td>XHTML-IM</td>
 * <td><a href="https://xmpp.org/extensions/xep-0071.html">XEP-0071</a></td>
 * <td></td>
 * <td>Allows send and receiving formatted messages using XHTML.</td>
 * </tr>
 * <tr>
 * <td>In-Band Registration</td>
 * <td><a href="https://xmpp.org/extensions/xep-0077.html">XEP-0077</a></td>
 * <td></td>
 * <td>In-band registration with XMPP services.</td>
 * </tr>
 * <tr>
 * <td>Advanced Message Processing</td>
 * <td><a href="https://xmpp.org/extensions/xep-0079.html">XEP-0079</a></td>
 * <td></td>
 * <td>Enables entities to request, and servers to perform, advanced processing of XMPP message stanzas.</td>
 * </tr>
 * <tr>
 * <td>User Location</td>
 * <td><a href="https://xmpp.org/extensions/xep-0080.html">XEP-0080</a></td>
 * <td></td>
 * <td>Enabled communicating information about the current geographical or physical location of an entity.</td>
 * </tr>
 * <tr>
 * <td>XMPP Date Time Profiles</td>
 * <td><a href="https://xmpp.org/extensions/xep-0082.html">XEP-0082</a></td>
 * <td></td>
 * <td>Standardization of Date and Time representation in XMPP.</td>
 * </tr>
 * <tr>
 * <td>Chat State Notifications</td>
 * <td><a href="https://xmpp.org/extensions/xep-0085.html">XEP-0085</a></td>
 * <td></td>
 * <td>Communicating the status of a user in a chat session.</td>
 * </tr>
 * <tr>
 * <td>Time Exchange</td>
 * <td><a href="https://xmpp.org/extensions/xep-0090.html">XEP-0090</a></td>
 * <td></td>
 * <td>Allows local time information to be shared between users.</td>
 * </tr>
 * <tr>
 * <td>Software Version</td>
 * <td><a href="https://xmpp.org/extensions/xep-0092.html">XEP-0092</a></td>
 * <td></td>
 * <td>Retrieve and announce the software application of an XMPP entity.</td>
 * </tr>
 * <tr>
 * <td>Roster Item Exchange</td>
 * <td><a href="https://xmpp.org/extensions/xep-0093.html">XEP-0093</a></td>
 * <td></td>
 * <td>Allows roster data to be shared between users.</td>
 * </tr>
 * <tr>
 * <td>Stream Initiation</td>
 * <td><a href="https://xmpp.org/extensions/xep-0095.html">XEP-0095</a></td>
 * <td></td>
 * <td>Initiating a data stream between any two XMPP entities.</td>
 * </tr>
 * <tr>
 * <td>SI File Transfer</td>
 * <td><a href="https://xmpp.org/extensions/xep-0096.html">XEP-0096</a></td>
 * <td></td>
 * <td>Transfer files between two users over XMPP.</td>
 * </tr>
 * <tr>
 * <td>User Mood</td>
 * <td><a href="https://xmpp.org/extensions/xep-0107.html">XEP-0107</a></td>
 * <td></td>
 * <td>Communicate the users current mood.</td>
 * </tr>
 * <tr>
 * <td>Entity Capabilities</td>
 * <td><a href="https://xmpp.org/extensions/xep-0115.html">XEP-0115</a></td>
 * <td>{@link org.jivesoftware.smackx.caps.EntityCapsManager}</td>
 * <td>Broadcasting and dynamic discovery of entity capabilities.</td>
 * </tr>
 * <tr>
 * <td>User Tune</td>
 * <td><a href="https://xmpp.org/extensions/xep-0118.html">XEP-0118</a></td>
 * <td></td>
 * <td>Defines a payload format for communicating information about music to which a user is listening.</td>
 * </tr>
 * <tr>
 * <td>Data Forms Validation</td>
 * <td><a href="https://xmpp.org/extensions/xep-0122.html">XEP-0122</a></td>
 * <td></td>
 * <td>Enables an application to specify additional validation guidelines.</td>
 * </tr>
 * <tr>
 * <td>Stanza Headers and Internet Metadata (SHIM)</td>
 * <td><a href="https://xmpp.org/extensions/xep-0131.html">XEP-0131</a></td>
 * <td></td>
 * <td>Add Metadata Headers to Stanzas.</td>
 * </tr>
 * <tr>
 * <td>Service Administration</td>
 * <td><a href="https://xmpp.org/extensions/xep-0133.html">XEP-0133</a></td>
 * <td></td>
 * <td>Recommended best practices for service-level administration of servers and components using Ad-Hoc Commands.</td>
 * </tr>
 * <tr>
 * <td>Stream Compression</td>
 * <td><a href="https://xmpp.org/extensions/xep-0138.html">XEP-0138</a></td>
 * <td></td>
 * <td>Support for optional compression of the XMPP stream.</td>
 * </tr>
 * <tr>
 * <td>Data Forms Layout</td>
 * <td><a href="https://xmpp.org/extensions/xep-0141.html">XEP-0141</a></td>
 * <td></td>
 * <td>Enables an application to specify form layouts.</td>
 * </tr>
 * <tr>
 * <td>Discovering Alternative XMPP Connection Methods</td>
 * <td><a href="https://xmpp.org/extensions/xep-0156.html">XEP-0156</a></td>
 * <td></td>
 * <td>Defines ways to discover alternative connection methods.</td>
 * </tr>
 * <tr>
 * <td>Personal Eventing Protocol</td>
 * <td><a href="https://xmpp.org/extensions/xep-0163.html">XEP-0163</a></td>
 * <td></td>
 * <td>Using the XMPP publish-subscribe protocol to broadcast state change events associated with an XMPP account.</td>
 * </tr>
 * <tr>
 * <td><a href="jingle.html">Jingle</a></td>
 * <td><a href="https://xmpp.org/extensions/xep-0166.html">XEP-0166</a></td>
 * <td></td>
 * <td>Initiate and manage sessions between two XMPP entities.</td>
 * </tr>
 * <tr>
 * <td>User Nickname</td>
 * <td><a href="https://xmpp.org/extensions/xep-0172.html">XEP-0172</a></td>
 * <td></td>
 * <td>Communicate user nicknames.</td>
 * </tr>
 * <tr>
 * <td>Message Delivery Receipts</td>
 * <td><a href="https://xmpp.org/extensions/xep-0184.html">XEP-0184</a></td>
 * <td></td>
 * <td>Extension for message delivery receipts. The sender can request notification that the message has been
 * delivered.</td>
 * </tr>
 * <tr>
 * <td>Blocking Command</td>
 * <td><a href="https://xmpp.org/extensions/xep-0191.html">XEP-0191</a></td>
 * <td>{@link org.jivesoftware.smackx.blocking.BlockingCommandManager}</td>
 * <td>Communications blocking that is intended to be simpler than privacy lists (XEP-0016).</td>
 * </tr>
 * <tr>
 * <td>Stream Management</td>
 * <td><a href="https://xmpp.org/extensions/xep-0198.html">XEP-0198</a></td>
 * <td></td>
 * <td>Allows active management of an XML Stream between two XMPP entities (stanza acknowledgement, stream
 * resumption).</td>
 * </tr>
 * <tr>
 * <td>XMPP Ping</td>
 * <td><a href="https://xmpp.org/extensions/xep-0199.html">XEP-0199</a></td>
 * <td></td>
 * <td>Sending application-level pings over XML streams.</td>
 * </tr>
 * <tr>
 * <td>Entity Time</td>
 * <td><a href="https://xmpp.org/extensions/xep-0202.html">XEP-0202</a></td>
 * <td></td>
 * <td>Allows entities to communicate their local time</td>
 * </tr>
 * <tr>
 * <td>Delayed Delivery</td>
 * <td><a href="https://xmpp.org/extensions/xep-0203.html">XEP-0203</a></td>
 * <td></td>
 * <td>Extension for communicating the fact that an XML stanza has been delivered with a delay.</td>
 * </tr>
 * <tr>
 * <td>XMPP Over BOSH</td>
 * <td><a href="https://xmpp.org/extensions/xep-0206.html">XEP-0206</a></td>
 * <td></td>
 * <td>Use Bidirectional-streams Over Synchronous HTTP (BOSH) to transport XMPP stanzas.</td>
 * </tr>
 * <tr>
 * <td>Data Forms Media Element</td>
 * <td><a href="https://xmpp.org/extensions/xep-0221.html">XEP-0221</a></td>
 * <td></td>
 * <td>Allows to include media data in XEP-0004 data forms.</td>
 * </tr>
 * <tr>
 * <td>Attention</td>
 * <td><a href="https://xmpp.org/extensions/xep-0224.html">XEP-0224</a></td>
 * <td></td>
 * <td>Getting attention of another user.</td>
 * </tr>
 * <tr>
 * <td>Bits of Binary</td>
 * <td><a href="https://xmpp.org/extensions/xep-0231.html">XEP-0231</a></td>
 * <td></td>
 * <td>Including or referring to small bits of binary data in an XML stanza.</td>
 * </tr>
 * <tr>
 * <td>Software Information</td>
 * <td><a href="https://xmpp.org/extensions/xep-0232.html">XEP-0232</a></td>
 * <td></td>
 * <td>Allows an entity to provide detailed data about itself in Service Discovery responses.</td>
 * </tr>
 * <tr>
 * <td>Roster Versioning</td>
 * <td><a href="https://xmpp.org/extensions/xep-0237.html">XEP-0237</a></td>
 * <td></td>
 * <td>Efficient roster synchronization.</td>
 * </tr>
 * <tr>
 * <td>Message Carbons</td>
 * <td><a href="https://xmpp.org/extensions/xep-0280.html">XEP-0280</a></td>
 * <td>{@link org.jivesoftware.smackx.carbons}</td>
 * <td>Keep all IM clients for a user engaged in a conversation, by carbon-copy outbound messages to all interested
 * resources.</td>
 * </tr>
 * <tr>
 * <td>Best Practices for Resource Locking</td>
 * <td><a href="https://xmpp.org/extensions/xep-0296.html">XEP-0296</a></td>
 * <td></td>
 * <td>Specifies best practices to be followed by Jabber/XMPP clients about when to lock into, and unlock away from,
 * resources.</td>
 * </tr>
 * <tr>
 * <td>Stanza Forwarding</td>
 * <td><a href="https://xmpp.org/extensions/xep-0297.html">XEP-0297</a></td>
 * <td></td>
 * <td>Allows forwarding of Stanzas.</td>
 * </tr>
 * <tr>
 * <td>Last Message Correction</td>
 * <td><a href="https://xmpp.org/extensions/xep-0308.html">XEP-0308</a></td>
 * <td></td>
 * <td>Provides a method for indicating that a message is a correction of the last sent message.</td>
 * </tr>
 * <tr>
 * <td>Message Archive Management</td>
 * <td><a href="https://xmpp.org/extensions/xep-0313.html">XEP-0313</a></td>
 * <td></td>
 * <td>Query and control an archive of messages stored on a server.</td>
 * </tr>
 * <tr>
 * <td>Data Forms XML Element</td>
 * <td><a href="https://xmpp.org/extensions/xep-0315.html">XEP-0315</a></td>
 * <td></td>
 * <td>Allows to include XML-data in XEP-0004 data forms.</td>
 * </tr>
 * <tr>
 * <td>Last User Interaction in Presence</td>
 * <td><a href="https://xmpp.org/extensions/xep-0319.html">XEP-0319</a></td>
 * <td></td>
 * <td>Communicate time of last user interaction via XMPP presence notifications.</td>
 * </tr>
 * <tr>
 * <td>Internet of Things - Sensor Data</td>
 * <td><a href="https://xmpp.org/extensions/xep-0323.html">XEP-0323</a></td>
 * <td></td>
 * <td>Sensor data interchange over XMPP.</td>
 * </tr>
 * <tr>
 * <td>Internet of Things - Provisioning</td>
 * <td><a href="https://xmpp.org/extensions/xep-0324.html">XEP-0324</a></td>
 * <td></td>
 * <td>Provisioning, access rights and user privileges for the Internet of Things.</td>
 * </tr>
 * <tr>
 * <td>Internet of Things - Control</td>
 * <td><a href="https://xmpp.org/extensions/xep-0325.html">XEP-0325</a></td>
 * <td></td>
 * <td>Describes how to control devices or actuators in an XMPP-based sensor network.</td>
 * </tr>
 * <tr>
 * <td>Jid Prep</td>
 * <td><a href="https://xmpp.org/extensions/xep-0328.html">XEP-0328</a></td>
 * <td></td>
 * <td>Describes a way for an XMPP client to request an XMPP server to prep and normalize a given JID.</td>
 * </tr>
 * <tr>
 * <td>HTTP over XMPP transport</td>
 * <td><a href="https://xmpp.org/extensions/xep-0332.html">XEP-0332</a></td>
 * <td>{@link org.jivesoftware.smackx.hoxt}</td>
 * <td>Allows to transport HTTP communication over XMPP peer-to-peer networks.</td>
 * </tr>
 * <tr>
 * <td>Chat Markers</td>
 * <td><a href="https://xmpp.org/extensions/xep-0333.html">XEP-0333</a></td>
 * <td></td>
 * <td>A solution of marking the last received, displayed and acknowledged message in a chat.</td>
 * </tr>
 * <tr>
 * <td>Message Processing Hints</td>
 * <td><a href="https://xmpp.org/extensions/xep-0334.html">XEP-0334</a></td>
 * <td></td>
 * <td>Hints to entities routing or receiving a message.</td>
 * </tr>
 * <tr>
 * <td>JSON Containers</td>
 * <td><a href="https://xmpp.org/extensions/xep-0335.html">XEP-0335</a></td>
 * <td></td>
 * <td>Encapsulation of JSON data within XMPP Stanzas.</td>
 * </tr>
 * <tr>
 * <td>Internet of Things - Discovery</td>
 * <td><a href="https://xmpp.org/extensions/xep-0347.html">XEP-0347</a></td>
 * <td></td>
 * <td>Describes how Things can be installed and discovered by their owners.</td>
 * </tr>
 * <tr>
 * <td>Data Forms Geolocation Element</td>
 * <td><a href="https://xmpp.org/extensions/xep-0350.html">XEP-0350</a></td>
 * <td></td>
 * <td>Allows to include XEP-0080 gelocation data in XEP-0004 data forms.</td>
 * </tr>
 * <tr>
 * <td>Client State Indication</td>
 * <td><a href="https://xmpp.org/extensions/xep-0352.html">XEP-0352</a></td>
 * <td></td>
 * <td>A way for the client to indicate its active/inactive state.</td>
 * </tr>
 * <tr>
 * <td>Push Notifications</td>
 * <td><a href="https://xmpp.org/extensions/xep-0357.html">XEP-0357</a></td>
 * <td></td>
 * <td>Defines a way to manage push notifications from an XMPP Server.</td>
 * </tr>
 * <tr>
 * <td>Stable and Unique Stanza IDs</td>
 * <td><a href="https://xmpp.org/extensions/xep-0359.html">XEP-0359</a></td>
 * <td></td>
 * <td>This specification describes unique and stable IDs for messages.</td>
 * </tr>
 * <tr>
 * <td>Nonzas</td>
 * <td><a href="https://xmpp.org/extensions/xep-0360.html">XEP-0360</a></td>
 * <td>{@link org.jivesoftware.smack.packet.Nonza}</td>
 * <td>Defines the term “Nonza”, describing every top level stream element that is not a Stanza.</td>
 * </tr>
 * <tr>
 * <td>HTTP File Upload</td>
 * <td><a href="https://xmpp.org/extensions/xep-0363.html">XEP-0363</a></td>
 * <td></td>
 * <td>Protocol to request permissions to upload a file to an HTTP server and get a shareable URL.</td>
 * </tr>
 * <tr>
 * <td>References</td>
 * <td><a href="https://xmpp.org/extensions/xep-0363.html">XEP-0372</a></td>
 * <td></td>
 * <td>Add references like mentions or external data to stanzas.</td>
 * </tr>
 * <tr>
 * <td>Explicit Message Encryption</td>
 * <td><a href="https://xmpp.org/extensions/xep-0380.html">XEP-0380</a></td>
 * <td></td>
 * <td>Mark a message as explicitly encrypted.</td>
 * </tr>
 * <tr>
 * <td>OpenPGP for XMPP</td>
 * <td><a href="https://xmpp.org/extensions/xep-0373.html">XEP-0373</a></td>
 * <td></td>
 * <td>Utilize OpenPGP to exchange encrypted and signed content.</td>
 * </tr>
 * <tr>
 * <td>OpenPGP for XMPP: Instant Messaging</td>
 * <td><a href="https://xmpp.org/extensions/xep-0374.html">XEP-0374</a></td>
 * <td></td>
 * <td>OpenPGP encrypted Instant Messaging.</td>
 * </tr>
 * <tr>
 * <td>Spoiler Messages</td>
 * <td><a href="https://xmpp.org/extensions/xep-0382.html">XEP-0382</a></td>
 * <td></td>
 * <td>Indicate that the body of a message should be treated as a spoiler.</td>
 * </tr>
 * <tr>
 * <td>OMEMO Multi End Message and Object Encryption</td>
 * <td><a href="https://xmpp.org/extensions/xep-0384.html">XEP-0384</a></td>
 * <td></td>
 * <td>Encrypt messages using OMEMO encryption (currently only with smack-omemo-signal -&gt; GPLv3).</td>
 * </tr>
 * <tr>
 * <td>Consistent Color Generation</td>
 * <td><a href="https://xmpp.org/extensions/xep-0392.html">XEP-0392</a></td>
 * <td>{@link org.jivesoftware.smackx.colors.ConsistentColor}</td>
 * <td>Generate consistent colors for identifiers like usernames to provide a consistent user experience.</td>
 * </tr>
 * <tr>
 * <td>Message Markup</td>
 * <td><a href="https://xmpp.org/extensions/xep-0394.html">XEP-0394</a></td>
 * <td>{@link org.jivesoftware.smackx.message_markup.element}</td>
 * <td>Style message bodies while keeping body and markup information separated.</td>
 * </tr>
 * <tr>
 * <td>DNS Queries over XMPP (DoX)</td>
 * <td><a href="https://xmpp.org/extensions/xep-0418.html">XEP-0418</a></td>
 * <td></td>
 * <td>Send DNS queries and responses over XMPP.</td>
 * </tr>
 * <tr>
 * <td>Stanza Content Encryption</td>
 * <td><a href="https://xmpp.org/extensions/xep-0420.html">XEP-0420</a></td>
 * <td></td>
 * <td>End-to-end encryption of arbitrary extension elements. Smack provides elements and providers to be used by
 * encryption mechanisms.</td>
 * </tr>
 * <tr>
 * <td>Message Fastening</td>
 * <td><a href="https://xmpp.org/extensions/xep-0422.html">XEP-0422</a></td>
 * <td></td>
 * <td>Mark payloads on a message to be logistically fastened to a previous message.</td>
 * </tr>
 * <tr>
 * <td>Message Retraction</td>
 * <td><a href="https://xmpp.org/extensions/xep-0424.html">XEP-0424</a></td>
 * <td></td>
 * <td>Mark messages as retracted.</td>
 * </tr>
 * <tr>
 * <td>Fallback Indication</td>
 * <td><a href="https://xmpp.org/extensions/xep-0428.html">XEP-0428</a></td>
 * <td></td>
 * <td>Declare body elements of a message as ignorable fallback for naive legacy clients.</td>
 * </tr>
 * <tr>
 * <td>Google GCM JSON payload</td>
 * <td></td>
 * <td></td>
 * <td>Semantically the same as XEP-0335: JSON Containers.</td>
 * </tr>
 * <tr>
 * <td>Multi-User Chat Light</td>
 * <td><a href=
 * "https://mongooseim.readthedocs.io/en/latest/open-extensions/xeps/xep-muc-light.html">XEP-MUCLIGHT</a></td>
 * <td></td>
 * <td>Multi-User Chats for mobile XMPP applications and specific environment.</td>
 * </tr>
 * <tr>
 * <td>Group Chat Invitations</td>
 * <td></td>
 * <td></td>
 * <td>Send invitations to other users to join a group chat room.</td>
 * </tr>
 * <tr>
 * <td><a href="properties.md">Jive Properties</a></td>
 * <td></td>
 * <td></td>
 * <td>TODO</td>
 * </tr>
 * </tbody>
 * </table>
 */
package org.jivesoftware.smackx;
