/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
 *
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
package org.jivesoftware.smack.util;

import java.text.SimpleDateFormat;

/**
 * Defines the various date and time profiles used in XMPP along with their associated formats.
 * @author Robin Collier
 *
 */
public enum DateFormatType
{
	XEP_0082_DATE_PROFILE("yyyy-MM-dd"), 
	XEP_0082_DATETIME_PROFILE("yyyy-MM-dd'T'HH:mm:ssZ"),
	XEP_0082_DATETIME_MILLIS_PROFILE("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
	XEP_0082_TIME_PROFILE("hh:mm:ss"), 
	XEP_0082_TIME_ZONE_PROFILE("hh:mm:ssZ"), 
	XEP_0082_TIME_MILLIS_PROFILE("hh:mm:ss.SSS"), 
	XEP_0082_TIME_MILLIS_ZONE_PROFILE("hh:mm:ss.SSSZ"),
	XEP_0091_DATETIME("yyyyMMdd'T'HH:mm:ss");
	
	private String formatString;
	
	private DateFormatType(String dateFormat)
	{
		formatString = dateFormat;
	}
	
	/**
	 * Get the format string as defined in either XEP-0082 or XEP-0091.
	 * @return The defined string format for the date.
	 */
	public String getFormatString()
	{
		return formatString;
	}
	
	/**
	 * Create a {@link SimpleDateFormat} object with the format defined by {@link #getFormatString()}.
	 * @return A new date formatter.
	 */
	public SimpleDateFormat createFormatter()
	{
		return new SimpleDateFormat(getFormatString());
	}
}
