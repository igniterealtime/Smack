/**
 *
 * Copyright 2017 Fernando Ramirez
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
package org.jivesoftware.smackx.avatar;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.avatar.element.DataExtension;
import org.jivesoftware.smackx.avatar.provider.DataProvider;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class DataExtensionTest extends SmackTestSuite {

    // @formatter:off
    String dataExtensionExample = "<data xmlns='urn:xmpp:avatar:data'>"
            + "qANQR1DBwU4DX7jmYZnnfe32"
            + "</data>";
    // @formatter:on

    @Test
    public void checkDataExtensionParse() throws Exception {
        byte[] data = Base64.decode("qANQR1DBwU4DX7jmYZnnfe32");
        DataExtension dataExtension = new DataExtension(data);
        Assert.assertEquals(dataExtensionExample, dataExtension.toXML().toString());

        XmlPullParser parser = PacketParserUtils.getParserFor(dataExtensionExample);
        DataExtension dataExtensionFromProvider = new DataProvider().parse(parser);
        Assert.assertEquals(Base64.encodeToString(data), Base64.encodeToString(dataExtensionFromProvider.getData()));
    }

}
