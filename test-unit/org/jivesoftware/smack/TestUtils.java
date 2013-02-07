/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smack;

import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final public class TestUtils
{
	private TestUtils() {}
	
	public static XmlPullParser getIQParser(String stanza)
	{
		return getParser(stanza, "iq");
	}

	public static XmlPullParser getMessageParser(String stanza)
	{
		return getParser(stanza, "message");
	}

	public static XmlPullParser getPresenceParser(String stanza)
	{
		return getParser(stanza, "presence");
	}
	
	public static XmlPullParser getParser(String stanza, String startTag)
	{
		XmlPullParser parser = new MXParser();
		try
		{
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
			parser.setInput(new StringReader(stanza));
			boolean found = false;

			while (!found)
			{
				if ((parser.next() == XmlPullParser.START_TAG) && parser.getName().equals(startTag))
					found = true;
			}
			
			if (!found)
				throw new IllegalArgumentException("Cannot parse start tag [" + startTag + "] from stanze [" + stanza + "]");
		}
		catch (XmlPullParserException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return parser;
	}

}
