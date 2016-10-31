DNSSEC and DANE
===============

[Back](index.md)

**DNSSEC and DANE support in Smack and MiniDNS is still in its
infancy. It should be considered experimental and not ready for
production use at this time.** We would like to see more thorough
testing and review by the security community. If you can help, then
please do not hesitate to contact us.

About
-----

DNSSEC ([RFC 4033](https://tools.ietf.org/html/rfc4033) and others)
authenticates DNS answers, positive and negative ones. This means that
if a DNS response secured by DNSSEC turns out to be authentic, then
you can be sure that the domain either exists, and that the returned
resource records (RRs) are the ones the domain owner authorized, or
that the domain does not exists and that nobody tried to fake its non
existence.

The tricky part is that an application using DNSSEC can not determine
whether a domain uses DNSSEC, does not use DNSSEC or if someone
downgraded your DNS query using DNSSEC to a response without DNSSEC.

[DANE](https://tools.ietf.org/html/rfc6698) allows the verification of
a TLS certificate with information stored in the DNS system and
secured by DNSSEC. Thus DANE requires DNSSEC.

Prerequisites
-------------

From the three DNS resolver providers (MiniDNS, javax, dnsjava)
supported by Smack only [MiniDNS](https://github.com/rtreffer/minidns)
currently supports DNSSEC. MiniDNS is the default resolver when
smack-android is used. For other configurations, make sure to add
smack-resolver-minidns to your dependencies and call
`MiniDnsResolver.setup()` prior using Smack (e.g. in a `static {}`
code block).

DNSSEC API
----------

Smack's DNSSEC API is very simple: Just use
`ConnectionConfiguration.Builder..setDnssecMode(DnssecMode)` to enable
DNSSEC. `DnssecMode` can be one of

- `disabled`
- `needsDnssec`
- `needsDnssecAndDane`

The default is `disabled`.

If `needsDnssec` is used, then Smack will only connect if the DNS
results required to determine a host for the XMPP domain could be
verified using DNSSEC.

If `needsDnssecAndDane` then DANE will be used to verify the XMPP
service's TLS certificate if STARTTLS is used. Note that you may want
to configure
`ConnectionConfiguration.Builder.setSecurityMode(SecurityMode.required)`
if you use this DNSSEC mode setting.

Best practices
--------------

We recommend that applications using Smack's DNSSEC API do not ask the
user if DNSSEC is avaialble. Instead they should check for DNSSEC
suport on every connection attempt. Once DNSSEC support has been
discovered, the application should use the `needsDnssec` mode for all
future connection attempts. The same scheme can be applied when using
DANE. This approach is similar to the scheme established by
to
["HTTP Strict Transport Security" (HSTS, RFC 6797)](https://tools.ietf.org/html/rfc6797).
