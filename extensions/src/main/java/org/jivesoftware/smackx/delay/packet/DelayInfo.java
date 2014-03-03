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
package org.jivesoftware.smackx.delay.packet;

import java.util.Date;

import org.jivesoftware.smack.util.XmppDateTime;

/**
 * A decorator for the {@link DelayInformation} class to transparently support
 * both the new <b>Delay Delivery</b> specification <a href="http://xmpp.org/extensions/xep-0203.html">XEP-0203</a> and 
 * the old one <a href="http://xmpp.org/extensions/xep-0091.html">XEP-0091</a>.
 * 
 * Existing code can be backward compatible. 
 * 
 * @author Robin Collier
 */
public class DelayInfo extends DelayInformation
{
    
	DelayInformation wrappedInfo;

        /**
         * Creates a new instance with given delay information. 
         * @param delay the delay information
         */
	public DelayInfo(DelayInformation delay)
	{
		super(delay.getStamp());
		wrappedInfo = delay;
	}
	
	@Override
	public String getFrom()
	{
		return wrappedInfo.getFrom();
	}

	@Override
	public String getReason()
	{
		return wrappedInfo.getReason();
	}

	@Override
	public Date getStamp()
	{
		return wrappedInfo.getStamp();
	}

	@Override
	public void setFrom(String from)
	{
		wrappedInfo.setFrom(from);
	}

	@Override
	public void setReason(String reason)
	{
		wrappedInfo.setReason(reason);
	}

	@Override
	public String getElementName()
	{
		return "delay";
	}

	@Override
	public String getNamespace()
	{
		return "urn:xmpp:delay";
	}

	@Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
                "\"");
        buf.append(" stamp=\"");
        buf.append(XmppDateTime.formatXEP0082Date(getStamp()));
        buf.append("\"");
        if (getFrom() != null && getFrom().length() > 0) {
            buf.append(" from=\"").append(getFrom()).append("\"");
        }
        buf.append(">");
        if (getReason() != null && getReason().length() > 0) {
            buf.append(getReason());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }
	
}
