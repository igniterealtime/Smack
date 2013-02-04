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
