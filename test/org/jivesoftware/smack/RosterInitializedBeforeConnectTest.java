package org.jivesoftware.smack;

/**
 * Run all tests defined in RosterTest but initialize the roster before connection is logged in and
 * authenticated.
 * 
 * @author Henning Staib
 */
public class RosterInitializedBeforeConnectTest extends RosterSmackTest {

    public RosterInitializedBeforeConnectTest(String name) {
        super(name);
    }

    protected boolean createOfflineConnections() {
        return true;
    }

    protected void setUp() throws Exception {
        super.setUp();

        // initialize all rosters before login
        for (int i = 0; i < getMaxConnections(); i++) {
            XMPPConnection connection = getConnection(i);
            assertFalse(connection.isConnected());

            Roster roster = connection.getRoster();
            assertNotNull(roster);

            connectAndLogin(i);
        }
    }

}
