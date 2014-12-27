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
package org.jivesoftware.smackx.xdatavalidation.packet;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdatavalidation.DataValidationHelper;

/**
 * DataValidation Extension according to XEP-0122: Data Forms Validation.
 * This specification defines a backwards-compatible extension to the XMPP Data Forms protocol 
 * that enables applications to specify additional validation guidelines related to a {@link FormField} in a {@link DataForm}, 
 * such as validation of standard XML datatypes, application-specific datatypes, value ranges, and regular expressions.
 *
 * @author Anno van Vliet
 */
public abstract class ValidateElement implements PacketExtension {

    /**
     * 
     */
    public static final String DATATYPE_XS_STRING = "xs:string";
    public static final String ELEMENT = "validate";
    public static final String NAMESPACE = "http://jabber.org/protocol/xdata-validate";

    private final String datatype;
    
    private ListRange listRange = null;
    
    /**
     * The 'datatype' attribute specifies the datatype. This attribute is OPTIONAL, and when not specified, defaults to "xs:string".
     * 
     * @param datatype the data type of any value contained within the {@link FormField} element.
     */
    public ValidateElement(String datatype) {
        this.datatype = StringUtils.isNotEmpty(datatype) ? datatype : null;
    }
    
    /**
     * Specifies the data type of any value contained within the {@link FormField} element.
     * 
     *  It MUST meet one of the following conditions:
     *  <ul>
     *  <li>Start with "xs:", and be one of the "built-in" datatypes defined in XML Schema Part 2 <a href="http://www.xmpp.org/extensions/xep-0122.html#nt-idp1476016">[2]</a></li>
     *  <li>Start with a prefix registered with the XMPP Registrar <a href="http://www.xmpp.org/extensions/xep-0122.html#nt-idp1478544">[3]</a></li>
     *  <li>Start with "x:", and specify a user-defined datatype <a href="http://www.xmpp.org/extensions/xep-0122.html#nt-idp1477360">[4]</a></li>
     *  </ul>
     * 
     * @return the datatype
     */
    public String getDatatype() {
        return ( datatype != null ? datatype : DATATYPE_XS_STRING );
    }

    /*
     * (non-Javadoc)
     * @see org.jivesoftware.smack.packet.PacketExtension#getElementName()
     */
    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /*
     * (non-Javadoc)
     * @see org.jivesoftware.smack.packet.PacketExtension#getNamespace()
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /*
     * (non-Javadoc)
     * @see org.jivesoftware.smack.packet.PacketExtension#toXML()
     */
    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder(this);
        buf.optAttribute("datatype", datatype);
        buf.rightAngleBracket();
        appendXML(buf);
        if ( getListRange() != null ) {
            buf.append(getListRange().toXML());
        }
        
        buf.closeElement(this);

        return buf;
    }

    /**
     * @param buf
     */
    protected abstract void appendXML(XmlStringBuilder buf);

    /**
     * @param listRange the listRange to set
     */
    public void setListRange(ListRange listRange) {
        this.listRange = listRange;
    }
    
    /**
     * @return the listRange
     */
    public ListRange getListRange() {
        return listRange;
    }

    /**
     * Check if this element is consistent according to the business rules in XEP=0122
     * 
     * @param formField
     */
    public abstract void checkConsistency(FormField formField);
    
    /**
     * Validation only against the datatype itself.
     * 
     * Indicates that the value(s) should simply match the field type and datatype constraints.
     * 
     * @see ValidateElement
     *
     */
    public static class BasicValidateElement extends ValidateElement{

        public static final String METHOD = "basic";
        
        /**
         * @param dataType
         */
        public BasicValidateElement(String dataType) {
            super(dataType);
        }

        /* (non-Javadoc)
         * @see org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement#appendXML(org.jivesoftware.smack.util.XmlStringBuilder)
         */
        @Override
        protected void appendXML(XmlStringBuilder buf) {
            buf.emptyElement(METHOD);
        }
        
        public void checkConsistency(FormField formField) {
            DataValidationHelper.checkConsistency(formField,this);
            
        }

    }

    /**
     * For "list-single" or "list-multi", indicates that the user may enter a custom value 
     * (matching the datatype constraints) or choose from the predefined values.
     * 
     * @see ValidateElement
     */
    public static class OpenValidateElement extends ValidateElement{

        public static final String METHOD = "open";
        
        /**
         * @param dataType
         */
        public OpenValidateElement(String dataType) {
            super(dataType);
        }

        /* (non-Javadoc)
         * @see org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement#appendXML(org.jivesoftware.smack.util.XmlStringBuilder)
         */
        @Override
        protected void appendXML(XmlStringBuilder buf) {
            buf.emptyElement(METHOD);
        }
        
        public void checkConsistency(FormField formField) {
            DataValidationHelper.checkConsistency(formField,this);
            
        }

    }
    
    /**
     * Indicate that the value should fall within a certain range.
     * 
     * @see ValidateElement
     */
    public static class RangeValidateElement extends ValidateElement{

        public static final String METHOD = "range";
        private final String min;
        private final String max;

        /**
         * @param min the minimum allowable value. This attribute is OPTIONAL. The value depends on the datatype in use.
         * @param max the maximum allowable value. This attribute is OPTIONAL. The value depends on the datatype in use.
         */
        public RangeValidateElement(String dataType, String min, String max) {
            super(dataType);
            this.min = min;
            this.max = max;
        }

        /* (non-Javadoc)
         * @see org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement#appendXML(org.jivesoftware.smack.util.XmlStringBuilder)
         */
        @Override
        protected void appendXML(XmlStringBuilder buf) {
            buf.halfOpenElement(METHOD);
            buf.optAttribute("min", getMin());
            buf.optAttribute("max", getMax());
            buf.closeEmptyElement();
        }

        /**
         * The 'min' attribute specifies the minimum allowable value.
         * 
         * @return the minimum allowable value. This attribute is OPTIONAL. The value depends on the datatype in use.
         */
        public String getMin() {
            return min;
        }
        
        /**
         * The 'max' attribute specifies the maximum allowable value.
         * 
         * @return the maximum allowable value. This attribute is OPTIONAL. The value depends on the datatype in use.
         */
        public String getMax() {
            return max;
        }

        public void checkConsistency(FormField formField) {
            DataValidationHelper.checkConsistency(formField,this);
            
        }

    }

    /**
     * Indicates that the value should be restricted to a regular expression.
     * The regular expression must be that defined for <a href="http://www.xmpp.org/extensions/xep-0122.html#nt-idp1501344"> POSIX extended regular expressions </a> 
     * including support for <a href="http://www.xmpp.org/extensions/xep-0122.html#nt-idp1502496">Unicode</a>.
     * 
     * @see ValidateElement
     */
    public static class RegexValidateElement extends ValidateElement{

        public static final String METHOD = "regex";
        private final String regex;

        /**
         * @param dataType
         * @param regex 
         */
        public RegexValidateElement(String dataType, String regex) {
            super(dataType);
            this.regex = regex;
        }
        
        /**
         * the expression is that defined for POSIX extended regular expressions, including support for Unicode.
         * 
         * @return the regex
         */
        public String getRegex() {
            return regex;
        }

        /* (non-Javadoc)
         * @see org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement#appendXML(org.jivesoftware.smack.util.XmlStringBuilder)
         */
        @Override
        protected void appendXML(XmlStringBuilder buf) {
            buf.element("regex", getRegex());
        }
        
        public void checkConsistency(FormField formField) {
            DataValidationHelper.checkConsistency(formField,this);
            
        }

    }

    /**
     * This element indicates for "list-multi", that a minimum and maximum number of options should be selected and/or entered. 
     */
    public static class ListRange implements NamedElement{

        public static final String ELEMENT = "list-range";
        private final Long min;
        private final Long max;

        /**
         * The 'max' attribute specifies the maximum allowable number of selected/entered values. 
         * The 'min' attribute specifies the minimum allowable number of selected/entered values. 
         * Both attributes are optional and must be a positive integer.
         * 
         * @param min
         * @param max
         */
        public ListRange(Long min, Long max) {
            this.min = min;
            this.max = max;
        }

        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder(this);
            if ( getMin() != null )
                buf.optLongAttribute("min", getMin());
            if ( getMax() != null )
                buf.optLongAttribute("max", getMax());
            buf.closeEmptyElement();
            return buf;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
        
        /**
         * The minimum allowable number of selected/entered values.
         * @return a positive integer, can be null
         */
        public Long getMin() {
            return min;
        }
        
        /**
         * The maximum allowable number of selected/entered values.
         * @return a positive integer, can be null
         */
        public Long getMax() {
            return max;
        }

    }

}

