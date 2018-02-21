/**
 *
 * Copyright © 2014-2018 Florian Schmaus
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

package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;

/**
 * Smack provider are the parsers used to deserialize raw XMPP into the according Java {@link Element}s.
 * <p>
 * At any time when {@link #parse(XmlPullParser, int)} is invoked any type of exception can be thrown. If the parsed
 * element does not follow the specification, for example by putting a string where only integers are allowed, then a
 * {@link org.jivesoftware.smack.SmackException} should be thrown.
 * </p>
 * 
 * @author Florian Schmaus
 * @param <E> the type of the resulting element.
 */
public abstract class Provider<E extends Element> {

    public final E parse(XmlPullParser parser) throws Exception {
        // XPP3 calling convention assert: Parser should be at start tag
        ParserUtils.assertAtStartTag(parser);

        final int initialDepth = parser.getDepth();
        E e = parse(parser, initialDepth);

        // XPP3 calling convention assert: Parser should be at end tag of the consumed/parsed element
        ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
        return e;
    }

    public abstract E parse(XmlPullParser parser, int initialDepth) throws Exception;
}
