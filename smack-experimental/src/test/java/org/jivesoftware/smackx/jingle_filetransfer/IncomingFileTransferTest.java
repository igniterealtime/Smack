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
package org.jivesoftware.smackx.jingle_filetransfer;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;

import org.junit.Test;

public class IncomingFileTransferTest extends SmackTestSuite {

    @Test
    public void incomingFileOfferTest() {
        Date date = new Date();
        JingleFileTransferChildElement offerElement = new JingleFileTransferChildElement(date, "description", null, "application/octet-stream", "name", 1234, null);
        JingleIncomingFileOffer offer = new JingleIncomingFileOffer(offerElement);
        assertTrue(offer.isOffer());
        assertFalse(offer.isRequest());
    }
}
