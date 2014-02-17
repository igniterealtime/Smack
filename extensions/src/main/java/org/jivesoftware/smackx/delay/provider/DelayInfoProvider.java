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
package org.jivesoftware.smackx.delay.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.delay.packet.DelayInfo;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

/**
 * This provider simply creates a {@link DelayInfo} decorator for the {@link DelayInformation} that
 * is returned by the superclass.  This allows the new code using
 * <a href="http://xmpp.org/extensions/xep-0203.html">Delay Information XEP-0203</a> to be
 * backward compatible with <a href="http://xmpp.org/extensions/xep-0091.html">XEP-0091</a>.  
 * 
 * <p>This provider must be registered in the <b>smack.properties</b> file for the element 
 * <b>delay</b> with namespace <b>urn:xmpp:delay</b></p>
 *  
 * @author Robin Collier
 */
public class DelayInfoProvider extends DelayInformationProvider
{

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception
	{
		return new DelayInfo((DelayInformation)super.parseExtension(parser));
	}

}
