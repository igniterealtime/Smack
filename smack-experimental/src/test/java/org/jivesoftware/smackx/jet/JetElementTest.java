/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet;

import static junit.framework.TestCase.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ciphers.Aes128GcmNoPadding;
import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.xml.sax.SAXException;

public class JetElementTest extends SmackTestSuite {

    @Test
    public void jetTest() throws InterruptedException, JingleEnvelopeManager.JingleEncryptionException, NoSuchAlgorithmException, SmackException.NotConnectedException, SmackException.NoResponseException, IOException, SAXException {
        ExtensionElement child = new SecurityStub().encryptJingleTransfer(null, null);
        JetSecurityElement element = new JetSecurityElement("content1", Aes128GcmNoPadding.NAMESPACE, child);
        JetSecurity security = new JetSecurity(element);
        assertEquals(SecurityStub.NAMESPACE, security.getEnvelopeNamespace());
        assertEquals(Aes128GcmNoPadding.NAMESPACE, element.getCipherName());
        assertEquals(SecurityStub.NAMESPACE, element.getEnvelopeNamespace());
        assertEquals("content1", element.getContentName());

        String xml = "<security xmlns='" + JetSecurity.NAMESPACE + "' " +
                "name='content1' " +
                "cipher='" + Aes128GcmNoPadding.NAMESPACE + "' " +
                "type='" + SecurityStub.NAMESPACE + "'>" +
                "<security-stub/>" +
                "</security>";
        assertXMLEqual(xml, security.getElement().toXML().toString());
    }

    private static class SecurityStub implements JingleEnvelopeManager {
        public static final String NAMESPACE = "urn:xmpp:security-stub";

        @Override
        public ExtensionElement encryptJingleTransfer(FullJid recipient, byte[] keyData) throws JingleEncryptionException, InterruptedException, NoSuchAlgorithmException, SmackException.NotConnectedException, SmackException.NoResponseException {
            return new ExtensionElement() {
                @Override
                public String getNamespace() {
                    return NAMESPACE;
                }

                @Override
                public String getElementName() {
                    return "security-stub";
                }

                @Override
                public CharSequence toXML() {
                    return "<security-stub/>";
                }
            };
        }

        @Override
        public byte[] decryptJingleTransfer(FullJid sender, ExtensionElement envelope) throws JingleEncryptionException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
            return new byte[0];
        }

        @Override
        public XMPPConnection getConnection() {
            return null;
        }

        @Override
        public String getJingleEnvelopeNamespace() {
            return NAMESPACE;
        }
    }
}
