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

package org.jivesoftware.smackx.packet;

import java.util.*;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * An XHTML sub-packet, which is used by XMPP clients to exchange formatted text. The XHTML 
 * extension is only a subset of XHTML 1.0.<p>
 * 
 * The following link summarizes the requirements of XHTML IM:
 * <a href="http://www.jabber.org/jeps/jep-0071.html#sect-id2598018">Valid tags</a>.<p>
 * 
 * Warning: this is an non-standard protocol documented by
 * <a href="http://www.jabber.org/jeps/jep-0071.html">JEP-71</a>. Because this is a
 * non-standard protocol, it is subject to change.
 *
 * @author Gaston Dombiak
 */
public class XHTMLExtension implements PacketExtension {

    private List bodies = new ArrayList();

    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "html"
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return "html";
    }

    /** 
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "http://jabber.org/protocol/xhtml-im"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return "http://jabber.org/protocol/xhtml-im";
    }

    /**
     * Returns the XML representation of a XHTML extension according the specification.
     * 
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following example:
     * <pre>
     * &lt;message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack"&gt;
     *     &lt;subject&gt;Any subject you want&lt;/subject&gt;
     *     &lt;body&gt;This message contains something interesting.&lt;/body&gt;
     *     &lt;html xmlns="http://jabber.org/protocol/xhtml-im"&gt;
     *         &lt;body&gt;&lt;p style='font-size:large'&gt;This message contains something &lt;em&gt;interesting&lt;/em&gt;.&lt;/p&gt;&lt;/body&gt;
     *     &lt;/html&gt;
     * &lt;/message&gt;
     * </pre>
     * 
     */
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        // Loop through all the bodies and append them to the string buffer
        for (Iterator i = getBodies(); i.hasNext();) {
            buf.append((String) i.next());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * Returns an Iterator for the bodies in the packet.
     *
     * @return an Iterator for the bodies in the packet.
     */
    public Iterator getBodies() {
        synchronized (bodies) {
            return Collections.unmodifiableList(new ArrayList(bodies)).iterator();
        }
    }

    /**
     * Adds a body to the packet.
     *
     * @param body the body to add.
     */
    public void addBody(String body) {
        synchronized (bodies) {
            bodies.add(body);
        }
    }

    /**
     * Returns a count of the bodies in the XHTML packet.
     *
     * @return the number of bodies in the XHTML packet.
     */
    public int getBodiesCount() {
        return bodies.size();
    }

}
