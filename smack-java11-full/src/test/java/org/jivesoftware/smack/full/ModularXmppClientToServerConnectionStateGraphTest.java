/**
 *
 * Copyright 2020-2021 Florian Schmaus
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
package org.jivesoftware.smack.full;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.compression.CompressionModuleDescriptor;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;

import com.google.common.io.Resources;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.DOTImporter;
import org.jgrapht.io.ImportException;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class ModularXmppClientToServerConnectionStateGraphTest {

    @Test
    public void testStateGraphDotOutput() throws IOException, ImportException {
        URL stateGraphDotFileUrl = Resources.getResource("state-graph.dot");
        String expectedStateGraphDot = Resources.toString(stateGraphDotFileUrl, StandardCharsets.UTF_8);

        StringWriter sw = new StringWriter();
        PrintWriter pw  = new PrintWriter(sw);
        ModularXmppClientToServerConnectionTool.printStateGraph(pw, false);
        String currentStateGraphDot = sw.toString();

        @SuppressWarnings("serial")
        DOTImporter<String, DefaultEdge> dotImporter = new DOTImporter<>(
                (id, attributes) -> id,
                (from, to, label, attributes) -> {
                        return new DefaultEdge() {
                            @Override
                            public int hashCode() {
                                return HashCode.builder()
                                        .append(getSource())
                                        .append(getTarget())
                                        .build();
                            }

                            @Override
                            public boolean equals(Object other) {
                                return EqualsUtil.equals(this, other, (b, o) ->
                                    b.append(getSource(), o.getSource())
                                     .append(getTarget(), o.getTarget())
                                );
                            }
                        };
                    }
                );

        DirectedPseudograph<String, DefaultEdge> currentStateGraph = new DirectedPseudograph<>(DefaultEdge.class);
        DirectedPseudograph<String, DefaultEdge> expectedStateGraph = new DirectedPseudograph<>(DefaultEdge.class);

        dotImporter.importGraph(expectedStateGraph, new StringReader(expectedStateGraphDot));
        dotImporter.importGraph(currentStateGraph, new StringReader(currentStateGraphDot));

        assertEquals(expectedStateGraph, currentStateGraph);
    }

    @Test
    public void testNoUnknownStates() throws XmppStringprepException {
        ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("user", "password")
            .setXmppDomain("example.org")
            .failOnUnknownStates() // This is the actual option that enableds this test.
            .build();
    }

    @Test
    public void throwsOnUnknownStates() throws XmppStringprepException {
        assertThrows(IllegalStateException.class, () ->
            ModularXmppClientToServerConnectionConfiguration.builder()
                .setUsernameAndPassword("user", "password")
                .setXmppDomain("example.org")
                .removeModule(CompressionModuleDescriptor.class)
                .failOnUnknownStates() // This is the actual option that enableds this test.
                .build()
        );
    }
}
