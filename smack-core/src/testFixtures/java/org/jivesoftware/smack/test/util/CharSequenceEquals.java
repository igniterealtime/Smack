/**
 *
 * Copyright © 2014-2020 Florian Schmaus
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
package org.jivesoftware.smack.test.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CharSequenceEquals extends TypeSafeMatcher<CharSequence> {

    private final String charSequenceString;

    public CharSequenceEquals(CharSequence charSequence) {
        charSequenceString = charSequence.toString();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Does not match CharSequence ").appendValue(charSequenceString);
    }

    @Override
    protected boolean matchesSafely(CharSequence item) {
        String itemString = item.toString();
        return charSequenceString.equals(itemString);
    }

    public static Matcher<CharSequence> equalsCharSequence(CharSequence charSequence) {
        return new CharSequenceEquals(charSequence);
    }
}
