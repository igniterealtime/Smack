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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.xmlpull.v1.XmlPullParser;

/**
* The DiscoverInfoProvider parses Service Discovery information packets.
*
* @author Gaston Dombiak
*/
public class DiscoverInfoProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        DiscoverInfo discoverInfo = new DiscoverInfo();
        boolean done = false;
        DiscoverInfo.Feature feature = null;
        DiscoverInfo.Identity identity = null;
        String category = "";
        String name = "";
        String type = "";
        String variable = "";
        discoverInfo.setNode(parser.getAttributeValue("", "node"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("identity")) {
                    // Initialize the variables from the parsed XML
                    category = parser.getAttributeValue("", "category");
                    name = parser.getAttributeValue("", "name");
                    type = parser.getAttributeValue("", "type");
                }
                else if (parser.getName().equals("feature")) {
                    // Initialize the variables from the parsed XML
                    variable = parser.getAttributeValue("", "var");
                }
                // Otherwise, it must be a packet extension.
                else {
                    discoverInfo.addExtension(PacketParserUtils.parsePacketExtension(parser
                            .getName(), parser.getNamespace(), parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("identity")) {
                    // Create a new identity and add it to the discovered info.
                    identity = new DiscoverInfo.Identity(category, name);
                    identity.setType(type);
                    discoverInfo.addIdentity(identity);
                }
                if (parser.getName().equals("feature")) {
                    // Create a new feature and add it to the discovered info.
                    discoverInfo.addFeature(variable);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return discoverInfo;
    }
}