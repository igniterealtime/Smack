/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.softwareinfo.form;

import java.util.List;

import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smackx.mediaelement.element.MediaElement;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.form.FilledForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * {@link Form} that contains the software information.
 * <br>
 * Instance of {@link SoftwareInfoForm} can be created using {@link Builder#build()} method.
 * <br>
 * To obtain an instance of {@link Builder}, use {@link SoftwareInfoForm#getBuilder()} method.
 * <br>
 * An example to illustrate is provided inside SoftwareInfoFormTest inside the test package.
 */
public final class SoftwareInfoForm extends FilledForm {

    public static final String FORM_TYPE = "urn:xmpp:dataforms:softwareinfo";
    public static final String OS = "os";
    public static final String OS_VERSION = "os_version";
    public static final String SOFTWARE = "software";
    public static final String SOFTWARE_VERSION = "software_version";
    public static final String ICON = "icon";

    private SoftwareInfoForm(DataForm dataForm) {
        super(dataForm);
    }

    /**
     * Returns name of the OS used by client.
     * <br>
     * @return os
     */
    public String getOS() {
        return readFirstValue(OS);
    }

    /**
     * Returns version of the OS used by client.
     * <br>
     * @return os_version
     */
    public String getOSVersion() {
        return readFirstValue(OS_VERSION);
    }

    /**
     * Returns name of the software used by client.
     * <br>
     * @return software
     */
    public String getSoftwareName() {
        return readFirstValue(SOFTWARE);
    }

    /**
     * Returns version of the software used by client.
     * <br>
     * @return software_version
     */
    public String getSoftwareVersion () {
        return readFirstValue(SOFTWARE_VERSION);
    }

    /**
     * Returns the software icon if used by client.
     * <br>
     * @return {@link MediaElement} MediaElement or null
     */
    public MediaElement getIcon () {
        FormField field = getField(ICON);
        if (field == null) {
            return null;
        }
        FormFieldChildElement media = field.getFormFieldChildElement(MediaElement.QNAME);
        if (media == null) {
            return null;
        }
        return (MediaElement) media;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, otherObj) -> {
            equalsBuilder.append(getDataForm().getType(), otherObj.getDataForm().getType())
                         .append(getDataForm().getTitle(), otherObj.getDataForm().getTitle())
                         .append(getDataForm().getReportedData(), otherObj.getDataForm().getReportedData())
                         .append(getDataForm().getItems(), otherObj.getDataForm().getItems())
                         .append(getDataForm().getFields(), otherObj.getDataForm().getFields())
                         .append(getDataForm().getExtensionElements(), otherObj.getDataForm().getExtensionElements());
        });
    }

    @Override
    public int hashCode() {
        HashCode.Builder builder = HashCode.builder();
        builder.append(getDataForm().getFields());
        builder.append(getDataForm().getItems());
        builder.append(getDataForm().getExtensionElements());
        return builder.build();
    }

    /**
     * Returns a new instance of {@link Builder}.
     * <br>
     * @return Builder
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    /**
     * Builder class for {@link SoftwareInfoForm}.
     * <br>
     * To obtain an instance of {@link Builder}, use {@link SoftwareInfoForm#getBuilder()} method.
     * <br>
     * Use appropriate setters to include information inside SoftwareInfoForms.
     */
    public static final class Builder {
        DataForm.Builder dataFormBuilder;

        private Builder() {
            dataFormBuilder = DataForm.builder(DataForm.Type.result);
            TextSingleFormField formField = FormField.buildHiddenFormType(FORM_TYPE);
            dataFormBuilder.addField(formField);
        }

        /**
         * This will allow to include Icon using height, width and Uri's as a
         * {@link FormField}.
         * <br>
         * @param height  Height of the image
         * @param width   Width of the image
         * @param uriList List of URIs
         * @return Builder
         */
        public Builder setIcon(int height, int width, List<MediaElement.Uri> uriList) {
            MediaElement.Builder mediaBuilder = MediaElement.builder();
            for (MediaElement.Uri uri : uriList) {
                mediaBuilder.addUri(uri);
            }
            MediaElement mediaElement = mediaBuilder.setHeightAndWidth(height, width).build();
            return setIcon(mediaElement);
        }

        /**
         * This will allow to include {@link MediaElement} directly as a
         * {@link FormField}.
         * <br>
         * @param mediaElement MediaElement to be included
         * @return Builder
         */
        public Builder setIcon(MediaElement mediaElement) {
            FormField.Builder<?, ?> builder = FormField.builder(ICON);
            builder.addFormFieldChildElement(mediaElement);
            dataFormBuilder.addField(builder.build());
            return this;
        }

        /**
         * Include Operating System's name as a {@link FormField}.
         * <br>
         * @param os Name of the OS
         * @return Builder
         */
        public Builder setOS(String os) {
            TextSingleFormField.Builder builder = FormField.builder(OS);
            builder.setValue(os);
            dataFormBuilder.addField(builder.build());
            return this;
        }

        /**
         * Include Operating System's version as a {@link FormField}.
         * <br>
         * @param os_version Version of OS
         * @return Builder
         */
        public Builder setOSVersion(String os_version) {
            TextSingleFormField.Builder builder = FormField.builder(OS_VERSION);
            builder.setValue(os_version);
            dataFormBuilder.addField(builder.build());
            return this;
        }

        /**
         * Include Software name as a {@link FormField}.
         * <br>
         * @param software Name of the software
         * @return Builder
         */
        public Builder setSoftware(String software) {
            TextSingleFormField.Builder builder = FormField.builder(SOFTWARE);
            builder.setValue(software);
            dataFormBuilder.addField(builder.build());
            return this;
        }

        /**
         * Include Software Version as a {@link FormField}.
         * <br>
         * @param softwareVersion Version of the Software in use
         * @return Builder
         */
        public Builder setSoftwareVersion(String softwareVersion) {
            TextSingleFormField.Builder builder = FormField.builder(SOFTWARE_VERSION);
            builder.setValue(softwareVersion);
            dataFormBuilder.addField(builder.build());
            return this;
        }

        /**
         * Include {@link DataForm} to be encapsulated under SoftwareInfoForm.
         * <br>
         * @param dataForm The dataform containing Software Information
         * @return Builder
         */
        public Builder setDataForm(DataForm dataForm) {
            if (dataForm.getTitle() != null || !dataForm.getItems().isEmpty()
                    || dataForm.getReportedData() != null || !dataForm.getInstructions().isEmpty()) {
                throw new IllegalArgumentException("Illegal Arguements for SoftwareInformation");
            }
            String formTypeValue = dataForm.getFormType();
            if (formTypeValue == null) {
                throw new IllegalArgumentException("FORM_TYPE Formfield missing");
            }
            if (!formTypeValue.equals(SoftwareInfoForm.FORM_TYPE)) {
                throw new IllegalArgumentException("Malformed FORM_TYPE Formfield encountered");
            }
            this.dataFormBuilder = dataForm.asBuilder();
            return this;
        }

        /**
         * This method is called to build a {@link SoftwareInfoForm}.
         * <br>
         * @return Builder
         */
        public SoftwareInfoForm build() {
            return new SoftwareInfoForm(dataFormBuilder.build());
        }
    }
}
