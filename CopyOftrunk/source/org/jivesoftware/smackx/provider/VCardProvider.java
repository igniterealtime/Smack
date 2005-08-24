/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.VCard;
import org.w3c.dom.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Gaston
 * Date: Jun 18, 2005
 * Time: 1:00:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class VCardProvider implements IQProvider {

  public IQ parseIQ(XmlPullParser parser) throws Exception {
      StringBuffer sb = new StringBuffer();
      try {
          int event = parser.getEventType();
          // get the content
          while (true) {
              switch (event) {
                  case XmlPullParser.TEXT:
                      sb.append(parser.getText());
                      break;
                  case XmlPullParser.START_TAG:
                      sb.append('<' + parser.getName() + '>');
                      break;
                  case XmlPullParser.END_TAG:
                      sb.append("</" + parser.getName() + '>');
                      break;
                  default:
              }

              if (event == XmlPullParser.END_TAG && "vCard".equals(parser.getName())) break;

              event = parser.next();
          }
      } catch (XmlPullParserException e) {
          e.printStackTrace();
      } catch (IOException e) {
          e.printStackTrace();
      }

      String xmlText = sb.toString();
      VCard vCard = new VCard();
      try {
          DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
          Document document = documentBuilder.parse(new ByteArrayInputStream(xmlText.getBytes()));

          new VCardReader(vCard, document).initializeFields();

      } catch (Exception e) {
          e.printStackTrace(System.err);
      }
      return vCard;
  }

    private class VCardReader {
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

            setupEmails();

            vCard.setOrganization(getTagContents("ORGNAME"));
            vCard.setOrganizationUnit(getTagContents("ORGUNIT"));

            setupSimpleFields();
            setupPhones("WORK", true);
            setupPhones("HOME", false);

            setupAddress("WORK", true);
            setupAddress("HOME", false);
        }

        private void setupEmails() {
            NodeList nodes = document.getElementsByTagName("USERID");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                if ("WORK".equals(element.getParentNode().getFirstChild().getNodeName())) {
                    vCard.setEmailWork(getTextContent(element));
                } else {
                    vCard.setEmailHome(getTextContent(element));
                }
            }
        }

        private void setupPhones(String type, boolean work) {
            NodeList allPhones = document.getElementsByTagName("TEL");
            for (int i = 0; i < allPhones.getLength(); i++) {
                Element node = (Element) allPhones.item(i);
                if (type.equals(node.getChildNodes().item(1).getNodeName())) {
                    String code = node.getFirstChild().getNodeName();
                    String value = getTextContent(node.getChildNodes().item(2));
                    if (work) {
                        vCard.setPhoneWork(code, value);
                    }
                    else {
                        vCard.setPhoneHome(code, value);
                    }
                }
            }
        }

        private void setupAddress(String type, boolean work) {
            NodeList allAddresses = document.getElementsByTagName("ADR");
            for (int i = 0; i < allAddresses.getLength(); i++) {
                Element node = (Element) allAddresses.item(i);
                NodeList childNodes = node.getChildNodes();
                if (type.equals(childNodes.item(0).getNodeName())) {
                    for (int j = 1; j < childNodes.getLength(); j++) {
                        Node item = childNodes.item(j);
                        if (item instanceof Element) {
                            if (work) {
                                vCard.setAddressFieldWork(item.getNodeName(), getTextContent(item));
                            }
                            else {
                                vCard.setAddressFieldHome(item.getNodeName(), getTextContent(item));
                            }
                        }
                    }
                }
            }
        }

        private String getTagContents(String tag) {
            NodeList nodes = document.getElementsByTagName(tag);
            if (nodes.getLength() == 1) {
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
                    if ("FN".equals(element.getNodeName())) continue;

                    if (element.getChildNodes().getLength() == 0) {
                        vCard.setField(element.getNodeName(), "");
                    } else if (element.getChildNodes().getLength() == 1 &&
                            element.getChildNodes().item(0) instanceof Text) {
                        vCard.setField(element.getNodeName(), getTextContent(element));
                    }
                }
            }
        }

        private String getTextContent(Node node) {
            StringBuffer result = new StringBuffer();
            appendText(result, node);
            return result.toString();
        }

        private void appendText(StringBuffer result, Node node) {
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
