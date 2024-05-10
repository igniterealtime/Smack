/**
 *
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Verifies the functionality that's implemented in {@link Configuration}.
 */
public class ConfigurationTest {
    @Test
    public void testNormalizeXepUpperCaseNoSeperator() {
        // Setup test fixture.
        final String input = "XEP0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }

    @Test
    public void testNormalizeXepLowerCaseNoSeperator() {
        // Setup test fixture.
        final String input = "xep0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }

    @Test
    public void testNormalizeXepUpperCaseDash() {
        // Setup test fixture.
        final String input = "XEP-0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }

    @Test
    public void testNormalizeXepLowerCaseDash() {
        // Setup test fixture.
        final String input = "xep-0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }

    @Test
    public void testNormalizeXepUpperCaseSpace() {
        // Setup test fixture.
        final String input = "XEP 0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }

    @Test
    public void testNormalizeXepLowerCaseSpace() {
        // Setup test fixture.
        final String input = "xep 0001";

        // Execute system under test.
        final String output = Configuration.normalizeSpecification(input);

        // Verify results.
        assertEquals("XEP0001", output);
    }
}
