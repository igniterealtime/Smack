Entity Capabilities
===================

[Back](index.md)

This section details the usage of Smacks implementation of Entity
Capabilities.

**XEP related:** [XEP-0115: Entity Capabilities](http://xmpp.org/extensions/xep-0115.html)

**Description**

Entity Capabilities is an XMPP Protocol extension, which, in order to minimize
network impact, caches the capabilities of XMPP entities. Those capabilities
are determined with the help of the Service Discovery Protocol
([XEP-0030](http://xmpp.org/extensions/xep-0030.html)).

**Usage**

Entity Capabilities work silenty in background when enabled. If the remote
XMPP entity does not support XEP-0115 but XEP-0030 then XEP-0030 mechanisms
are transparently used. You can enable or disable Entity Capabilities by using
_**EntityCapsManager**_.

The cache used by Smack for Entity Capabilities is non-persistent as default.
That is, the cache only uses memory. But it is also possible to set a
persistent Entity Capabilities cache, which is recommended.

**Examples**

Enable Entity Capabilities

```
// Get an instance of entity caps manager for the specified connection
EntityCapsManager mgr = EntityCapsManager.getInstanceFor(connection);
// Enable entity capabilities
mgr.enableEntityCaps();
```

Configure a persistent cache for Entity Capabilities

```
// Get an instance of entity caps manager for the specified connection
EntityCapsManager mgr = EntityCapsManager.getInstanceFor(connection);
// Create an cache, see smackx.entitycaps.cache for pre-defined cache implementations
EntityCapsPersistentCache cache = new SimpleDirectoryPersistentCache(new File("/foo/cachedir"));
// Set the cache
mgr.setPersistentCache(cache);
```
