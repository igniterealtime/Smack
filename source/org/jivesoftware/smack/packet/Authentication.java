/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
     * <p/>
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
     * @param password     the password.
     * @see org.jivesoftware.smack.Connection#getConnectionID()
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
     *               the user's password, encoded as hex.
     * @see org.jivesoftware.smack.Connection#getConnectionID()
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
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"jabber:iq:auth\">");
        if (username != null) {
            if (username.equals("")) {
                buf.append("<username/>");
            }
            else {
                buf.append("<username>").append(username).append("</username>");
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
                buf.append("<password>").append(StringUtils.escapeForXML(password)).append("</password>");
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
