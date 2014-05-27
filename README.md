Smack
=====

About
-----

[Smack] is an Open Source, cross-platform, easy to use Java XMPP
client library.

Communicate with XMPP servers to perform real-time collaboration,
including instant messaging and group chat.

Key Advantages :
  - Extremely simple to use, yet powerful API. Sending a text message to a user can be accomplished in only a few lines of code:

    ```java
    AbstractXMPPConnection connection = new XMPPTCPConnection("jabber.org");
    connection.connect();
    connection.login("mtucker", "password");
    Chat chat = ChatManager.getInstanceFor(connection)
        .createChat("jsmith@jivesoftware.com", new MessageListener() {

        public void processMessage(Chat chat, Message message) {
            System.out.println("Received message: " + message);
        }
    });
    chat.sendMessage("Howdy!");
    ```

  - Doesn't force you to code at the packet level, as other libraries do. Smack provides intelligent higher level constructs such as the Chat and Roster classes, which let you program more efficiently.
  - Does not require that you're familiar with the XMPP XML format, or even that you're familiar with XML.
  - Provides easy machine to machine communication. Smack lets you set any number of properties on each message, including properties that are Java objects.
  - Open Source under the Apache License, which means you can incorporate Smack into your commercial or non-commercial applications.

[Smack] is an Open Source [XMPP (Jabber)] client library for instant
messaging and presence. A pure Java library, it can be embedded into
your applications to create anything from a full XMPP client to simple
XMPP integrations such as sending notification messages and
presence-enabling devices.

[Smack] - an [Ignite Realtime] community project.

Bug Reporting
-------------

Only a few usrs have acces for for filling bugs in the tracker. New
users should:

1. Create a forums account (only e-mail is a requirement, you can skip all the other fields).
2. Login to a forum account
3. Press New in your toolbar and choose Discussion
4. Choose the [Smack Dev forum](http://community.igniterealtime.org/community/developers/smack) of Smack and add the tag 'bug_report' to your new post

Please search for your issues in the bug tracker before reporting.

Contact
-------

The developeres hang around in #smack (freenode, IRC). Remeber that it
may take some time (~hours) to get a response.
 
You can also reach us via the
[Smack Developers Forum](http://community.igniterealtime.org/community/developers/smack).

Resources
---------

- Bug Tracker: http://issues.igniterealtime.org/browse/SMACK
- Nightly Builds: http://www.igniterealtime.org/downloads/nightly_smack.jsp
- Nightly Javadoc: http://www.igniterealtime.org/builds/smack/dailybuilds/javadoc/
- Nightly Documentation: http://www.igniterealtime.org/builds/smack/dailybuilds/documentation/
- User Forum: http://community.igniterealtime.org/community/support/smack_users
- Dev Forum: http://community.igniterealtime.org/community/developers/smack
- Maven Releases: https://oss.sonatype.org/content/repositories/releases/org/igniterealtime/smack/
- Maven Snapshots: https://oss.sonatype.org/content/repositories/snapshots/org/igniterealtime/smack/

[![Build Status](https://travis-ci.org/igniterealtime/Smack.svg?branch=master)](https://travis-ci.org/igniterealtime/Smack)

Ignite Realtime
===============

[Ignite Realtime] is an Open Source community composed of end-users and developers around the world who 
are interested in applying innovative, open-standards-based Real Time Collaboration to their businesses and organizations. 
We're aimed at disrupting proprietary, non-open standards-based systems and invite you to participate in what's already one 
of the biggest and most active Open Source communities.

[Smack]: http://www.igniterealtime.org/projects/smack/index.jsp
[Ignite Realtime]: http://www.igniterealtime.org
[XMPP (Jabber)]: http://xmpp.org/
