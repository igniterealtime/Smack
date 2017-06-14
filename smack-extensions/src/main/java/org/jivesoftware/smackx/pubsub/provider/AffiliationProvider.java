/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.pubsub.Affiliation;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

import org.jxmpp.jid.BareJid;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses the affiliation element out of the reply stanza from the server
 * as specified in the <a href="http://xmpp.org/extensions/xep-0060.html#schemas-pubsub">affiliation schema</a>.
 * 
 * @author Robin Collier
 */
public class AffiliationProvider extends ExtensionElementProvider<Affiliation> {

    @Override
    public Affiliation parse(XmlPullParser parser, int initialDepth)
            throws Exception {
        String node = parser.getAttributeValue(null, "node");
        BareJid jid = ParserUtils.getBareJidAttribute(parser);

        String affiliationString = parser.getAttributeValue(null, "affiliation");
        Affiliation.Type affiliationType = null;
        if (affiliationString != null) {
            affiliationType = Affiliation.Type.valueOf(affiliationString);
        }
        Affiliation affiliation;
        if (node != null && jid == null) {
            // affiliationType may be empty
            affiliation = new Affiliation(node, affiliationType);
        }
        else if (node == null && jid != null) {
            PubSubNamespace namespace = null; // TODO
            affiliation = new Affiliation(jid, affiliationType, namespace);
        }
        else {
            throw new SmackException("Invalid affililation. Either one of 'node' or 'jid' must be set"
                    + ". Node: " + node
                    + ". Jid: " + jid
                    + '.');
        }
        return affiliation;
    }

}
