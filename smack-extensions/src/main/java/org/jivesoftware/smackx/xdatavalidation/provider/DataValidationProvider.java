/**
 *
 * Copyright 2014 Anno van Vliet
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
package org.jivesoftware.smackx.xdatavalidation.provider;

import java.io.IOException;

import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.ListRange;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.OpenValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RegexValidateElement;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Extension Provider for Data validation of forms.
 *
 * @author Anno van Vliet
 *
 */
public class DataValidationProvider {

    public static ValidateElement parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        final String dataType = parser.getAttributeValue("", "datatype");
        ValidateElement dataValidation = new DefaultValidateElement(dataType); 
        ListRange listRange = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                switch (parser.getName()) {
                case OpenValidateElement.METHOD:
                    dataValidation = new OpenValidateElement(dataType);
                    break;
                case BasicValidateElement.METHOD:
                    dataValidation = new BasicValidateElement(dataType);
                    break;
                case RangeValidateElement.METHOD:
                    dataValidation = new RangeValidateElement(dataType,
                                    parser.getAttributeValue("", "min"),
                                    parser.getAttributeValue("", "max")
                                    );
                    break;
                case RegexValidateElement.METHOD:
                    dataValidation = new RegexValidateElement(dataType,parser.nextText());
                    break;
                case ListRange.ELEMENT:
                    listRange = new ListRange(
                                    ParserUtils.getLongAttribute(parser, "min"),
                                    ParserUtils.getLongAttribute(parser, "max")
                                    );
                    break;
                default:
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    dataValidation.setListRange(listRange);
                    done = true;
                }
                break;
            }
        }
        return dataValidation;
    }

    /**
     * 
     * This defines the default when no type is specified. 
     *
     */
    protected static class DefaultValidateElement extends BasicValidateElement{

        /**
         * @param dataType
         */
        public DefaultValidateElement(String dataType) {
            super(dataType);
        }

        /* (non-Javadoc)
         * @see org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement#appendXML(org.jivesoftware.smack.util.XmlStringBuilder)
         */
        @Override
        protected void appendXML(XmlStringBuilder buf) {
        }
    }


}
