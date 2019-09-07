/**
 *
 * Copyright 2017-2019 Paul Schaub, 2019 Florian Schmaus
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
package org.jivesoftware.smackx.jingle;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleReason;

import org.junit.jupiter.api.Test;

/**
 * Test JingleReason functionality.
 */
public class JingleReasonTest extends SmackTestSuite {

    @Test
    public void parserTest() {
        assertReasonXml("<reason><success/></reason>",
                JingleReason.Success);
        assertReasonXml("<reason><busy/></reason>",
                JingleReason.Busy);
        assertReasonXml("<reason><cancel/></reason>",
                JingleReason.Cancel);
        assertReasonXml("<reason><connectivity-error/></reason>",
                JingleReason.ConnectivityError);
        assertReasonXml("<reason><decline/></reason>",
                JingleReason.Decline);
        assertReasonXml("<reason><expired/></reason>",
                JingleReason.Expired);
        assertReasonXml("<reason><unsupported-transports/></reason>",
                JingleReason.UnsupportedTransports);
        assertReasonXml("<reason><failed-transport/></reason>",
                JingleReason.FailedTransport);
        assertReasonXml("<reason><general-error/></reason>",
                JingleReason.GeneralError);
        assertReasonXml("<reason><gone/></reason>",
                JingleReason.Gone);
        assertReasonXml("<reason><media-error/></reason>",
                JingleReason.MediaError);
        assertReasonXml("<reason><security-error/></reason>",
                JingleReason.SecurityError);
        assertReasonXml("<reason><unsupported-applications/></reason>",
                JingleReason.UnsupportedApplications);
        assertReasonXml("<reason><timeout/></reason>",
                JingleReason.Timeout);
        assertReasonXml("<reason><failed-application/></reason>",
                JingleReason.FailedApplication);
        assertReasonXml("<reason><incompatible-parameters/></reason>",
                JingleReason.IncompatibleParameters);
        assertReasonXml("<reason><alternative-session><sid>1234</sid></alternative-session></reason>",
                JingleReason.AlternativeSession("1234"));
    }

    private static void assertReasonXml(String expected, JingleReason reason) {
        String actualXml = reason.toXML(JingleReason.NAMESPACE).toString();
        assertEquals(expected, actualXml);
    }

    @Test
    public void alternativeSessionEmptyStringTest() {
        assertThrows(NullPointerException.class, () ->
            // Alternative sessionID must not be empty
            JingleReason.AlternativeSession("")
        );
    }

    @Test
    public void alternativeSessionNullStringTest() {
        assertThrows(NullPointerException.class, () ->
            // Alternative sessionID must not be null
            JingleReason.AlternativeSession(null)
        );
    }

    @Test
    public void illegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () ->
            JingleReason.Reason.fromString("illegal-reason")
        );
    }
}
