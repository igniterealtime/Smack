/**
 *
 * Copyright the original author or authors
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

/**
 * 
 * This class simplifies parsing of embedded elements by using the 
 * <a href="http://en.wikipedia.org/wiki/Template_method_pattern">Template Method Pattern</a>.  
 * After extracting the current element attributes and content of any child elements, the template method 
 * ({@link #createReturnExtension(String, String, Map, List)} is called.  Subclasses
 * then override this method to create the specific return type.
 * 
 * <p>To use this class, you simply register your subclasses as extension providers in the 
 * <b>smack.properties</b> file.  Then they will be automatically picked up and used to parse
 * any child elements.  
 * 
 * <pre>
 * For example, given the following message
 * 
 * &lt;message from='pubsub.shakespeare.lit' to='francisco@denmark.lit' id='foo&gt;
 *    &lt;event xmlns='http://jabber.org/protocol/pubsub#event&gt;
 *       &lt;items node='princely_musings'&gt;
 *          &lt;item id='asdjkwei3i34234n356'&gt;
 *             &lt;entry xmlns='http://www.w3.org/2005/Atom'&gt;
 *                &lt;title&gt;Soliloquy&lt;/title&gt;
 *                &lt;link rel='alternative' type='text/html'/&gt;
 *                &lt;id>tag:denmark.lit,2003:entry-32397&lt;/id&gt;
 *             &lt;/entry&gt;
 *          &lt;/item&gt;
 *       &lt;/items&gt;
 *    &lt;/event&gt;
 * &lt;/message&gt;
 * 
 * I would have a classes
 * <tt>ItemsProvider</tt> extends {@link EmbeddedExtensionProvider}
 * <tt>ItemProvider</tt> extends {@link EmbeddedExtensionProvider}
 * and
 * AtomProvider extends {@link ExtensionElementProvider}
 * 
 * These classes are then registered in the meta-inf/smack.providers file
 * as follows.
 * 
 *   &lt;extensionProvider&gt;
 *      &lt;elementName&gt;items&lt;/elementName&gt;
 *      &lt;namespace&gt;http://jabber.org/protocol/pubsub#event&lt;/namespace&gt;
 *      &lt;className&gt;org.jivesoftware.smackx.provider.ItemsEventProvider&lt;/className&gt;
 *   &lt;/extensionProvider&gt;
 *   &lt;extensionProvider&gt;
 *       &lt;elementName&gt;item&lt;/elementName&gt;
 *       &lt;namespace&gt;http://jabber.org/protocol/pubsub#event&lt;/namespace&gt;
 *       &lt;className&gt;org.jivesoftware.smackx.provider.ItemProvider&lt;/className&gt;
 *   &lt;/extensionProvider&gt;
 * 
 * </pre>
 * 
 * @author Robin Collier
 */
public abstract class EmbeddedExtensionProvider<PE extends ExtensionElement> extends ExtensionElementProvider<PE> {

    @Override
    public final PE parse(XmlPullParser parser, int initialDepth) throws Exception {
        final String namespace = parser.getNamespace();
        final String name = parser.getName();
        final int attributeCount = parser.getAttributeCount();
        Map<String, String> attMap = new HashMap<>(attributeCount);

        for (int i = 0; i < attributeCount; i++) {
            attMap.put(parser.getAttributeName(i), parser.getAttributeValue(i));
        }

        List<ExtensionElement> extensions = new ArrayList<>();
        int event;
        do {
            event = parser.next();

            if (event == XmlPullParser.START_TAG)
                PacketParserUtils.addExtensionElement(extensions, parser);
        }
        while (!(event == XmlPullParser.END_TAG && parser.getDepth() == initialDepth));

        return createReturnExtension(name, namespace, attMap, extensions);
    }

    protected abstract PE createReturnExtension(String currentElement, String currentNamespace,
                    Map<String, String> attributeMap, List<? extends ExtensionElement> content);
}
