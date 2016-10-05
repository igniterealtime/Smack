/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smack.isr;

import java.io.IOException;

import org.jivesoftware.smack.isr.element.InstantStreamResumption;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Instant Stream Resumption utils.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx: Instant
 *      Stream Resumption</a>
 * 
 */
public class ISRUtils {

    /**
     * Check if is a nonza from Instant Stream Resumption.
     * 
     * @param parser
     * @return true if is a nonza from Instant Stream Resumption
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static boolean isISRNonza(XmlPullParser parser) throws XmlPullParserException, IOException {
        String isrNamespace = parser.getNamespace(InstantStreamResumption.NAMESPACE_PREFIX);
        return (isrNamespace != null && isrNamespace.equals(InstantStreamResumption.NAMESPACE))
                || parser.getNamespace().equals(InstantStreamResumption.NAMESPACE);
    }

}
