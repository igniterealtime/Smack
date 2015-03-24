/**
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UnknownFormatConversionException;

import org.jxmpp.util.XmppDateTime;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * A decorator for a {@link Form} to easily enable reading and updating
 * of subscription options.  All operations read or update the underlying {@link DataForm}.
 * 
 * <p>Unlike the {@link Form}.setAnswer(XXX)} methods, which throw an exception if the field does not
 * exist, all <b>SubscribeForm.setXXX</b> methods will create the field in the wrapped form
 * if it does not already exist.
 * 
 * @author Robin Collier
 */
public class SubscribeForm extends Form
{
	public SubscribeForm(DataForm configDataForm)
	{
		super(configDataForm);
	}

	public SubscribeForm(Form subscribeOptionsForm)
	{
		super(subscribeOptionsForm.getDataFormToSend());
	}

	public SubscribeForm(DataForm.Type formType)
	{
		super(formType);
	}

	/**
	 * Determines if an entity wants to receive notifications.
	 * 
	 * @return true if want to receive, false otherwise
	 */
	public boolean isDeliverOn()
	{
		return parseBoolean(getFieldValue(SubscribeOptionFields.deliver));
	}

	/**
	 * Sets whether an entity wants to receive notifications.
	 *
	 * @param deliverNotifications
	 */
	public void setDeliverOn(boolean deliverNotifications)
	{
		addField(SubscribeOptionFields.deliver, FormField.Type.bool);
		setAnswer(SubscribeOptionFields.deliver.getFieldName(), deliverNotifications);
	}

	/**
	 * Determines if notifications should be delivered as aggregations or not.
	 * 
	 * @return true to aggregate, false otherwise
	 */
	public boolean isDigestOn()
	{
		return parseBoolean(getFieldValue(SubscribeOptionFields.digest));
	}

	/**
	 * Sets whether notifications should be delivered as aggregations or not.
	 * 
	 * @param digestOn true to aggregate, false otherwise 
	 */
	public void setDigestOn(boolean digestOn)
	{
		addField(SubscribeOptionFields.deliver, FormField.Type.bool);
		setAnswer(SubscribeOptionFields.deliver.getFieldName(), digestOn);
	}

	/**
	 * Gets the minimum number of milliseconds between sending notification digests
	 * 
	 * @return The frequency in milliseconds
	 */
	public int getDigestFrequency()
	{
		return Integer.parseInt(getFieldValue(SubscribeOptionFields.digest_frequency));
	}

	/**
	 * Sets the minimum number of milliseconds between sending notification digests
	 * 
	 * @param frequency The frequency in milliseconds
	 */
	public void setDigestFrequency(int frequency)
	{
		addField(SubscribeOptionFields.digest_frequency, FormField.Type.text_single);
		setAnswer(SubscribeOptionFields.digest_frequency.getFieldName(), frequency);
	}

	/**
	 * Get the time at which the leased subscription will expire, or has expired.
	 * 
	 * @return The expiry date
	 */
	public Date getExpiry()
	{
		String dateTime = getFieldValue(SubscribeOptionFields.expire);
		try
		{
			return XmppDateTime.parseDate(dateTime);
		}
		catch (ParseException e)
		{
			UnknownFormatConversionException exc = new UnknownFormatConversionException(dateTime);
			exc.initCause(e);
			throw exc;
		}
	}

	/**
	 * Sets the time at which the leased subscription will expire, or has expired.
	 * 
	 * @param expire The expiry date
	 */
	public void setExpiry(Date expire)
	{
		addField(SubscribeOptionFields.expire, FormField.Type.text_single);
		setAnswer(SubscribeOptionFields.expire.getFieldName(), XmppDateTime.formatXEP0082Date(expire));
	}

	/**
	 * Determines whether the entity wants to receive an XMPP message body in 
	 * addition to the payload format.
	 * 
	 * @return true to receive the message body, false otherwise
	 */
	public boolean isIncludeBody()
	{
		return parseBoolean(getFieldValue(SubscribeOptionFields.include_body));
	}

	/**
	 * Sets whether the entity wants to receive an XMPP message body in 
	 * addition to the payload format.
	 * 
	 * @param include true to receive the message body, false otherwise
	 */
	public void setIncludeBody(boolean include)
	{
		addField(SubscribeOptionFields.include_body, FormField.Type.bool);
		setAnswer(SubscribeOptionFields.include_body.getFieldName(), include);
	}

	/**
	 * Gets the {@link PresenceState} for which an entity wants to receive 
	 * notifications.
	 * 
	 * @return the list of states
	 */
	public List<PresenceState> getShowValues()
	{
		ArrayList<PresenceState> result = new ArrayList<PresenceState>(5);

		for (String state : getFieldValues(SubscribeOptionFields.show_values))
		{
			result.add(PresenceState.valueOf(state));
		}
		return result;
	}

	/**
	 * Sets the list of {@link PresenceState} for which an entity wants
	 * to receive notifications.
	 * 
	 * @param stateValues The list of states
	 */
	public void setShowValues(Collection<PresenceState> stateValues)
	{
		ArrayList<String> values = new ArrayList<String>(stateValues.size());

		for (PresenceState state : stateValues)
		{
			values.add(state.toString());
		}
		addField(SubscribeOptionFields.show_values, FormField.Type.list_multi);
		setAnswer(SubscribeOptionFields.show_values.getFieldName(), values);
	}


	static private boolean parseBoolean(String fieldValue)
	{
		return ("1".equals(fieldValue) || "true".equals(fieldValue));
	}

	private String getFieldValue(SubscribeOptionFields field)
	{
		FormField formField = getField(field.getFieldName());

		return formField.getValues().get(0);
	}

	private List<String> getFieldValues(SubscribeOptionFields field)
	{
		FormField formField = getField(field.getFieldName());

		return formField.getValues();
	}

	private void addField(SubscribeOptionFields nodeField, FormField.Type type)
	{
		String fieldName = nodeField.getFieldName();

		if (getField(fieldName) == null)
		{
			FormField field = new FormField(fieldName);
			field.setType(type);
			addField(field);
		}
	}
}
