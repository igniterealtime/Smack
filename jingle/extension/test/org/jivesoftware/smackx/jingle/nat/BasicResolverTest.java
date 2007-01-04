package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.test.SmackTestCase;

public class BasicResolverTest extends SmackTestCase {

	private int counter;

	private final Object mutex = new Object();

	public BasicResolverTest(String arg) {
		super(arg);
	}

	// Counter management

	private void resetCounter() {
		synchronized (mutex) {
			counter = 0;
		}
	}

	private void incCounter() {
		synchronized (mutex) {
			counter++;
		}
	}

	private int valCounter() {
		int val;
		synchronized (mutex) {
			val = counter;
		}
		return val;
	}

	public void testCheckValidHostname() {
		String validHostname = new String("slashdot.org");
		BasicResolver br = new BasicResolver();
		TransportCandidate tc = new TransportCandidate.Fixed(validHostname, 0);

		resetCounter();
		
		tc.addListener(new TransportResolverListener.Checker() {
			public void candidateChecked(TransportCandidate cand, boolean result) {
				if(result == true) {
					System.out.println(cand.getIp() + " is reachable (as expected)");
					incCounter();					
				}
			}

            public void candidateChecking(TransportCandidate cand) {
                
            }
        });
		
		tc.check();
		
		try {
			Thread.sleep(TransportResolver.CHECK_TIMEOUT);
		} catch (Exception e) {
		}
		
		assertTrue(valCounter() > 0);
	}

	public void testCheckInvalidHostname() {
		String invalidHostname = new String("camupilosupino.org");
		BasicResolver br = new BasicResolver();
		TransportCandidate tc = new TransportCandidate.Fixed(invalidHostname, 0);

		resetCounter();
		
		tc.addListener(new TransportResolverListener.Checker() {
			public void candidateChecked(TransportCandidate cand, boolean result) {
				if(result == false) {
					System.out.println(cand.getIp() + " is _not_ reachable (as expected)");
					incCounter();					
				}
			}

            public void candidateChecking(TransportCandidate cand) {
            }
        });
		
		tc.check();
		
		try {
			Thread.sleep(TransportResolver.CHECK_TIMEOUT);
		} catch (Exception e) {
		}

		assertTrue(valCounter() > 0);
	}

	
	protected int getMaxConnections() {
		return 0;
	}

}
