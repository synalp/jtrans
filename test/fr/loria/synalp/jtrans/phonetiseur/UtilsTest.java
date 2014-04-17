package fr.loria.synalp.jtrans.phonetiseur;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsTest {

	@Test
	public void testStringToArrayString() {
		String[] blah = Utils.stringToArrayString("blah");
		assertEquals(4, blah.length);
		assertEquals("b", blah[0]);
		assertEquals("l", blah[1]);
		assertEquals("a", blah[2]);
		assertEquals("h", blah[3]);

		assertEquals(0, Utils.stringToArrayString("").length);
	}

}
