/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
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

package org.jivesoftware.smackx.workgroup.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.Map;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Collections;
import java.io.IOException;

import org.jivesoftware.smackx.workgroup.packet.MetaData;

/**
 * Utility class for meta-data parsing and writing.
 *
 * @author Matt Tucker
 */
public class MetaDataUtils {

    /**
     * Parses any available meta-data and returns it as a Map of String name/value
     * pairs. The parser must be positioned at an opening meta-data tag, or an
     * empty map will be returned.
     *
     * @param parser the XML parser positioned at an opening meta-data tag.
     * @return the meta-data.
     * @throws XmlPullParserException if an error occurs while parsing the XML.
     * @throws IOException if an error occurs while parsing the XML.
     */
    public static Map parseMetaData(XmlPullParser parser) throws XmlPullParserException,
            IOException
    {
        int eventType = parser.getEventType();

        // If correctly positioned on an opening meta-data tag, parse meta-data.
        if ((eventType == XmlPullParser.START_TAG)
                && parser.getName().equals(MetaData.ELEMENT_NAME)
                && parser.getNamespace().equals(MetaData.NAMESPACE)) {
            Map metaData = new Hashtable();

            eventType = parser.nextTag();

            // Keep parsing until we've gotten to end of meta-data.
            while ((eventType != XmlPullParser.END_TAG)
                  || (! parser.getName().equals(MetaData.ELEMENT_NAME))) {
                String name = parser.getAttributeValue(0);
                String value = parser.nextText();

                metaData.put(name, value);

                eventType = parser.nextTag();
            }

            return metaData;
        }

        return Collections.EMPTY_MAP;
    }

    /**
     * Encodes a Map of String name/value pairs into the meta-data XML format.
     *
     * @param metaData the Map of meta-data.
     * @return the meta-data values in XML form.
     */
    public static String encodeMetaData(Map metaData) {
        StringBuffer buf = new StringBuffer();
        if (metaData != null && metaData.size() > 0) {
            buf.append("<metadata xmlns=\"http://www.jivesoftware.com/workgroup/metadata\">");
            for (Iterator i=metaData.keySet().iterator(); i.hasNext(); ) {
                Object key = i.next();
                String value = metaData.get(key).toString();
                buf.append("<value name=\"").append(key).append("\">");
                buf.append(value);
                buf.append("</value>");
            }
            buf.append("</metadata>");
        }
        return buf.toString();
    }
}