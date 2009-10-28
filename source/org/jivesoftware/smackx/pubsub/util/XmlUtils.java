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
package org.jivesoftware.smackx.pubsub.util;

import java.io.StringReader;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Simple utility for pretty printing xml.
 * 
 * @author Robin Collier
 */
public class XmlUtils
{
	/**
	 * 
	 * @param header Just a title for the stanza for readability.  Single word no spaces since
	 * it is inserted as the root element in the output.
	 * @param xml The string to pretty print
	 */
	static public void prettyPrint(String header, String xml)
	{
		try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

			if (header != null)
			{
				xml = "\n<" + header + ">" + xml + "</" + header + '>';
			}
			transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(System.out));
		}
		catch (Exception e)
		{
			System.out.println("Something wrong with xml in \n---------------\n" + xml + "\n---------------");
			e.printStackTrace();
		}
	}

	static public void appendAttribute(StringBuilder builder, String att, String value)
	{
		builder.append(" ");
		builder.append(att);
		builder.append("='");
		builder.append(value);
		builder.append("'");
	}

}
