package org.jivesoftware.smackx;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPException;

import java.util.List;

/**
 * Test cases for getting the shared groups of a user.<p>
 *
 * Important note: This functionality is not part of the XMPP spec and it will only work
 * with Wildfire.
 *
 * @author Gaston Dombiak
 */
public class SharedGroupsTest extends SmackTestCase {

    public SharedGroupsTest(String arg0) {
        super(arg0);
    }

    public void testGetUserSharedGroups() throws XMPPException {
        List<String> groups = SharedGroupManager.getSharedGroups(getConnection(0));

        assertNotNull("User groups was null", groups);
    }

    protected int getMaxConnections() {
        return 1;
    }
}
