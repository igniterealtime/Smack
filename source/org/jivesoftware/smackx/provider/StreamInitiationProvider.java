/*
 * Created on Jun 16, 2005
 */
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.StreamInitiation.File;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * The StreamInitiationProvider parses StreamInitiation packets.
 * 
 * @author Alexander Wenckus
 * 
 */
public class StreamInitiationProvider implements IQProvider {

	public IQ parseIQ(final XmlPullParser parser) throws Exception {
		boolean done = false;

		// si
		String id = parser.getAttributeValue("", "id");
		String mimeType = parser.getAttributeValue("", "mime-type");

		StreamInitiation initiation = new StreamInitiation();

		// file
		String name = null;
		String size = null;
		String hash = null;
		String date = null;
		String desc = null;
		boolean isRanged = false;

		// feature
		DataForm form = null;
		DataFormProvider dataFormProvider = new DataFormProvider();

		int eventType;
		String elementName;
		String namespace;
		while (!done) {
			eventType = parser.next();
			elementName = parser.getName();
			namespace = parser.getNamespace();
			if (eventType == XmlPullParser.START_TAG) {
				if (elementName.equals("file")) {
					name = parser.getAttributeValue("", "name");
					size = parser.getAttributeValue("", "size");
					hash = parser.getAttributeValue("", "hash");
					date = parser.getAttributeValue("", "date");
				} else if (elementName.equals("desc")) {
					desc = parser.nextText();
				} else if (elementName.equals("range")) {
					isRanged = true;
				} else if (elementName.equals("x")
						&& namespace.equals("jabber:x:data")) {
					form = (DataForm) dataFormProvider.parseExtension(parser);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (elementName.equals("si")) {
					done = true;
				} else if (elementName.equals("file")) {
					File file = new File(name, Long.parseLong(size));
					file.setHash(hash);
					if (date != null)
						file.setDate(DelayInformation.UTC_FORMAT.parse(date));
					file.setDesc(desc);
					file.setRanged(isRanged);
					initiation.setFile(file);
				}
			}
		}

		initiation.setSesssionID(id);
		initiation.setMimeType(mimeType);

		initiation.setFeatureNegotiationForm(form);

		return initiation;
	}

}
