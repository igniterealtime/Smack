package org.jivesoftware.smackx.jingle;

import java.util.ArrayList;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.media.PayloadType.Audio;

public class PayloadTypeTest extends SmackTestCase {

	public PayloadTypeTest(final String arg0) {
		super(arg0);
	}

	public void testEqualsObject() {
		PayloadType p1 = new PayloadType(0, "pt1", 2);
		PayloadType p2 = new PayloadType(0, "pt1", 2);
		assertTrue(p1.equals(p2));
	}
	
	/**
	 * Test for the difference of payloads.
	 */
	public void testDifference() {
		ArrayList<Audio> set1 = new ArrayList<Audio>();
		ArrayList<Audio> set2 = new ArrayList<Audio>();
		
		PayloadType.Audio common1 = new PayloadType.Audio(34, "supercodec-1", 2, 14000);
		PayloadType.Audio common2 = new PayloadType.Audio(56, "supercodec-2", 1, 44000);
		
		set1.add(common1);
		set1.add(common2);
		set1.add(new PayloadType.Audio(36, "supercodec-3", 2, 28000));
		set1.add(new PayloadType.Audio(45, "supercodec-4", 1, 98000));
				
		set2.add(new PayloadType.Audio(27, "supercodec-3", 2, 28000));
		set2.add(common2);
		set2.add(new PayloadType.Audio(32, "supercodec-4", 1, 98000));
		set2.add(common1);
		
		// Get the difference
		ArrayList<Audio> commonSet = new ArrayList<Audio>();			
		commonSet.addAll(set1);
		commonSet.retainAll(set2);

		assertTrue(commonSet.size() == 2);
		System.out.println("Codec " + ((PayloadType.Audio)commonSet.get(0)).getId());
		System.out.println("Codec " + ((PayloadType.Audio)commonSet.get(1)).getId());
		
		assertTrue(commonSet.contains(common1));
		assertTrue(commonSet.contains(common2));
	}

	/**
	 * Test for the difference of payloads when we are handling the same sets.
	 */
	public void testDifferenceSameSet() {
		ArrayList<Audio> set1 = new ArrayList<Audio>();
		ArrayList<Audio> set2 = new ArrayList<Audio>();
		
		PayloadType.Audio common1 = new PayloadType.Audio(34,  "supercodec-1", 2, 14000);
		PayloadType.Audio common2 = new PayloadType.Audio(56,  "supercodec-2", 1, 44000);
		PayloadType.Audio common3 = new PayloadType.Audio(0,   "supercodec-3", 1, 44000);
		PayloadType.Audio common4 = new PayloadType.Audio(120, "supercodec-4", 2, 66060);
		
		set1.add(common1);
		set1.add(common2);
		set1.add(common3);
		set1.add(common4);
				
		set2.add(common1);
		set2.add(common2);
		set2.add(common3);
		set2.add(common4);
		
		// Get the difference
		ArrayList<Audio> commonSet = new ArrayList<Audio>();			
		commonSet.addAll(set1);
		commonSet.retainAll(set2);

		assertTrue(commonSet.size() == 4);
		assertTrue(commonSet.contains(common1));
		assertTrue(commonSet.contains(common2));
	}

	protected int getMaxConnections() {
		return 0;
	}

}
