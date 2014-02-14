package org.jivesoftware.smackx.muc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;

public class ConnectionDetachedPacketCollectorTest
{

	@Test
	public void verifyRollover()
	{
		ConnectionDetachedPacketCollector collector = new ConnectionDetachedPacketCollector(5);
		
		for (int i=0; i<6; i++)
		{
			Packet testPacket = new TestPacket(i);
			collector.processPacket(testPacket);
		}
		
		// Assert that '0' has rolled off
		assertEquals("1", collector.nextResult().getPacketID());
		assertEquals("2", collector.nextResult().getPacketID());
		assertEquals("3", collector.nextResult().getPacketID());
		assertEquals("4", collector.nextResult().getPacketID());
		assertEquals("5", collector.pollResult().getPacketID());
		assertNull(collector.pollResult());
		
		for (int i=10; i<15; i++)
		{
			Packet testPacket = new TestPacket(i);
			collector.processPacket(testPacket);
		}
		
		assertEquals("10", collector.nextResult().getPacketID());
		assertEquals("11", collector.nextResult().getPacketID());
		assertEquals("12", collector.nextResult().getPacketID());
		assertEquals("13", collector.nextResult().getPacketID());
		assertEquals("14", collector.pollResult().getPacketID());
		assertNull(collector.pollResult());
		
		assertNull(collector.nextResult(1000));
	}

	/**
	 * Although this doesn't guarentee anything due to the nature of threading, it can 
	 * potentially catch problems.
	 */
	@Test
	public void verifyThreadSafety()
	{
		int insertCount = 500;
		final ConnectionDetachedPacketCollector collector = new ConnectionDetachedPacketCollector(insertCount);
		
		Thread consumer1 = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					while (true)
					{
						try
						{
							Thread.sleep(3);
						}
						catch (InterruptedException e)
						{
						}
						Packet packet = collector.nextResult();
//						System.out.println(Thread.currentThread().getName() + "  packet: " + packet);
					}
				}
				catch (RuntimeException re)
				{
					if (re.getCause() instanceof InterruptedException)
					{
//						System.out.println(Thread.currentThread().getName() + " has been interupted");
					}
				}
			}
		});
		consumer1.setName("consumer 1");

		Thread consumer2 = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Packet p = null;
				
				do
				{
					try
					{
						Thread.sleep(3);
					}
					catch (InterruptedException e)
					{
					}
					p = collector.nextResult(1);
//					System.out.println(Thread.currentThread().getName() + "  packet: " + p);
				}
				while (p != null);
			}
		});
		consumer2.setName("consumer 2");

		Thread consumer3 = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Packet p = null;
				
				do
				{
					try
					{
						Thread.sleep(3);
					}
					catch (InterruptedException e)
					{
					}
					p = collector.pollResult();
//					System.out.println(Thread.currentThread().getName() + "  packet: " + p);
				}
				while (p != null);
			}
		});
		consumer3.setName("consumer 3");

		consumer1.start();
		consumer2.start();
		consumer3.start();
		
		for(int i=0; i<insertCount; i++)
		{
			collector.processPacket(new TestPacket(i));
		}

		try
		{
			Thread.sleep(5000);
			consumer3.join();
			consumer2.join();
			consumer1.interrupt();
		}
		catch (InterruptedException e)
		{
		}
		//We cannot guarantee that this is going to pass due to the possible issue of timing between consumer 1 
		// and main, but the probability is extremely remote.
		assertNull(collector.pollResult());
	}
	
	class TestPacket extends Packet
	{
		public TestPacket(int i)
		{
			setPacketID(String.valueOf(i));
		}

		@Override
		public String toString()
		{
			return toXML();
		}

		@Override
		public String toXML()
		{
			return "<packetId>" + getPacketID() + "</packetId>";
		}
	}
}
