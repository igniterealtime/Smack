/**
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
package org.jivesoftware.smackx.pubsub;

import java.util.Collections;
import java.util.List;

/**
 * Represents the <b>affiliations</b> element of the reply to a request for affiliations.
 * It is defined in the specification in section <a href="http://xmpp.org/extensions/xep-0060.html#entity-affiliations">5.7 Retrieve Affiliations</a>.
 * 
 * @author Robin Collier
 */
public class AffiliationsExtension extends NodeExtension
{
	protected List<Affiliation> items = Collections.EMPTY_LIST;
	
	public AffiliationsExtension()
	{
		super(PubSubElementType.AFFILIATIONS);
	}
	
	public AffiliationsExtension(List<Affiliation> subList)
	{
		super(PubSubElementType.AFFILIATIONS);
		items = subList;
	}

	public List<Affiliation> getAffiliations()
	{
		return items;
	}

	@Override
	public String toXML()
	{
		if ((items == null) || (items.size() == 0))
		{
			return super.toXML();
		}
		else
		{
			StringBuilder builder = new StringBuilder("<");
			builder.append(getElementName());
			builder.append(">");
			
			for (Affiliation item : items)
			{
				builder.append(item.toXML());
			}
			
			builder.append("</");
			builder.append(getElementName());
			builder.append(">");
			return builder.toString();
		}
	}
}
