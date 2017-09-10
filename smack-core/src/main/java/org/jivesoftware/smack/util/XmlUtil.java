/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smack.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlUtil {

    private static final Logger LOGGER = Logger.getLogger(XmlUtil.class.getName());

    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    static {
        transformerFactory.setAttribute("indent-number", 2);
    }

    public static String prettyFormatXml(String xml) {
        StreamSource source = new StreamSource(new StringReader(xml));
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);

        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            // Transform the requested string into a nice formatted XML string
            transformer.transform(source, result);
        }
        catch (TransformerException | IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Transformer error", e);
            return xml;
        }

        return stringWriter.toString();
    }
}
