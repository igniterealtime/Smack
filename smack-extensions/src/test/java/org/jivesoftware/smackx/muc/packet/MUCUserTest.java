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
package org.jivesoftware.smackx.muc.packet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smackx.muc.packet.MUCUser.Status;

import org.junit.Test;

public class MUCUserTest {

    private static Set<Status> createStatusSet() {
        Status status301 = Status.create(301);
        Status status110 = Status.create(110);
        Set<Status> statusSet = new HashSet<>();
        statusSet.add(status301);
        statusSet.add(status110);

        return statusSet;
    }

    @Test
    public void mucUserStatusShouldCompareWithStatus() {
        Set<Status> statusSet = createStatusSet();

        assertTrue(statusSet.contains(Status.create(110)));
        assertTrue(statusSet.contains(Status.create(301)));
        assertFalse(statusSet.contains(Status.create(332)));

        assertTrue(statusSet.contains(Status.create("110")));
    }
}
