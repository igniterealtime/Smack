package org.jivesoftware.smackx.stateless_file_sharing;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.stateless_file_sharing.element.FileSharingElement;
import org.jivesoftware.smackx.stateless_file_sharing.element.SourcesElement;
import org.jivesoftware.smackx.stateless_file_sharing.provider.FileSharingElementProvider;
import org.jivesoftware.smackx.url_address_information.element.UrlDataElement;

import org.junit.jupiter.api.Test;

public class FileSharingElementTest extends SmackTestSuite {

    @Test
    public void simpleElementTest() throws XmlPullParserException, IOException, SmackParsingException {
        FileSharingElement fileSharingElement = new FileSharingElement(
                FileMetadataElement.builder()
                        .setMediaType("image/jpeg")
                        .setName("summit.jpg")
                        .setSize(3032449)
                        .setDimensions(4096, 2160)
                        .addHash(new HashElement(HashManager.ALGORITHM.SHA3_256, "2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU="))
                        .addHash(new HashElement(HashManager.ALGORITHM.BLAKE2B256, "2AfMGH8O7UNPTvUVAM9aK13mpCY="))
                        .addDescription("Photo from the summit.")
                        .addOtherChildElement(
                                StandardExtensionElement.builder("thumbnail", "urn:xmpp:thumbs:1")
                                        .addAttribute("uri", "cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org")
                                        .addAttribute("media-type", "image/png")
                                        .addAttribute("width", "128")
                                        .addAttribute("height", "96")
                                        .build())
                        .build(),
                new SourcesElement(Collections.singletonList(
                        new UrlDataElement(
                                "https://download.montague.lit/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/summit.jpg",
                                null
                        )
                ), Collections.singletonList(
                        StandardExtensionElement.builder("jinglepub", "urn:xmpp:jinglepub:1")
                                .addAttribute("from", "romeo@montague.lit/resource")
                                .addAttribute("id", "9559976B-3FBF-4E7E-B457-2DAA225972BB")
                                .addElement(new StandardExtensionElement("description", "urn:xmpp:jingle:apps:file-transfer:5"))
                                .build()
                )));

        final String expectedXml = "" +
                "  <file-sharing xmlns='urn:xmpp:sfs:0'>\n" +
                "    <file xmlns='urn:xmpp:file:metadata:0'>\n" +
                "      <media-type>image/jpeg</media-type>\n" +
                "      <name>summit.jpg</name>\n" +
                "      <size>3032449</size>\n" +
                "      <dimensions>4096x2160</dimensions>\n" +
                "      <hash xmlns='urn:xmpp:hashes:2' algo='sha3-256'>2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU=</hash>\n" +
                "      <hash xmlns='urn:xmpp:hashes:2' algo='id-blake2b256'>2AfMGH8O7UNPTvUVAM9aK13mpCY=</hash>\n" +
                "      <desc>Photo from the summit.</desc>\n" +
                "      <thumbnail xmlns='urn:xmpp:thumbs:1' uri='cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org' media-type='image/png' width='128' height='96'/>\n" +
                "    </file>\n" +
                "    <sources>\n" +
                "      <url-data xmlns='http://jabber.org/protocol/url-data' target='https://download.montague.lit/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/summit.jpg' />\n" +
                "      <jinglepub xmlns='urn:xmpp:jinglepub:1' from='romeo@montague.lit/resource' id='9559976B-3FBF-4E7E-B457-2DAA225972BB'>" +
                "        <description xmlns='urn:xmpp:jingle:apps:file-transfer:5' />\n" +
                "      </jinglepub>\n" +
                "    </sources>\n" +
                "  </file-sharing>";
        assertXmlSimilar(expectedXml, fileSharingElement.toXML().toString());

        FileSharingElement parsed = FileSharingElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        // While I'd rather test for equality here, we have to compare XML instead, as thumbnail is not implemented
        //  and we have to fall back to a StandardExtensionElement which has a non-ideal equals() implementation.
        assertXmlSimilar(expectedXml, parsed.toXML().toString());
    }
}
