/**
 *
 * Copyright 2015-2022 Florian Schmaus
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
 * Smack's API for DNS related tasks.
 * <h2>DNSSEC and DANE</h2>
 * <h3>About</h3>
 * <p>
 * DNSSEC (<a href="https://tools.ietf.org/html/rfc4033">RFC 4033</a>, and others) authenticates DNS answers, positive
 * and negative ones. This means that if a DNS response secured by DNSSEC turns out to be authentic, then you can be
 * sure that the domain either exists, and that the returned resource records (RRs) are the ones the domain owner
 * authorized, or that the domain does not exists and that nobody tried to fake its non existence.
 * </p>
 * <p>
 * The tricky part is that an application using DNSSEC can not determine whether a domain uses DNSSEC, does not use
 * DNSSEC or if someone downgraded your DNS query using DNSSEC to a response without DNSSEC.
 * </p>
 * <p>
 * DANE (<a href="https://tools.ietf.org/html/rfc6698">RFC 6698</a>) allows the verification of a TLS certificate with
 * information stored in the DNS system and secured by DNSSEC. Thus DANE requires DNSSEC.
 * </p>
 * <h3>Prerequisites</h3>
 * <p>
 * From the three DNS resolver providers (MiniDNS, javax, dnsjava) supported by Smack we currently only support DNSSEc
 * with <a href="https://github.com/minidns/minidns">MiniDNS</a>. MiniDNS is the default resolver when smack-android is
 * used. For other configurations, make sure to add smack-resolver-minidns to your dependencies and call
 * `MiniDnsResolver.setup()` prior using Smack (e.g. in a `static {}` code block).
 * </p>
 * <h3>DNSSEC API</h3>
 * <p>
 * Smack's DNSSEC API is very simple. Just use
 * {@link org.jivesoftware.smack.ConnectionConfiguration.Builder#setDnssecMode(org.jivesoftware.smack.ConnectionConfiguration.DnssecMode)}
 * to enable DNSSEC. The argument, {@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode}, can be one of
 * <ul>
 * <li>{@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode#disabled}</li>
 * <li>{@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode#needsDnssec}</li>
 * <li>{@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode#needsDnssecAndDane}</li>
 * </ul>
 * The default is disabled.
 * <p>
 * If {@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode#needsDnssec} is used, then then Smack will only
 * connect if the DNS results required to determine a host for the XMPP domain could be verified using DNSSEC.
 * </p>
 * <p>
 * If set to {@link org.jivesoftware.smack.ConnectionConfiguration.DnssecMode#needsDnssecAndDane}, then then DANE will
 * be used to verify the XMPP service's TLS certificate if STARTTLS is used.
 * </p>
 * <h2>Best Practices</h2>
 * <p>
 * We recommend that applications using Smack's DNSSEC API do not ask the user if DNSSEC is avaialble. Instead they
 * should check for DNSSEC suport on every connection attempt. Once DNSSEC support has been discovered, the application
 * should use the `needsDnssec` mode for all future connection attempts. The same scheme can be applied when using DANE.
 * This approach is similar to the scheme established by to <a href="https://tools.ietf.org/html/rfc6797">HTTP Strict
 * Transport Security" (HSTS, RFC 6797</a>.
 * </p>
 */
package org.jivesoftware.smack.util.dns;
