package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;

public class TransportResolverTest extends SmackTestCase {

	public TransportResolverTest(final String arg) {
		super(arg);
	}

	public void testIsResolving() {
		final TransportResolver tr = new BasicResolver();
		
		tr.addListener(
				new TransportResolverListener.Resolver() {
					public void candidateAdded(final TransportCandidate cand) {
						System.out.println("candidateAdded() called.");
						assertTrue(tr.isResolving() || (!tr.isResolving() && tr.isResolved()));
					}

					public void end() {
						System.out.println("end() called.");
						assertFalse(tr.isResolving());
						assertTrue(tr.isResolved());
					}

					public void init() {
						System.out.println("init() called.");
						assertTrue(tr.isResolving());
						assertFalse(tr.isResolved());
					}
				});

		assertFalse(tr.isResolving());
		assertFalse(tr.isResolved());
		
		try {
			tr.resolve();
		} catch (XMPPException e) {
			e.printStackTrace();
			fail("Error resolving");
		}
	}

	protected int getMaxConnections() {
		return 0;
	}

}
