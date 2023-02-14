/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.softwareinfo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.mediaelement.element.MediaElement;
import org.jivesoftware.smackx.softwareinfo.form.SoftwareInfoForm;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.JidTestUtil;

public class SoftwareInfoManagerTest {

    private static final EntityFullJid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    private XMPPConnection connection;
    private Protocol protocol;

    @BeforeEach
    public void setup() throws XMPPException, SmackException, InterruptedException {
        protocol = new Protocol();
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID);
    }

    @Test
    public void softwareInfoManagerTest() throws IOException, XmlPullParserException, SmackParsingException, URISyntaxException {
        SoftwareInfoManager manager = SoftwareInfoManager.getInstanceFor(connection);
        manager.publishSoftwareInformationForm(buildSoftwareInfoFormUsingBuilder());
        manager.publishSoftwareInformationForm(buildSoftwareInfoFromDataForm());
    }

    public static SoftwareInfoForm buildSoftwareInfoFormUsingBuilder() throws URISyntaxException {
        SoftwareInfoForm.Builder builder = SoftwareInfoForm.getBuilder();
        MediaElement mediaElement = createMediaElement();
        builder.setIcon(mediaElement);
        builder.setOS("Windows");
        builder.setOSVersion("XP");
        builder.setSoftware("Exodus");
        builder.setSoftwareVersion("0.9.1");
        return builder.build();
    }

    public static SoftwareInfoForm buildSoftwareInfoFromDataForm() throws URISyntaxException {
        DataForm.Builder dataFormBuilder = DataForm.builder(DataForm.Type.result);
        dataFormBuilder.addField(FormField.buildHiddenFormType(SoftwareInfoForm.FORM_TYPE));
        dataFormBuilder.addField(FormField.builder("icon")
                                   .addFormFieldChildElement(createMediaElement())
                                   .build());
        dataFormBuilder.addField(FormField.builder("os")
                                    .setValue("Windows")
                                    .build());
        dataFormBuilder.addField(FormField.builder("os_version")
                       .setValue("XP")
                       .build());
        dataFormBuilder.addField(FormField.builder("software")
                       .setValue("Exodus")
                       .build());
        dataFormBuilder.addField(FormField.builder("software_version")
                       .setValue("0.9.1")
                       .build());
        SoftwareInfoForm softwareInfoForm = SoftwareInfoForm.getBuilder()
                                                 .setDataForm(dataFormBuilder.build())
                                                 .build();
        return softwareInfoForm;
    }

    public static MediaElement createMediaElement() throws URISyntaxException {
        return MediaElement.builder()
                           .addUri(new MediaElement.Uri(new URI("http://example.org"), "test-type"))
                           .setHeightAndWidth(16, 16)
                           .build();
    }
}
