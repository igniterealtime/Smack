/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2004 Jive Software.
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

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * IQ packet used by Smack to bind a resource and to obtain the jid assigned by the server.
 * There are two ways to bind a resource. One is simply sending an empty Bind packet where the
 * server will assign a new resource for this connection. The other option is to set a desired
 * resource but the server may return a modified version of the sent resource.<p>
 *
 * For more information refer to the following
 * <a href=http://www.xmpp.org/specs/rfc3920.html#bind>link</a>. 
 *
 * @author Gaston Dombiak
 */
public class Bind extends IQ {

    private String resource = null;
    private String jid = null;

    public Bind() {
        setType(IQ.Type.SET);
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">");
        if (resource != null) {
            buf.append("<resource>").append(resource).append("</resource>");
        }
        if (jid != null) {
            buf.append("<jid>").append(jid).append("</jid>");
        }
        buf.append("</bind>");
        return buf.toString();
    }

    public static class Provider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            Bind bind = new Bind();
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("resource")) {
                        bind.setResource(parser.nextText());
                    }
                    else if (parser.getName().equals("jid")) {
                        bind.setJid(parser.nextText());
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("bind")) {
                        done = true;
                    }
                }
            }

            return bind;
        }
    }
}
