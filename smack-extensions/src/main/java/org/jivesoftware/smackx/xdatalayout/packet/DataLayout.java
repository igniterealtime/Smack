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
package org.jivesoftware.smackx.xdatalayout.packet;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * DataLayout Extension according to XEP-0141: Data Forms Layout.
 * Defines a backwards-compatible extension to the XMPP Data Forms protocol that 
 * enables an application to specify form layouts, including the layout of 
 * form fields, sections within pages, and subsections within sections.
 *
 * @author Anno van Vliet
 */
public class DataLayout implements ExtensionElement {

    public static final String ELEMENT = "page";
    public static final String NAMESPACE = "http://jabber.org/protocol/xdata-layout";

    private final List<DataFormLayoutElement> pageLayout = new ArrayList<DataFormLayoutElement>();
    private final String label;

    /**
     * Data layout constructor.
     * @param label
     */
    public DataLayout(String label) {
        this.label = label;
    }

    /**
     * Gets the value of the pageLayout property.
     * <p>
     * Objects of the following type(s) are allowed in the list: {@link String },
     * {@link Section }, {@link Fieldref } and {@link Reportedref }
     */
    public List<DataFormLayoutElement> getPageLayout() {
        return this.pageLayout;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return possible object is {@link String }
     */
    public String getLabel() {
        return label;
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
        buf.optAttribute("label", getLabel());
        buf.rightAngleBracket();

        walkList(buf, getPageLayout());

        buf.closeElement(this);

        return buf;
    }

    /**
     * @param buf
     * @param pageLayout2
     */
    private static void walkList(XmlStringBuilder buf, List<DataFormLayoutElement> pageLayout) {
        for (DataFormLayoutElement object : pageLayout) {
            buf.append(object.toXML());
        }
    }

    public static class Fieldref implements DataFormLayoutElement{

        public static final String ELEMENT = "fieldref";
        private final String var;

        /**
         * Field ref constructor.
         * @param var reference to a field
         */
        public Fieldref(String var) {
            this.var = var;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder(this);
            buf.attribute("var", getVar());
            buf.closeEmptyElement();
            return buf;
        }

        /**
         * Gets the value of the var property.
         * 
         * @return possible object is {@link String }
         */
        public String getVar() {
            return var;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

    }

    public static class Section implements DataFormLayoutElement{

        public static final String ELEMENT = "section";
        private final List<DataFormLayoutElement> sectionLayout = new ArrayList<DataFormLayoutElement>();
        private final String label;

        /**
         * Section constructor.
         * @param label
         */
        public Section(String label) {
            this.label = label;
        }

        /**
         * Gets the value of the sectionLayout property.
         * <p>
         * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you
         * make to the returned list will be present inside the object. This is why there is not a <CODE>set</CODE>
         * method for the sectionLayout property.
         * <p>
         * For example, to add a new item, do as follows:
         * 
         * <pre>
         * getSectionLayout().add(newItem);
         * </pre>
         * <p>
         * Objects of the following type(s) are allowed in the list: {@link String },
         * {@link Section }, {@link Fieldref } and {@link Reportedref }
         */
        public List<DataFormLayoutElement> getSectionLayout() {
            return this.sectionLayout;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder(this);
            buf.optAttribute("label", getLabel());
            buf.rightAngleBracket();

            walkList(buf, getSectionLayout());
            buf.closeElement(ELEMENT);
            return buf;
        }

        /**
         * Gets the value of the label property.
         * 
         * @return possible object is {@link String }
         */
        public String getLabel() {
            return label;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

    }

    public static class Reportedref implements DataFormLayoutElement{

        public static final String ELEMENT = "reportedref";

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder(this);
            buf.closeEmptyElement();
            return buf;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

    }

    public static class Text implements DataFormLayoutElement{
        public static final String ELEMENT = "text";
        private final String text;

        /**
         * Text constructor.
         * @param text reference to a field
         */
        public Text(String text) {
            this.text = text;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder();
            buf.element(ELEMENT, getText());
            return buf;
        }

        /**
         * Gets the value of the var property.
         * 
         * @return possible object is {@link String }
         */
        public String getText() {
            return text;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

    }

    public static interface DataFormLayoutElement extends NamedElement {
    }

}
