Jingle
======

**XEP related:** [XEP-0116: Jingle](http://xmpp.org/extensions/xep-0166.html)

Jingle Element Structure
------------------------

```
jingle
│  action (REQUIRED, XEP-0166 § 7.2)
|    content-accept
|    content-add
|    content-modify
|    content-reject
|    content-remove
|    description-info
|    security-info
|    session-accept
|    session-info
|    session-initiate
|    transport-accept
|    transport-info
|    transport-reject
|    transport-replace
│  initator (RECOMMENDED for session initiate, NOT RECOMMENDED otherwise, full JID, XEP-0166 § 7.1)
│  responder (RECOMMENDED for session accept, NOT RECOMMENDED otherwise, full JID. XEP-0166 § 7.1)
│  sid (REQUIRED, SHOULD match XML Nmtoken production)
│
├── <reason/> (optional, XEP-0166 § 7.4)
│    │
│    └──(alternative─session│busy│..)
│
└── <content/> (one or more, XEP-0166 § 7.3)
     │  creator (REQUIRED, must be one of)
	 |    initiator
	 |    responder
     │  disposition (OPTIONAL)
     │  name (REQUIRED)
     │  senders (OPTIONAL, except when content-modify then REQUIRED)
	 |    both (default)
	 |    initiator
	 |    none
	 |    responder
     │
     ├──description
     │  │  media
     │  │  xmlns
     │  │
     │  ├──payload─type
     │  │
     │  └──file (XEP─0234)
     │
     └──transport
        │  xmlns
        │  pwd (OPTIONAL, XEP-0176 Jingle ICE)
        │  ufrag (OPTIONAL, XEP-0176 Jingle ICE)
        │  mode (XEP-0234 Jingle File Transfer)
        │  sid (XEP-0234 Jingle File Transfer)
        │
        └──candidate
              component
              foundation
              generation
              id
              ip
              network
              port
              priority
              protocol
              type
```
