# Smack Changelog

# 4.4.5 -- 2022-03-02

### Bug

- [SMACK-923](https://igniterealtime.atlassian.net/browse/SMACK-923) Smack reactor should immediately handle scheduled actions that are due in zero milliseconds
- [SMACK-921](https://igniterealtime.atlassian.net/browse/SMACK-921) XmlStringBuilder.attribute\(String name, Enum<?> value\) should use value.toString\(\) \(and not value.name\(\)\)
- [SMACK-920](https://igniterealtime.atlassian.net/browse/SMACK-920) SASL GSSAPI mechanism should be marked to not require a password
- [SMACK-918](https://igniterealtime.atlassian.net/browse/SMACK-918) Self presences in MUC are no longer handled correctly

### Improvement

- [SMACK-922](https://igniterealtime.atlassian.net/browse/SMACK-922) Support 'optional text' and arbitrary element in Jingle 'reason' element
- [SMACK-919](https://igniterealtime.atlassian.net/browse/SMACK-919) PubSub ItemProvider should ignore character data in <item/>s

## 4.4.4 -- 2021-11-01

### Bug

-   [SMACK-916](https://igniterealtime.atlassian.net/browse/SMACK-916) -
    XMPPErrorException.stanza is missing a getter method
-   [SMACK-915](https://igniterealtgime.atlassian.net/browse/SMACK-915) -
    Smack does not process MUC destroy message if they contain
    \'status\'
-   [SMACK-914](https://igniterealtime.atlassian.net/browse/SMACK-914) -
    MultiUserChat may be become unjoinable due to a race condition
-   [SMACK-913](https://igniterealtime.atlassian.net/browse/SMACK-913) -
    MultiUserChat.serviceSupportsStableIds\\(\\) may throws a
    NullPointerException
-   [SMACK-912](https://igniterealtime.atlassian.net/browse/SMACK-912) -
    Smack does not start the local SOCKS5 proxy automatically
-   [SMACK-910](https://igniterealtime.atlassian.net/browse/SMACK-910) -
    FormNode and FormNodeProvide should handle non-existent DataForm
-   [SMACK-909](https://igniterealtime.atlassian.net/browse/SMACK-909) -
    Must use the raw character data of a form field in entity caps hash
    calculation

## 4.4.3 -- 2021-07-06

### Bug

-   [SMACK-905](https://igniterealtime.atlassian.net/browse/SMACK-905) -
    The class org.jivesoftware.smackx.offline.packet.OfflineMessageInfo
    has no ELEMENT, NAMESPACE or QNAME member
-   [SMACK-907](https://igniterealtime.atlassian.net/browse/SMACK-907) -
    Possible NPE in MultipleRecipientManager

## 4.4.2 -- 2021-03-25

### Bug

-   [SMACK-903](https://igniterealtime.atlassian.net/browse/SMACK-903) -
    StaxXmlPullParser.getNamespace() may throws IllegalArgumentException
-   [SMACK-904](https://igniterealtime.atlassian.net/browse/SMACK-904) -
    XEP-0096 file transfer fails because of a hidden ClastCastException

## 4.4.1 -- 2021-03-03

### Bug

-   [SMACK-895](https://igniterealtime.atlassian.net/browse/SMACK-895) -
    BoBIQ#getIQChildElementBuilder throws NPE when the BoB data does not
    contain 'max-age'.
-   [SMACK-896](https://igniterealtime.atlassian.net/browse/SMACK-896) -
    BoBDataExtension is missing getter for BoBData and ContentId
-   [SMACK-897](https://igniterealtime.atlassian.net/browse/SMACK-897) -
    DirectoryRosterStore.readEntry() should also catch
    IllegalArgumentException
-   [SMACK-898](https://igniterealtime.atlassian.net/browse/SMACK-898) -
    AbstractProvider should also consider TypeVariable
-   [SMACK-899](https://igniterealtime.atlassian.net/browse/SMACK-899) -
    NullPointerException in EntityCapsManager.addCapsExtension
-   [SMACK-900](https://igniterealtime.atlassian.net/browse/SMACK-900) -
    NPE in DataForm.Builder.addItem()
-   [SMACK-902](https://igniterealtime.atlassian.net/browse/SMACK-902) -
    DataFormProvider should retrieve the type of fields from
    \<reported/> elements if possible

### Improvement

-   [SMACK-901](https://igniterealtime.atlassian.net/browse/SMACK-901) -
    BoBDataExtension.from() should also allow IQs

## 4.4.0 -- 2020-12-06

### Bug

-   [SMACK-561](https://igniterealtime.atlassian.net/browse/SMACK-561) -
    Smack should not reply with multiple stream types after stream
    initiation is offered
-   [SMACK-624](https://igniterealtime.atlassian.net/browse/SMACK-624) -
    AdHocCommandManager\'s session sweeping thread does never stop
-   [SMACK-729](https://igniterealtime.atlassian.net/browse/SMACK-729) -
    Not all providers from smack-legacy.jar are loaded
-   [SMACK-770](https://igniterealtime.atlassian.net/browse/SMACK-770) -
    There is no Bits of Binary Extension Element provider registered
-   [SMACK-848](https://igniterealtime.atlassian.net/browse/SMACK-848) -
    Make MultiUserChat.leave() wait for response
-   [SMACK-874](https://igniterealtime.atlassian.net/browse/SMACK-874) -
    PacketParserUtilsTest#invalidXMLInMessageBody() fails on non-english
    machines
-   [SMACK-881](https://igniterealtime.atlassian.net/browse/SMACK-881) -
    Deadlock between reader and writer if Stream Mangement unacked
    stanza queue is full
-   [SMACK-888](https://igniterealtime.atlassian.net/browse/SMACK-888) -
    MUC roomDestroyed() callback is not invoked

### New Feature

-   [SMACK-257](https://igniterealtime.atlassian.net/browse/SMACK-257) -
    Add support for XEP-0118: User Tune
-   [SMACK-636](https://igniterealtime.atlassian.net/browse/SMACK-636) -
    Add support for XEP-0319: Last User Interaction in Presence
-   [SMACK-743](https://igniterealtime.atlassian.net/browse/SMACK-743) -
    Add support for XEP-0384: OMEMO Encryption
-   [SMACK-801](https://igniterealtime.atlassian.net/browse/SMACK-801) -
    Update Smack to Java 8
-   [SMACK-824](https://igniterealtime.atlassian.net/browse/SMACK-824) -
    Add support for XEP-0221: Data Forms Media Element
-   [SMACK-862](https://igniterealtime.atlassian.net/browse/SMACK-862) -
    Add support for XEP-0418: DNS Queries over XMPP (DoX)
-   [SMACK-871](https://igniterealtime.atlassian.net/browse/SMACK-871) -
    Add support for XEP-0350: Data Forms Geolocation Element
-   [SMACK-872](https://igniterealtime.atlassian.net/browse/SMACK-872) -
    Add support for XEP-0315: Data Forms XML Element
-   [SMACK-878](https://igniterealtime.atlassian.net/browse/SMACK-878) -
    Add support for XEP-0328: JID Prep
-   [SMACK-884](https://igniterealtime.atlassian.net/browse/SMACK-884) -
    Add support for XEP-0422: Message Fastening
-   [SMACK-885](https://igniterealtime.atlassian.net/browse/SMACK-885) -
    Add support for XEP-0420 Stanza Content Encryption
-   [SMACK-889](https://igniterealtime.atlassian.net/browse/SMACK-889) -
    Add support for XEP-0428: Fallback Indication

### Improvement

-   [SMACK-591](https://igniterealtime.atlassian.net/browse/SMACK-591) -
    Replace XPP3 by SmackXmlPullParser (wrapping Stax\'s XmlStreamReader
    and XPP3 on Android)
-   [SMACK-650](https://igniterealtime.atlassian.net/browse/SMACK-650) -
    Enable Java8\'s javadoc doclint
-   [SMACK-651](https://igniterealtime.atlassian.net/browse/SMACK-651) -
    Perform sound cross-compilation: Use newer javac\'s \--release
    feature
-   [SMACK-718](https://igniterealtime.atlassian.net/browse/SMACK-718) -
    Prevent extremely long reply timeouts from being set
-   [SMACK-821](https://igniterealtime.atlassian.net/browse/SMACK-821) -
    Make Forwarded a generic type
-   [SMACK-822](https://igniterealtime.atlassian.net/browse/SMACK-822) -
    Add API for XEP-0313 § 6.2 Advanced configuration via Ad-Hoc
    commands
-   [SMACK-825](https://igniterealtime.atlassian.net/browse/SMACK-825) -
    Discourage Stanza.getExtension(String, String) in favor of
    Stanza.getExtension(Class\<E extends ExtensionElement>)
-   [SMACK-826](https://igniterealtime.atlassian.net/browse/SMACK-826) -
    Add support for XEP-0373:\" OpenPGP for XMPP\" and XEP-0374:
    \"OpenPGP for XMPP Instant Messaging\"
-   [SMACK-828](https://igniterealtime.atlassian.net/browse/SMACK-828) -
    Add support for XEP-0107: User Mood
-   [SMACK-836](https://igniterealtime.atlassian.net/browse/SMACK-836) -
    Save a ServiceDiscoveryManager instance in a private field of
    MultiUserChatManger
-   [SMACK-839](https://igniterealtime.atlassian.net/browse/SMACK-839) -
    Provider.parse() should not throw a generic Exception, but instead
    IOException and XmlPullParserException
-   [SMACK-852](https://igniterealtime.atlassian.net/browse/SMACK-852) -
    Message thread and subject should be designed and implemented as
    ExtensionElements
-   [SMACK-854](https://igniterealtime.atlassian.net/browse/SMACK-854) -
    Rename smack-java7 to smack-java8
-   [SMACK-866](https://igniterealtime.atlassian.net/browse/SMACK-866) -
    Remove all tabs from the source code and add checkstyle rule that
    enforces no-tabs
-   [SMACK-867](https://igniterealtime.atlassian.net/browse/SMACK-867) -
    Extend HttpFileUploadManager by methods with InputStream parameter
-   [SMACK-882](https://igniterealtime.atlassian.net/browse/SMACK-882) -
    Add support for MUC status code 333
-   [SMACK-883](https://igniterealtime.atlassian.net/browse/SMACK-883) -
    Add generic MUC callback for \"participant left\" caused by
    unavailable presences
-   [SMACK-890](https://igniterealtime.atlassian.net/browse/SMACK-890) -
    Update Message Archive Management (XEP-0313) support to
    urn:xmpp:mam:2
-   [SMACK-892](https://igniterealtime.atlassian.net/browse/SMACK-892) -
    Smack performs unnecessary escaping in XML text

### Task

-   [SMACK-750](https://igniterealtime.atlassian.net/browse/SMACK-750) -
    Raise Smack\'s minimum required Android SDK level to 19 (Android
    4.4, Kit Kat, 2013-10)
-   [SMACK-840](https://igniterealtime.atlassian.net/browse/SMACK-840) -
    Remove smack-compression-jzlib, as it is obsolete (Smack uses Java 7
    de- and inflate API now)

## 4.3.4 -- 2019-05-27

### Bug

-   [SMACK-861](https://igniterealtime.atlassian.net/browse/SMACK-861) -
    Potential NPE in Roster.getPresencesInternal(BareJid)
-   [SMACK-863](https://igniterealtime.atlassian.net/browse/SMACK-863) -
    ServiceDiscoveryManger does not use the main identity, causing
    setIdentity() to have no effect
-   [SMACK-864](https://igniterealtime.atlassian.net/browse/SMACK-864) -
    Potential Denial of Service (DOS) by remote entities caused by
    unlimited threads for asynchronous operations
-   [SMACK-865](https://igniterealtime.atlassian.net/browse/SMACK-865) -
    Some Manager.getInsanceFor() methods are missing the
    \'synchronized\' keyword
-   [SMACK-868](https://igniterealtime.atlassian.net/browse/SMACK-868) -
    XHTMLText.appendOpenBodyTag() produces invalid XML
-   [SMACK-870](https://igniterealtime.atlassian.net/browse/SMACK-870) -
    TLS X.509 certificate verification should be performed with the ACE
    representation of the XMPP service domain when possible

### Improvement

-   [SMACK-869](https://igniterealtime.atlassian.net/browse/SMACK-869) -
    Exceptions in async tasks should not go uncaught to the call stack
    to avoid program termination

## 4.3.3 -- 2019-03-14

### Bug

-   [SMACK-856](https://igniterealtime.atlassian.net/browse/SMACK-856) -
    Smack fails under JDK 11 because com.sun.jndi.dns.DnsContextFactory
    is not inaccessible

### Improvement

-   [SMACK-858](https://igniterealtime.atlassian.net/browse/SMACK-858) -
    Dependency version specifier of jxmpp and MiniDNS include
    alpha/beta/\... versions of the follow up version when Maven is used
-   [SMACK-859](https://igniterealtime.atlassian.net/browse/SMACK-859) -
    MultiUserChat enter() should reset the timeout of the collector
    waiting for the final self presence to prevent timeouts for large
    MUCs

## 4.3.2 -- 2019-02-22

### Bug

-   [SMACK-842](https://igniterealtime.atlassian.net/browse/SMACK-842) -
    The RFC 3920 xml-not-well-formed error condition should be handled
    in stream error not a stanza error
-   [SMACK-843](https://igniterealtime.atlassian.net/browse/SMACK-843) -
    ManManager.pagePrevious() pages into the wrong direction
-   [SMACK-844](https://igniterealtime.atlassian.net/browse/SMACK-844) -
    Check if bounded unacknowledged stanzas queue is full before adding
    to it to avoid IllegalStateException
-   [SMACK-845](https://igniterealtime.atlassian.net/browse/SMACK-845) -
    Ensure that IQ response \'to\' address and ID are set correctly
-   [SMACK-846](https://igniterealtime.atlassian.net/browse/SMACK-846) -
    XMPPTCPConnection does not wait for stream features after
    authentication if compression is disabled
-   [SMACK-848](https://igniterealtime.atlassian.net/browse/SMACK-848) -
    Make MultiUserChat.leave() wait for response
-   [SMACK-850](https://igniterealtime.atlassian.net/browse/SMACK-850) -
    DeliveryReceiptManager should not send receipts with messages of
    type \'groupchat\'
-   [SMACK-855](https://igniterealtime.atlassian.net/browse/SMACK-855) -
    XMPPTCPConnection sometimes has two writer threads running

### Improvement

-   [SMACK-847](https://igniterealtime.atlassian.net/browse/SMACK-847) -
    Make TCP socket connection attempt interruptable
-   [SMACK-849](https://igniterealtime.atlassian.net/browse/SMACK-849) -
    Smack Local SOCKS5 Proxy thread should be marked as daemon thread

## 4.3.1 -- 2018-10-14

### Bug

-   [SMACK-833](https://igniterealtime.atlassian.net/browse/SMACK-833) -
    XMLUtil.prettyFormatXml() throws on some Android devices

### Improvement

-   [SMACK-829](https://igniterealtime.atlassian.net/browse/SMACK-829) -
    Disconnect BOSH client on shutdown
-   [SMACK-838](https://igniterealtime.atlassian.net/browse/SMACK-838) -
    FormField.getFirstValue() throws IndexOutOfBoundsException if there
    are no values

## 4.3.0 -- 2018-08-02

### Bug

-   [SMACK-759](https://igniterealtime.atlassian.net/browse/SMACK-759) -
    PubSubManager.getLeafNode() throws
    PubSubAssertionError.DiscoInfoNodeAssertionError if node exists but
    its not a PubSub Node
-   [SMACK-814](https://igniterealtime.atlassian.net/browse/SMACK-814) -
    NPE when using Node.getAffiliationsAsOwner()
-   [SMACK-815](https://igniterealtime.atlassian.net/browse/SMACK-815) -
    XEP-0184: DeliveryReceipt requires ID, although the XEP defines it
    as optional attribute
-   [SMACK-818](https://igniterealtime.atlassian.net/browse/SMACK-818) -
    EntityCapsManager sends presences with multiple CapsExtension
    causing disco#info lookup to fail
-   [SMACK-819](https://igniterealtime.atlassian.net/browse/SMACK-819) -
    ConcurrentModification Exception in MultiUserChatManager.java
-   [SMACK-820](https://igniterealtime.atlassian.net/browse/SMACK-820) -
    DNSUtil.setDaneProvider() does not set the DANE provider

### Task

-   [SMACK-769](https://igniterealtime.atlassian.net/browse/SMACK-769) -
    Rename XMPPError to StanzaError
-   [SMACK-776](https://igniterealtime.atlassian.net/browse/SMACK-776) -
    Remove deprecated reconnection callbacks in ConnectionListener

### Improvement

-   [SMACK-761](https://igniterealtime.atlassian.net/browse/SMACK-761) -
    Adopt ChatStateManager to new Chat API (chat2)
-   [SMACK-812](https://igniterealtime.atlassian.net/browse/SMACK-812) -
    Enable ModifierOrder checkstyle check
-   [SMACK-816](https://igniterealtime.atlassian.net/browse/SMACK-816) -
    SimplePayload should infer the XML element name and namespace

## 4.2.4 -- 2018-04-15

### Bug

-   [SMACK-804](https://igniterealtime.atlassian.net/browse/SMACK-804) -
    ServiceAdministrationManager does not use correct form actions
-   [SMACK-805](https://igniterealtime.atlassian.net/browse/SMACK-805) -
    ServiceDiscoveryManager.findService() only considers the first
    service by feature
-   [SMACK-813](https://igniterealtime.atlassian.net/browse/SMACK-813) -
    Smack uses hostname instead of XMPP service name for SNI

### New Feature

-   [SMACK-794](https://igniterealtime.atlassian.net/browse/SMACK-794) -
    Add support for XEP-0394: Message Markup
-   [SMACK-795](https://igniterealtime.atlassian.net/browse/SMACK-795) -
    Add support for XEP-0382: Spoiler messages
-   [SMACK-799](https://igniterealtime.atlassian.net/browse/SMACK-799) -
    Add support for XEP-0372: References
-   [SMACK-800](https://igniterealtime.atlassian.net/browse/SMACK-800) -
    Add support for XEP-0392: Consistent Color Generation

### Improvement

-   [SMACK-802](https://igniterealtime.atlassian.net/browse/SMACK-802) -
    Rename and deprecate: addPacketSendingListener(),
    removePacketSendingListener(), addPacketInterceptor() and
    removePacketInterceptor()
-   [SMACK-809](https://igniterealtime.atlassian.net/browse/SMACK-809) -
    Make Roster\'s non-roster presence map second-level map bounded

## 4.2.3 -- 2018-02-07

### Bug

-   [SMACK-788](https://igniterealtime.atlassian.net/browse/SMACK-788) -
    NullPointerException if hostAddresses is null
-   [SMACK-789](https://igniterealtime.atlassian.net/browse/SMACK-789) -
    AffiliationsExtension toXml() produces invalid XML
-   [SMACK-790](https://igniterealtime.atlassian.net/browse/SMACK-790) -
    Some HTTP File Upload elements are not correctly parsed and
    serialized
-   [SMACK-791](https://igniterealtime.atlassian.net/browse/SMACK-791) -
    NumberFormatException in IpAddressUtil.isIPv4LiteralAddress
-   [SMACK-796](https://igniterealtime.atlassian.net/browse/SMACK-796) -
    SOCKS5 authentication erroneously uses \'user\' when it should use
    \'passwd\', causes authentication to fail

## 4.2.2 -- 2017-11-25

### Bug

-   [SMACK-775](https://igniterealtime.atlassian.net/browse/SMACK-775) -
    Create callback interface for ReconnectionManager
-   [SMACK-778](https://igniterealtime.atlassian.net/browse/SMACK-778) -
    ReconnectionManager.reconnect() can throw NotConnectedException
-   [SMACK-779](https://igniterealtime.atlassian.net/browse/SMACK-779) -
    smack-android erroneously depends on smack-omemo and
    smack-omemo-signal
-   [SMACK-780](https://igniterealtime.atlassian.net/browse/SMACK-780) -
    PushNotificationManager\'s isSupported logic does query the server,
    whereas it should query the bare JID
-   [SMACK-781](https://igniterealtime.atlassian.net/browse/SMACK-781) -
    MiniDnsResolver does not correctly handle the case when NOERROR is
    returned together with an empty answer section.
-   [SMACK-782](https://igniterealtime.atlassian.net/browse/SMACK-782) -
    MultiUserChat does not remove the subject listener causing a memory
    leak
-   [SMACK-783](https://igniterealtime.atlassian.net/browse/SMACK-783) -
    InvitationRejectionListener fires multiple times
-   [SMACK-784](https://igniterealtime.atlassian.net/browse/SMACK-784) -
    StringUtils.numbersAndLetters has the numbers twice, resulting in a
    lower entropy
-   [SMACK-785](https://igniterealtime.atlassian.net/browse/SMACK-785) -
    OfflineMessageManager.getMessages() does count the pending messages
    incorrectly, causing an unnecessary delay
-   [SMACK-786](https://igniterealtime.atlassian.net/browse/SMACK-786) -
    Race condition when resuming a stream
-   [SMACK-787](https://igniterealtime.atlassian.net/browse/SMACK-787) -
    Presence.getPriority() may return Integer.MIN_VALUE.

## 4.2.1 -- 2017-08-14

### Bug

-   [SMACK-749](https://igniterealtime.atlassian.net/browse/SMACK-749) -
    SCRAM-SHA-1 and SCRAM-SHA-1-PLUS SASL mechanisms have the same
    priority, causing SASL authentication failures
-   [SMACK-755](https://igniterealtime.atlassian.net/browse/SMACK-755) -
    DIGEST-MD5 sometimes causes malformed request server response
-   [SMACK-756](https://igniterealtime.atlassian.net/browse/SMACK-756) -
    IoTIsFriendResponse has invalid name and produces invalid XML
-   [SMACK-759](https://igniterealtime.atlassian.net/browse/SMACK-759) -
    PubSubManager.getLeafNode() throws
    PubSubAssertionError.DiscoInfoNodeAssertionError if node exists but
    its not a PubSub Node
-   [SMACK-764](https://igniterealtime.atlassian.net/browse/SMACK-764) -
    NPE in hashCode() in Occupant when jid is null
-   [SMACK-766](https://igniterealtime.atlassian.net/browse/SMACK-766) -
    Smack possibly includes \'ask\' attribute in roster items when
    sending requests
-   [SMACK-768](https://igniterealtime.atlassian.net/browse/SMACK-768) -
    Smack throws NoResponse timeout when waiting for IQ although there
    was a response
-   [SMACK-771](https://igniterealtime.atlassian.net/browse/SMACK-771) -
    XMPPTCPConnection should use KeyManagerFactory.getDefaultAlgorithm()
    instead of KeyManagerFactory.getInstance(\"sunX509\");
-   [SMACK-772](https://igniterealtime.atlassian.net/browse/SMACK-772) -
    HostAddress must deal with \'fqdn\' being null.
-   [SMACK-773](https://igniterealtime.atlassian.net/browse/SMACK-773) -
    Allow roster pushes from our full JID for backwards compatibility
-   [SMACK-774](https://igniterealtime.atlassian.net/browse/SMACK-774) -
    HTTP File Upload\'s SlotRequest metadata should be attributes not
    child elements

### New Feature

-   [SMACK-746](https://igniterealtime.atlassian.net/browse/SMACK-746) -
    Add support for XEP-0380: Explicit Message Encryption
-   [SMACK-758](https://igniterealtime.atlassian.net/browse/SMACK-758) -
    Add support for XEP-0334: Message Processing Hints
-   [SMACK-760](https://igniterealtime.atlassian.net/browse/SMACK-760) -
    Smack does not allow custom extension elements in SM\'s \<failed/>

### Improvement

-   [SMACK-752](https://igniterealtime.atlassian.net/browse/SMACK-752) -
    XEP-0357 Push Notification enable IQ uses wrong form type: Should be
    \'submit\' instead of \'form\'
-   [SMACK-754](https://igniterealtime.atlassian.net/browse/SMACK-754) -
    Allow MUC room subject changes from the MUCs bare JID
-   [SMACK-777](https://igniterealtime.atlassian.net/browse/SMACK-777) -
    MamManager should use the user\'s bare JID to check if MAM is
    supported

## 4.2.0 -- 2017-03-10

## Sub-task

-   [SMACK-639](https://igniterealtime.atlassian.net/browse/SMACK-639) -
    Add support for pre-approved subscription requests (RFC 6121 § 3.4)

### Bug

-   [SMACK-306](https://igniterealtime.atlassian.net/browse/SMACK-306) -
    loadRosterOnLogin has non-trivial side effect on getRoster
-   [SMACK-416](https://igniterealtime.atlassian.net/browse/SMACK-416) -
    Refactor PEP to make it use the existing pubsub API.
-   [SMACK-674](https://igniterealtime.atlassian.net/browse/SMACK-674) -
    PubSub Affiliation extension element is missing \'jid\' attribute,
    and is using wrong element name \'subscription\'
-   [SMACK-682](https://igniterealtime.atlassian.net/browse/SMACK-682) -
    Add support for \"XEP-0360: Nonzas (are not Stanzas)\"
-   [SMACK-683](https://igniterealtime.atlassian.net/browse/SMACK-683) -
    Using a Proxy with XMPPTCPConnection failes with \"SocketException:
    Unconnected sockets not implemented\"
-   [SMACK-691](https://igniterealtime.atlassian.net/browse/SMACK-691) -
    Add support for MUCItem\'s Actor \'nick\'
-   [SMACK-705](https://igniterealtime.atlassian.net/browse/SMACK-705) -
    PubSub\'s Affiliation.getElementName() returns wrong name
-   [SMACK-722](https://igniterealtime.atlassian.net/browse/SMACK-722) -
    SASL X-OAUTH2 implementation incorrectly performs Base64 encoding
    twice
-   [SMACK-723](https://igniterealtime.atlassian.net/browse/SMACK-723) -
    Support \"Caps Optimizations\" (XEP-0115 § 8.4)
-   [SMACK-724](https://igniterealtime.atlassian.net/browse/SMACK-724) -
    Do not re-use the Socket after connect() failed.
-   [SMACK-725](https://igniterealtime.atlassian.net/browse/SMACK-725) -
    ReconnectionManager should handle AlreadyConnectedException and
    AlreadyLoggedInException not as failure
-   [SMACK-741](https://igniterealtime.atlassian.net/browse/SMACK-741) -
    Ad-hoc command \'note\' element \'type\' attribute should be treated
    as optional
-   [SMACK-745](https://igniterealtime.atlassian.net/browse/SMACK-745) -
    Memory leak in MultiUserChat

### New Feature

-   [SMACK-366](https://igniterealtime.atlassian.net/browse/SMACK-366) -
    Add support for DNSSEC.
-   [SMACK-610](https://igniterealtime.atlassian.net/browse/SMACK-610) -
    Add support for XEP-0080: User Location
-   [SMACK-619](https://igniterealtime.atlassian.net/browse/SMACK-619) -
    Add roomDestroyed to MUC UserStatusListener
-   [SMACK-625](https://igniterealtime.atlassian.net/browse/SMACK-625) -
    Add support for XEP-313: Message Archive Management
-   [SMACK-675](https://igniterealtime.atlassian.net/browse/SMACK-675) -
    Add support for PubSub affiliation actions as owner
-   [SMACK-677](https://igniterealtime.atlassian.net/browse/SMACK-677) -
    Add support for SASL \'authzid\' (Authorization Identity)
-   [SMACK-690](https://igniterealtime.atlassian.net/browse/SMACK-690) -
    Add support for DNS-Based Authentication of Named Entities (DANE,
    RFC 6698)
-   [SMACK-731](https://igniterealtime.atlassian.net/browse/SMACK-731) -
    Add support for XEP-0191: Blocking Command
-   [SMACK-732](https://igniterealtime.atlassian.net/browse/SMACK-732) -
    Smack should be able to handle \"single equals sign\" SASL responses
-   [SMACK-740](https://igniterealtime.atlassian.net/browse/SMACK-740) -
    Add support for Multi-User Chat Light
-   [SMACK-742](https://igniterealtime.atlassian.net/browse/SMACK-742) -
    Add support for XEP-0133: Service Administration
-   [SMACK-747](https://igniterealtime.atlassian.net/browse/SMACK-747) -
    Add support for XEP-0363: HTTP File Upload

### Task

-   [SMACK-638](https://igniterealtime.atlassian.net/browse/SMACK-638) -
    Call connection creation listeners from within
    AbstractXMPPConnection\'s constructor
-   [SMACK-644](https://igniterealtime.atlassian.net/browse/SMACK-644) -
    Throw exception if account creation or password change is performed
    over insecure connections
-   [SMACK-655](https://igniterealtime.atlassian.net/browse/SMACK-655) -
    Enable StreamManagement by default

### Improvement

-   [SMACK-372](https://igniterealtime.atlassian.net/browse/SMACK-372) -
    Make package protected methods in PEPItem public
-   [SMACK-572](https://igniterealtime.atlassian.net/browse/SMACK-572) -
    Rejoin MUC rooms after reconnect
-   [SMACK-628](https://igniterealtime.atlassian.net/browse/SMACK-628) -
    Rework Roster handling with anonymous connections
-   [SMACK-629](https://igniterealtime.atlassian.net/browse/SMACK-629) -
    Rework how Smack handles anonymous connections
-   [SMACK-631](https://igniterealtime.atlassian.net/browse/SMACK-631) -
    Improve ParsingExceptionCallback, allow it to be a functional
    interface
-   [SMACK-632](https://igniterealtime.atlassian.net/browse/SMACK-632) -
    Make Smack interruptible
-   [SMACK-633](https://igniterealtime.atlassian.net/browse/SMACK-633) -
    Allow clean and graceful disconnects (stream closing)
-   [SMACK-634](https://igniterealtime.atlassian.net/browse/SMACK-634) -
    Use jxmpp-jid, add Jid class to replace String\'s being used as JIDs
-   [SMACK-646](https://igniterealtime.atlassian.net/browse/SMACK-646) -
    Add support for MUC roomnick rewrite
-   [SMACK-647](https://igniterealtime.atlassian.net/browse/SMACK-647) -
    Don\'t automatically call login() on connect() if the connection was
    authenticated before
-   [SMACK-648](https://igniterealtime.atlassian.net/browse/SMACK-648) -
    Improve MultiUserChat API
-   [SMACK-657](https://igniterealtime.atlassian.net/browse/SMACK-657) -
    Rename RosterEntry.getStatus and RosterPacket.ItemStatus to
    ItemAskStatus
-   [SMACK-663](https://igniterealtime.atlassian.net/browse/SMACK-663) -
    Roster should be fully loaded when
    Roster.getInstanceFor(XMPPConnection) is called with a authenticated
    connection
-   [SMACK-665](https://igniterealtime.atlassian.net/browse/SMACK-665) -
    Rename \'serviceName\' to \'xmppServiceDomain\'
-   [SMACK-666](https://igniterealtime.atlassian.net/browse/SMACK-666) -
    Typo in \'RosterEntries.rosterEntires()\', change to
    \'RosterEntries.rosterEntries()\'
-   [SMACK-703](https://igniterealtime.atlassian.net/browse/SMACK-703) -
    Limit the stored presences of entities not in Roster
-   [SMACK-704](https://igniterealtime.atlassian.net/browse/SMACK-704) -
    Pass down Message stanza in ChatStateListener
-   [SMACK-711](https://igniterealtime.atlassian.net/browse/SMACK-711) -
    Improve the logging of TCP connection attempts.
-   [SMACK-720](https://igniterealtime.atlassian.net/browse/SMACK-720) -
    Improve support for Tor and Hidden Services.
-   [SMACK-721](https://igniterealtime.atlassian.net/browse/SMACK-721) -
    Report illegal Stream Management states to avoid OOM Exception
-   [SMACK-727](https://igniterealtime.atlassian.net/browse/SMACK-727) -
    Add partial support for the IoT XEPs (XEP-0323, -0324, -0325, -0347)
-   [SMACK-733](https://igniterealtime.atlassian.net/browse/SMACK-733) -
    Handle outgoing \'unavailable\' Presences in Roster
-   [SMACK-736](https://igniterealtime.atlassian.net/browse/SMACK-736) -
    Add support for Chat Markers (XEP-0333)
-   [SMACK-737](https://igniterealtime.atlassian.net/browse/SMACK-737) -
    Add support for Bits of Binary (XEP-0231)
-   [SMACK-738](https://igniterealtime.atlassian.net/browse/SMACK-738) -
    Add support for Push Notifications (XEP-0357)

## 4.1.9 -- 2016-11-19

### Bug

-   [SMACK-739](https://igniterealtime.atlassian.net/browse/SMACK-739) -
    Smack starts SASL step without TLS in case STARTTLS is stripped even
    if SecurityMode.Required is used
-   [SMACK-735](https://igniterealtime.atlassian.net/browse/SMACK-735) -
    Smack sometimes sends invalid SCRAM-SHA1 nonce

## 4.1.8 -- 2016-07-30

### Bug

-   [SMACK-722](https://igniterealtime.atlassian.net/browse/SMACK-722) -
    SASL X-OAUTH2 implementation incorrectly performs Base64 encoding
    twice
-   [SMACK-724](https://igniterealtime.atlassian.net/browse/SMACK-724) -
    Do not re-use the Socket after connect() failed.
-   [SMACK-725](https://igniterealtime.atlassian.net/browse/SMACK-725) -
    ReconnectionManager should handle AlreadyConnectedException and
    AlreadyLoggedInException not as failure
-   [SMACK-726](https://igniterealtime.atlassian.net/browse/SMACK-726) -
    \'purge\' and \'remove\' IQ of XEP-0013 must be of type \'set\'

## 4.1.7 -- 2016-04-14

### Bug

-   [SMACK-712](https://igniterealtime.atlassian.net/browse/SMACK-712) -
    XMPPTCPConnection\'s setEnabledSSL(Protocols\|Ciphers) has no effect
-   [SMACK-716](https://igniterealtime.atlassian.net/browse/SMACK-716) -
    EntityTimeManager.getTime() does not set the recipients JID
-   [SMACK-719](https://igniterealtime.atlassian.net/browse/SMACK-719) -
    XMPPError should use Locale.US in toUpperCase()

### Improvement

-   [SMACK-715](https://igniterealtime.atlassian.net/browse/SMACK-715) -
    Add Roster.setRosterLoadedAtLoginDefault(boolean)

## 4.1.6 -- 2016-01-23

### Bug

-   [SMACK-705](https://igniterealtime.atlassian.net/browse/SMACK-705) -
    PubSub\'s Affiliation.getElementName() returns wrong name
-   [SMACK-706](https://igniterealtime.atlassian.net/browse/SMACK-706) -
    Smack may sends \<bind/> and \<session/> twice if Stream Management
    is used and a previous SM state exists
-   [SMACK-707](https://igniterealtime.atlassian.net/browse/SMACK-707) -
    Infinite loop of NullPointerExceptions in Socks5Proxy
-   [SMACK-708](https://igniterealtime.atlassian.net/browse/SMACK-708) -
    DeliveryReceipt(Manager) should ensure that receipts (and requests)
    have an ID set
-   [SMACK-709](https://igniterealtime.atlassian.net/browse/SMACK-709) -
    Don\'t request delivery receipts for messages without a body
-   [SMACK-710](https://igniterealtime.atlassian.net/browse/SMACK-710) -
    SASL DIGEST-MD5 backslash must be quoted

## 4.1.5 -- 2015-11-22

### Bug

-   [SMACK-698](https://igniterealtime.atlassian.net/browse/SMACK-698) -
    Time creates invalid XML
-   [SMACK-700](https://igniterealtime.atlassian.net/browse/SMACK-700) -
    Duplicate stanzas in unacknowledgedStanzas queue when stream is
    resumed
-   [SMACK-702](https://igniterealtime.atlassian.net/browse/SMACK-702) -
    RejectedExecutionException in AbstractXMPPConnection.processPacket()
    causes connection Termination

## 4.1.4 -- 2015-09-14

### Bug

-   [SMACK-688](https://igniterealtime.atlassian.net/browse/SMACK-688) -
    Reset carbons state if session got not resumed or cleanly
    disconnected
-   [SMACK-689](https://igniterealtime.atlassian.net/browse/SMACK-689) -
    PEPPubSub creates malformed XML
-   [SMACK-693](https://igniterealtime.atlassian.net/browse/SMACK-693) -
    MultiUserChat\'s UserStatusListener is not getting triggered
-   [SMACK-695](https://igniterealtime.atlassian.net/browse/SMACK-695) -
    JSON and GCM parser does an erroneous extra next()
-   [SMACK-697](https://igniterealtime.atlassian.net/browse/SMACK-697) -
    PrivacyListManager should handle the case where not default and
    active list are currently set

### Improvement

-   [SMACK-686](https://igniterealtime.atlassian.net/browse/SMACK-686) -
    Provide a hint that connect() needs to be called prior login() in
    NotConnectedException
-   [SMACK-687](https://igniterealtime.atlassian.net/browse/SMACK-687) -
    Update to jxmpp 0.4.2
-   [SMACK-696](https://igniterealtime.atlassian.net/browse/SMACK-696) -
    Drop stream state after stream error

## 4.1.3 -- 2015-07-15

### Bug

-   [SMACK-679](https://igniterealtime.atlassian.net/browse/SMACK-679) -
    Memory leak in Socks5BytestreamManager. Should use weak map for
    \'managers\'
-   [SMACK-680](https://igniterealtime.atlassian.net/browse/SMACK-680) -
    XHTML bodies are un-escaped after parsing
-   [SMACK-681](https://igniterealtime.atlassian.net/browse/SMACK-681) -
    Roster presence callbacks may not be invoked right after login

## 4.1.2 -- 2015-06-27

### Bug

-   [SMACK-664](https://igniterealtime.atlassian.net/browse/SMACK-664) -
    Invalid IQ error response to OfferRequestPacket and
    OfferRevokePacket
-   [SMACK-668](https://igniterealtime.atlassian.net/browse/SMACK-668) -
    ReconnectionManager\'s value of \'attempts\' is not reset after
    successful reconnection
-   [SMACK-669](https://igniterealtime.atlassian.net/browse/SMACK-669) -
    Only add Entity Capabilities extension to available presences
-   [SMACK-670](https://igniterealtime.atlassian.net/browse/SMACK-670) -
    SASLMechanism.authenticate should treat an empty byte array like
    \'null\' byte array
-   [SMACK-672](https://igniterealtime.atlassian.net/browse/SMACK-672) -
    Memory leak caused by RosterGroup declaring a strong reference to
    XMPPConnection
-   [SMACK-673](https://igniterealtime.atlassian.net/browse/SMACK-673) -
    VCard API does not support all elements
-   [SMACK-676](https://igniterealtime.atlassian.net/browse/SMACK-676) -
    ConcurrentModificationException in ServerPingWithAlarmManager
-   [SMACK-678](https://igniterealtime.atlassian.net/browse/SMACK-678) -
    Login hangs if starttls advertised, but security is set to
    \'disabled\' and compression is also advertised

### Improvement

-   [SMACK-667](https://igniterealtime.atlassian.net/browse/SMACK-667) -
    Request Stream Mangement Acknowledgement after re-sending unack\'ed
    stanzas after stream resumption
-   [SMACK-671](https://igniterealtime.atlassian.net/browse/SMACK-671) -
    Don\'t disable Scoks5BytestreamManager on connection termination

## 4.1.1 -- 2015-05-09

### Bug

-   [SMACK-649](https://igniterealtime.atlassian.net/browse/SMACK-649) -
    DIGEST-MD5 challenge/response parsing must handle linear white
    spaces after the comma
-   [SMACK-652](https://igniterealtime.atlassian.net/browse/SMACK-652) -
    SynchronizationPoint should use signalAll
-   [SMACK-653](https://igniterealtime.atlassian.net/browse/SMACK-653) -
    Integer overflow if both client and server don\'t specify a max
    resumption time
-   [SMACK-654](https://igniterealtime.atlassian.net/browse/SMACK-654) -
    isSmResumptionPossible() returns wrong values
-   [SMACK-656](https://igniterealtime.atlassian.net/browse/SMACK-656) -
    DeliveryReceipts auto add should use packet interceptors and should
    not be requested for messages with ACKs.
-   [SMACK-659](https://igniterealtime.atlassian.net/browse/SMACK-659) -
    Memory leak caused by RosterEntry declaring a strong reference to
    XMPPConnection
-   [SMACK-660](https://igniterealtime.atlassian.net/browse/SMACK-660) -
    ReconnectionManager\'s RANDOM_INCREASING_DELAY is erroneously using
    a fixed value.
-   [SMACK-661](https://igniterealtime.atlassian.net/browse/SMACK-661) -
    Add method to set ProxyInfo in ConnectionConfiguration.Builder
-   [SMACK-662](https://igniterealtime.atlassian.net/browse/SMACK-662) -
    RosterEntry.setName() does not change the name

## 4.1.0 -- 2015-03-29

## Sub-task

-   [SMACK-398](https://igniterealtime.atlassian.net/browse/SMACK-398) -
    Implement SCRAM support

### Bug

-   [SMACK-65](https://igniterealtime.atlassian.net/browse/SMACK-65) -
    Packet parsing should look for depth
-   [SMACK-237](https://igniterealtime.atlassian.net/browse/SMACK-237) -
    Handle more vCard values (XEP-0054)
-   [SMACK-383](https://igniterealtime.atlassian.net/browse/SMACK-383) -
    Allow the garbage collection of all object instances of a closed and
    unreferenced connection
-   [SMACK-424](https://igniterealtime.atlassian.net/browse/SMACK-424) -
    Add a MultiUserChat.presenceChanged callback method to be informed
    if a presence within a MUC has changed (joined, leaved, status
    change)
-   [SMACK-542](https://igniterealtime.atlassian.net/browse/SMACK-542) -
    MUC: RoomInfo should hold more data if the result contains a
    FORM_TYPE field
-   [SMACK-549](https://igniterealtime.atlassian.net/browse/SMACK-549) -
    MUCUser#getStatus should be a List
-   [SMACK-564](https://igniterealtime.atlassian.net/browse/SMACK-564) -
    Some tests fail with Java 8
-   [SMACK-570](https://igniterealtime.atlassian.net/browse/SMACK-570) -
    Smack does not support resourceparts which contain the \'@\'
    character.
-   [SMACK-571](https://igniterealtime.atlassian.net/browse/SMACK-571) -
    Don\'t remove the MUC listeners after a disconnect() , keep state of
    Connection between disconnect() and connect()/login()
-   [SMACK-573](https://igniterealtime.atlassian.net/browse/SMACK-573) -
    MessageEventManager treats error replies as message events
-   [SMACK-583](https://igniterealtime.atlassian.net/browse/SMACK-583) -
    PacketListeners may not be invoked in delivery order
-   [SMACK-585](https://igniterealtime.atlassian.net/browse/SMACK-585) -
    XMPPTCPConnection does not set \'host\' and \'port\'
-   [SMACK-590](https://igniterealtime.atlassian.net/browse/SMACK-590) -
    Don\'t use IQReplyFilter for the bind set/result exchange
-   [SMACK-597](https://igniterealtime.atlassian.net/browse/SMACK-597) -
    PingManager.getLastReceivedPong() always returns -1
-   [SMACK-604](https://igniterealtime.atlassian.net/browse/SMACK-604) -
    MUCUser must support multiple status codes
-   [SMACK-620](https://igniterealtime.atlassian.net/browse/SMACK-620) -
    Smack should use a safe SAX parser, e.g. with entity reference
    expansion disabled
-   [SMACK-635](https://igniterealtime.atlassian.net/browse/SMACK-635) -
    Typo DNSUtil.init() prevents DNS SRV lookups to fail in some cases
-   [SMACK-643](https://igniterealtime.atlassian.net/browse/SMACK-643) -
    Smack should not set the service name to the vale of the \'from\'
    attribute of the opening stream element received from the service

### Improvement

-   [SMACK-340](https://igniterealtime.atlassian.net/browse/SMACK-340) -
    Should make the wait/timeouts on SASL authentication configurable.
-   [SMACK-402](https://igniterealtime.atlassian.net/browse/SMACK-402) -
    Update obsolete \"Message Delivery Receipts\" support from
    (JEP\|XEP)-0022 to XEP-0184
-   [SMACK-453](https://igniterealtime.atlassian.net/browse/SMACK-453) -
    Add support for all primitive types in
    IntrospectionProvider.decode()
-   [SMACK-521](https://igniterealtime.atlassian.net/browse/SMACK-521) -
    Clear PacketWriters queue when the connection is shut down
-   [SMACK-532](https://igniterealtime.atlassian.net/browse/SMACK-532) -
    Evaluate if its possible to guarantee the order of listeners by
    using a LinkedHashMap
-   [SMACK-566](https://igniterealtime.atlassian.net/browse/SMACK-566) -
    Create public method that parses Strings/CharSequences to messages,
    IQs and presence instances
-   [SMACK-587](https://igniterealtime.atlassian.net/browse/SMACK-587) -
    Subprojects should uses versions when importing the OSGi smack-core
    components
-   [SMACK-595](https://igniterealtime.atlassian.net/browse/SMACK-595) -
    Add an API to send a stanza and wait asynchronously for a response
-   [SMACK-599](https://igniterealtime.atlassian.net/browse/SMACK-599) -
    Provide string messages to all exceptions thrown by Smack
-   [SMACK-600](https://igniterealtime.atlassian.net/browse/SMACK-600) -
    RoomInfo Class should add the information from the Identity element.
-   [SMACK-608](https://igniterealtime.atlassian.net/browse/SMACK-608) -
    Add support for XMPP error conditions text
-   [SMACK-622](https://igniterealtime.atlassian.net/browse/SMACK-622) -
    Add support for \'optional\' in session stream features
-   [SMACK-626](https://igniterealtime.atlassian.net/browse/SMACK-626) -
    Add support for \'ofrom\' Extended Stanza Addressing type
-   [SMACK-627](https://igniterealtime.atlassian.net/browse/SMACK-627) -
    Smack should allow null usernames under certain circumstances
-   [SMACK-645](https://igniterealtime.atlassian.net/browse/SMACK-645) -
    Roster should not leak internal state e.g. presences

### New Feature

-   [SMACK-234](https://igniterealtime.atlassian.net/browse/SMACK-234) -
    Add support for SASL EXTERNAL: PKI (Client SSL Cert) Support
-   [SMACK-333](https://igniterealtime.atlassian.net/browse/SMACK-333) -
    Implement XEP-0198: Stream Management
-   [SMACK-378](https://igniterealtime.atlassian.net/browse/SMACK-378) -
    Give access to the socket outside the XMPPconnection
-   [SMACK-581](https://igniterealtime.atlassian.net/browse/SMACK-581) -
    Add support for \"Result Set Management\" (XEP-59)
-   [SMACK-607](https://igniterealtime.atlassian.net/browse/SMACK-607) -
    Add support for XEP-0352: Client State Indication
-   [SMACK-612](https://igniterealtime.atlassian.net/browse/SMACK-612) -
    Add support for XEP-0141: Data Forms Layout
-   [SMACK-621](https://igniterealtime.atlassian.net/browse/SMACK-621) -
    Add support for XEP-0122: Data Forms Validation
-   [SMACK-623](https://igniterealtime.atlassian.net/browse/SMACK-623) -
    Add API to retrieve the subscriptions of a PubSub node as owner

### Task

-   [SMACK-365](https://igniterealtime.atlassian.net/browse/SMACK-365) -
    SmackConfiguration should only report errors if the file fails to
    load, not when it fails to load for a specific classloader.
-   [SMACK-371](https://igniterealtime.atlassian.net/browse/SMACK-371) -
    Some MUC tasks are using stanza\'s as defined in an older version of
    the spec. Fails to work on some servers.
-   [SMACK-569](https://igniterealtime.atlassian.net/browse/SMACK-569) -
    Move Message Event code to legacy subproject
-   [SMACK-578](https://igniterealtime.atlassian.net/browse/SMACK-578) -
    Remove decorators for \"Legacy Delayed Delivery\" (XEP-91) in favor
    of Delayed Delivery (XEP-203)
-   [SMACK-579](https://igniterealtime.atlassian.net/browse/SMACK-579) -
    FileTransferManager and FileTransferNegoiator should use
    WeakHashMaps and extend Manager
-   [SMACK-582](https://igniterealtime.atlassian.net/browse/SMACK-582) -
    Change ReceiptReceivedListener.onReceiptReceived parameters
-   [SMACK-637](https://igniterealtime.atlassian.net/browse/SMACK-637) -
    Move Roster and Chat code to new smack-im subproject

## 4.0.7 -- 2015-02-20

### Bug

-   [SMACK-635](https://igniterealtime.atlassian.net/browse/SMACK-635) -
    Typo DNSUtil.init() prevents DNS SRV lookups to fail in some cases
-   [SMACK-643](https://igniterealtime.atlassian.net/browse/SMACK-643) -
    Smack should not set the service name to the vale of the \'from\'
    attribute of the opening stream element received from the service

## 4.0.6 -- 2014-11-23

### Bug

-   [SMACK-616](https://igniterealtime.atlassian.net/browse/SMACK-616) -
    Smack should fallback to using host with default port if DNS SRV
    lookup fails
-   [SMACK-617](https://igniterealtime.atlassian.net/browse/SMACK-617) -
    Message Digest in EntityCapsManager should be synchronized

## 4.0.5 -- 2014-10-22

### Bug

-   [SMACK-609](https://igniterealtime.atlassian.net/browse/SMACK-609) -
    PingManager.ping(String, long) does not respect timeout
-   [SMACK-613](https://igniterealtime.atlassian.net/browse/SMACK-613) -
    Parsing exception causes infinite loop if the exception is not
    thrown

## 4.0.4 -- 2014-09-05

### Bug

-   [SMACK-596](https://igniterealtime.atlassian.net/browse/SMACK-596) -
    Smack should load roster before sending the initial presence
-   [SMACK-598](https://igniterealtime.atlassian.net/browse/SMACK-598) -
    Smack should allow the empty string as content of message body
    element
-   [SMACK-601](https://igniterealtime.atlassian.net/browse/SMACK-601) -
    PubSub ItemProvider does only process the outermost namespace
    definition when creating PayloadItems
-   [SMACK-602](https://igniterealtime.atlassian.net/browse/SMACK-602) -
    PacketCollector must handle InterruptException
-   [SMACK-603](https://igniterealtime.atlassian.net/browse/SMACK-603) -
    XMPPError.Condition.equals() should be null-safe

## 4.0.3 -- 2014-08-16

### Bug

-   [SMACK-589](https://igniterealtime.atlassian.net/browse/SMACK-589) -
    FormField.Option toXML() produces malformed XML
-   [SMACK-592](https://igniterealtime.atlassian.net/browse/SMACK-592) -
    OfflineMessagesManager.getMessages() does send request before
    collector is set up and could leak collector
-   [SMACK-594](https://igniterealtime.atlassian.net/browse/SMACK-594) -
    PrivateData Bookmarks.toXML() returns invalid XML

### Improvement

-   [SMACK-539](https://igniterealtime.atlassian.net/browse/SMACK-539) -
    Verify ConnectionConfiguration parameters
-   [SMACK-588](https://igniterealtime.atlassian.net/browse/SMACK-588) -
    Typo in org.jivesoftware.smackx.pubsub.ConfigureForm:
    s/isSubscibe/isSubscribe/
-   [SMACK-593](https://igniterealtime.atlassian.net/browse/SMACK-593) -
    Smack should prefer full flush over sync flush when using
    compression

## 4.0.2 -- 2014-07-27

### Improvement

-   [SMACK-576](https://igniterealtime.atlassian.net/browse/SMACK-576) -
    smack-resolver-javax should become a OSGi ServiceComponent
-   [SMACK-586](https://igniterealtime.atlassian.net/browse/SMACK-586) -
    Extend API to configure a HostnameVerifier

## 4.0.1 -- 2014-07-20

## Sub-task

-   [SMACK-346](https://igniterealtime.atlassian.net/browse/SMACK-346) -
    Bug in return code for rejection handling in FileTransferManager

### Bug

-   [SMACK-574](https://igniterealtime.atlassian.net/browse/SMACK-574) -
    Documentation still refers at some places to Connection
-   [SMACK-575](https://igniterealtime.atlassian.net/browse/SMACK-575) -
    PingManager schedules pings after pingInterval when it should be use
    nextPingIn instead
-   [SMACK-577](https://igniterealtime.atlassian.net/browse/SMACK-577) -
    Bookmarks and FormField toXml() methods do not properly escape XML
-   [SMACK-583](https://igniterealtime.atlassian.net/browse/SMACK-583) -
    PacketListeners may not be invoked in delivery order

### Improvement

-   [SMACK-576](https://igniterealtime.atlassian.net/browse/SMACK-576) -
    smack-resolver-javax should become a OSGi ServiceComponent

### New Feature

-   [SMACK-580](https://igniterealtime.atlassian.net/browse/SMACK-580) -
    Add support for retrieving a PubSub node\'s affiliations

## 4.0.0 -- 2014-06-08

## Sub-task

-   [SMACK-399](https://igniterealtime.atlassian.net/browse/SMACK-399) -
    Add support for Roster Versioning (was XEP-0237, now in RFC 6121)
-   [SMACK-400](https://igniterealtime.atlassian.net/browse/SMACK-400) -
    Change xml-not-well-formed to not-well-formed
-   [SMACK-401](https://igniterealtime.atlassian.net/browse/SMACK-401) -
    Remove \<invalid-id/>
-   [SMACK-445](https://igniterealtime.atlassian.net/browse/SMACK-445) -
    XMPPError class is based on deprecated XEP-0086

### Bug

-   [SMACK-357](https://igniterealtime.atlassian.net/browse/SMACK-357) -
    Error in SASL authentication when SASL authzid parameter is null
-   [SMACK-410](https://igniterealtime.atlassian.net/browse/SMACK-410) -
    Any valid SSL server certificate can be used to perform a
    man-in-the-middle attack
-   [SMACK-411](https://igniterealtime.atlassian.net/browse/SMACK-411) -
    ServiceDiscoveryManager identities should be non-static and kept in
    a Set to allow multiple identities as per XEP-30
-   [SMACK-414](https://igniterealtime.atlassian.net/browse/SMACK-414) -
    Smack does not announce the support for XEP-54 aka vcard-temp
-   [SMACK-427](https://igniterealtime.atlassian.net/browse/SMACK-427) -
    Typo in code - StreamInitiation.setSesssionID()
-   [SMACK-467](https://igniterealtime.atlassian.net/browse/SMACK-467) -
    Don\'t use the default locale for machine-readable output, use
    Locale.US instead
-   [SMACK-531](https://igniterealtime.atlassian.net/browse/SMACK-531) -
    Add missing namespace attribute to XHTML-IM body tags
-   [SMACK-533](https://igniterealtime.atlassian.net/browse/SMACK-533) -
    Smack should prevent IQ response spoofing
-   [SMACK-535](https://igniterealtime.atlassian.net/browse/SMACK-535) -
    jul.properties should only configure the \'org.igniterealtime\'
    namespace
-   [SMACK-538](https://igniterealtime.atlassian.net/browse/SMACK-538) -
    ParseRoster does not check the sender of the roster and for pending
    roster queries
-   [SMACK-541](https://igniterealtime.atlassian.net/browse/SMACK-541) -
    XHTMLExtensionProvider relies on incorrect behavior of MXParser,
    violating the contract of the XMLPullParser interface
-   [SMACK-543](https://igniterealtime.atlassian.net/browse/SMACK-543) -
    packet.Time is not thread-safe
-   [SMACK-546](https://igniterealtime.atlassian.net/browse/SMACK-546) -
    PubSub\'s Item needs to escape its XML payload
-   [SMACK-548](https://igniterealtime.atlassian.net/browse/SMACK-548) -
    PingManager notifies pingFailedListeners multiple times
-   [SMACK-551](https://igniterealtime.atlassian.net/browse/SMACK-551) -
    ChatManager throws NPE, when Message has no \'from\' attribute
-   [SMACK-554](https://igniterealtime.atlassian.net/browse/SMACK-554) -
    Memory leak in BookmarkManager
-   [SMACK-555](https://igniterealtime.atlassian.net/browse/SMACK-555) -
    VCardProvider should consider some elements as optional
-   [SMACK-558](https://igniterealtime.atlassian.net/browse/SMACK-558) -
    connect() must wait until the stream features have been parsed
-   [SMACK-559](https://igniterealtime.atlassian.net/browse/SMACK-559) -
    Roster entries without a group are not updated
-   [SMACK-560](https://igniterealtime.atlassian.net/browse/SMACK-560) -
    Race condition in PacketWriter
-   [SMACK-567](https://igniterealtime.atlassian.net/browse/SMACK-567) -
    XMPPConnection leaks listenerExecutor ExecutorService

### Improvement

-   [SMACK-343](https://igniterealtime.atlassian.net/browse/SMACK-343) -
    Make Smack jar an OSGi bundle.
-   [SMACK-356](https://igniterealtime.atlassian.net/browse/SMACK-356) -
    There is no way to reliably end a Chat and have a new one created.
-   [SMACK-454](https://igniterealtime.atlassian.net/browse/SMACK-454) -
    Follow XEP-0170 recommendation: Compression before Resource Binding
-   [SMACK-459](https://igniterealtime.atlassian.net/browse/SMACK-459) -
    Add option to configure the default identity in
    ServiceDiscoveryManager
-   [SMACK-465](https://igniterealtime.atlassian.net/browse/SMACK-465) -
    Replace custom wrapped Throwable in XMPPException with
    Exception.cause
-   [SMACK-468](https://igniterealtime.atlassian.net/browse/SMACK-468) -
    Don\'t throw an IOException in IBBStreams when the stream got closed
    by the remote
-   [SMACK-536](https://igniterealtime.atlassian.net/browse/SMACK-536) -
    JUL Loggers should become final
-   [SMACK-537](https://igniterealtime.atlassian.net/browse/SMACK-537) -
    Move XMPP Ping code to smackx, add keep-alive functionality to
    PingManager
-   [SMACK-545](https://igniterealtime.atlassian.net/browse/SMACK-545) -
    Change API to the style mentioned in Smack\'s Code Guidelines
-   [SMACK-547](https://igniterealtime.atlassian.net/browse/SMACK-547) -
    Consistent behavior for \"from\" attribute on outgoing stanzas
-   [SMACK-556](https://igniterealtime.atlassian.net/browse/SMACK-556) -
    Make ConnectionConfigration getters public
-   [SMACK-557](https://igniterealtime.atlassian.net/browse/SMACK-557) -
    Provide a MultiUserChat method to create \*or\* join a room
-   [SMACK-568](https://igniterealtime.atlassian.net/browse/SMACK-568) -
    Don\'t exclude groupchat messages without body element in
    MultiUserChat MessageListeners

### New Feature

-   [SMACK-53](https://igniterealtime.atlassian.net/browse/SMACK-53) -
    Add support for XEP-0092: Software Version
-   [SMACK-71](https://igniterealtime.atlassian.net/browse/SMACK-71) -
    Create new FromFilter that checks for exact matching
-   [SMACK-187](https://igniterealtime.atlassian.net/browse/SMACK-187) -
    Add HTTP Binding support (BOSH / XEP-0124)
-   [SMACK-265](https://igniterealtime.atlassian.net/browse/SMACK-265) -
    Move to a newer build process with artifacts published to maven
    central repo
-   [SMACK-426](https://igniterealtime.atlassian.net/browse/SMACK-426) -
    Improve XMPPException
-   [SMACK-544](https://igniterealtime.atlassian.net/browse/SMACK-544) -
    Add support for XEP-0079: Advanced Message Processing
-   [SMACK-552](https://igniterealtime.atlassian.net/browse/SMACK-552) -
    Add support for \"HTTP over XMPP transport\" aka. XEP-0332

### Task

-   [SMACK-371](https://igniterealtime.atlassian.net/browse/SMACK-371) -
    Some MUC tasks are using stanza\'s as defined in an older version of
    the spec. Fails to work on some servers.
-   [SMACK-432](https://igniterealtime.atlassian.net/browse/SMACK-432) -
    Code cleanup of deprecated methods
-   [SMACK-446](https://igniterealtime.atlassian.net/browse/SMACK-446) -
    Remove non-SASL authentication code

## 3.4.1 -- 2014-02-09

### Bug

-   [SMACK-540](https://igniterealtime.atlassian.net/browse/SMACK-540) -
    Memory leak in MultiUserChat

## 3.4.0 -- 2014-02-02

### Bug Fixes

-   [SMACK-442](https://igniterealtime.atlassian.net/browse/SMACK-442) -
    Manager\'s should also handle connectionClosedOnError()
-   [SMACK-443](https://igniterealtime.atlassian.net/browse/SMACK-443) -
    ReconnectionSuccessful listeners are invoked twice on reconnection
    if connect() failed before
-   [SMACK-452](https://igniterealtime.atlassian.net/browse/SMACK-452) -
    PacketParserUtils.parseStreamError() is not aware of optional text
    element and therefore failes to parse stream error\'s correctly.
    Prevents ReconnectionManager from reconnecting.
-   [SMACK-458](https://igniterealtime.atlassian.net/browse/SMACK-458) -
    Smack\'s Managers should not remove itself when the connection is
    closed or should re-add themselfs if the connection get reconnected
-   [SMACK-462](https://igniterealtime.atlassian.net/browse/SMACK-462) -
    Prevent duplicate manager instances by using the manager\'s
    constructor in the ConnectionCreationListener\'s connectionCreated
-   [SMACK-463](https://igniterealtime.atlassian.net/browse/SMACK-463) -
    packet listeners silently fail when preceding listener caused
    exception
-   [SMACK-524](https://igniterealtime.atlassian.net/browse/SMACK-524) -
    Use correct block-size definition for IBB transfers
-   [SMACK-525](https://igniterealtime.atlassian.net/browse/SMACK-525) -
    NPE in XMPPConnection.notifyConnectionError
-   [SMACK-529](https://igniterealtime.atlassian.net/browse/SMACK-529) -
    Add support for XEP-0280 \"Message Carbons\"
-   [SMACK-530](https://igniterealtime.atlassian.net/browse/SMACK-530) -
    DNSUtilTest requires an internet connection to work, it should be
    moved to integration tests.

### New Feature

-   [SMACK-286](https://igniterealtime.atlassian.net/browse/SMACK-286) -
    Need to change ProviderManager to support loading smack.providers
    from alternative locations
-   [SMACK-387](https://igniterealtime.atlassian.net/browse/SMACK-387) -
    Allow configuration of ChatManager to be able to allow message
    handling to be customized.
-   [SMACK-403](https://igniterealtime.atlassian.net/browse/SMACK-403) -
    Add support for XEP-0297 \"Stanza Forwarding\"
-   [SMACK-434](https://igniterealtime.atlassian.net/browse/SMACK-434) -
    Create a project to contain non production ready implementations of
    specifications

### Improvement

-   [SMACK-343](https://igniterealtime.atlassian.net/browse/SMACK-343) -
    Make Smack jar an OSGi bundle.
-   [SMACK-381](https://igniterealtime.atlassian.net/browse/SMACK-381) -
    Separate the configuration for smack extension related classes from
    the smack jar.
-   [SMACK-444](https://igniterealtime.atlassian.net/browse/SMACK-444) -
    Allow \'null\' for TruststorePath and TruststorePassword in
    ServerTrustManager
-   [SMACK-456](https://igniterealtime.atlassian.net/browse/SMACK-456) -
    Add the causing exception to the XMPPExceptions thrown in
    XMPPConnection
-   [SMACK-457](https://igniterealtime.atlassian.net/browse/SMACK-457) -
    Remove unnecessary printStackTrace() in XMPPConnection
-   [SMACK-460](https://igniterealtime.atlassian.net/browse/SMACK-460) -
    ServiceDiscoveryManager should not use the constructor in
    connectionCreated()
-   [SMACK-461](https://igniterealtime.atlassian.net/browse/SMACK-461) -
    Remove incorrect deprecated marker for
    DiscoverInfo.Identity.setType()
-   [SMACK-464](https://igniterealtime.atlassian.net/browse/SMACK-464) -
    Make it clear that PacketListener\'s added with
    XMPPConnection.addPacketListener() are only for received packets
-   [SMACK-534](https://igniterealtime.atlassian.net/browse/SMACK-534) -
    Convert all System.out and printStackTrace calls to use Java util
    logging.
-   [SMACK-339](https://igniterealtime.atlassian.net/browse/SMACK-339) -
    Allow ConnectionListeners to be added before XMPPConnection is
    connected. Currently throws exception
-   [SMACK-373](https://igniterealtime.atlassian.net/browse/SMACK-373) -
    Don\'t remove listeners after a disconnect() , keep state of
    XMPPConnection between disconnect() and connect()/login()
-   [SMACK-434](https://igniterealtime.atlassian.net/browse/SMACK-434) -
    Create a project to contain non production ready implementations of
    specifications
-   [SMACK-526](https://igniterealtime.atlassian.net/browse/SMACK-526) -
    Deprecate all PEP related classes.

## 3.3.1 -- 2013-10-06

### Bug Fixes

-   [SMACK-428](https://igniterealtime.atlassian.net/browse/SMACK-428) -
    RosterEntry overrides equals, but not hashcode.
-   [SMACK-438](https://igniterealtime.atlassian.net/browse/SMACK-438) -
    Possible NPE in
    MultiUserChat.InvitationsMonitor.getInvitationsMonitor()
-   [SMACK-441](https://igniterealtime.atlassian.net/browse/SMACK-441) -
    Memory leak in KeepAliveManager
-   [SMACK-447](https://igniterealtime.atlassian.net/browse/SMACK-447) -
    Compression is not enabled for Java7ZlibInputOutputStream
-   [SMACK-448](https://igniterealtime.atlassian.net/browse/SMACK-448) -
    Java7ZlibInputOutputStream does not work. Deflater.DEFAULT_STRATEGY
    is used as compression level when it should use
    Deflater.DEFAULT_COMPRESSION
-   [SMACK-450](https://igniterealtime.atlassian.net/browse/SMACK-450) -
    VCard.load() throws null pointer exception if there is no VCard for
    the user
-   [SMACK-455](https://igniterealtime.atlassian.net/browse/SMACK-455) -
    Multiple items doesn\`t not parse correctly in a pubsub message

### New Feature

-   [SMACK-425](https://igniterealtime.atlassian.net/browse/SMACK-425) -
    Collect (parser) Exceptions and unparseable stanzas. Provide a
    callback method so that the user is notified about them if he wants
    to

### Improvement

-   [SMACK-369](https://igniterealtime.atlassian.net/browse/SMACK-369) -
    Exceptions during login should get thrown back up to the caller.
-   [SMACK-439](https://igniterealtime.atlassian.net/browse/SMACK-439) -
    Improve documentation for MultiUserChat.InvitationsListener
-   [SMACK-451](https://igniterealtime.atlassian.net/browse/SMACK-451) -
    PingManager entry in META-INF/smack.providers is within Ad-Hoc
    Command section
-   [SMACK-431](https://igniterealtime.atlassian.net/browse/SMACK-431) -
    Enable Entity Caps as default for new connections and write
    extensions documentation html page
-   [SMACK-405](https://igniterealtime.atlassian.net/browse/SMACK-405) -
    Cleanup of redundant code in XMPPConnection.shutdown()

## 3.3.0 -- 2013-05-04

### Bug Fixes

-   [SMACK-225](https://igniterealtime.atlassian.net/browse/SMACK-225) -
    Improper handeling of DNS SRV records
-   [SMACK-238](https://igniterealtime.atlassian.net/browse/SMACK-238) -
    The vCard avatar type always return jpg
-   [SMACK-270](https://igniterealtime.atlassian.net/browse/SMACK-270) -
    Fix for a memory leak in MUC with MUC.finalize()
-   [SMACK-278](https://igniterealtime.atlassian.net/browse/SMACK-278) -
    Deadlock during Smack disconnect
-   [SMACK-342](https://igniterealtime.atlassian.net/browse/SMACK-342) -
    VCards causes ConcurrentModificationException
-   [SMACK-344](https://igniterealtime.atlassian.net/browse/SMACK-344) -
    Bug in SASL authentication mechanism when SRV records are being
    used.
-   [SMACK-351](https://igniterealtime.atlassian.net/browse/SMACK-351) -
    Rework File Transfer
-   [SMACK-352](https://igniterealtime.atlassian.net/browse/SMACK-352) -
    Update the licensing headers in various files.
-   [SMACK-355](https://igniterealtime.atlassian.net/browse/SMACK-355) -
    IO Error if smack cant use port for local proxy
-   [SMACK-371](https://igniterealtime.atlassian.net/browse/SMACK-371) -
    Some MUC tasks are using stanza\'s as defined in an older version of
    the spec. Fails to work on some servers.
-   [SMACK-375](https://igniterealtime.atlassian.net/browse/SMACK-375) -
    Node strings in the discovery info packets are not escaped as in the
    other packets
-   [SMACK-382](https://igniterealtime.atlassian.net/browse/SMACK-382) -
    Prevent memory leak in AdHocCommandManager
-   [SMACK-384](https://igniterealtime.atlassian.net/browse/SMACK-384) -
    Endless waiting for connection to be established
-   [SMACK-390](https://igniterealtime.atlassian.net/browse/SMACK-390) -
    Smack login will fail if a bad delay packet is received
-   [SMACK-392](https://igniterealtime.atlassian.net/browse/SMACK-392) -
    In ant build, compile-test target doesn\'t work.
-   [SMACK-394](https://igniterealtime.atlassian.net/browse/SMACK-394) -
    Erroneous cast in IBBInputStream\'s read() method
-   [SMACK-395](https://igniterealtime.atlassian.net/browse/SMACK-395) -
    Socks5BytestreamManager\'s establishConnection() should still try to
    use the local streamhost proxy if the server doesn\'t provide one
-   [SMACK-404](https://igniterealtime.atlassian.net/browse/SMACK-404) -
    Smack uses the wrong method to decode Base64 Strings
-   [SMACK-413](https://igniterealtime.atlassian.net/browse/SMACK-413) -
    VCardProvider incorrectly parses binary value of avatars
-   [SMACK-415](https://igniterealtime.atlassian.net/browse/SMACK-415) -
    ItemProvider relies on incorrect behavior of MXParser, violating the
    contract of the XMLPullParser interface
-   [SMACK-417](https://igniterealtime.atlassian.net/browse/SMACK-417) -
    If both PacketReader and PacketWriter fail at the same time,
    connectionClosedonError() is called two times

### New Features

-   [SMACK-331](https://igniterealtime.atlassian.net/browse/SMACK-331) -
    Add support for XEP-0184: Message Delivery Receipts
-   [SMACK-345](https://igniterealtime.atlassian.net/browse/SMACK-345) -
    Inproved detection of last activity
-   [SMACK-361](https://igniterealtime.atlassian.net/browse/SMACK-361) -
    Add support for XEP-0115 Entity Capabilities
-   [SMACK-376](https://igniterealtime.atlassian.net/browse/SMACK-376) -
    Setting a custom trust manager to control certificates from outside
-   [SMACK-388](https://igniterealtime.atlassian.net/browse/SMACK-388) -
    XEP-199 XMPP Ping support

### Improvements

-   [SMACK-341](https://igniterealtime.atlassian.net/browse/SMACK-341) -
    Update the PacketCollector and ConnectionDetachedPacketCollector to
    use the java concurrent classes.
-   [SMACK-358](https://igniterealtime.atlassian.net/browse/SMACK-358) -
    Support additional properties for account creation in test cases.
-   [SMACK-363](https://igniterealtime.atlassian.net/browse/SMACK-363) -
    Code Cleanup
-   [SMACK-377](https://igniterealtime.atlassian.net/browse/SMACK-377) -
    avoid unnecessary DNS requests in XMPPconnection
-   [SMACK-379](https://igniterealtime.atlassian.net/browse/SMACK-379) -
    Sessions were removed from the specification but Smack still uses
    them. Should be updated to reflect the spec changes.
-   [SMACK-385](https://igniterealtime.atlassian.net/browse/SMACK-385) -
    Reusing KeyStore in order to reduce memory usage
-   [SMACK-389](https://igniterealtime.atlassian.net/browse/SMACK-389) -
    Add java.util.zip.Deflater(In\|Out)putStream as Java7 API native
    alternative to JZlib
-   [SMACK-391](https://igniterealtime.atlassian.net/browse/SMACK-391) -
    Improve date parsing in StringUtils and make
    DelayInformationProvider use StringUtils for date parsing.
-   [SMACK-412](https://igniterealtime.atlassian.net/browse/SMACK-412) -
    Replace the whitespace ping with a XEP-0199 ping
-   [SMACK-419](https://igniterealtime.atlassian.net/browse/SMACK-419) -
    PacketWriter: Only flush the BufferedWriter if the packet queue is
    empty
-   [SMACK-423](https://igniterealtime.atlassian.net/browse/SMACK-423) -
    Investigate whether unhandled packets should still parse the child
    xml into a string as content
-   [SMACK-430](https://igniterealtime.atlassian.net/browse/SMACK-430) -
    Throw an exception if
    FileTransferManager.createOutgoingFileTransfer() was used with a
    bare JID

## 3.2.2 -- 2011-12-23

### Bug Fixes

-   [SMACK-263](https://igniterealtime.atlassian.net/browse/SMACK-263) -
    Set file info in all send\* methods
-   [SMACK-322](https://igniterealtime.atlassian.net/browse/SMACK-322) -
    NPE in XMPPConnection
-   [SMACK-324](https://igniterealtime.atlassian.net/browse/SMACK-324) -
    Investigate SASL issue with jabberd2 servers
-   [SMACK-338](https://igniterealtime.atlassian.net/browse/SMACK-338) -
    IBB filetransfer doesn\'t work as expected
-   [SMACK-346](https://igniterealtime.atlassian.net/browse/SMACK-346) -
    Bug in return code for rejection handling in FileTransferManager
-   [SMACK-348](https://igniterealtime.atlassian.net/browse/SMACK-348) -
    Documentation error - broken link
-   [SMACK-349](https://igniterealtime.atlassian.net/browse/SMACK-349) -
    Smack\'s IBB sends too much data in a packet
-   [SMACK-350](https://igniterealtime.atlassian.net/browse/SMACK-350) -
    Bytestream is not working in Spark 2.6.3 from XP to W7
-   [SMACK-353](https://igniterealtime.atlassian.net/browse/SMACK-353) -
    Thread leak in the FaultTolerantNegotiator
-   [SMACK-362](https://igniterealtime.atlassian.net/browse/SMACK-362) -
    smack throw NoSuchElementException if the muc#roominfo_subject has
    no values

### Improvements

-   [SMACK-343](https://igniterealtime.atlassian.net/browse/SMACK-343) -
    Make Smack jar an OSGi bundle.
-   [SMACK-354](https://igniterealtime.atlassian.net/browse/SMACK-354) -
    Provide milliseconds in timestamp colum debugwindow

## 3.2.1 -- 2011-07-04

### Bug Fixes

-   [SMACK-129](https://igniterealtime.atlassian.net/browse/SMACK-129) -
    MultiUserChat will Store Messages in its PacketCollector
    irregardless of whether or not they are being read
-   [SMACK-230](https://igniterealtime.atlassian.net/browse/SMACK-230) -
    Disconnect Can Cause Null Pointer Exception
-   [SMACK-273](https://igniterealtime.atlassian.net/browse/SMACK-273) -
    Bug in RoomListenerMultiplexor.java
-   [SMACK-329](https://igniterealtime.atlassian.net/browse/SMACK-329) -
    XHTMLText uses improper format for br tag
-   [SMACK-338](https://igniterealtime.atlassian.net/browse/SMACK-338) -
    IBB filetransfer doesn\'t work as expected
-   [SMACK-324](https://igniterealtime.atlassian.net/browse/SMACK-324) -
    Investigate SASL issue with jabberd2 servers

## 3.2.0 -- 2011-05-03

### New Features

-   [SMACK-272](https://igniterealtime.atlassian.net/browse/SMACK-272) -
    Add support for pubsub (XEP-0060)
-   [SMACK-296](https://igniterealtime.atlassian.net/browse/SMACK-296) -
    Add support for XEP-0224: Attention
-   [SMACK-319](https://igniterealtime.atlassian.net/browse/SMACK-319) -
    Add common interfaces for SOCKS5 Bytestreams and In-Band Bytestreams

### Improvements

-   [SMACK-137](https://igniterealtime.atlassian.net/browse/SMACK-137) -
    File Transfer Settings
-   [SMACK-156](https://igniterealtime.atlassian.net/browse/SMACK-156) -
    Add the ability to register for roster events before logging in
-   [SMACK-261](https://igniterealtime.atlassian.net/browse/SMACK-261) -
    Minor Jingle cleanup to better support Jingle in Spark
-   [SMACK-277](https://igniterealtime.atlassian.net/browse/SMACK-277) -
    Update XMLUnit to the latest version
-   [SMACK-282](https://igniterealtime.atlassian.net/browse/SMACK-282) -
    Support SASL-related error conditions.
-   [SMACK-283](https://igniterealtime.atlassian.net/browse/SMACK-283) -
    Investigate why Jingle is connecting to stun.xten.net
-   [SMACK-285](https://igniterealtime.atlassian.net/browse/SMACK-285) -
    Add support for Nicks
-   [SMACK-289](https://igniterealtime.atlassian.net/browse/SMACK-289) -
    There is no way of retrieving items from a pubsub node when the user
    has multiple subscriptions.
-   [SMACK-294](https://igniterealtime.atlassian.net/browse/SMACK-294) -
    Handle empty roster groups and no goups in the same way
-   [SMACK-295](https://igniterealtime.atlassian.net/browse/SMACK-295) -
    Fire reconnectionSuccessful event when session is established
-   [SMACK-297](https://igniterealtime.atlassian.net/browse/SMACK-297) -
    add configuration for local Socks5 proxy
-   [SMACK-298](https://igniterealtime.atlassian.net/browse/SMACK-298) -
    Respond to all incoming Socks5 bytestream requests
-   [SMACK-299](https://igniterealtime.atlassian.net/browse/SMACK-299) -
    Improve accepting of Socks5 bytestream requests
-   [SMACK-300](https://igniterealtime.atlassian.net/browse/SMACK-300) -
    improve local Socks5 proxy implemetation
-   [SMACK-301](https://igniterealtime.atlassian.net/browse/SMACK-301) -
    support for bytestream packets to query Socks5 proxy for network
    address
-   [SMACK-302](https://igniterealtime.atlassian.net/browse/SMACK-302) -
    Improve establishing of Socks5 bytestreams
-   [SMACK-303](https://igniterealtime.atlassian.net/browse/SMACK-303) -
    integrate of the extracted Socks5 bytestream API in file transfer
    API
-   [SMACK-304](https://igniterealtime.atlassian.net/browse/SMACK-304) -
    Extend the IQ API to create empty IQ results and IQ error response
    packets
-   [SMACK-307](https://igniterealtime.atlassian.net/browse/SMACK-307) -
    Improve Message Parser Robustness and Message Body I18N
-   [SMACK-309](https://igniterealtime.atlassian.net/browse/SMACK-309) -
    Fully implement XEP-0047 In-Band Bytestreams
-   [SMACK-310](https://igniterealtime.atlassian.net/browse/SMACK-310) -
    Add Support for Localized Message Subjects

### Bug Fixes

-   [SMACK-163](https://igniterealtime.atlassian.net/browse/SMACK-163) -
    Fix NPE in RoomInfo when subject has not value
-   [SMACK-207](https://igniterealtime.atlassian.net/browse/SMACK-207) -
    Parsing of messages may disconnect Smack/Spark
-   [SMACK-225](https://igniterealtime.atlassian.net/browse/SMACK-225) -
    Improper handeling of DNS SRV records
-   [SMACK-232](https://igniterealtime.atlassian.net/browse/SMACK-232) -
    Better handling of Roster error
-   [SMACK-243](https://igniterealtime.atlassian.net/browse/SMACK-243) -
    Packet with wrong date format makes Smack to disconnect
-   [SMACK-264](https://igniterealtime.atlassian.net/browse/SMACK-264) -
    fix for NPE in SASLMechanism.java
-   [SMACK-269](https://igniterealtime.atlassian.net/browse/SMACK-269) -
    Smack 3.1.0 creates a new chat for every incoming message
-   [SMACK-271](https://igniterealtime.atlassian.net/browse/SMACK-271) -
    Deadlock in XMPPConnection while login and parsing stream features
-   [SMACK-275](https://igniterealtime.atlassian.net/browse/SMACK-275) -
    Patch: Fix for broken SASL DIGEST-MD5 implementation
-   [SMACK-280](https://igniterealtime.atlassian.net/browse/SMACK-280) -
    The authentification should use the XMPPConnection#sendPacket method
    and work transparent with packets and packet listeners.
-   [SMACK-288](https://igniterealtime.atlassian.net/browse/SMACK-288) -
    The parsing of the result for a LeafNode.getItems() call is
    incorrect. It creates a DefaultPacketExtension instead of an Item
    for every other item in the result.
-   [SMACK-290](https://igniterealtime.atlassian.net/browse/SMACK-290) -
    Deadlock while getting Roster before it\'s initialized
-   [SMACK-291](https://igniterealtime.atlassian.net/browse/SMACK-291) -
    RosterGroup modifications should depend on roster push
-   [SMACK-293](https://igniterealtime.atlassian.net/browse/SMACK-293) -
    Support optional roster subscription attribute
-   [SMACK-305](https://igniterealtime.atlassian.net/browse/SMACK-305) -
    RosterEntry#getGroups causing a roster reload
-   [SMACK-308](https://igniterealtime.atlassian.net/browse/SMACK-308) -
    Multiple errors in pubsub GetItemsRequest
-   [SMACK-312](https://igniterealtime.atlassian.net/browse/SMACK-312) -
    Only fire RosterListener#entriesUpdated for RosterEntries that
    changed
-   [SMACK-327](https://igniterealtime.atlassian.net/browse/SMACK-327) -
    getFeatures() method on DiscoverInfo is improperly set to be package
    protected instead of public
-   [SMACK-328](https://igniterealtime.atlassian.net/browse/SMACK-328) -
    Number format exception while parsing dates.
-   [SMACK-332](https://igniterealtime.atlassian.net/browse/SMACK-332) -
    Smack 3.2.0b2 shows wrong version in Smack Dubugger Window
-   [SMACK-334](https://igniterealtime.atlassian.net/browse/SMACK-334) -
    Error in form for FileTransferNegotiator

## 3.1.0 -- 2008-11-20

### New Features

-   [SMACK-142](https://igniterealtime.atlassian.net/browse/SMACK-142) -
    Added support for Kerberos/NTLM. **(6 votes)**
-   [SMACK-210](https://igniterealtime.atlassian.net/browse/SMACK-210) -
    Added support for MD5 SASL. **(1 vote)**
-   [SMACK-256](https://igniterealtime.atlassian.net/browse/SMACK-256) -
    Added support for new sophisticated TLS mechanisms including
    SmartCard and Apple\'s KeychainStore.
-   [SMACK-242](https://igniterealtime.atlassian.net/browse/SMACK-242) -
    Added support for JEP-50: Ad-hoc commands.
-   [SMACK-251](https://igniterealtime.atlassian.net/browse/SMACK-251) -
    Added support for XEP-0163: Personal Eventing Protocol. **(1 vote)**
-   [SMACK-226](https://igniterealtime.atlassian.net/browse/SMACK-226) -
    XMLConnection can now be used with an http/socks proxy. **(2
    votes)**
-   [SMACK-254](https://igniterealtime.atlassian.net/browse/SMACK-254) -
    Loading the Roster during login is now optional.
-   [SMACK-255](https://igniterealtime.atlassian.net/browse/SMACK-255) -
    Added ability to set mime type for avatar.
-   [SMACK-235](https://igniterealtime.atlassian.net/browse/SMACK-235) -
    Improved performance of Roster class.
-   [SMACK-241](https://igniterealtime.atlassian.net/browse/SMACK-241) -
    Updated Base64 implementation to match Openfire\'s.
-   [SMACK-240](https://igniterealtime.atlassian.net/browse/SMACK-240) -
    Updated Jingle implementation to newest version.
-   [SMACK-246](https://igniterealtime.atlassian.net/browse/SMACK-246) -
    Improve Jingle logging using commons-logging
-   [SMACK-244](https://igniterealtime.atlassian.net/browse/SMACK-244) -
    Updated JSTUN to 0.7.2.
-   [SMACK-259](https://igniterealtime.atlassian.net/browse/SMACK-259) -
    Updated XPP library to latest version.

### Bug Fixes

-   [SMACK-231](https://igniterealtime.atlassian.net/browse/SMACK-231) -
    IBB Outputstream was not being flushed before it was closed.
-   [SMACK-236](https://igniterealtime.atlassian.net/browse/SMACK-236) -
    Renamed stanza error \"unexpected-condition\" to
    \"unexpected-request\".
-   [SMACK-258](https://igniterealtime.atlassian.net/browse/SMACK-258) -
    Fixed disconnection issue when parsing SASL success that contained a
    payload.
-   [SMACK-175](https://igniterealtime.atlassian.net/browse/SMACK-175) -
    Fixed typo in RosterPacket.ItemStatus constant.
-   [SMACK-260](https://igniterealtime.atlassian.net/browse/SMACK-260) -
    Added handling of error presence packets

## 3.0.3 -- 2007-05-31

### New Features

-   [SMACK-99](https://igniterealtime.atlassian.net/browse/SMACK-99) -
    Added support for multiple message bodies and message body
    languages.
-   [SMACK-218](https://igniterealtime.atlassian.net/browse/SMACK-218) -
    Implemented GSSAPI for single-sign on.

### Bug Fixes

-   [SMACK-219](https://igniterealtime.atlassian.net/browse/SMACK-219) -
    The getPresence method was not working correctly with offline
    presence.
-   [SMACK-224](https://igniterealtime.atlassian.net/browse/SMACK-224) -
    SASL authenticion was using the XMPP domain instead of the FQDN.

## 3.0.2 -- 2007-05-03

### Bug Fixes

-   [SMACK-212](https://igniterealtime.atlassian.net/browse/SMACK-212) -
    Jingle can\'t establish session if only one side has a relay service
-   [SMACK-213](https://igniterealtime.atlassian.net/browse/SMACK-213) -
    RTP Bridge Resolver get wrong localhost address in certain
    situations
-   [SMACK-214](https://igniterealtime.atlassian.net/browse/SMACK-214) -
    Presences with a negative priority of -1 are not sending the
    priority to the server

## 3.0.1 -- 2007-04-12

### Bug Fixes

-   [SMACK-211](https://igniterealtime.atlassian.net/browse/SMACK-211) -
    Jingle ICE with relay sometimes closed sessions.
-   Upgraded bundled version of JSTUN.

## 3.0.0 -- 2007-03-31

### Important Changes

-   Java 5 is now required.
-   Several API changes are not backwards compatible. In particular,
    connection handling has been significantly updated, the GroupChat
    class has been dropped in favor of the standardized MultiUserChat,
    and the Chat class has an updated API.

### New Features

-   [SMACK-74](https://igniterealtime.atlassian.net/browse/SMACK-74) -
    Added support for unavailable presences with status text. **(4
    votes)**
-   [SMACK-191](https://igniterealtime.atlassian.net/browse/SMACK-191) -
    RosterListener API improvements.
-   [SMACK-194](https://igniterealtime.atlassian.net/browse/SMACK-194) -
    Roster.getPresence(String) now considers mode after priority to
    determine the presence value to return.
-   [SMACK-195](https://igniterealtime.atlassian.net/browse/SMACK-195) -
    Added the ability to disconnect with a custom presence value (for
    offline status).
-   [SMACK-200](https://igniterealtime.atlassian.net/browse/SMACK-200) -
    Added convenience methods to Presence class.
-   [SMACK-31](https://igniterealtime.atlassian.net/browse/SMACK-31) -
    Added support for privacy lists. **(4 votes)**
-   [SMACK-94](https://igniterealtime.atlassian.net/browse/SMACK-94) -
    Added support for last activity of online users. **(1 vote)**
-   [SMACK-121](https://igniterealtime.atlassian.net/browse/SMACK-121) -
    Added support for stream errors.
-   [SMACK-136](https://igniterealtime.atlassian.net/browse/SMACK-136) -
    Added support for XEP-0048: bookmark storage.
-   [SMACK-144](https://igniterealtime.atlassian.net/browse/SMACK-144) -
    Added bookmark manager for central bookmark management.
-   [SMACK-150](https://igniterealtime.atlassian.net/browse/SMACK-150) -
    Added support for handling node features in disco.
-   [SMACK-167](https://igniterealtime.atlassian.net/browse/SMACK-167) -
    Added support for XEP-0106: JID Escaping
-   [SMACK-171](https://igniterealtime.atlassian.net/browse/SMACK-171) -
    The presence of available contacts is now changed to offline when
    the connection is closed.
-   [SMACK-172](https://igniterealtime.atlassian.net/browse/SMACK-172) -
    Added support for re-connection when the connection is abruptly
    closed.
-   [SMACK-182](https://igniterealtime.atlassian.net/browse/SMACK-182) -
    ProviderManager is now pluggable (for Eclipse ECF).
-   [SMACK-185](https://igniterealtime.atlassian.net/browse/SMACK-185) -
    Added the workgroup API to Smack.
-   [SMACK-206](https://igniterealtime.atlassian.net/browse/SMACK-206) -
    Added the option to specify the username to use for the automated
    test cases.
-   [SMACK-208](https://igniterealtime.atlassian.net/browse/SMACK-208) -
    Added a max queue size for outgoing packets to prevent memory issues
    during extreme load.
-   [SMACK-209](https://igniterealtime.atlassian.net/browse/SMACK-209) -
    Initial Jingle support implemented.

### Bug Fixes

-   [SMACK-6](https://igniterealtime.atlassian.net/browse/SMACK-6) -
    Don\'t force use of collectors in Chat class.
-   [SMACK-10](https://igniterealtime.atlassian.net/browse/SMACK-10) -
    Flush pending packets before closing the connection. **(4 votes)**
-   [SMACK-51](https://igniterealtime.atlassian.net/browse/SMACK-51) -
    Use unique Thread names among connections.
-   [SMACK-54](https://igniterealtime.atlassian.net/browse/SMACK-54) -
    Add #equals and #hashCode to Occupant.
-   [SMACK-86](https://igniterealtime.atlassian.net/browse/SMACK-86) -
    Made presence checks case in-sensitive.
-   [SMACK-93](https://igniterealtime.atlassian.net/browse/SMACK-93) -
    XHTML provider wasn\'t handling some tags correctly.
-   [SMACK-138](https://igniterealtime.atlassian.net/browse/SMACK-138) -
    Added caching to file transfer negotiation operations.
-   [SMACK-143](https://igniterealtime.atlassian.net/browse/SMACK-143) -
    Updated XMPPError to be compliant with RFC3920.
-   [SMACK-145](https://igniterealtime.atlassian.net/browse/SMACK-145) -
    XHTML parsing could fail in some cases.
-   [SMACK-146](https://igniterealtime.atlassian.net/browse/SMACK-146) -
    DNS lookups were failing with some DNS servers.
-   [SMACK-147](https://igniterealtime.atlassian.net/browse/SMACK-147) -
    Removed invisibility presence mode.
-   [SMACK-148](https://igniterealtime.atlassian.net/browse/SMACK-148) -
    Socks 5 listening thread was not cleaning up correctly. **(2
    votes)**
-   [SMACK-149](https://igniterealtime.atlassian.net/browse/SMACK-149) -
    Fixed possible memory leaking in PacketReader.
-   [SMACK-151](https://igniterealtime.atlassian.net/browse/SMACK-151) -
    Now use getBytes(\"UTF-8\") instead of getBytes().
-   [SMACK-152](https://igniterealtime.atlassian.net/browse/SMACK-152) -
    The FN field is duplicated when loading vCards from the server.
-   [SMACK-153](https://igniterealtime.atlassian.net/browse/SMACK-153) -
    Optimized performance by replacing StringBuffer with StringBuilder.
-   [SMACK-154](https://igniterealtime.atlassian.net/browse/SMACK-154) -
    Fixed roster test cases that were sometimes failing.
-   [SMACK-155](https://igniterealtime.atlassian.net/browse/SMACK-155) -
    Optimized MUC performance by reducing number of packet collectors
    and listeners.
-   [SMACK-158](https://igniterealtime.atlassian.net/browse/SMACK-158) -
    FileTransfer isDone() method was returning true even when the
    transfer was refused.
-   [SMACK-159](https://igniterealtime.atlassian.net/browse/SMACK-159) -
    Filenames were not escaped for file transfers.
-   [SMACK-160](https://igniterealtime.atlassian.net/browse/SMACK-160) -
    Now use stream:feature to discover registration support.
-   [SMACK-161](https://igniterealtime.atlassian.net/browse/SMACK-161) -
    Improved connection speed.
-   [SMACK-162](https://igniterealtime.atlassian.net/browse/SMACK-162) -
    Fixed NPE in SmackConfiguration.
-   [SMACK-163](https://igniterealtime.atlassian.net/browse/SMACK-163) -
    Fixed NPE in RoomInfo when subject was null.
-   [SMACK-164](https://igniterealtime.atlassian.net/browse/SMACK-164) -
    Contact name was not being escaped.
-   [SMACK-165](https://igniterealtime.atlassian.net/browse/SMACK-165) -
    Listeners were not being removed from PacketReader.
-   [SMACK-166](https://igniterealtime.atlassian.net/browse/SMACK-166) -
    Packet reader thread was freezing when parsing an error text with no
    description.
-   [SMACK-168](https://igniterealtime.atlassian.net/browse/SMACK-168) -
    Fixed possible delay in PacketReader when negotiating TLS.
-   [SMACK-173](https://igniterealtime.atlassian.net/browse/SMACK-173) -
    Renamed ConnectionEstablishedListener to ConnectionCreationListener.
-   [SMACK-176](https://igniterealtime.atlassian.net/browse/SMACK-176) -
    Fixed incorrect property initialization.
-   [SMACK-177](https://igniterealtime.atlassian.net/browse/SMACK-177) -
    Removed synchronization from Roster.
-   [SMACK-178](https://igniterealtime.atlassian.net/browse/SMACK-178) -
    Added NodeInformation#getNodeIdentities() to return identities of
    hosted nodes
-   [SMACK-181](https://igniterealtime.atlassian.net/browse/SMACK-181) -
    Improved parsing of certificates to get signed domains.
-   [SMACK-183](https://igniterealtime.atlassian.net/browse/SMACK-183) -
    Documentation fixes.
-   [SMACK-184](https://igniterealtime.atlassian.net/browse/SMACK-184) -
    Simplified XMPPConnection constructors.
-   [SMACK-203](https://igniterealtime.atlassian.net/browse/SMACK-203) -
    NULL thread IDs would cause an error inside of the Chat Manager.
-   [SMACK-205](https://igniterealtime.atlassian.net/browse/SMACK-205) -
    Fixed PacketReader concurrency problems.
-   [SMACK-188](https://igniterealtime.atlassian.net/browse/SMACK-188) -
    Resources are now closed after reading the keystore.
-   [SMACK-189](https://igniterealtime.atlassian.net/browse/SMACK-189) -
    The listener was remaining blocked forever in some cases.
-   [SMACK-190](https://igniterealtime.atlassian.net/browse/SMACK-190) -
    Exceptions while notifying packet reader listeners was stopping the
    notification thread.
-   [SMACK-192](https://igniterealtime.atlassian.net/browse/SMACK-192) -
    Roster.getPresence(String) now forces use of the bare JID.
-   [SMACK-193](https://igniterealtime.atlassian.net/browse/SMACK-193) -
    New presence packets now default to a null presence mode.
-   [SMACK-196](https://igniterealtime.atlassian.net/browse/SMACK-196) -
    Now set closed to true at the start of the connection shutdown
    method and not the end.
-   [SMACK-197](https://igniterealtime.atlassian.net/browse/SMACK-197) -
    The source build was failing.
-   [SMACK-198](https://igniterealtime.atlassian.net/browse/SMACK-198) -
    File transfer streams were not being closed properly in some cases.
-   [SMACK-199](https://igniterealtime.atlassian.net/browse/SMACK-199) -
    MultiUserChat invitation listeners are no longer removed on
    disconnects.
-   [SMACK-201](https://igniterealtime.atlassian.net/browse/SMACK-201) -
    Roster no longer exposes that it implements ConnectionListener.

## 2.2.1 -- 2006-06-12

-   [SMACK-141](https://igniterealtime.atlassian.net/browse/SMACK-141) -
    Fixed SSL exception while creating new XMPPConnections. **(1 vote)**
-   [SMACK-127](https://igniterealtime.atlassian.net/browse/SMACK-127) -
    Fixed incorrect file transfer progress.
-   [SMACK-130](https://igniterealtime.atlassian.net/browse/SMACK-130) -
    Fixed VCard escaping problem that was crashing connections.
-   [SMACK-134](https://igniterealtime.atlassian.net/browse/SMACK-134) -
    VCards were not being saved when avatar was the only element.
-   [SMACK-131](https://igniterealtime.atlassian.net/browse/SMACK-131) -
    Illegal XML characters are now properly escaped in the presence
    status.
-   [SMACK-133](https://igniterealtime.atlassian.net/browse/SMACK-133) -
    Illegal XML characters are now properly escaped in groups names.
-   [SMACK-132](https://igniterealtime.atlassian.net/browse/SMACK-132) -
    Fixed IBB problem triggered when buffersize was increased.
-   [SMACK-135](https://igniterealtime.atlassian.net/browse/SMACK-135) -
    Moved to new Base64 implementation to fix line break issue in old
    implementation.

## 2.2.0 -- 2006-03-09

-   [SMACK-122](https://igniterealtime.atlassian.net/browse/SMACK-122) -
    Added support for JEP-96: File Transfer. **(1 vote)**
-   [SMACK-72](https://igniterealtime.atlassian.net/browse/SMACK-72) -
    Added support for JEP-47: In-Band Bytestreams. **(2 votes)**
-   [SMACK-122](https://igniterealtime.atlassian.net/browse/SMACK-122) -
    Added support for JEP-65: SOCKS5 Bytestreams. **(1 vote)**
-   [SMACK-112](https://igniterealtime.atlassian.net/browse/SMACK-112) -
    Added support for JEP-38 Stream Compression.
-   [SMACK-117](https://igniterealtime.atlassian.net/browse/SMACK-117) -
    Added support for JEP-33: Extended Stanza Addressing.
-   [SMACK-27](https://igniterealtime.atlassian.net/browse/SMACK-27) -
    Certification validation is now pluggable.
-   [SMACK-118](https://igniterealtime.atlassian.net/browse/SMACK-118) -
    Added methods to dynamically remove providers.
-   [SMACK-125](https://igniterealtime.atlassian.net/browse/SMACK-125) -
    Added support for deaf occupant in MUC rooms.
-   [SMACK-109](https://igniterealtime.atlassian.net/browse/SMACK-109) -
    Optimized client performance. **(1 vote)**
-   [SMACK-113](https://igniterealtime.atlassian.net/browse/SMACK-113) -
    Added support for choosing if TLS should be used or not.
-   [SMACK-114](https://igniterealtime.atlassian.net/browse/SMACK-114) -
    Added support for choosing if SASL should be used or not.
-   [SMACK-123](https://igniterealtime.atlassian.net/browse/SMACK-123) -
    A thread is no longer used for packet writer listeners.
-   [SMACK-110](https://igniterealtime.atlassian.net/browse/SMACK-110) -
    Resource binding and session establishment are now requested only if
    the server offered them.
-   [SMACK-111](https://igniterealtime.atlassian.net/browse/SMACK-111) -
    Fixed concurrency issue with date formatter.
-   [SMACK-116](https://igniterealtime.atlassian.net/browse/SMACK-116) -
    Fixed vCard issues.
-   [SMACK-119](https://igniterealtime.atlassian.net/browse/SMACK-119) -
    Fixed AccessControlException when using vCard from an applet.
-   [SMACK-120](https://igniterealtime.atlassian.net/browse/SMACK-120) -
    Listener thread was not being shutdown properly.
-   [SMACK-124](https://igniterealtime.atlassian.net/browse/SMACK-124) -
    Parsing resource binding packets was requiring smackx.jar file to be
    in the classpath.
-   [SMACK-97](https://igniterealtime.atlassian.net/browse/SMACK-97) -
    Fixed functional test failures in PresencePriorityTest and
    RosterTest.
