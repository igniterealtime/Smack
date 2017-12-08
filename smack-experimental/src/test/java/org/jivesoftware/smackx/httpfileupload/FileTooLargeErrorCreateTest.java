/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload;

import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError;
import org.jivesoftware.smackx.httpfileupload.element.FileTooLargeError_V0;

import org.junit.Assert;
import org.junit.Test;

public class FileTooLargeErrorCreateTest {
    private static final String fileTooLargeErrorExtensionExample_vBase
            = "<file-too-large xmlns='urn:xmpp:http:upload'>"
            +   "<max-file-size>20000</max-file-size>"
            + "</file-too-large>";

    private static final String fileTooLargeErrorExtensionExample_v0
            = "<file-too-large xmlns='urn:xmpp:http:upload:0'>"
            +   "<max-file-size>20000</max-file-size>"
            + "</file-too-large>";

    @Test
    public void checkFileTooLargeErrorExtensionCreation_v0() {
        FileTooLargeError_V0 fileTooLargeError = new FileTooLargeError_V0(20000);

        Assert.assertEquals(20000, fileTooLargeError.getMaxFileSize());
        Assert.assertEquals(fileTooLargeErrorExtensionExample_v0, fileTooLargeError.toXML().toString());
    }

    @Test
    public void checkFileTooLargeErrorExtensionCreation_vBase() {
        FileTooLargeError fileTooLargeError = new FileTooLargeError(20000);

        Assert.assertEquals(20000, fileTooLargeError.getMaxFileSize());
        Assert.assertEquals(fileTooLargeErrorExtensionExample_vBase, fileTooLargeError.toXML().toString());
    }
}
