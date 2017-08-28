/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.element;

import org.jivesoftware.smack.packet.NamedElement;

/**
 * Abstract JingleContentTransportInfo element.
 * The JingleContentTransportInfo element can have certain states defined by the respective Transport XEP.
 * Examples are Jingle Socks5Bytestream's <candidate-used/> (Example 5), <candidate-error/> (Example 7) etc.
 *
 * <pre> {@code
 * <jingle>
 *     <content>
 *         <description/>
 *         <transport>
 *             <xyz/> <- This is us.
 *         </transport>
 *         <security/>
 *     </content>
 * </jingle>
 * } </pre>
 */
public abstract class JingleContentTransportInfoElement implements NamedElement {

}
