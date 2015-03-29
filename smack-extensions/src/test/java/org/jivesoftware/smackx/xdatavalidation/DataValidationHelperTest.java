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
package org.jivesoftware.smackx.xdatavalidation;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.ListRange;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.OpenValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RegexValidateElement;
import org.junit.Test;

/**
 * Data validation helper test.
 * @author Anno van Vliet
 *
 */
public class DataValidationHelperTest {


    @Test
    public void testCheckConsistencyFormFieldBasicValidateElement() {
        FormField field = new FormField("var");
        field.setType(FormField.Type.jid_single);
        BasicValidateElement element = new BasicValidateElement(null);
        try {
            element.checkConsistency(field);
            fail("No correct check on consistency");
        }
        catch (ValidationConsistencyException e) {
            assertEquals("Field type 'jid-single' is not consistent with validation method 'basic'.", e.getMessage());
        }

        try {
            new ListRange(-1L, 1L);
            fail("No correct check on consistency");
        }
        catch (IllegalArgumentException e) {
            assertEquals("unsigned 32-bit integers can't be negative", e.getMessage());
        }

        element.setListRange(new ListRange(10L, 100L));
        try {
            element.checkConsistency(field);
            fail("No correct check on consistency");
        }
        catch (ValidationConsistencyException e) {
            assertEquals("Field type is not of type 'list-multi' while a 'list-range' is defined.", e.getMessage());
        }

        field.setType(FormField.Type.list_multi);
        try {
            element.checkConsistency(field);
        }
        catch (ValidationConsistencyException e) {
            fail("No correct check on consistency");
        }
    }


    @Test
    public void testCheckConsistencyFormFieldOpenValidateElement() {
        FormField field = new FormField("var");
        field.setType(FormField.Type.hidden);
        OpenValidateElement element = new OpenValidateElement(null);
        try {
            element.checkConsistency(field);
            fail("No correct check on consistency");
        }
        catch (ValidationConsistencyException e) {
            assertEquals("Field type 'hidden' is not consistent with validation method 'open'.", e.getMessage());
        }
    }

    @Test
    public void testCheckConsistencyFormFieldRangeValidateElement() {
        FormField field = new FormField("var");
        field.setType(FormField.Type.text_multi);
        RangeValidateElement element = new RangeValidateElement("xs:integer",null, "99");
        try {
            element.checkConsistency(field);
            fail("No correct check on consistency");
        }
        catch (ValidationConsistencyException e) {
            assertEquals("Field type 'text-multi' is not consistent with validation method 'range'.", e.getMessage());
        }
    }

    @Test
    public void testCheckConsistencyFormFieldRegexValidateElement() {
        FormField field = new FormField("var");
        field.setType(FormField.Type.list_multi);
        RegexValidateElement element = new RegexValidateElement(null, ".*");
        try {
            element.checkConsistency(field);
            fail("No correct check on consistency");
        }
        catch (ValidationConsistencyException e) {
            assertEquals("Field type 'list-multi' is not consistent with validation method 'regex'.", e.getMessage());
        }
    }
}
