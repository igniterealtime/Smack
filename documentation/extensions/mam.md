Message Archive Management
==========================

[Back](index.md)

Query and control an archive of messages stored on a server.

  * Check MAM support
  * Query archive
  * Paging
  * Get form fields
  * Get preferences
  * Update preferences


**XEP related:** [XEP-0313](http://xmpp.org/extensions/xep-0313.html)


Get an instance of Message Archive Management Manager 
-----------------------------------------------------

```
MamManager mamManager = MamManager.getInstanceFor(connection);
```


Check MAM support
-----------------

```
boolean isSupported = mamManager.isSupported();
```


Query archive
-------------

```
MamQueryResult mamQueryResult = mamManager.queryArchive(max);
```
*max* is an `Integer`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchive(withJid);
```
*withJid* is a `Jid`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchive(start, end);
```
*start* is a `Date`

*end* is a `Date`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchive(additionalFields);
```
*additionalFields* is a `List<FormField>`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchiveWithStartDate(start);
```
*start* is a `Date`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchiveWithEndDate(end);
```
*end* is a `Date`

or

```
MamQueryResult mamQueryResult = mamManager.queryArchive(max, start, end, withJid, additionalFields);
```
*max* is an `Integer`

*start* is a `Date`

*end* is a `Date`

*withJid* is a `Jid`

*additionalFields* is a `List<FormField>`


**Get data from mamQueryResult object**

```
// Get forwarded messages
List<Forwarded> forwardedMessages = mamQueryResult.forwardedMessages;

// Get fin IQ
MamFinIQ mamFinIQ = mamQueryResult.mamFin;
```


Paging
------

**Get a page**

```
MamQueryResult mamQueryResult = mamManager.page(dataForm, rsmSet);
```
*dataForm* is a `DataForm`

*rsmSet* is a `RSMSet`


**Get the next page**

```
MamQueryResult mamQueryResult = mamManager.pageNext(previousMamQueryResult, count);
```
*previousMamQueryResult* is a `MamQueryResult`

*count* is an `int`


**Get page before the first message saved (specific chat)**

```
MamQueryResult mamQueryResult = mamManager.pageBefore(chatJid, firstMessageId, max);
```
*chatJid* is a `Jid`

*firstMessageId* is a `String`

*max* is an `int`


**Get page after the last message saved (specific chat)**

```
MamQueryResult mamQueryResult = mamManager.pageAfter(chatJid, lastMessageId, max);
```
*chatJid* is a `Jid`

*lastMessageId* is a `String`

*max* is an `int`


Get form fields
---------------

```
List<FormField> formFields = mamManager.retrieveFormFields();
```


Get preferences
---------------

```
MamPrefsResult mamPrefsResult = mamManager.retrieveArchivingPreferences();

// Get preferences IQ
MamPrefsIQ mamPrefs = mamPrefsResult.mamPrefs;

// Obtain always and never list
List<Jid> alwaysJids = mamPrefs.getAlwaysJids();
List<Jid> neverJids = mamPrefs.getNeverJids();

// Obtain default behaviour (can be 'always', 'never' or 'roster')
DefaultBehavior defaultBehavior = mamPrefs.getDefault();

// Get the data form
DataForm dataForm = mamPrefsResult.form;
```


Update preferences
------------------

```
MamPrefsResult mamPrefsResult = mamManager.updateArchivingPreferences(alwaysJids, neverJids, defaultBehavior);
```
*alwaysJids* is a `List<Jid>`

*neverJids* is a `List<Jid>`

*defaultBehavior* is a `DefaultBehavior`

