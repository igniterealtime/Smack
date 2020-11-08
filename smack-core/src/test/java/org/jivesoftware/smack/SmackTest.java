/**
 *
 * Copyright 2020 Florian Schmaus
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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

public class SmackTest {

    @Test
    public void getNoticeStreamTest() throws IOException {
        Set<String> expectedStrings = Sets.newHashSet(
                        "Florian Schmaus"
                      , "Paul Schaub"
        );
        int maxLineLength = 0;

        try (InputStream inputStream = Smack.getNoticeStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            while (reader.ready()) {
                String line = reader.readLine();

                int lineLength = line.length();
                maxLineLength = Math.max(maxLineLength, lineLength);

                expectedStrings.removeIf(s -> s.equals(line));
            }
        }

        assertTrue(expectedStrings.isEmpty());
        assertTrue(maxLineLength < 60);
    }
}
