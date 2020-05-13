Smack's Modular Connection Architecture
======================================

[Back](index.md)

**Note: Everything related to the modular connection architecture is currently considered experimental and should not be used in production. Use the mature `XMPPTCPConnection` if you do not feel adventurous.

Smack's modular connection architecture allows to extend a XMPP c2s (client-to-server) connection with additional functionality by adding modules.
Those modules extend the Finite State Machine (FSM) within the `ModularXmppClientToServerConnection` with new states.

Connection modules can either be
- Transports
- Extensions

Transports bind the XMPP XML stream to an underlying transport like TCP, WebSockets, BOSH, and allow for the different particularities of transports like DirectTLS ([XEP-0368](https://xmpp.org/extensions/xep-0368.html)).
This eventually means that a single transport module can implement multiple transport mechanisms.
For example the TCP transport module implements the RFC6120 TCP and the XEP-0368 direct TLS TCP transport bindings.

Extensions allow for a richer functionality of the connection. Those include
- Compression
  - zlib ([XEP-0138](https://xmpp.org/extensions/xep-0138.html))
  - [Efficient XML Interchange (EXI)](https://www.w3.org/TR/exi/)
- Instant Stream Resumption ([XEP-0397](https://xmpp.org/extensions/xep-0397.html)
- Bind2
- Stream Management

Note that not all extensions work with every transport.
For example compression only works with TCP-based transport bindings.


Connection modules are plugged into the the modular connection via their constructor. and they usually declare backwards edges to some common, generic connection state of the FSM.

Modules and states always have an accompanying *descriptor* type.
`ModuleDescriptor` and `StateDescriptor` exist without an connection instance.
They describe the module and state metadata, while their modules and states are instanciated once a modular connection is instanciated.
