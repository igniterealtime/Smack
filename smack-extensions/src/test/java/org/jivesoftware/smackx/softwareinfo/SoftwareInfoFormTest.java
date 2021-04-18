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

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.mediaelement.element.MediaElement;
import org.jivesoftware.smackx.softwareinfo.form.SoftwareInfoForm;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class SoftwareInfoFormTest extends SmackTestSuite {

    private final String xml =
            "<x xmlns='jabber:x:data' type='result'>" +
                    "<field var='FORM_TYPE' type='hidden'>" +
                        "<value>urn:xmpp:dataforms:softwareinfo</value>" +
                    "</field>" +
                    "<field var='icon'>" +
                        "<media xmlns='urn:xmpp:media-element' height='80' width='290'>" +
                            "<uri type='image/jpeg'>" +
                                "http://www.shakespeare.lit/clients/exodus.jpg" +
                            "</uri>" +
                            "<uri type='image/jpeg'>" +
                                "cid:sha1+f24030b8d91d233bac14777be5ab531ca3b9f102@bob.xmpp.org" +
                            "</uri>" +
                        "</media>" +
                    "</field>" +
                    "<field var='os'>" +
                        "<value>Windows</value>" +
                    "</field>" +
                    "<field var='os_version'>" +
                        "<value>XP</value>" +
                    "</field>" +
                    "<field var='software'>" +
                        "<value>Exodus</value>" +
                    "</field>" +
                    "<field var='software_version'>" +
                        "<value>0.9.1</value>" +
                    "</field>" +
            "</x>";

    @Test
    public void softwareInfoBuilderTest() throws URISyntaxException {
        SoftwareInfoForm softwareInfoForm = createSoftwareInfoForm();
        assertXmlSimilar(xml, softwareInfoForm.getDataForm().toXML());

        softwareInfoForm = createSoftwareInfoFormUsingDataForm();
        assertXmlSimilar(xml, softwareInfoForm.getDataForm().toXML());
    }

    @Test
    public void getInfoFromSoftwareInfoFormTest() throws URISyntaxException {
        SoftwareInfoForm softwareInfoForm = createSoftwareInfoForm();
        assertEquals("Windows", softwareInfoForm.getOS());
        assertEquals("XP", softwareInfoForm.getOSVersion());
        assertEquals("Exodus", softwareInfoForm.getSoftwareName());
        assertEquals("0.9.1", softwareInfoForm.getSoftwareVersion());
        assertXmlSimilar(createMediaElement().toXML(), softwareInfoForm.getIcon().toXML());
    }

    @Test
    public void faultySoftwareInfoFormsTest() {
        DataForm.Builder dataFormbuilder = DataForm.builder(DataForm.Type.result);
        TextSingleFormField formField = FormField.buildHiddenFormType("faulty_formtype");
        dataFormbuilder.addField(formField);
        assertThrows(IllegalArgumentException.class, () -> {
            SoftwareInfoForm.getBuilder().setDataForm(dataFormbuilder.build()).build();
        });

        DataForm.Builder builderWithoutFormType = DataForm.builder(DataForm.Type.result);
        assertThrows(IllegalArgumentException.class, () -> {
            SoftwareInfoForm.getBuilder().setDataForm(builderWithoutFormType.build()).build();
        });
    }

    public static SoftwareInfoForm createSoftwareInfoFormUsingDataForm() throws URISyntaxException {
        DataForm.Builder dataFormBuilder = DataForm.builder(DataForm.Type.result);
        TextSingleFormField formField = FormField.buildHiddenFormType(SoftwareInfoForm.FORM_TYPE);
        dataFormBuilder.addField(formField);

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

        SoftwareInfoForm softwareInfoForm = SoftwareInfoForm.getBuilder().setDataForm(dataFormBuilder.build()).build();
        return softwareInfoForm;
    }

    public static SoftwareInfoForm createSoftwareInfoForm() throws URISyntaxException {
        return SoftwareInfoForm.getBuilder()
                .setIcon(createMediaElement())
                .setOS("Windows")
                .setOSVersion("XP")
                .setSoftware("Exodus")
                .setSoftwareVersion("0.9.1")
                .build();
    }

    public static MediaElement createMediaElement() throws URISyntaxException {
        return MediaElement.builder()
                .addUri(new URI("http://www.shakespeare.lit/clients/exodus.jpg"), "image/jpeg")
                .addUri(new URI("cid:sha1+f24030b8d91d233bac14777be5ab531ca3b9f102@bob.xmpp.org"), "image/jpeg")
                .setHeightAndWidth(80, 290)
                .build();
    }
}
