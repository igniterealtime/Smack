/**
 *
 * Copyright 2015-2022 Florian Schmaus
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

/**
 * Smacks API for Data Forms (XEP-0004).
 * <p>
 * XMPP data forms allow the exchange of structured data between users and applications for common tasks such as
 * registration, searching user forms, or answering a questionnaire.
 * </p>
 * <h2>Create a Form to fill out</h2>
 * <h3>Description</h3>
 * <p>
 * An XMPP entity may need to gather data from another XMPP entity. Therefore, the data-gathering entity will need to
 * create a new Form, specify the fields that will conform to the Form and finally send the Form to the data-providing
 * entity.
 * </p>
 * <h3>Usage</h3> TODO
 * <h3>Example</h3>
 *
 * <h2>Answer a Form</h2>
 * <h3>Description</h3>
 * <p>
 * Under many situations an XMPP entity could receive a form to fill out. For example, some hosts may require to fill
 * out a form in order to register new users. Smack lets the data-providing entity to complete the form in an easy way
 * and send it back to the data-gathering entity.
 * </p>
 * <h3>Usage</h3> TODO
 * <h3>Example</h3>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0004.html">XEP-0004: Data Forms</a>
 */
package org.jivesoftware.smackx.xdata;
