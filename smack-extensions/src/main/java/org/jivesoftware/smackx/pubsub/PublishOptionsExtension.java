/*
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * Represents a Publish-Option extension Element used by omemo:2.
 *
 * @author Eng Chong Meng
 */
public class PublishOptionsExtension extends NodeExtension {
    final DataForm.Builder mBuilder;

    public PublishOptionsExtension() {
        super(PubSubElementType.PUBLISH_OPTIONS);
        mBuilder = DataForm.builder(DataForm.Type.submit);

        FormField ff = FormField.buildHiddenFormType(PubSub.NAMESPACE + "#publish-options");
        mBuilder.addField(ff);
    }

    /**
     * Sets the value of access model.
     *
     * @param accessModel use in publish-options.
     */
    public void setAccessModel(AccessModel accessModel) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.access_model.getFieldName())
                .setValue(accessModel)
                .build();
        mBuilder.addField(formField);
    }

    /**
     * Set the maximum number of items to persisted to this node.
     *
     * @param max The maximum number of items to persist.
     */
    public void setMaxItems(String max) {
        FormField formField = FormField.textSingleBuilder(ConfigureNodeFields.max_items.getFieldName())
                .setValue(max)
                .build();
        mBuilder.addField(formField);
    }

    @Override
    protected void addXml(XmlStringBuilder xml) {
        xml.rightAngleBracket();

        xml.append(mBuilder.build());
        xml.closeElement(getElementName());
    }
}
