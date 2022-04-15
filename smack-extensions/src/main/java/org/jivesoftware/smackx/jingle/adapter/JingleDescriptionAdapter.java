/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.adapter;

import org.jivesoftware.smackx.jingle.component.JingleDescription;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;

/**
 * Adapter that creates a Description object from an element.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public interface JingleDescriptionAdapter<D extends JingleDescription<?>> {

    D descriptionFromElement(JingleContent.Creator creator, JingleContent.Senders senders,
                             String contentName, String contentDisposition, JingleContentDescription contentDescription);

    String getNamespace();
}
