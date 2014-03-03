package fr.loria.synalp.jtrans.viterbi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;

public class StateGraphTest {
	StateGraph sg;


	@Before
	public void setUp() throws IOException {
		sg = StateGraph.createStandardStateGraph(
				"quatre mois seulement après la mise en place");

		sg.dumpDot(new FileWriter("grammar_unittest.dot"));
	}


	@Test
	public void testSurfaceSplit() throws IOException {
		// Refer to the DOT visualisation of the grammar
		// to make sense of the numbers below

		Assert.assertEquals(129, sg.getStateCount());

		Assert.assertEquals(64, sg.surfaceSplit(0, 128));
			Assert.assertEquals(28, sg.surfaceSplit(0, 64));
				Assert.assertEquals(13, sg.surfaceSplit(0, 28));
					Assert.assertEquals(6, sg.surfaceSplit(0, 13));
					Assert.assertEquals(21, sg.surfaceSplit(14, 28));
				Assert.assertEquals(48, sg.surfaceSplit(29, 64));
					Assert.assertEquals(38, sg.surfaceSplit(29, 48));
					Assert.assertEquals(52, sg.surfaceSplit(49, 64));
			Assert.assertEquals(94, sg.surfaceSplit(65, 128));
				Assert.assertEquals(79, sg.surfaceSplit(65, 94));
				Assert.assertEquals(111, sg.surfaceSplit(95, 128));

		// Subgraph with all nodes RD >= 1
		Assert.assertEquals(-1, sg.surfaceSplit(54, 59));
		Assert.assertEquals(-1, sg.surfaceSplit(53, 59));

		Assert.assertEquals(52, sg.surfaceSplit(52, 59)); // even length
		Assert.assertEquals(52, sg.surfaceSplit(52, 58)); // odd length

		// #60 is RD0, but it's the final state in the subgraph,
		// so there's nothing to cut
		Assert.assertEquals(-1, sg.surfaceSplit(54, 60)); // odd length
		Assert.assertEquals(-1, sg.surfaceSplit(55, 60)); // even length
	}
}
