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
package org.jivesoftware.smackx.softwareInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.Async.ThrowingRunnable;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mediaelement.element.MediaElement;
import org.jivesoftware.smackx.softwareinfo.SoftwareInfoManager;
import org.jivesoftware.smackx.softwareinfo.form.SoftwareInfoForm;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;

public class SoftwareInfoIntegrationTest extends AbstractSmackIntegrationTest {

    public final SoftwareInfoManager sim1;
    public final SoftwareInfoManager sim2;

    public SoftwareInfoIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws IOException, XmlPullParserException, SmackParsingException {
        super(environment);
        sim1 = SoftwareInfoManager.getInstanceFor(conOne);
        sim2 = SoftwareInfoManager.getInstanceFor(conTwo);
    }

    @BeforeClass
    public void setUp() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);
    }

    @SmackIntegrationTest
    public void test() throws Exception {
        SoftwareInfoForm softwareInfoSent = createSoftwareInfoForm();
        performActionAndWaitForPresence(conTwo, conOne, new ThrowingRunnable() {
            @Override
            public void runOrThrow() throws Exception {
                sim1.publishSoftwareInformationForm(softwareInfoSent);
            }
        });
        SoftwareInfoForm softwareInfoFormReceived = sim2.fromJid(conOne.getUser());
        assertEquals(softwareInfoSent, softwareInfoFormReceived);
    }

    private static SoftwareInfoForm createSoftwareInfoForm() throws URISyntaxException {
        SoftwareInfoForm.Builder builder = SoftwareInfoForm.getBuilder();
        MediaElement mediaElement = MediaElement.builder()
                                       .addUri(new MediaElement.Uri(new URI("http://example.org"), "test-type"))
                                       .setHeightAndWidth(16, 16)
                                       .build();

        SoftwareInfoForm softwareInfoForm = builder.setIcon(mediaElement)
                                                   .setOS("Windows")
                                                   .setOSVersion("XP")
                                                   .setSoftware("Exodus")
                                                   .setSoftwareVersion("0.9.1")
                                                   .build();
        return softwareInfoForm;
    }
}
