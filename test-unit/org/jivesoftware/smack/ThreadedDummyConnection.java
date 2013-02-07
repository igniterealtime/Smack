/**
 * $RCSfile$
 * $Revision$
 * $Date$
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
package org.jivesoftware.smack;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ.Type;

public class ThreadedDummyConnection extends DummyConnection
{
	private BlockingQueue<IQ>  replyQ = new ArrayBlockingQueue<IQ>(1);
	private BlockingQueue<Packet>  messageQ = new LinkedBlockingQueue<Packet>(5);

	@Override
	public void sendPacket(Packet packet)
	{
		super.sendPacket(packet);

		if ((packet instanceof IQ) && !replyQ.isEmpty())
		{
			// Set reply packet to match one being sent.  We haven't started the 
			// other thread yet so this is still safe.
			IQ replyPacket = replyQ.peek();
			replyPacket.setPacketID(packet.getPacketID());
			replyPacket.setFrom(packet.getTo());
			replyPacket.setTo(packet.getFrom());
			replyPacket.setType(Type.RESULT);
			
			new ProcessQueue(replyQ).start();
		}
	}
	
	public void addMessage(Message msgToProcess)
	{
		messageQ.add(msgToProcess);
	}
	
	public void addIQReply(IQ reply)
	{
		replyQ.add(reply);
	}
	
	public void processMessages()
	{
		if (!messageQ.isEmpty())
			new ProcessQueue(messageQ).start();
		else
			System.out.println("No messages to process");
	}

	class ProcessQueue extends Thread
	{
		private BlockingQueue<? extends Packet> processQ;
		
		ProcessQueue(BlockingQueue<? extends Packet> queue)
		{
			processQ = queue;
		}
		
		@Override
		public void run()
		{
			try
			{
				processPacket(processQ.take());
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	};

}
