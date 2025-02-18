package org.jivesoftware.smackx.reply;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.reply.element.ReplyElement;
import org.jivesoftware.smackx.reply.provider.ReplyElementProvider;

import org.junit.jupiter.api.Test;

public class ReplyTest {

    @Test
    public void serializationTest() {

        String replyTo = "anna@example.com";
        String replyId = "message-id1";
        ReplyElement element = new ReplyElement(replyTo, replyId);
        System.out.println(element.toXML());
        assertXmlSimilar("<reply xmlns=\"urn:xmpp:reply:0\" to=\"anna@example.com\" id=\"message-id1\" />", element.toXML());
    }

    @Test
    public void deserializationTest() throws Exception {

        String xml = "<reply xmlns=\"urn:xmpp:reply:0\" to=\"anna@example.com\" id=\"message-id1\" />";

        XmlPullParser parser = TestUtils.getParser(xml);

        ReplyElementProvider provider = new ReplyElementProvider();

        ReplyElement element = provider.parse(parser, 1, null);

        assertEquals("anna@example.com", element.getReplyTo());
        assertEquals("message-id1", element.getReplyId());
    }

}
