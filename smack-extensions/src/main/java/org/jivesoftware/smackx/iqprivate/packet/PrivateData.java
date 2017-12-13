/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.iqprivate.packet;

/**
 * Interface to represent private data. Each private data chunk is an XML sub-document
 * with a root element name and namespace.
 *
 * @see org.jivesoftware.smackx.iqprivate.PrivateDataManager
 * @author Matt Tucker
 */
public interface PrivateData {

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    String getElementName();

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    String getNamespace();

    /**
     * Returns the XML reppresentation of the PrivateData.
     *
     * @return the private data as XML.
     */
    CharSequence toXML();
}
