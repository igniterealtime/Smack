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
 *    contact webmaster@coolservlets.com.
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

/**
 * A generic IQ packet.
 *
 * @author Matt Tucker
 */
public abstract class IQ extends Packet {

    private Type type = Type.GET;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<iq ");
        if (getPacketID() != null) {
            buf.append("id=\"" + getPacketID() + "\" ");
        }
        if (getTo() != null) {
            buf.append("to=\"").append(getTo()).append("\" ");
        }
        if (getFrom() != null) {
            buf.append("from=\"").append(getFrom()).append("\" ");
        }
        if (type == null) {
            buf.append("type=\"get\">");
        }
        else {
            buf.append("type=\"").append(getType()).append("\">");
        }
        // Add the query section if there is one.
        String queryXML = getQueryXML();
        if (queryXML != null) {
            buf.append(queryXML);
        }
        buf.append("</iq>");
        // Add packet properties, if any are defined.
        String propertiesXML = getPropertiesXML();
        if (propertiesXML != null) {
            buf.append(propertiesXML);
        }
        // Add the error sub-packet, if there is one.
        Error error = getError();
        if (error != null) {
            buf.append(error.toXML());
        }
        return buf.toString();
    }

    /**
     * Implementations of this method should return the XML of the query section of
     * an IQ packet if that section should exist, or <tt>null</tt> otherwise. This lets
     * the majority of IQ XML writing be generic, with each sub-class providing just the
     * packet specific XML.
     *
     * @return the query section of the IQ XML.
     */
    public abstract String getQueryXML();

    /**
     * A class to represent the type of the IQ packet. Valid types are:
     *
     * <ul>
     *      <li>IQ.Type.GET
     *      <li>IQ.Type.SET
     *      <li>IQ.Type.RESULT
     *      <li>IQ.Type.ERROR
     * </ul>
     */
    public static class Type {

        public static final Type GET = new Type("get");
        public static final Type SET = new Type("set");
        public static final Type RESULT = new Type("result");
        public static final Type ERROR = new Type("error");

        public static Type fromString(String type) {
            if (type.equals(GET.toString())) {
                return GET;
            }
            else if (type.equals(SET.toString())) {
                return SET;
            }
            else if (type.equals(ERROR.toString())) {
                return ERROR;
            }
            else if (type.equals(RESULT.toString())) {
                return RESULT;
            }
            else {
                return null;
            }
        }

        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}
