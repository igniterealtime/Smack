/**
 *
 * Copyright 2025 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;


@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class XmlStringBuilderJmh {

    private int countOuter = 500;
    private int countInner = 50;

    private XmlStringBuilder xmlStringBuilder;

    @Setup(Level.Invocation)
    public void setup() {
        xmlStringBuilder = new XmlStringBuilder();
        for (int i = 0; i < countOuter; i++) {
            var inner = new XmlStringBuilder();
            for (int j = 0; j < countInner; j++) {
                inner.append("foo");
            }
            xmlStringBuilder.append((CharSequence) inner);
        }
    }

    @Benchmark
    public void simpleToString(Blackhole blackhole) {
        var string = xmlStringBuilder.toString();
        blackhole.consume(string);
    }
}
