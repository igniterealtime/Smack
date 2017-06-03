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
                new JingleReason(JingleReason.Reason.success).toXML().toString());
        assertEquals("<reason><busy/></reason>",
                new JingleReason(JingleReason.Reason.busy).toXML().toString());
        assertEquals("<reason><cancel/></reason>",
                new JingleReason(JingleReason.Reason.cancel).toXML().toString());
        assertEquals("<reason><connectivity-error/></reason>",
                new JingleReason(JingleReason.Reason.connectivity_error).toXML().toString());
        assertEquals("<reason><decline/></reason>",
                new JingleReason(JingleReason.Reason.decline).toXML().toString());
        assertEquals("<reason><expired/></reason>",
                new JingleReason(JingleReason.Reason.expired).toXML().toString());
        assertEquals("<reason><unsupported-transports/></reason>",
                new JingleReason(JingleReason.Reason.unsupported_transports).toXML().toString());
        assertEquals("<reason><failed-transport/></reason>",
                new JingleReason(JingleReason.Reason.failed_transport).toXML().toString());
        assertEquals("<reason><general-error/></reason>",
                new JingleReason(JingleReason.Reason.general_error).toXML().toString());
        assertEquals("<reason><gone/></reason>",
                new JingleReason(JingleReason.Reason.gone).toXML().toString());
        assertEquals("<reason><media-error/></reason>",
                new JingleReason(JingleReason.Reason.media_error).toXML().toString());
        assertEquals("<reason><security-error/></reason>",
                new JingleReason(JingleReason.Reason.security_error).toXML().toString());
        assertEquals("<reason><unsupported-applications/></reason>",
                new JingleReason(JingleReason.Reason.unsupported_applications).toXML().toString());
        assertEquals("<reason><timeout/></reason>",
                new JingleReason(JingleReason.Reason.timeout).toXML().toString());
        assertEquals("<reason><failed-application/></reason>",
                new JingleReason(JingleReason.Reason.failed_application).toXML().toString());
        assertEquals("<reason><incompatible-parameters/></reason>",
                new JingleReason(JingleReason.Reason.incompatible_parameters).toXML().toString());
        assertEquals(JingleReason.Reason.alternative_session, JingleReason.Reason.fromString("alternative-session"));
        try {
            JingleReason.Reason nonExistent = JingleReason.Reason.fromString("illegal-reason");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
