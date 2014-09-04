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
package org.jivesoftware.smack.util.stringencoder;

import static org.junit.Assert.assertEquals;


// TODO those tests should be run with the Java7 and Android impl
public class Base64Test {
    /**
     * This method tests 2 StringUtil methods - encodeBase64(String) and encodeBase64(byte[]).
     */
    public void testEncodeBase64() {
        String input = "";
        String output = "";
        assertEquals(Base64.encode(input), output);

        input = "foo bar 123";
        output = "Zm9vIGJhciAxMjM=";
        assertEquals(Base64.encode(input), output);

        input = "=";
        output = "PQ==";
        assertEquals(Base64.encode(input), output);

        input = "abcdefghijklmnopqrstuvwxyz0123456789\n\t\"?!.@{}[]();',./<>#$%^&*";
        output = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5CgkiPyEuQHt9W10oKTsnLC4vPD4jJCVeJio=";
        assertEquals(Base64.encode(input), output);
    }

    /***
     * This method tests 2 StringUtil methods - decodeBase64(String) and decodeBase64(byte[]).
     */
    public void testDecodeBase64() {
        String input = "";
        String output = "";
        assertEquals(Base64.decodeToString(input), output);

        input = "Zm9vIGJhciAxMjM=";
        output = "foo bar 123";
        assertEquals(Base64.decodeToString(input), output);

        input = "PQ==";
        output = "=";
        assertEquals(Base64.decodeToString(input), output);

        input = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5CgkiPyEuQHt9W10oKTsnLC4vPD4jJCVeJio=";
        output = "abcdefghijklmnopqrstuvwxyz0123456789\n\t\"?!.@{}[]();',./<>#$%^&*";
        assertEquals(Base64.decodeToString(input), output);
    }
}
