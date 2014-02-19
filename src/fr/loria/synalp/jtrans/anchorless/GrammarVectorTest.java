package fr.loria.synalp.jtrans.anchorless;

import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


public class GrammarVectorTest {
	private GrammarNode rootNode;


	@Before
	public void setUp() throws Exception {
		rootNode = GrammarVector.createGrammarGraph("ben euh");
	}


	@Test
	public void testGraph() {
		Cell<String>
				preSil1	= new Cell<String>("SIL"),
				preSil2	= new Cell<String>("SIL"),

				ben1b	= new Cell<String>("b"),
				ben1swa	= new Cell<String>("swa"),
				ben1n	= new Cell<String>("n"),

				ben2b	= new Cell<String>("b"),
				ben2in	= new Cell<String>("in"),
				ben2n	= new Cell<String>("n"),

				interSil = new Cell<String>("SIL"),

				eu		= new Cell<String>("eu"),

				postSil1 = new Cell<String>("SIL"),
				postSil2 = new Cell<String>("SIL");

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

		Cell<String> rootPhone = GrammarVector.traversePhoneGraph(
				rootNode, new HashMap<GrammarNode, Cell<String>>());

		Assert.assertEquals(preSil1, rootPhone);
		Assert.assertFalse(preSil2.equals(rootPhone));
		Assert.assertFalse(postSil1.equals(rootPhone));
		Assert.assertFalse(postSil2.equals(rootPhone));

		ben1swa.item = "SIL";
		Assert.assertFalse(preSil1.equals(rootPhone));
	}
}
