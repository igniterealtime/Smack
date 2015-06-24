/**
 *
 * Copyright 2015 Florian Schmaus
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

package org.jivesoftware.smack.java7;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.jivesoftware.smack.util.IpAddressUtil;

/**
 * HostnameVerifier implementation for XMPP. Verifies a given name, the 'hostname' argument, which
 * should be the XMPP domain of the used XMPP service. The verifier compares the name with the
 * servers TLS certificate's <a href="https://tools.ietf.org/html/rfc5280#section-4.2.1.6">Subject
 * Alternative Name (SAN)</a> DNS name ('dNSName'), and, if there are no SANs, which the Common Name
 * (CN).
 * <p>
 * Based on the <a href="http://kevinlocke.name/bits/2012/10/03/ssl-certificate-verification-in-dispatch-and-asynchttpclient/">work by Kevin
 * Locke</a> (released under CC0 1.0 Universal / Public Domain Dedication).
 * </p>
 */
public class XmppHostnameVerifier implements HostnameVerifier {

    private static final Logger LOGGER = Logger.getLogger(XmppHostnameVerifier.class.getName());

    @Override
    public boolean verify(String hostname, SSLSession session) {
        boolean validCertificate = false, validPrincipal = false;
        try {
            Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates.length == 0) {
                return false;
            }
            if (!(peerCertificates[0] instanceof X509Certificate)) {
                return false;
            }
            X509Certificate peerCertificate = (X509Certificate) peerCertificates[0];
            try {
                match(hostname, peerCertificate);
                // Certificate matches hostname
                validCertificate = true;
            }
            catch (CertificateException e) {
                LOGGER.log(Level.INFO, "Certificate does not match hostname", e);
            }
        }
        catch (SSLPeerUnverifiedException e) {
            // Not using certificates for peers, try verifying the principal
            Principal peerPrincipal = null;
            try {
                peerPrincipal = session.getPeerPrincipal();
            }
            catch (SSLPeerUnverifiedException e2) {
                LOGGER.log(Level.INFO, "Can't verify principal for " + hostname + ". Not kerberos",
                                e2);
            }
            if (peerPrincipal instanceof KerberosPrincipal) {
                validPrincipal = match(hostname, (KerberosPrincipal) peerPrincipal);
            }
            else {
                LOGGER.info("Can't verify principal for " + hostname + ". Not kerberos");
            }
        }

        return validCertificate || validPrincipal;
    }

    private static void match(String name, X509Certificate cert) throws CertificateException {
        if (IpAddressUtil.isIpAddress(name)) {
            matchIp(name, cert);
        }
        else {
            matchDns(name, cert);
        }
    }

    private static boolean match(String name, KerberosPrincipal peerPrincipal) {
        // TODO
        LOGGER.warning("KerberosPrincipal validation not implemented yet. Can not verify " + name);
        return false;
    }

    /**
     * As defined in RFC 5280 § 4.2.1.6
     * <pre>
     * GeneralName ::= CHOICE {
     *   ...
     *   dNSName                         [2]     IA5String,
     *   ...
     * }
     * </pre>
     */
    private static final int ALTNAME_DNS = 2;

    /**
     * Try to match a certificate with a DNS name. This method returns if the certificate matches or
     * throws a {@link CertificateException} if not.
     *
     * @param name the DNS name.
     * @param cert the certificate.
     * @throws CertificateException if the DNS name does not match the certificate.
     */
    private static void matchDns(String name, X509Certificate cert) throws CertificateException {
        Collection<List<?>> subjAltNames = cert.getSubjectAlternativeNames();
        if (subjAltNames != null) {
            List<String> nonMatchingDnsAltnames = new LinkedList<>();
            for (List<?> san : subjAltNames) {
                if (((Integer) san.get(0)).intValue() != ALTNAME_DNS) {
                    continue;
                }
                String dnsName = (String) san.get(1);
                if (matchesPerRfc2818(name, dnsName)) {
                    // Signal success by returning.
                    return;
                }
                else {
                    nonMatchingDnsAltnames.add(dnsName);
                }
            }
            if (!nonMatchingDnsAltnames.isEmpty()) {
                // Reject if certificate contains subject alt names, but none of them matches
                StringBuilder sb = new StringBuilder("No subject alternative DNS name matching "
                                + name + " found. Tried: ");
                for (String nonMatchingDnsAltname : nonMatchingDnsAltnames) {
                    sb.append(nonMatchingDnsAltname).append(",");
                }
                throw new CertificateException(sb.toString());
            }
        }

        // Control flow will end here if the X509 certificate does not have *any* Subject
        // Alternative Names (SANs). Fallback trying to validate against the CN of the subject.
        LdapName dn = null;
        try {
            dn = new LdapName(cert.getSubjectX500Principal().getName());
        } catch (InvalidNameException e) {
            LOGGER.warning("Invalid DN: " + e.getMessage());
        }
        if (dn != null) {
            for (Rdn rdn : dn.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    if (matchesPerRfc2818(name, rdn.getValue().toString())) {
                        // Signal success by returning.
                        return;
                    }
                    break;
                }
            }
        }

        throw new CertificateException("No name matching " + name + " found");
    }

    private static boolean matchesPerRfc2818(String name, String template) {
        String[] nameParts = name.toLowerCase(Locale.US).split(".");
        String[] templateParts = template.toLowerCase(Locale.US).split(".");

        if (nameParts.length != templateParts.length) {
            return false;
        }

        for (int i = 0; i < nameParts.length; i++) {
            if (!matchWildCards(nameParts[i], templateParts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if the name matches against the template that may contain the wildcard char '*'.
     *
     * @param name
     * @param template
     * @return true if <code>name</code> matches <code>template</code>.
     */
    private static boolean matchWildCards(String name, String template) {
        int wildcardIndex = template.indexOf("*");
        if (wildcardIndex == -1) {
            return name.equals(template);
        }

        boolean isBeginning = true;
        String beforeWildcard = "";
        String afterWildcard = template;
        while (wildcardIndex != -1) {
            beforeWildcard = afterWildcard.substring(0, wildcardIndex);
            afterWildcard = afterWildcard.substring(wildcardIndex + 1);

            int beforeStartIndex = name.indexOf(beforeWildcard);
            if ((beforeStartIndex == -1) || (isBeginning && beforeStartIndex != 0)) {
                return false;
            }
            isBeginning = false;

            name = name.substring(beforeStartIndex + beforeWildcard.length());
            wildcardIndex = afterWildcard.indexOf("*");
        }

        return name.endsWith(afterWildcard);
    }

    private static final int ALTNAME_IP = 7;

    /**
     * Check if the certificate allows use of the given IP address.
     * <p>
     * From RFC2818 § 3.1: "In some cases, the URI is specified as an IP address rather than a
     * hostname. In this case, the iPAddress subjectAltName must be present in the certificate and
     * must exactly match the IP in the URI."
     * <p>
     *
     * @param expectedIP
     * @param cert
     * @throws CertificateException
     */
    private static void matchIp(String expectedIP, X509Certificate cert)
                    throws CertificateException {
        Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
        if (subjectAlternativeNames == null) {
            throw new CertificateException("No subject alternative names present");
        }
        List<String> nonMatchingIpAltnames = new LinkedList<>();
        for (List<?> san : subjectAlternativeNames) {
            if (((Integer) san.get(0)).intValue() != ALTNAME_IP) {
                continue;
            }
            String ipAddress = (String) san.get(1);
            if (expectedIP.equalsIgnoreCase(ipAddress)) {
                return;
            }
            else {
                try {
                    // See if the addresses match if we transform then, useful for IPv6 addresses
                    if (InetAddress.getByName(expectedIP).equals(InetAddress.getByName(ipAddress))) {
                        // expectedIP matches the given ipAddress, return
                        return;
                    }
                }
                catch (UnknownHostException | SecurityException e) {
                    LOGGER.log(Level.FINE, "Comparing IP strings failed", e);
                }
            }
            nonMatchingIpAltnames.add(ipAddress);
        }
        StringBuilder sb = new StringBuilder("No subject alternative names matching IP address "
                        + expectedIP + " found. Tried: ");
        for (String s : nonMatchingIpAltnames) {
            sb.append(s).append(",");
        }
        throw new CertificateException(sb.toString());
    }
}
