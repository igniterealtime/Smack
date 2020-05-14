Smack Extensions User Manual
============================

The XMPP protocol includes a base protocol and many optional extensions
typically documented as "XEP's". Smack provides the org.jivesoftware.smack
package for the core XMPP protocol, and the org.jivesoftware.smackx package
for many of the protocol extensions.

This manual provides details about each of the "smackx" extensions, including
what it is, how to use it, and some simple example code.

Currently supported XEPs of Smack (all sub-projects)
---------------------------------------------------

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------|
| Nonzas                                      | [XEP-0360](https://xmpp.org/extensions/xep-0360.html)  | n/a       | Defines the term "Nonza", describing every top level stream element that is not a Stanza.                                         |

Currently supported XEPs of smack-tcp
-------------------------------------

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------|
| [Stream Management](streammanagement.md)    | [XEP-0198](https://xmpp.org/extensions/xep-0198.html)  | n/a       | Allows active management of an XML Stream between two XMPP entities (stanza acknowledgement, stream resumption). |

Currently supported XEPs of smack-im
------------------------------------

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|-----------------------------------|--
| Roster Versioning                           | [XEP-0237](https://xmpp.org/extensions/xep-0237.html)  | n/a       | Efficient roster synchronization. |

Smack Extensions and currently supported XEPs of smack-extensions
-----------------------------------------------------------------

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------|
| [Data Forms](dataforms.md)                  | [XEP-0004](https://xmpp.org/extensions/xep-0004.html)  | n/a       | Allows to gather data using Forms. |
| Last Activity                               | [XEP-0012](https://xmpp.org/extensions/xep-0012.html)  | n/a       | Communicating information about the last activity associated with an XMPP entity. |
| Flexible Offline Message Retrieval          | [XEP-0013](https://xmpp.org/extensions/xep-0013.html)  | n/a       | Extension for flexible, POP3-like handling of offline messages. |
| [Privacy Lists](privacy.md)                 | [XEP-0016](https://xmpp.org/extensions/xep-0016.html)  | n/a       | Enabling or disabling communication with other entities. |
| [Service Discovery](disco.md)               | [XEP-0030](https://xmpp.org/extensions/xep-0030.html)  | n/a       | Allows to discover services in XMPP entities. |
| Extended Stanza Addressing                  | [XEP-0033](https://xmpp.org/extensions/xep-0033.html)  | n/a       | Allows to include headers in stanzas in order to specifiy multiple recipients or sub-addresses. |
| [Multi User Chat](muc.md)                   | [XEP-0045](https://xmpp.org/extensions/xep-0045.html)  | n/a       | Allows configuration of, participation in, and administration of individual text-based conference rooms. |
| In-Band Bytestreams                         | [XEP-0047](https://xmpp.org/extensions/xep-0047.html)  | n/a       | Enables any two entities to establish a one-to-one bytestream between themselves using plain XMPP. |
| Bookmarks                                   | [XEP-0048](https://xmpp.org/extensions/xep-0048.html)  | n/a       | Bookmarks, for e.g. MUC and web pages. |
| [Private Data](privatedata.md)              | [XEP-0049](https://xmpp.org/extensions/xep-0049.html)  | n/a       | Manages private data. |
| Ad-Hoc Commands                             | [XEP-0050](https://xmpp.org/extensions/xep-0050.html)  | n/a       | Advertising and executing application-specific commands. |
| vcard-temp                                  | [XEP-0054](https://xmpp.org/extensions/xep-0054.html)  | n/a       | The vCard-XML format currently in use. |
| Jabber Search                               | [XEP-0055](https://xmpp.org/extensions/xep-0055.html)  | n/a       | Search information repositories on the XMPP network. |
| Result Set Management                       | [XEP-0059](https://xmpp.org/extensions/xep-0059.html)  | n/a       | Page through and otherwise manage the receipt of large result sets |
| [PubSub](pubsub.md)                         | [XEP-0060](https://xmpp.org/extensions/xep-0060.html)  | n/a       | Generic publish and subscribe functionality. |
| SOCKS5 Bytestreams                          | [XEP-0065](https://xmpp.org/extensions/xep-0065.html)  | n/a       | Out-of-band bytestream between any two XMPP entities. |
| Field Standardization for Data Forms        | [XEP-0068](https://xmpp.org/extensions/xep-0068.html)  | n/a       | Standardized field variables used in the context of jabber:x:data forms. |
| [XHTML-IM](xhtml.md)                        | [XEP-0071](https://xmpp.org/extensions/xep-0071.html)  | n/a       | Allows send and receiving formatted messages using XHTML. |
| In-Band Registration                        | [XEP-0077](https://xmpp.org/extensions/xep-0077.html)  | n/a       | In-band registration with XMPP services. |
| Advanced Message Processing                 | [XEP-0079](https://xmpp.org/extensions/xep-0079.html)  | n/a       | Enables entities to request, and servers to perform, advanced processing of XMPP message stanzas. |
| User Location                               | [XEP-0080](https://xmpp.org/extensions/xep-0080.html)  | n/a       | Enabled communicating information about the current geographical or physical location of an entity. |
| XMPP Date Time Profiles                     | [XEP-0082](https://xmpp.org/extensions/xep-0082.html)  | n/a       | Standardization of Date and Time representation in XMPP. |
| Chat State Notifications                    | [XEP-0085](https://xmpp.org/extensions/xep-0085.html)  | n/a       | Communicating the status of a user in a chat session. |
| [Time Exchange](time.md)                    | [XEP-0090](https://xmpp.org/extensions/xep-0090.html)  | n/a       | Allows local time information to be shared between users. |
| Software Version                            | [XEP-0092](https://xmpp.org/extensions/xep-0092.html)  | n/a       | Retrieve and announce the software application of an XMPP entity. |
| Stream Initiation                           | [XEP-0095](https://xmpp.org/extensions/xep-0095.html)  | n/a       | Initiating a data stream between any two XMPP entities. |
| [SI File Transfer](filetransfer.md)         | [XEP-0096](https://xmpp.org/extensions/xep-0096.html)  | n/a       | Transfer files between two users over XMPP. |
| User Mood                                   | [XEP-0107](https://xmpp.org/extensions/xep-0107.html)  | 1.2.1     | Communicate the users current mood. |
| [Entity Capabilities](caps.md)              | [XEP-0115](https://xmpp.org/extensions/xep-0115.html)  | n/a       | Broadcasting and dynamic discovery of entity capabilities. |
| User Tune                                   | [XEP-0118](https://xmpp.org/extensions/xep-0118.html)  | n/a       | Defines a payload format for communicating information about music to which a user is listening. |
| Data Forms Validation                       | [XEP-0122](https://xmpp.org/extensions/xep-0122.html)  | n/a       | Enables an application to specify additional validation guidelines . |
| Stanza Headers and Internet Metadata (SHIM) | [XEP-0131](https://xmpp.org/extensions/xep-0131.html)  | 1.2       | Add Metadata Headers to Stanzas. |
| Service Administration                      | [XEP-0133](https://xmpp.org/extensions/xep-0133.html)  | n/a       | Recommended best practices for service-level administration of servers and components using Ad-Hoc Commands. |
| Stream Compression                          | [XEP-0138](https://xmpp.org/extensions/xep-0138.html)  | n/a       | Support for optional compression of the XMPP stream.
| Data Forms Layout                           | [XEP-0141](https://xmpp.org/extensions/xep-0141.html)  | n/a       | Enables an application to specify form layouts. |
| Personal Eventing Protocol                  | [XEP-0163](https://xmpp.org/extensions/xep-0163.html)  | n/a       | Using the XMPP publish-subscribe protocol to broadcast state change events associated with an XMPP account. |
| [Jingle](jingle.html)                       | [XEP-0166](https://xmpp.org/extensions/xep-0166.html)  | n/a       | Initiate and manage sessions between two XMPP entities. |
| User Nickname                               | [XEP-0172](https://xmpp.org/extensions/xep-0172.html)  | n/a       | Communicate user nicknames. |
| Message Delivery Receipts                   | [XEP-0184](https://xmpp.org/extensions/xep-0184.html)  | n/a       | Extension for message delivery receipts. The sender can request notification that the message has been delivered. |
| [Blocking Command](blockingcommand.md)      | [XEP-0191](https://xmpp.org/extensions/xep-0191.html)  | n/a       | Communications blocking that is intended to be simpler than privacy lists (XEP-0016). |
| XMPP Ping                                   | [XEP-0199](https://xmpp.org/extensions/xep-0199.html)  | n/a       | Sending application-level pings over XML streams.
| Entity Time                                 | [XEP-0202](https://xmpp.org/extensions/xep-0202.html)  | n/a       | Allows entities to communicate their local time |
| Delayed Delivery                            | [XEP-0203](https://xmpp.org/extensions/xep-0203.html)  | n/a       | Extension for communicating the fact that an XML stanza has been delivered with a delay. |
| XMPP Over BOSH                              | [XEP-0206](https://xmpp.org/extensions/xep-0206.html)  | n/a       | Use Bidirectional-streams Over Synchronous HTTP (BOSH) to transport XMPP stanzas. |
| Data Forms Media Element                    | [XEP-0221](https://xmpp.org/extensions/xep-0221.html)  | n/a       | Allows to include media data in XEP-0004 data forms. |
| Attention                                   | [XEP-0224](https://xmpp.org/extensions/xep-0224.html)  | n/a       | Getting attention of another user. |
| Bits of Binary                              | [XEP-0231](https://xmpp.org/extensions/xep-0231.html)  | n/a       | Including or referring to small bits of binary data in an XML stanza. |
| Best Practices for Resource Locking         | [XEP-0296](https://xmpp.org/extensions/xep-0296.html)  | n/a       | Specifies best practices to be followed by Jabber/XMPP clients about when to lock into, and unlock away from, resources. |
| Stanza Forwarding                           | [XEP-0297](https://xmpp.org/extensions/xep-0297.html)  | n/a       | Allows forwarding of Stanzas. |
| Last Message Correction                     | [XEP-0308](https://xmpp.org/extensions/xep-0308.html)  | n/a       | Provides a method for indicating that a message is a correction of the last sent message. |
| Last User Interaction in Presence           | [XEP-0319](https://xmpp.org/extensions/xep-0319.html)  | n/a       | Communicate time of last user interaction via XMPP presence notifications. |
| Data Forms Geolocation Element              | [XEP-0350](https://xmpp.org/extensions/xep-0350.html)  | n/a       | Allows to include XEP-0080 gelocation data in XEP-0004 data forms.  |
| [Group Chat Invitations](invitation.md)     | n/a                                                    | n/a       | Send invitations to other users to join a group chat room. |
| [Jive Properties](properties.md)            | n/a                                                    | n/a       | TODO |


Experimental Smack Extensions and currently supported XEPs of smack-experimental
--------------------------------------------------------------------------------

| Name                                                      | XEP                                                    | Version   | Description |
|-----------------------------------------------------------|--------------------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------|
| Message Carbons                                           | [XEP-0280](https://xmpp.org/extensions/xep-0280.html)  | n/a       | Keep all IM clients for a user engaged in a conversation, by carbon-copy outbound messages to all interested resources. |
| [Message Archive Management](mam.md)                      | [XEP-0313](https://xmpp.org/extensions/xep-0313.html)  | n/a       | Query and control an archive of messages stored on a server. |
| Data Forms XML Element                                    | [XEP-0315](https://xmpp.org/extensions/xep-0315.html)  | n/a       | Allows to include XML-data in XEP-0004 data forms. |
| [Internet of Things - Sensor Data](iot.md)                | [XEP-0323](https://xmpp.org/extensions/xep-0323.html)  | n/a       | Sensor data interchange over XMPP. |
| [Internet of Things - Provisioning](iot.md)               | [XEP-0324](https://xmpp.org/extensions/xep-0324.html)  | n/a       | Provisioning, access rights and user privileges for the Internet of Things. |
| [Internet of Things - Control](iot.md)                    | [XEP-0325](https://xmpp.org/extensions/xep-0325.html)  | n/a       | Describes how to control devices or actuators in an XMPP-based sensor network. |
| Jid Prep                                                  | [XEP-0328](https://xmpp.org/extensions/xep-0328.html)  | 0.1       | Describes a way for an XMPP client to request an XMPP server to prep and normalize a given JID. |
| [HTTP over XMPP transport](hoxt.md)                       | [XEP-0332](https://xmpp.org/extensions/xep-0332.html)  | n/a       | Allows to transport HTTP communication over XMPP peer-to-peer networks. |
| Chat Markers                                              | [XEP-0333](https://xmpp.org/extensions/xep-0333.html)  | n/a       | A solution of marking the last received, displayed and acknowledged message in a chat. |
| Message Processing Hints                                  | [XEP-0334](https://xmpp.org/extensions/xep-0334.html)  | n/a       | Hints to entities routing or receiving a message. |
| JSON Containers                                           | [XEP-0335](https://xmpp.org/extensions/xep-0335.html)  | n/a       | Encapsulation of JSON data within XMPP Stanzas. |
| [Internet of Things - Discovery](iot.md)                  | [XEP-0347](https://xmpp.org/extensions/xep-0347.html)  | n/a       | Describes how Things can be installed and discovered by their owners. |
| Client State Indication                                   | [XEP-0352](https://xmpp.org/extensions/xep-0352.html)  | n/a       | A way for the client to indicate its active/inactive state. |
| [Push Notifications](pushnotifications.md)                | [XEP-0357](https://xmpp.org/extensions/xep-0357.html)  | n/a       | Defines a way to manage push notifications from an XMPP Server. |
| Stable and Unique Stanza IDs                              | [XEP-0359](https://xmpp.org/extensions/xep-0359.html)  | 0.5.0     | This specification describes unique and stable IDs for messages. |
| HTTP File Upload                                          | [XEP-0363](https://xmpp.org/extensions/xep-0363.html)  | 0.3.1     | Protocol to request permissions to upload a file to an HTTP server and get a shareable URL. |
| References                                                | [XEP-0372](https://xmpp.org/extensions/xep-0363.html)  | 0.2.0     | Add references like mentions or external data to stanzas. |
| Explicit Message Encryption                               | [XEP-0380](https://xmpp.org/extensions/xep-0380.html)  | 0.3.0     | Mark a message as explicitly encrypted. |
| [OpenPGP for XMPP](ox.md)                                 | [XEP-0373](https://xmpp.org/extensions/xep-0373.html)  | 0.3.2     | Utilize OpenPGP to exchange encrypted and signed content. |
| [OpenPGP for XMPP: Instant Messaging](ox-im.md)           | [XEP-0374](https://xmpp.org/extensions/xep-0374.html)  | 0.2.0     | OpenPGP encrypted Instant Messaging. |
| [Spoiler Messages](spoiler.md)                            | [XEP-0382](https://xmpp.org/extensions/xep-0382.html)  | 0.2.0     | Indicate that the body of a message should be treated as a spoiler. |
| [OMEMO Multi End Message and Object Encryption](omemo.md) | [XEP-0384](https://xmpp.org/extensions/xep-0384.html)  | n/a       | Encrypt messages using OMEMO encryption (currently only with smack-omemo-signal -> GPLv3). |
| [Consistent Color Generation](consistent_colors.md)       | [XEP-0392](https://xmpp.org/extensions/xep-0392.html)  | 0.6.0     | Generate consistent colors for identifiers like usernames to provide a consistent user experience. |
| [Message Markup](messagemarkup.md)                        | [XEP-0394](https://xmpp.org/extensions/xep-0394.html)  | 0.1.0     | Style message bodies while keeping body and markup information separated. |
| DNS Queries over XMPP (DoX)                               | [XEP-0418](https://xmpp.org/extensions/xep-0418.html)  | 0.1.0     | Send DNS queries and responses over XMPP. |
| Message Fastening                                         | [XEP-0422](https://xmpp.org/extensions/xep-0422.html)  | 0.1.1     | Mark payloads on a message to be logistically fastened to a previous message. |
| Message Retraction                                        | [XEP-0424](https://xmpp.org/extensions/xep-0424.html)  | 0.2.0     | Mark messages as retracted. |
| Fallback Indication                                       | [XEP-0428](https://xmpp.org/extensions/xep-0428.html)  | 0.1.0     | Declare body elements of a message as ignorable fallback for naive legacy clients. |

Unofficial XMPP Extensions
--------------------------

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------|
| [Multi-User Chat Light](muclight.md)        | [XEP-xxxx](https://mongooseim.readthedocs.io/en/latest/open-extensions/xeps/xep-muc-light.html) | n/a     | Multi-User Chats for mobile XMPP applications and specific environment. |
| Google GCM JSON payload                     | n/a                                                    | n/a       | Semantically the same as XEP-0335: JSON Containers. |

Legacy Smack Extensions and currently supported XEPs of smack-legacy
--------------------------------------------------------------------

If a XEP becomes 'Deprecated' or 'Obsolete' the code will be moved to the *smack-legacy* subproject.

| Name                                        | XEP                                                    | Version   | Description |
|---------------------------------------------|--------------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------|
| [Message Events](messageevents.md)          | [XEP-0022](https://xmpp.org/extensions/xep-0022.html)  | n/a       | Requests and responds to message events. |
| [Roster Item Exchange](rosterexchange.md)   | [XEP-0093](https://xmpp.org/extensions/xep-0093.html)  | n/a       | Allows roster data to be shared between users. |
