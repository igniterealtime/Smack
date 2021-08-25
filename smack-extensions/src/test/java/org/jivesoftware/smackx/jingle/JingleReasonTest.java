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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

import org.junit.jupiter.api.Test;

/**
 * Test JingleReasonElement functionality.
 */
public class JingleReasonTest extends SmackTestSuite {

    @Test
    public void parserTest() {
        assertReasonXml("<reason><success/></reason>",
                JingleReasonElement.Success);
        assertReasonXml("<reason><busy/></reason>",
                JingleReasonElement.Busy);
        assertReasonXml("<reason><cancel/></reason>",
                JingleReasonElement.Cancel);
        assertReasonXml("<reason><connectivity-error/></reason>",
                JingleReasonElement.ConnectivityError);
        assertReasonXml("<reason><decline/></reason>",
                JingleReasonElement.Decline);
        assertReasonXml("<reason><expired/></reason>",
                JingleReasonElement.Expired);
        assertReasonXml("<reason><unsupported-transports/></reason>",
                JingleReasonElement.UnsupportedTransports);
        assertReasonXml("<reason><failed-transport/></reason>",
                JingleReasonElement.FailedTransport);
        assertReasonXml("<reason><general-error/></reason>",
                JingleReasonElement.GeneralError);
        assertReasonXml("<reason><gone/></reason>",
                JingleReasonElement.Gone);
        assertReasonXml("<reason><media-error/></reason>",
                JingleReasonElement.MediaError);
        assertReasonXml("<reason><security-error/></reason>",
                JingleReasonElement.SecurityError);
        assertReasonXml("<reason><unsupported-applications/></reason>",
                JingleReasonElement.UnsupportedApplications);
        assertReasonXml("<reason><timeout/></reason>",
                JingleReasonElement.Timeout);
        assertReasonXml("<reason><failed-application/></reason>",
                JingleReasonElement.FailedApplication);
        assertReasonXml("<reason><incompatible-parameters/></reason>",
                JingleReasonElement.IncompatibleParameters);
        assertReasonXml("<reason><alternative-session><sid>1234</sid></alternative-session></reason>",
                JingleReasonElement.AlternativeSession("1234"));
    }

    private static void assertReasonXml(String expected, JingleReasonElement reason) {
        String actualXml = reason.toXML(JingleReasonElement.ELEMENT).toString();
        assertEquals(expected, actualXml);
    }

    @Test
    public void alternativeSessionEmptyStringTest() {
        assertThrows(NullPointerException.class, () ->
            // Alternative sessionID must not be empty
            JingleReasonElement.AlternativeSession("")
        );
    }

    @Test
    public void alternativeSessionNullStringTest() {
        assertThrows(NullPointerException.class, () ->
            // Alternative sessionID must not be null
            JingleReasonElement.AlternativeSession(null)
        );
    }

    @Test
    public void illegalArgumentTest() {
        assertThrows(IllegalArgumentException.class, () ->
            JingleReasonElement.Reason.fromString("illegal-reason")
        );
    }
}
