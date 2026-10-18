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
package org.jivesoftware.smack.sasl.sasl2.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.sasl.sasl2.Sasl2AuthenticationHook;

public final class Sasl2AuthenticationContext {

    private final List<Sasl2AuthenticationHook> hooks = new ArrayList<>();
    private final List<XmlElement> extensions = new ArrayList<>();
    private final List<Consumer<Sasl2AuthenticationResult>> successCallbacks = new ArrayList<>();

    public Sasl2AuthenticationContext() {
    }

    public void addExtension(XmlElement extension) {
        if (extension != null) {
            extensions.add(extension);
        }
    }

    public List<XmlElement> getExtensions() {
        return new ArrayList<>(extensions);
    }

    public void addHook(Sasl2AuthenticationHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    public List<Sasl2AuthenticationHook> getHooks() {
        return new ArrayList<>(hooks);
    }

    public void addSuccessCallback(Consumer<Sasl2AuthenticationResult> callback) {
        if (callback != null) {
            successCallbacks.add(callback);
        }
    }

    public void forEachSuccessCallback(Sasl2AuthenticationResult result) {
        successCallbacks.forEach(callback -> callback.accept(result));
    }
}
