/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.junit.Test;

public class SmackExceptionTest {

    @Test
    public void testConnectionException() {
        List<HostAddress> failedAddresses = new LinkedList<HostAddress>();

        HostAddress hostAddress = new HostAddress("foo.bar.example", 1234);
        hostAddress.setException(new Exception("Failed for some reason"));
        failedAddresses.add(hostAddress);

        hostAddress = new HostAddress("barz.example", 5678);
        hostAddress.setException(new Exception("Failed for some other reason"));
        failedAddresses.add(hostAddress);

        ConnectionException connectionException = ConnectionException.from(failedAddresses);
        String message = connectionException.getMessage();
        assertEquals("The following addresses failed: 'foo.bar.example:1234' failed because java.lang.Exception: Failed for some reason, 'barz.example:5678' failed because java.lang.Exception: Failed for some other reason",
                        message);
    }

}
