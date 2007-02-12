/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import org.xmlpull.v1.XmlPullParser;
import org.jivesoftware.smackx.packet.PrivateData;

/**
 * An interface for parsing custom private data. Each PrivateDataProvider must
 * be registered with the PrivateDataManager class for it to be used. Every implementation
 * of this interface <b>must</b> have a public, no-argument constructor.
 *
 * @author Matt Tucker
 */
public interface PrivateDataProvider {

    /**
     * Parse the private data sub-document and create a PrivateData instance. At the
     * beginning of the method call, the xml parser will be positioned at the opening
     * tag of the private data child element. At the end of the method call, the parser
     * <b>must</b> be positioned on the closing tag of the child element.
     *
     * @param parser an XML parser.
     * @return a new PrivateData instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public PrivateData parsePrivateData(XmlPullParser parser) throws Exception;
}
