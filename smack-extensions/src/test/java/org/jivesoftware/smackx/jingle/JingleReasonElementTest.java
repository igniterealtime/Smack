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
package org.jivesoftware.smackx.jingle;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

import org.junit.Test;

/**
 * Test JingleReason functionality.
 */
public class JingleReasonElementTest extends SmackTestSuite {

    @Test
    public void parserTest() {
        assertEquals("<reason><success/></reason>",
                JingleReasonElement.Success.toXML(null).toString());
        assertEquals("<reason><busy/></reason>",
                JingleReasonElement.Busy.toXML(null).toString());
        assertEquals("<reason><cancel/></reason>",
                JingleReasonElement.Cancel.toXML(null).toString());
        assertEquals("<reason><connectivity-error/></reason>",
                JingleReasonElement.ConnectivityError.toXML(null).toString());
        assertEquals("<reason><decline/></reason>",
                JingleReasonElement.Decline.toXML(null).toString());
        assertEquals("<reason><expired/></reason>",
                JingleReasonElement.Expired.toXML(null).toString());
        assertEquals("<reason><unsupported-transports/></reason>",
                JingleReasonElement.UnsupportedTransports.toXML(null).toString());
        assertEquals("<reason><failed-transport/></reason>",
                JingleReasonElement.FailedTransport.toXML(null).toString());
        assertEquals("<reason><general-error/></reason>",
                JingleReasonElement.GeneralError.toXML(null).toString());
        assertEquals("<reason><gone/></reason>",
                JingleReasonElement.Gone.toXML(null).toString());
        assertEquals("<reason><media-error/></reason>",
                JingleReasonElement.MediaError.toXML(null).toString());
        assertEquals("<reason><security-error/></reason>",
                JingleReasonElement.SecurityError.toXML(null).toString());
        assertEquals("<reason><unsupported-applications/></reason>",
                JingleReasonElement.UnsupportedApplications.toXML(null).toString());
        assertEquals("<reason><timeout/></reason>",
                JingleReasonElement.Timeout.toXML(null).toString());
        assertEquals("<reason><failed-application/></reason>",
                JingleReasonElement.FailedApplication.toXML(null).toString());
        assertEquals("<reason><incompatible-parameters/></reason>",
                JingleReasonElement.IncompatibleParameters.toXML(null).toString());
        assertEquals("<reason><alternative-session><sid>1234</sid></alternative-session></reason>",
                JingleReasonElement.AlternativeSession("1234").toXML(null).toString());
    }

    @Test(expected = NullPointerException.class)
    public void alternativeSessionEmptyStringTest() {
        // Alternative sessionID must not be empty
        JingleReasonElement.AlternativeSession("");
    }

    @Test(expected = NullPointerException.class)
    public void alternativeSessionNullStringTest() {
        // Alternative sessionID must not be null
        JingleReasonElement.AlternativeSession(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentTest() {
        JingleReasonElement.Reason.fromString("illegal-reason");
    }
}
