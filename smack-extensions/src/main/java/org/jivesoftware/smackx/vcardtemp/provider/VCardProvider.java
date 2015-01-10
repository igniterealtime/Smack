/**
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smackx.vcardtemp.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * vCard provider.
 *
 * @author Gaston Dombiak
 * @author Derek DeMoro
 */
public class VCardProvider extends IQProvider<VCard> {
    private static final Logger LOGGER = Logger.getLogger(VCardProvider.class.getName());
    
    private static final String PREFERRED_ENCODING = "UTF-8";

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;

    static {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            LOGGER.finer("Could not disallow doctype decl: " + e.getMessage());
            // If we can't disable DTDs, then at least try the following
            // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
            // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
            try {
                documentBuilderFactory.setFeature( "http://xml.org/sax/features/external-general-entities", false);
                documentBuilderFactory.setFeature( "http://xml.org/sax/features/external-parameter-entities", false);
            } catch (ParserConfigurationException e1) {
                LOGGER.finer("Could not disallow external entities for xerces parser: " + e1.getMessage());
            }
        }
        // Android throws an UnsupportedOperationException when calling setXIncludeAware() and for some dumb reason also
        // when calling isXIncludeWare(), while it defaults according to the docs to 'false'.
        boolean isXIncludeAware;
        try {
            isXIncludeAware = documentBuilderFactory.isXIncludeAware();
        } catch (UnsupportedOperationException e) {
            // Assume we are on Android where isXIncludeAware defaults to 'false'
            isXIncludeAware = false;
        }
        if (isXIncludeAware) {
            documentBuilderFactory.setXIncludeAware(false);
        }
        documentBuilderFactory.setExpandEntityReferences(false);

        // Harden the parser even further
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setCoalescing(false);
        documentBuilderFactory.setValidating(false);

        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            LOGGER.info("Could not enable secure processing parsing feature: " + e.getMessage());
        }

        DOCUMENT_BUILDER_FACTORY = documentBuilderFactory;
    }

    @Override
    public VCard parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        final StringBuilder sb = new StringBuilder();
        try {
            int event = parser.getEventType();
            // get the content
            while (true) {
                switch (event) {
                    case XmlPullParser.TEXT:
                        // We must re-escape the xml so that the DOM won't throw an exception
                        sb.append(StringUtils.escapeForXML(parser.getText()));
                        break;
                    case XmlPullParser.START_TAG:
                        sb.append('<').append(parser.getName()).append('>');
                        break;
                    case XmlPullParser.END_TAG:
                        sb.append("</").append(parser.getName()).append('>');
                        break;
                    default:
                }

                if (event == XmlPullParser.END_TAG && "vCard".equals(parser.getName())) break;

                event = parser.next();
            }
        }
        catch (XmlPullParserException e) {
            LOGGER.log(Level.SEVERE, "Exception parsing VCard", e);
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception parsing VCard", e);
        }

        String xmlText = sb.toString();
        try {
            return createVCardFromXML(xmlText);
        } catch (SAXException | ParserConfigurationException e) {
            throw new SmackException(e);
        }
    }

    /**
     * Builds a users vCard from xml file.
     *
     * @param xml the xml representing a users vCard.
     * @return the VCard.
     * @throws IOException 
     * @throws SAXException 
     * @throws UnsupportedEncodingException 
     * @throws ParserConfigurationException 
     */
    public static VCard createVCardFromXML(String xml) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException {
        VCard vCard = new VCard();

        DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        Document document = documentBuilder.parse(
                new ByteArrayInputStream(xml.getBytes(PREFERRED_ENCODING)));

        new VCardReader(vCard, document).initializeFields();
        return vCard;
    }

    private static class VCardReader {

        private final VCard vCard;
        private final Document document;

        VCardReader(VCard vCard, Document document) {
            this.vCard = vCard;
            this.document = document;
        }

        public void initializeFields() {
            vCard.setFirstName(getTagContents("GIVEN"));
            vCard.setLastName(getTagContents("FAMILY"));
            vCard.setMiddleName(getTagContents("MIDDLE"));
            setupPhoto();

            setupEmails();

            vCard.setOrganization(getTagContents("ORGNAME"));
            vCard.setOrganizationUnit(getTagContents("ORGUNIT"));

            setupSimpleFields();

            setupPhones();
            setupAddresses();
        }

        private void setupPhoto() {
            String binval = null;
            String mimetype = null;

            NodeList photo = document.getElementsByTagName("PHOTO");
            if (photo.getLength() != 1)
                return;

            Node photoNode = photo.item(0);
            NodeList childNodes = photoNode.getChildNodes();

            int childNodeCount = childNodes.getLength();
            List<Node> nodes = new ArrayList<Node>(childNodeCount);
            for (int i = 0; i < childNodeCount; i++)
                nodes.add(childNodes.item(i));

            String name = null;
            String value = null;
            for (Node n : nodes) {
                name = n.getNodeName();
                value = n.getTextContent();
                if (name.equals("BINVAL")) {
                    binval = value;
                }
                else if (name.equals("TYPE")) {
                    mimetype = value;
                }
            }

            if (binval == null || mimetype == null)
                return;

            vCard.setAvatar(binval, mimetype);
        }

        private void setupEmails() {
            NodeList nodes = document.getElementsByTagName("USERID");
            if (nodes == null) return;
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                if ("WORK".equals(element.getParentNode().getFirstChild().getNodeName())) {
                    vCard.setEmailWork(getTextContent(element));
                }
                else {
                    vCard.setEmailHome(getTextContent(element));
                }
            }
        }

        private void setupPhones() {
            NodeList allPhones = document.getElementsByTagName("TEL");
            if (allPhones == null) return;
            for (int i = 0; i < allPhones.getLength(); i++) {
                NodeList nodes = allPhones.item(i).getChildNodes();
                String type = null;
                String code = null;
                String value = null;
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node node = nodes.item(j);
                    if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                    String nodeName = node.getNodeName();
                    if ("NUMBER".equals(nodeName)) {
                        value = getTextContent(node);
                    }
                    else if (isWorkHome(nodeName)) {
                        type = nodeName;
                    }
                    else {
                        code = nodeName;
                    }
                }
                if (value == null) continue;
                if (code == null)
                    code = "VOICE";
                if ("HOME".equals(type)) {
                    vCard.setPhoneHome(code, value);
                }
                else { // By default, setup work phone
                    vCard.setPhoneWork(code, value);
                }
            }
        }

        private boolean isWorkHome(String nodeName) {
            return "HOME".equals(nodeName) || "WORK".equals(nodeName);
        }

        private void setupAddresses() {
            NodeList allAddresses = document.getElementsByTagName("ADR");
            if (allAddresses == null) return;
            for (int i = 0; i < allAddresses.getLength(); i++) {
                Element addressNode = (Element) allAddresses.item(i);

                String type = null;
                List<String> code = new ArrayList<String>();
                List<String> value = new ArrayList<String>();
                NodeList childNodes = addressNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node node = childNodes.item(j);
                    if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                    String nodeName = node.getNodeName();
                    if (isWorkHome(nodeName)) {
                        type = nodeName;
                    }
                    else {
                        code.add(nodeName);
                        value.add(getTextContent(node));
                    }
                }
                for (int j = 0; j < value.size(); j++) {
                    if ("HOME".equals(type)) {
                        vCard.setAddressFieldHome((String) code.get(j), (String) value.get(j));
                    }
                    else { // By default, setup work address
                        vCard.setAddressFieldWork((String) code.get(j), (String) value.get(j));
                    }
                }
            }
        }

        private String getTagContents(String tag) {
            NodeList nodes = document.getElementsByTagName(tag);
            if (nodes != null && nodes.getLength() == 1) {
                return getTextContent(nodes.item(0));
            }
            return null;
        }

        private void setupSimpleFields() {
            NodeList childNodes = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;

                    String field = element.getNodeName();
                    if (element.getChildNodes().getLength() == 0) {
                        vCard.setField(field, "");
                    }
                    else if (element.getChildNodes().getLength() == 1 &&
                            element.getChildNodes().item(0) instanceof Text) {
                        vCard.setField(field, getTextContent(element));
                    }
                }
            }
        }

        private String getTextContent(Node node) {
            StringBuilder result = new StringBuilder();
            appendText(result, node);
            return result.toString();
        }

        private void appendText(StringBuilder result, Node node) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node nd = childNodes.item(i);
                String nodeValue = nd.getNodeValue();
                if (nodeValue != null) {
                    result.append(nodeValue);
                }
                appendText(result, nd);
            }
        }
    }
}
