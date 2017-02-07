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
package org.jivesoftware.smackx.pubsub;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents the <b>affiliations</b> element of the reply to a request for affiliations.
 * It is defined in the specification in section <a href="http://xmpp.org/extensions/xep-0060.html#entity-affiliations">5.7 Retrieve Affiliations</a> and
 * <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-affiliations">8.9 Manage Affiliations</a>.
 * 
 * @author Robin Collier
 */
public class AffiliationsExtension extends NodeExtension
{
    protected List<Affiliation> items = Collections.emptyList();
    private final String node;

    public AffiliationsExtension() {
        this(null, null);
    }

    public AffiliationsExtension(List<Affiliation> subList) {
        this(subList, null);
    }

    public AffiliationsExtension(List<Affiliation> subList, String node) {
        super(PubSubElementType.AFFILIATIONS);
        items = subList;
        this.node = node;
    }

    public List<Affiliation> getAffiliations()
    {
        return items;
    }

    @Override
    public CharSequence toXML()
    {
        if ((items == null) || (items.size() == 0))
        {
            return super.toXML();
        }
        else
        {
            // Can't use XmlStringBuilder(this), because we don't want the namespace to be included
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.openElement(getElementName());
            xml.optAttribute("node", node);
            xml.rightAngleBracket();
            xml.append(items);
            xml.closeElement(this);
            return xml;
        }
    }
}
