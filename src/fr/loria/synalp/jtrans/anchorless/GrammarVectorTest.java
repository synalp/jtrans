package fr.loria.synalp.jtrans.anchorless;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

public class GrammarVectorTest {
	private GrammarVector gv;


	@Before
	public void setUp() throws Exception {
		gv = new GrammarVector("ben euh");
	}


	@Test
	public void testGraph() {
		Cell
				preSil1	= new Cell("SIL"),
				preSil2	= new Cell("SIL"),

				ben1b	= new Cell("b"),
				ben1swa	= new Cell("swa"),
				ben1n	= new Cell("n"),

				ben2b	= new Cell("b"),
				ben2in	= new Cell("in"),
				ben2n	= new Cell("n"),

				interSil = new Cell("SIL"),

				eu		= new Cell("eu"),

				postSil1 = new Cell("SIL"),
				postSil2 = new Cell("SIL");

		preSil1.link(preSil2);

		for (Cell c: new Cell[]{preSil1, preSil2}) {
			c.link(ben1b);
			c.link(ben2b);
		}

		ben1b.link(ben1swa).link(ben1n);
		ben2b.link(ben2in).link(ben2n);

		for (Cell c: new Cell[]{ben1n, ben2in, ben2n}) {
			c.link(interSil);
			c.link(eu);
		}

		interSil.link(eu);
		eu.link(postSil1).link(postSil2);
		eu.link(postSil2);

		//------

		Assert.assertEquals(preSil1, gv.getRoot());
		Assert.assertFalse(preSil2.equals(gv.getRoot()));
		Assert.assertFalse(postSil1.equals(gv.getRoot()));
		Assert.assertFalse(postSil2.equals(gv.getRoot()));

		ben1swa.name = "SIL";
		Assert.assertFalse(preSil1.equals(gv.getRoot()));
	}
}
