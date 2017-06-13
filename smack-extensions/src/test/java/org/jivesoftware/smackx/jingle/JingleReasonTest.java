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

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

/**
 * Test JingleReason functionality.
 */
public class JingleReasonTest extends SmackTestSuite {

    @Test
    public void parserTest() {
        assertEquals("<reason><success/></reason>",
                JingleReason.Success.toXML().toString());
        assertEquals("<reason><busy/></reason>",
                JingleReason.Busy.toXML().toString());
        assertEquals("<reason><cancel/></reason>",
                JingleReason.Cancel.toXML().toString());
        assertEquals("<reason><connectivity-error/></reason>",
                JingleReason.ConnectivityError.toXML().toString());
        assertEquals("<reason><decline/></reason>",
                JingleReason.Decline.toXML().toString());
        assertEquals("<reason><expired/></reason>",
                JingleReason.Expired.toXML().toString());
        assertEquals("<reason><unsupported-transports/></reason>",
                JingleReason.UnsupportedTransports.toXML().toString());
        assertEquals("<reason><failed-transport/></reason>",
                JingleReason.FailedTransport.toXML().toString());
        assertEquals("<reason><general-error/></reason>",
                JingleReason.GeneralError.toXML().toString());
        assertEquals("<reason><gone/></reason>",
                JingleReason.Gone.toXML().toString());
        assertEquals("<reason><media-error/></reason>",
                JingleReason.MediaError.toXML().toString());
        assertEquals("<reason><security-error/></reason>",
                JingleReason.SecurityError.toXML().toString());
        assertEquals("<reason><unsupported-applications/></reason>",
                JingleReason.UnsupportedApplications.toXML().toString());
        assertEquals("<reason><timeout/></reason>",
                JingleReason.Timeout.toXML().toString());
        assertEquals("<reason><failed-application/></reason>",
                JingleReason.FailedApplication.toXML().toString());
        assertEquals("<reason><incompatible-parameters/></reason>",
                JingleReason.IncompatibleParameters.toXML().toString());
        assertEquals("<reason><alternative-session><sid>1234</sid></alternative-session></reason>",
                JingleReason.AlternativeSession("1234").toXML().toString());
        // Alternative sessionID must not be empty
        try {
            JingleReason.AlternativeSession("");
            fail();
        } catch (NullPointerException e) {
            // Expected
        }
        // Alternative sessionID must not be null
        try {
            JingleReason.AlternativeSession(null);
            fail();
        } catch (NullPointerException e) {
            // Expected
        }

        try {
            JingleReason.Reason nonExistent = JingleReason.Reason.fromString("illegal-reason");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
