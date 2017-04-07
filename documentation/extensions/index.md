smackSmack Extensions User Manual
============================

The XMPP protocol includes a base protocol and many optional extensions
typically documented as "XEP's". Smack provides the org.jivesoftware.smack
package for the core XMPP protocol, and the org.jivesoftware.smackx package
for many of the protocol extensions.

This manual provides details about each of the "smackx" extensions, including
what it is, how to use it, and some simple example code.

Currently supported XEPs of Smack (all subprojects)
---------------------------------------------------

| Name                                        | XEP                                                      | Description |
|---------------------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Nonzas  | [XEP-0360](http://xmpp.org/extensions/xep-0360.html) | Defines the term "Nonza", describing every top level stream element that is not a Stanza.                                         |

Currently supported XEPs of smack-tcp
-------------------------------------

| Name                                        | XEP                                                      | Description |
|---------------------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [Stream Management](streammanagement.md)  | [XEP-0198](http://xmpp.org/extensions/xep-0198.html) | Allows active management of an XML Stream between two XMPP entities (stanza acknowledgement, stream resumption). |


Smack Extensions and currently supported XEPs of smack-extensions
-----------------------------------------------------------------

| Name                                        | XEP                                                      | Description |
|---------------------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [Data Forms](dataforms.md)                | [XEP-0004](http://xmpp.org/extensions/xep-0004.html) | Allows to gather data using Forms. |
| Last Activity                               | [XEP-0012](http://xmpp.org/extensions/xep-0012.html)     | Communicating information about the last activity associated with an XMPP entity. |
| Flexible Offline Message Retrieval          | [XEP-0013](http://xmpp.org/extensions/xep-0013.html)  | Extension for flexible, POP3-like handling of offline messages. |
| [Privacy Lists](privacy.md)               | [XEP-0016](http://xmpp.org/extensions/xep-0016.html) | Enabling or disabling communication with other entities. |
| [Service Discovery](disco.md)             | [XEP-0030](http://xmpp.org/extensions/xep-0030.html) | Allows to discover services in XMPP entities. |
| Extended Stanza Addressing                  | [XEP-0033](http://xmpp.org/extensions/xep-0033.html) | Allows to include headers in stanzas in order to specifiy multiple recipients or sub-addresses. |
| [Multi User Chat](muc.md)                 | [XEP-0045](http://xmpp.org/extensions/xep-0045.html) | Allows configuration of, participation in, and administration of individual text-based conference rooms. |
| In-Band Bytestreams                         | [XEP-0047](http://xmpp.org/extensions/xep-0047.html) | Enables any two entities to establish a one-to-one bytestream between themselves using plain XMPP. |
| Bookmarks                                   | [XEP-0048](http://xmpp.org/extensions/xep-0048.html)     | Bookmarks, for e.g. MUC and web pages. |
| [Private Data](privatedata.md)            | [XEP-0049](http://xmpp.org/extensions/xep-0049.html) | Manages private data. |
| Ad-Hoc Commands                             | [XEP-0050](http://xmpp.org/extensions/xep-0050.html) | Advertising and executing application-specific commands. |
| vcard-temp                                  | [XEP-0054](http://xmpp.org/extensions/xep-0054.html) | The vCard-XML format currently in use. |
| Jabber Search                               | [XEP-0055](http://xmpp.org/extensions/xep-0055.html) | Search information repositories on the XMPP network. |
| Result Set Management                       | [XEP-0059](http://xmpp.org/extensions/xep-0059.html) | Page through and otherwise manage the receipt of large result sets |
| [PubSub](pubsub.md)                       | [XEP-0060](http://xmpp.org/extensions/xep-0060.html) | Generic publish and subscribe functionality. |
| SOCKS5 Bytestrams                           | [XEP-0065](http://xmpp.org/extensions/xep-0065.html) | Out-of-band bytestream between any two XMPP entities. |
| [XHTML-IM](xhtml.md)                      | [XEP-0071](http://xmpp.org/extensions/xep-0071.html) | Allows send and receiving formatted messages using XHTML. |
| In-Band Registration                        | [XEP-0077](http://xmpp.org/extensions/xep-0077.html) | In-band registration with XMPP services. |
| Advanced Message Processing                 | [XEP-0079](http://xmpp.org/extensions/xep-0079.html) | Enables entities to request, and servers to perform, advanced processing of XMPP message stanzas. |
| User Location                               | [XEP-0080](http://xmpp.org/extensions/xep-0080.html) | Enabled communicating information about the current geographical or physical location of an entity. |
| XMPP Date Time Profiles                     | [XEP-0082](http://xmpp.org/extensions/xep-0082.html)     | Standardization of Date and Time representation in XMPP. |
| Chat State Notifications                    | [XEP-0085](http://xmpp.org/extensions/xep-0085.html) | Communicating the status of a user in a chat session. |
| [Time Exchange](time.md)                  | [XEP-0090](http://xmpp.org/extensions/xep-0090.html) | Allows local time information to be shared between users. |
| Software Version                            | [XEP-0092](http://xmpp.org/extensions/xep-0092.html)     | Retrieve and announce the software application of an XMPP entity. |
| Stream Initation                            | [XEP-0095](http://xmpp.org/extensions/xep-0095.html) | Initiating a data stream between any two XMPP entities. |
| [SI File Transfer](filetransfer.md)       | [XEP-0096](http://xmpp.org/extensions/xep-0096.html) | Transfer files between two users over XMPP. |
| [Entity Capabilities](caps.md)            | [XEP-0115](http://xmpp.org/extensions/xep-0115.html) | Broadcasting and dynamic discovery of entity capabilities. |
| Data Forms Validation                       | [XEP-0122](http://xmpp.org/extensions/xep-0122.html) | Enables an application to specify additional validation guidelines . |
| Service Administration                      | [XEP-0133](http://xmpp.org/extensions/xep-0133.html) | Recommended best practices for service-level administration of servers and components using Ad-Hoc Commands. |
| Stream Compression                          | [XEP-0138](http://xmpp.org/extensions/xep-0138.html) | Support for optional compression of the XMPP stream.
| Data Forms Layout                           | [XEP-0141](http://xmpp.org/extensions/xep-0141.html) | Enables an application to specify form layouts. |
| Personal Eventing Protocol                  | [XEP-0163](http://xmpp.org/extensions/xep-0163.html) | Using the XMPP publish-subscribe protocol to broadcast state change events associated with an XMPP account. |
| Message Delivery Receipts                   | [XEP-0184](http://xmpp.org/extensions/xep-0184.html) | Extension for message delivery receipts. The sender can request notification that the message has been delivered. |
| [Blocking Command](blockingcommand.md)      | [XEP-0191](http://xmpp.org/extensions/xep-0191.html) | Communications blocking that is intended to be simpler than privacy lists (XEP-0016). |
| XMPP Ping                                   | [XEP-0199](http://xmpp.org/extensions/xep-0199.html) | Sending application-level pings over XML streams.
| Entity Time                                 | [XEP-0202](http://xmpp.org/extensions/xep-0202.html) | Allows entities to communicate their local time |
| Delayed Delivery                            | [XEP-0203](http://xmpp.org/extensions/xep-0203.html) | Extension for communicating the fact that an XML stanza has been delivered with a delay. |
| XMPP Over BOSH                              | [XEP-0206](http://xmpp.org/extensions/xep-0206.html) | Use Bidirectional-streams Over Synchronous HTTP (BOSH) to transport XMPP stanzas. |
| Attention                                   | [XEP-0224](http://xmpp.org/extensions/xep-0224.html) | Getting attention of another user. |
| Bits of Binary                              | [XEP-0231](http://xmpp.org/extensions/xep-0231.html) | Including or referring to small bits of binary data in an XML stanza. |
| Best Practices for Resource Locking         | [XEP-0296](https://xmpp.org/extensions/xep-0296.html) | Specifies best practices to be followed by Jabber/XMPP clients about when to lock into, and unlock away from, resources. |
| Last Message Correction                     | [XEP-0308](http://xmpp.org/extensions/xep-0308.html) | Provides a method for indicating that a message is a correction of the last sent message. |
| [Group Chat Invitations](invitation.md)   | n/a                                                      | Send invitations to other users to join a group chat room. |
| [Jive Properties](properties.md)          | n/a                                                      | TODO |


Experimental Smack Extensions and currently supported XEPs of smack-experimental
--------------------------------------------------------------------------------

| Name                                        | XEP                                                      | Description |
|---------------------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Message Carbons                             | [XEP-0280](http://xmpp.org/extensions/xep-0280.html) | Keep all IM clients for a user engaged in a conversation, by carbon-copy outbound messages to all interested resources. |
| [Message Archive Management](mam.md)        | [XEP-0313](http://xmpp.org/extensions/xep-0313.html) | Query and control an archive of messages stored on a server. |
| [Internet of Things - Sensor Data](iot.md)  | [XEP-0323](http://xmpp.org/extensions/xep-0323.html) | Sensor data interchange over XMPP. |
| [Internet of Things - Provisioning](iot.md) | [XEP-0324](http://xmpp.org/extensions/xep-0324.html) | Provisioning, access rights and user priviliges for the Internet of Things. |
| [Internet of Things - Control](iot.md)      | [XEP-0325](http://xmpp.org/extensions/xep-0325.html) | Describes how to control devices or actuators in an XMPP-based sensor netowrk. |
| [HTTP over XMPP transport](hoxt.md)         | [XEP-0332](http://xmpp.org/extensions/xep-0332.html) | Allows to transport HTTP communication over XMPP peer-to-peer networks. |
| Chat Markers         			  			  | [XEP-0333](http://xmpp.org/extensions/xep-0333.html) | A solution of marking the last received, displayed and acknowledged message in a chat. |
| JSON Containers                             | [XEP-0335](http://xmpp.org/extensions/xep-0335.html) | Encapsulation of JSON data within XMPP Stanzas. |
| [Internet of Things - Discovery](iot.md)    | [XEP-0347](http://xmpp.org/extensions/xep-0347.html) | Describes how Things can be installed and discovered by their owners. |
| Client State Indication                     | [XEP-0352](http://xmpp.org/extensions/xep-0352.html) | A way for the client to indicate its active/inactive state. |
| [Push Notifications](pushnotifications.md)  | [XEP-0357](http://xmpp.org/extensions/xep-0357.html) |  Defines a way to manage push notifications from an XMPP Server. |
| HTTP File Upload                            | [XEP-0363](http://xmpp.org/extensions/xep-0363.html) | Protocol to request permissions to upload a file to an HTTP server and get a shareable URL. |
| [Multi-User Chat Light](muclight.md)        | [XEP-xxxx](http://mongooseim.readthedocs.io/en/latest/open-extensions/xeps/xep-muc-light.html) | Multi-User Chats for mobile XMPP applications and specific enviroment. |
| Google GCM JSON payload                     | n/a                                                  | Semantically the same as XEP-0335: JSON Containers |


Legacy Smack Extensions and currently supported XEPs of smack-legacy
--------------------------------------------------------------------

If a XEP becomes 'Deprecated' or 'Obsolete' the code will be moved to the *smack-legacy* subproject.

| Name                                        | XEP                                                      | Description |
|---------------------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [Message Events](messageevents.md)        | [XEP-0022](http://xmpp.org/extensions/xep-0022.html) | Requests and responds to message events. |
| [Roster Item Exchange](rosterexchange.md) | [XEP-0093](http://xmpp.org/extensions/xep-0093.html) | Allows roster data to be shared between users. |
