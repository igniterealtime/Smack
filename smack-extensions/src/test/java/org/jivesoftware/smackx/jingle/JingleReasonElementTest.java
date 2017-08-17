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
                JingleReasonElement.Success.toXML().toString());
        assertEquals("<reason><busy/></reason>",
                JingleReasonElement.Busy.toXML().toString());
        assertEquals("<reason><cancel/></reason>",
                JingleReasonElement.Cancel.toXML().toString());
        assertEquals("<reason><connectivity-error/></reason>",
                JingleReasonElement.ConnectivityError.toXML().toString());
        assertEquals("<reason><decline/></reason>",
                JingleReasonElement.Decline.toXML().toString());
        assertEquals("<reason><expired/></reason>",
                JingleReasonElement.Expired.toXML().toString());
        assertEquals("<reason><unsupported-transports/></reason>",
                JingleReasonElement.UnsupportedTransports.toXML().toString());
        assertEquals("<reason><failed-transport/></reason>",
                JingleReasonElement.FailedTransport.toXML().toString());
        assertEquals("<reason><general-error/></reason>",
                JingleReasonElement.GeneralError.toXML().toString());
        assertEquals("<reason><gone/></reason>",
                JingleReasonElement.Gone.toXML().toString());
        assertEquals("<reason><media-error/></reason>",
                JingleReasonElement.MediaError.toXML().toString());
        assertEquals("<reason><security-error/></reason>",
                JingleReasonElement.SecurityError.toXML().toString());
        assertEquals("<reason><unsupported-applications/></reason>",
                JingleReasonElement.UnsupportedApplications.toXML().toString());
        assertEquals("<reason><timeout/></reason>",
                JingleReasonElement.Timeout.toXML().toString());
        assertEquals("<reason><failed-application/></reason>",
                JingleReasonElement.FailedApplication.toXML().toString());
        assertEquals("<reason><incompatible-parameters/></reason>",
                JingleReasonElement.IncompatibleParameters.toXML().toString());
        assertEquals("<reason><alternative-session><sid>1234</sid></alternative-session></reason>",
                JingleReasonElement.AlternativeSession("1234").toXML().toString());
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
