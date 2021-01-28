/**
 *
 * Copyright 2015-2021 Florian Schmaus
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
package org.jivesoftware.smack.iqrequest;

import org.jivesoftware.smack.packet.IQ;

/**
 * Convenience class to create IQ requests handlers.
 */
public abstract class AbstractIqRequestHandler implements IQRequestHandler {

    private final String element;
    private final String namespace;
    private final IQ.Type type;
    private final Mode mode;

    protected AbstractIqRequestHandler(String element, String namespace, IQ.Type type, Mode mode) {
        switch (type) {
        case set:
        case get:
            break;
        default:
            throw new IllegalArgumentException("Only get and set IQ type allowed");
        }
        this.element = element;
        this.namespace = namespace;
        this.type = type;
        this.mode = mode;
    }

    @Override
    public abstract IQ handleIQRequest(IQ iqRequest);

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public IQ.Type getType() {
        return type;
    }

    @Override
    public String getElement() {
        return element;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

}
