package fr.loria.synalp.jtrans.phonetiseur;

import junit.framework.Assert;
import org.junit.Test;

public class UtilsTest {

	@Test
	public void testStringToArrayString() {
		String[] blah = Utils.stringToArrayString("blah");
		Assert.assertEquals(4, blah.length);
		Assert.assertEquals("b", blah[0]);
		Assert.assertEquals("l", blah[1]);
		Assert.assertEquals("a", blah[2]);
		Assert.assertEquals("h", blah[3]);

		Assert.assertEquals(0, Utils.stringToArrayString("").length);
	}

}
