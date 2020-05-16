/**
 *
 * Copyright 2020 Florian Schmaus.
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

import java.io.IOException;

public class ExtendedAppendable implements Appendable {

    private final Appendable appendable;

    public ExtendedAppendable(Appendable appendable) {
        this.appendable = appendable;
    }

    @Override
    public ExtendedAppendable append(CharSequence csq) throws IOException {
        appendable.append(csq);
        return this;
    }

    @Override
    public ExtendedAppendable append(CharSequence csq, int start, int end) throws IOException {
        appendable.append(csq, start, end);
        return this;
    }

    @Override
    public ExtendedAppendable append(char c) throws IOException {
        appendable.append(c);
        return this;
    }

    public ExtendedAppendable append(boolean b) throws IOException {
        appendable.append(String.valueOf(b));
        return this;
    }

    public ExtendedAppendable append(int i) throws IOException {
        appendable.append(String.valueOf(i));
        return this;
    }
}
