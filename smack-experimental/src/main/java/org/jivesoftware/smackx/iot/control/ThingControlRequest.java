/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.control;

import java.util.Collection;

import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.iot.control.element.SetData;

import org.jxmpp.jid.Jid;

public interface ThingControlRequest {

    void processRequest(Jid from, Collection<SetData> setData) throws XMPPErrorException;

}
