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
