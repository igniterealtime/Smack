package org.jivesoftware.smack;

import org.junit.Test;

import static org.junit.Assert.*;

public class SmackConfigTest
{
	@Test
	public void validatePacketCollectorSize()
	{
		assertEquals(10000, SmackConfiguration.getPacketCollectorSize());
	}

	@Test
	public void validateKeepAliveInterval()
	{
		assertEquals(30000, SmackConfiguration.getKeepAliveInterval());
	}

	@Test
	public void validateLocalSocks5ProxyPort()
	{
		assertEquals(7777, SmackConfiguration.getLocalSocks5ProxyPort());
	}

	@Test
	public void validateIsLocalSocks5Proxy()
	{
		assertTrue(SmackConfiguration.isLocalSocks5ProxyEnabled());
	}
}
