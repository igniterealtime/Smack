/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.sasl2;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.sasl.SASLMechanism;

/**
 * Result returned by a {@link Sasl2AuthenticationHook} when it handles a SASL2 authentication failure
 * and requests a graceful fallback / retry with modified extensions and mechanism filters.
 */
public final class Sasl2Fallback {

    private final Collection<? extends XmlElement> fallbackExtensions;
    private final Function<SASLMechanism, String> mechanismFilter;

    public Sasl2Fallback(Collection<? extends XmlElement> fallbackExtensions,
                         Function<SASLMechanism, String> mechanismFilter) {
        this.fallbackExtensions = fallbackExtensions != null ? fallbackExtensions : Collections.emptyList();
        this.mechanismFilter = mechanismFilter;
    }

    public Collection<? extends XmlElement> getFallbackExtensions() {
        return fallbackExtensions;
    }

    public Function<SASLMechanism, String> getMechanismFilter() {
        return mechanismFilter;
    }
}
