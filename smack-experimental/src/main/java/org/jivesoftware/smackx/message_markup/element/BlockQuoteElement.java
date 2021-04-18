/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smackx.message_markup.element;

import javax.xml.namespace.QName;

public class BlockQuoteElement extends MarkupElement.BlockLevelMarkupElement {

    public static final String ELEMENT = "bquote";
    public static final QName QNAME = new QName(MarkupElement.NAMESPACE, ELEMENT);

    /**
     * Create a new Block Quote element.
     *
     * @param start start index
     * @param end end index
     */
    public BlockQuoteElement(int start, int end) {
        super(start, end);
    }

    @Override
    public String getElementName() {
        return QNAME.getLocalPart();
    }

}
