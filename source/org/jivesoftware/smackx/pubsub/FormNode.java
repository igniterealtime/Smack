/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smackx.Form;

/**
 * Generic packet extension which represents any pubsub form that is
 * parsed from the incoming stream or being sent out to the server.
 * 
 * Form types are defined in {@link FormNodeType}.
 * 
 * @author Robin Collier
 */
public class FormNode extends NodeExtension
{
	private Form configForm;
	
	/**
	 * Create a {@link FormNode} which contains the specified form.
	 * 
	 * @param formType The type of form being sent
	 * @param submitForm The form
	 */
	public FormNode(FormNodeType formType, Form submitForm)
	{
		super(formType.getNodeElement());

		if (submitForm == null)
			throw new IllegalArgumentException("Submit form cannot be null");
		configForm = submitForm;
	}
	
	/**
	 * Create a {@link FormNode} which contains the specified form, which is 
	 * associated with the specified node.
	 * 
	 * @param formType The type of form being sent
	 * @param nodeId The node the form is associated with
	 * @param submitForm The form
	 */
	public FormNode(FormNodeType formType, String nodeId, Form submitForm)
	{
		super(formType.getNodeElement(), nodeId);

		if (submitForm == null)
			throw new IllegalArgumentException("Submit form cannot be null");
		configForm = submitForm;
	}
	
	/**
	 * Get the Form that is to be sent, or was retrieved from the server.
	 * 
	 * @return The form
	 */
	public Form getForm()
	{
		return configForm;
	}
	
	@Override
	public String toXML()
	{
		if (configForm == null)
		{
			return super.toXML();
		}
		else
		{
			StringBuilder builder = new StringBuilder("<");
			builder.append(getElementName());
			
			if (getNode() != null)
			{
				builder.append(" node='");
				builder.append(getNode());
				builder.append("'>");
			}
			else
				builder.append('>');
			builder.append(configForm.getDataFormToSend().toXML());
			builder.append("</");
			builder.append(getElementName() + '>');
			return builder.toString();
		}
	}

}
