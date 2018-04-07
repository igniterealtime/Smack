/**
 *
 * Copyright © 2017 Ingo Bauersachs
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
package org.jivesoftware.smack.packet;

import static org.jivesoftware.smack.packet.StanzaError.Condition;
import static org.jivesoftware.smack.packet.StanzaError.Type;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class XMPPErrorTest {
    @Test
    public void testConditionHasDefaultTypeMapping() throws NoSuchFieldException, IllegalAccessException {
        Map<Condition, Type> conditionToTypeMap = StanzaError.CONDITION_TO_TYPE;
        assertEquals("CONDITION_TO_TYPE map is likely out of sync with Condition enum",
                Condition.values().length,
                conditionToTypeMap.size());
    }
}
