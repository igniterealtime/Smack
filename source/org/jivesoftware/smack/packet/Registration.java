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

import java.util.Map;
import java.util.Iterator;

/**
 * Represents registration packets. An empty GET query will cause the server to return information
 * about it's registration support. SET queries can be used to create accounts or update
 * existing account information. XMPP servers may require a number of attributes to be set
 * when creating a new account. The standard account attributes are as follows:
 * <ul>
 *      <li>name -- the user's name.
 *      <li>first -- the user's first name.
 *      <li>last -- the user's last name.
 *      <li>email -- the user's email address.
 *      <li>city -- the user's city.
 *      <li>state -- the user's state.
 *      <li>zip -- the user's ZIP code.
 *      <li>phone -- the user's phone number.
 *      <li>url -- the user's website.
 *      <li>date -- the date the registration took place.
 *      <li>misc -- other miscellaneous information to associate with the account.
 *      <li>text -- textual information to associate with the account.
 *      <li>remove -- empty flag to remove account.
 * </ul>
 *
 * @author Matt Tucker
 */
public class Registration extends IQ {

    private String username = null;
    private String password = null;
    private Map attributes = null;

    /**
     * Returns the username, or <tt>null</tt> if no username has ben set.
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
     * Returns the password, or <tt>null</tt> if no password has been set.
     *
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the map of String key/value pairs of account attributes.
     *
     * @return the account attributes.
     */
    public Map getAttributes() {
        return attributes;
    }

    /**
     * Sets the account attributes. The map must only contain String key/value pairs.
     *
     * @param attributes the account attributes.
     */
    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<query xmlns=\"jabber:iq:register\">");
        if (username != null) {
            buf.append("<username>").append(username).append("</username>");
        }
        if (password != null) {
            buf.append("<password>").append(password).append("</password>");
        }
        if (attributes != null && attributes.size() > 0) {
            Iterator fieldNames = attributes.keySet().iterator();
            while (fieldNames.hasNext()) {
                String name = (String)fieldNames.next();
                String value = (String)attributes.get(name);
                buf.append("<").append(name).append(">");
                buf.append(value);
                buf.append("</").append(name).append(">");
            }
        }
        buf.append("</query>");
        return buf.toString();
    }
}
