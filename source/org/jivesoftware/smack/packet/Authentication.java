/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;

/**
 * Authentication packet, which can be used to login to a XMPP server as well
 * as discover login information from the server.
 */
public class Authentication extends IQ {

    private String username = null;
    private String password = null;
    private String digest = null;
    private String resource = null;

    /**
     * Create a new authentication packet. By default, the packet will be in
     * "set" mode in order to perform an actual authentication with the server.
     * In order to send a "get" request to get the available authentication
     * modes back from the server, change the type of the IQ packet to "get":
     *
     * <p><tt>setType(IQ.Type.GET);</tt>
     */
    public Authentication() {
        setType(IQ.Type.SET);
    }

    /**
     * Returns the username, or <tt>null</tt> if the username hasn't been sent.
     *
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the plain text password or <tt>null</tt> if the password hasn't
     * been set.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the plain text password.
     *
     * @param password the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the password digest or <tt>null</tt> if the digest hasn't
     * been set. Password digests offer a more secure alternative for
     * authentication compared to plain text. The digest is the hex-encoded
     * SHA-1 hash of the connection ID plus the user's password. If the
     * digest and password are set, digest authentication will be used. If
     * only one value is set, the respective authentication mode will be used.
     *
     * @return the digest of the user's password.
     */
    public String getDigest() {
        return digest;
    }

    /**
     * Sets the digest value using a connection ID and password. Password
     * digests offer a more secure alternative for authentication compared to
     * plain text. The digest is the hex-encoded SHA-1 hash of the connection ID
     * plus the user's password. If the digest and password are set, digest
     * authentication will be used. If only one value is set, the respective
     * authentication mode will be used.
     *
     * @param connectionID the connection ID.
     * @param password the password.
     * @see XMPPConnection#getConnectionID()
     */
    public void setDigest(String connectionID, String password) {
        this.digest = StringUtils.hash(connectionID + password);
    }

    /**
     * Sets the digest value directly. Password digests offer a more secure
     * alternative for authentication compared to plain text. The digest is
     * the hex-encoded SHA-1 hash of the connection ID plus the user's password.
     * If the digest and password are set, digest authentication will be used.
     * If only one value is set, the respective authentication mode will be used.
     *
     * @param digest the digest, which is the SHA-1 hash of the connection ID
     *      the user's password, encoded as hex.
     * @see XMPPConnection#getConnectionID()
     */
    public void setDigest(String digest) {
        this.digest = digest;
    }

    /**
     * Returns the resource or <tt>null</tt> if the resource hasn't been set.
     *
     * @return the resource.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Sets the resource.
     *
     * @param resource the resource.
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<query xmlns=\"jabber:iq:auth\">");
        if (username != null) {
            if (username.equals("")) {
                buf.append("<username/>");
            }
            else {
                buf.append("<username>").append( username).append("</username>");
            }
        }
        if (digest != null) {
            if (digest.equals("")) {
                buf.append("<digest/>");
            }
            else {
                buf.append("<digest>").append(digest).append("</digest>");
            }
        }
        if (password != null && digest == null) {
            if (password.equals("")) {
                buf.append("<password/>");
            }
            else {
                buf.append("<password>").append(password).append("</password>");
            }
        }
        if (resource != null) {
            if (resource.equals("")) {
                buf.append("<resource/>");
            }
            else {
                buf.append("<resource>").append(resource).append("</resource>");
            }
        }
        buf.append("</query>");
        return buf.toString();
    }
}
