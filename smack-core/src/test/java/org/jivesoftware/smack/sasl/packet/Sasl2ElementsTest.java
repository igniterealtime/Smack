/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.packet;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.sasl2.Sasl2ModuleDescriptor;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Sasl2ElementsTest {

    @BeforeAll
    public static void setup() throws Exception {
        Class.forName(Bind2ModuleDescriptor.class.getName());
        Class.forName(Sasl2ModuleDescriptor.class.getName());
    }

    @Test
    public void testSasl2FeatureSerialization() throws Exception {
        Sasl2Feature feature = new Sasl2Feature(
            Arrays.asList("SCRAM-SHA-1", "PLAIN"),
            Collections.emptyList()
        );

        String xml = feature.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<authentication xmlns='urn:xmpp:sasl:2'><mechanism>SCRAM-SHA-1</mechanism><mechanism>PLAIN</mechanism></authentication>", xml);
        assertFalse(feature.hasInline());
        assertNull(feature.getInline());
        assertFalse(feature.hasInlineFeature(Bind2Elements.Bind.class));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testSasl2FeatureWithInlineSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Bind2Elements.Bind bindFeature = new Bind2Elements.Bind(
            Collections.singleton("urn:xmpp:carbons:2"), null, Collections.emptyList()
        );
        Sasl2Feature.Inline inline = new Sasl2Feature.Inline(Collections.singletonList(bindFeature));
        Sasl2Feature feature = new Sasl2Feature(
            Arrays.asList("SCRAM-SHA-1-PLUS", "SCRAM-SHA-1"),
            inline
        );

        String xml = feature.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<authentication xmlns='urn:xmpp:sasl:2'><mechanism>SCRAM-SHA-1-PLUS</mechanism><mechanism>SCRAM-SHA-1</mechanism><inline><bind xmlns='urn:xmpp:bind:0'><inline><feature var='urn:xmpp:carbons:2'/></inline></bind></inline></authentication>", xml);
        assertTrue(feature.hasInline());
        assertNotNull(feature.getInline());
        assertTrue(feature.getInline().hasFeature(Bind2Elements.Bind.class));
        assertNotNull(feature.getInline().getFeature(Bind2Elements.Bind.class));
        assertTrue(feature.hasInlineFeature(Bind2Elements.Bind.class));

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Feature parsed = Sasl2Provider.Sasl2FeatureProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals(2, parsed.getMechanisms().size());
        assertEquals("SCRAM-SHA-1-PLUS", parsed.getMechanisms().get(0));
        assertEquals("SCRAM-SHA-1", parsed.getMechanisms().get(1));
        assertTrue(parsed.hasInline());
        assertNotNull(parsed.getInline());
        assertTrue(parsed.getInline().hasFeature(Bind2Elements.Bind.class));
        assertNotNull(parsed.getInline().getFeature(Bind2Elements.Bind.class));
        assertTrue(parsed.hasInlineFeature(Bind2Elements.Bind.class));
        assertNotNull(parsed.getInlineFeature(Bind2Elements.Bind.class));
    }

    private static final UUID uuid = UUID.randomUUID();

    @Test
    public void testAuthenticateSerialization() throws Exception {
        Sasl2Nonza.UserAgent ua = new Sasl2Nonza.UserAgent(uuid, "AwesomeXMPP", "Smartphone");
        Sasl2Nonza.Authenticate auth = new Sasl2Nonza.Authenticate("PLAIN", "initial-data-here", ua, Collections.emptyList());

        String xml = auth.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<authenticate xmlns='urn:xmpp:sasl:2' mechanism='PLAIN'><initial-response>initial-data-here</initial-response><user-agent id='" + uuid + "'><software>AwesomeXMPP</software><device>Smartphone</device></user-agent></authenticate>", xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAuthenticateWithBind2SerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.UserAgent ua = new Sasl2Nonza.UserAgent(uuid, "AwesomeXMPP", null);
        Bind2Elements.Bind bind = new Bind2Elements.Bind(null, "AwesomeXMPP", Collections.emptyList());
        Sasl2Nonza.Authenticate auth = new Sasl2Nonza.Authenticate("SCRAM-SHA-1", "initial-data", ua, Collections.singletonList(bind));

        String xml = auth.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<authenticate xmlns='urn:xmpp:sasl:2' mechanism='SCRAM-SHA-1'><initial-response>initial-data</initial-response><user-agent id='" + uuid + "'><software>AwesomeXMPP</software></user-agent><bind xmlns='urn:xmpp:bind:0'><tag>AwesomeXMPP</tag></bind></authenticate>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Authenticate parsed = Sasl2Provider.AuthenticateProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("SCRAM-SHA-1", parsed.getMechanism());
        assertEquals("initial-data", parsed.getInitialResponse());
        assertNotNull(parsed.getUserAgent());
        assertEquals(uuid, parsed.getUserAgent().getId());
        assertEquals("AwesomeXMPP", parsed.getUserAgent().getSoftware());
        assertNull(parsed.getUserAgent().getDevice());
        assertEquals(1, parsed.getExtensionElements().size());
        assertTrue(parsed.hasExtension(Bind2Elements.Bind.class));
        Bind2Elements.Bind parsedBind = parsed.getExtension(Bind2Elements.Bind.class);
        assertNotNull(parsedBind);
        assertEquals("AwesomeXMPP", parsedBind.getTag());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testChallengeSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.Challenge challenge = new Sasl2Nonza.Challenge("c29tZS1jaGFsbGVuZ2U=");
        String xml = challenge.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<challenge xmlns='urn:xmpp:sasl:2'>c29tZS1jaGFsbGVuZ2U=</challenge>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Challenge parsed = Sasl2Provider.ChallengeProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("c29tZS1jaGFsbGVuZ2U=", parsed.getData());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testResponseSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.Response response = new Sasl2Nonza.Response("c29tZS1yZXNwb25zZQ==");
        String xml = response.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<response xmlns='urn:xmpp:sasl:2'>c29tZS1yZXNwb25zZQ==</response>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Response parsed = Sasl2Provider.ResponseProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("c29tZS1yZXNwb25zZQ==", parsed.getData());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testSuccessSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Bind2Elements.Bound bound = new Bind2Elements.Bound(null);
        Sasl2Nonza.Success success = new Sasl2Nonza.Success(
            "YWRkaXRpb25hbA==", "user@example.com/AwesomeXMPP.1234", Collections.singletonList(bound)
        );

        String xml = success.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<success xmlns='urn:xmpp:sasl:2'><additional-data>YWRkaXRpb25hbA==</additional-data><authorization-identifier>user@example.com/AwesomeXMPP.1234</authorization-identifier><bound xmlns='urn:xmpp:bind:0'/></success>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Success parsed = Sasl2Provider.SuccessProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("YWRkaXRpb25hbA==", parsed.getAdditionalData());
        assertEquals("user@example.com/AwesomeXMPP.1234", parsed.getAuthorizationIdentifier().toString());
        assertEquals(1, parsed.getExtensionElements().size());
        assertTrue(parsed.hasExtension(Bind2Elements.Bound.class));
        assertNotNull(parsed.getExtension(Bind2Elements.Bound.class));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testFailureSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Map<String, String> descriptiveTexts = new HashMap<>();
        descriptiveTexts.put("", "Invalid credentials");
        StandardExtensionElement appSpecific = StandardExtensionElement.builder("optional-app", "urn:custom:error").build();

        Sasl2Nonza.Failure failure = new Sasl2Nonza.Failure(
            SASLError.not_authorized, descriptiveTexts, Collections.singletonList(appSpecific)
        );

        String xml = failure.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<failure xmlns='urn:xmpp:sasl:2'><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/><text>Invalid credentials</text><optional-app xmlns='urn:custom:error'/></failure>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Failure parsed = Sasl2Provider.FailureProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals(SASLError.not_authorized, parsed.getSASLError());
        assertEquals("not-authorized", parsed.getSASLErrorString());
        assertEquals("Invalid credentials", parsed.getDescriptiveText());
        assertEquals(1, parsed.getExtensionElements().size());
        assertEquals("optional-app", parsed.getExtensionElements().get(0).getElementName());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testContinueSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.Continue continueElement = new Sasl2Nonza.Continue(
            "QWRkaXRpb25hbA==", Arrays.asList("TOTP", "UNREALISTIC-2FA"), "This account requires 2FA"
        );

        String xml = continueElement.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<continue xmlns='urn:xmpp:sasl:2'><additional-data>QWRkaXRpb25hbA==</additional-data><tasks><task>TOTP</task><task>UNREALISTIC-2FA</task></tasks><text>This account requires 2FA</text></continue>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Continue parsed = Sasl2Provider.ContinueProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("QWRkaXRpb25hbA==", parsed.getAdditionalData());
        assertEquals(2, parsed.getTasks().size());
        assertEquals("TOTP", parsed.getTasks().get(0));
        assertEquals("UNREALISTIC-2FA", parsed.getTasks().get(1));
        assertEquals("This account requires 2FA", parsed.getText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNextSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        StandardExtensionElement params = StandardExtensionElement.builder("parameters", "urn:example:totp")
            .setText("123456")
            .build();
        Sasl2Nonza.Next next = new Sasl2Nonza.Next("TOTP", Collections.singletonList(params));

        String xml = next.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<next xmlns='urn:xmpp:sasl:2' task='TOTP'><parameters xmlns='urn:example:totp'>123456</parameters></next>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Next parsed = Sasl2Provider.NextProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("TOTP", parsed.getTask());
        assertEquals(1, parsed.getExtensionElements().size());
        assertEquals("parameters", parsed.getExtensionElements().get(0).getElementName());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testTaskDataSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        StandardExtensionElement question = StandardExtensionElement.builder("question", "urn:example:2fa")
            .setText("prompt-data")
            .build();
        Sasl2Nonza.TaskData taskData = new Sasl2Nonza.TaskData(Collections.singletonList(question));

        String xml = taskData.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<task-data xmlns='urn:xmpp:sasl:2'><question xmlns='urn:example:2fa'>prompt-data</question></task-data>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.TaskData parsed = Sasl2Provider.TaskDataProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals(1, parsed.getExtensionElements().size());
        assertEquals("question", parsed.getExtensionElements().get(0).getElementName());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testAbortSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.Abort abort = new Sasl2Nonza.Abort("User canceled authentication");

        String xml = abort.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<abort xmlns='urn:xmpp:sasl:2'><text>User canceled authentication</text></abort>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Abort parsed = Sasl2Provider.AbortProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertEquals("User canceled authentication", parsed.getText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testEmptyAbortSerializationAndParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        Sasl2Nonza.Abort abort = new Sasl2Nonza.Abort();

        String xml = abort.toXML(XmlEnvironment.EMPTY).toString();
        assertXmlSimilar("<abort xmlns='urn:xmpp:sasl:2'/>", xml);

        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        Sasl2Nonza.Abort parsed = Sasl2Provider.AbortProvider.INSTANCE.parse(
            parser, parser.getDepth(), XmlEnvironment.EMPTY, null
        );
        assertNull(parsed.getText());
    }
}
