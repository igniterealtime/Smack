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

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.OpenValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RegexValidateElement;

/**
 * Helper class to check consistency of the {@link ValidateElement} and to validate the values of a {@link FormField}, using the ValidateElement.
 * 
 * Consistency and validation according to XEP-0122: Data Forms Validation.
 *
 * @author Anno van Vliet
 *
 */
public class DataValidationHelper {

    /*
     * The <list-range/> element SHOULD be included only when the <field/> is of type "list-multi" and SHOULD be ignored otherwise.
     * 
     * @param formField
     * @param validateElement
     */
    private static void checkListRangeConsistency(FormField formField, ValidateElement validateElement) {

        if (validateElement == null || validateElement.getListRange() == null) {
            return;
        }

        if (validateElement.getListRange().getMax() != null && validateElement.getListRange().getMax() <= 0L) {
            throw new ValidationConsistencyException("list-range: max atribute should be positive integer.");
        }
        if (validateElement.getListRange().getMin() != null && validateElement.getListRange().getMin() <= 0L) {
            throw new ValidationConsistencyException("list-range: min atribute should be positive integer.");
        }

        if (formField != null) {
            if (validateElement.getListRange().getMax() != null || validateElement.getListRange().getMin() != null) {
                if (formField.getType() == null || !formField.getType().equals(FormField.TYPE_LIST_MULTI)) {
                    throw new ValidationConsistencyException(
                                    "Field type is not of type 'list-multi' while a 'list-range' is defined.");
                }
            }
        }

    }
    
    /**
     * Checks if {@link ValidateElement} is consistent according to the business rules in XEP=0122.
     * 
     * @param formField
     * @param basicValidateElement
     * 
     * @throws ValidationConsistencyException when not consistent
     */
    public static void checkConsistency(FormField formField, BasicValidateElement basicValidateElement)
                    throws ValidationConsistencyException {

        checkListRangeConsistency(formField, basicValidateElement);

        if (formField.getType() != null) {
            switch (formField.getType()) {
            case FormField.TYPE_HIDDEN:
            case FormField.TYPE_JID_MULTI:
            case FormField.TYPE_JID_SINGLE:
                throw new ValidationConsistencyException(String.format(
                                "Field type '%1$s' is not consistent with validation method '%2$s'.",
                                formField.getType(), BasicValidateElement.METHOD));

            default:
                break;
            }
        }

    }

    /**
     * Checks if {@link ValidateElement} is consistent according to the business rules in XEP=0122.
     * 
     * @param formField
     * @param openValidateElement
     * @throws ValidationConsistencyException when not consistent
     */
    public static void checkConsistency(FormField formField, OpenValidateElement openValidateElement) throws ValidationConsistencyException {

        checkListRangeConsistency(formField, openValidateElement);
        
        if ( formField.getType() != null ) {
            switch (formField.getType()) {
            case FormField.TYPE_HIDDEN:
                throw new ValidationConsistencyException(String.format("Field type '%1$s' is not consistent with validation method '%2$s'.", formField.getType() , OpenValidateElement.METHOD ));

            default:
                break;
            }
        }
        
    }

    /**
     * Checks if {@link ValidateElement} is consistent according to the business rules in XEP=0122.
     * 
     * @param formField
     * @param rangeValidateElement
     * @throws ValidationConsistencyException when not consistent
     */
    public static void checkConsistency(FormField formField, RangeValidateElement rangeValidateElement) throws ValidationConsistencyException {

        checkNonMultiConsistency(formField, rangeValidateElement, RangeValidateElement.METHOD);
        
        if ( rangeValidateElement.getDatatype().equals(ValidateElement.DATATYPE_XS_STRING) ) {
            throw new ValidationConsistencyException(String.format("Field data type '%1$s' is not consistent with validation method '%2$s'.", rangeValidateElement.getDatatype() , RangeValidateElement.METHOD ));
        }
        
    }

    /**
     * Checks if {@link ValidateElement} is consistent according to the business rules in XEP=0122.
     * 
     * @param formField
     * @param regexValidateElement
     * @throws ValidationConsistencyException when not consistent
     */
    public static void checkConsistency(FormField formField, RegexValidateElement regexValidateElement) throws ValidationConsistencyException {

        checkNonMultiConsistency(formField, regexValidateElement, RegexValidateElement.METHOD);
        
    }

    /**
     * @param formField
     * @param regexValidateElement
     * @param method
     */
    private static void checkNonMultiConsistency(FormField formField, ValidateElement validateElement,
                    String method) {
        checkListRangeConsistency(formField, validateElement);

        if ( formField.getType() != null ) {
            switch (formField.getType()) {
            case FormField.TYPE_HIDDEN:
            case FormField.TYPE_JID_MULTI:
            case FormField.TYPE_LIST_MULTI:
            case FormField.TYPE_TEXT_MULTI:
                throw new ValidationConsistencyException(String.format("Field type '%1$s' is not consistent with validation method '%2$s'.", formField.getType() , method ));

            default:
                break;
            }
        }
    }

}
