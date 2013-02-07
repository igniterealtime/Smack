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
			tr.resolve(null);
		} catch (XMPPException e) {
			e.printStackTrace();
			fail("Error resolving");
		}
	}

	protected int getMaxConnections() {
		return 0;
	}

}
