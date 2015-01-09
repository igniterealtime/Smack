/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.delay.provider;

import java.text.ParseException;
import java.util.Date;

import org.jxmpp.util.XmppDateTime;

/**
 * The DelayInformationProvider parses DelayInformation packets.
 * 
 * @author Florian Schmaus
 */
public class DelayInformationProvider extends AbstractDelayInformationProvider {

    public static final DelayInformationProvider INSTANCE = new DelayInformationProvider();

    @Override
    protected Date parseDate(String string) throws ParseException {
        return XmppDateTime.parseXEP0082Date(string);
    }

}
