package org.jivesoftware.smackx.pubsub;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigureFormTest
{
	@Test
	public void checkChildrenAssocPolicy()
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setChildrenAssociationPolicy(ChildrenAssociationPolicy.owners);
		assertEquals(ChildrenAssociationPolicy.owners, form.getChildrenAssociationPolicy());
	}
}
