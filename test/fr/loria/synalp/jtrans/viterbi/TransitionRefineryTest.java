package fr.loria.synalp.jtrans.viterbi;

import junit.framework.Assert;
import org.junit.Test;

public class TransitionRefineryTest {

	@Test
	public void testNextTransition() {
		int[] tl = new int[] {
				0, 0, 0, 1, 1,
				2, 3, 4, 4, 4,
				4, 4, 4, 5, 5,
				6, 6, 6, 7, 8,
				8, 8, 8, 8, 8,
		};

		Assert.assertEquals(2, TransitionRefinery.nextTransition(0, tl));
		Assert.assertEquals(2, TransitionRefinery.nextTransition(1, tl));
		Assert.assertEquals(2, TransitionRefinery.nextTransition(2, tl));
		Assert.assertEquals(6, TransitionRefinery.nextTransition(3, tl));
		Assert.assertEquals(6, TransitionRefinery.nextTransition(4, tl));

		Assert.assertEquals(6, TransitionRefinery.nextTransition(5, tl));
		Assert.assertEquals(6, TransitionRefinery.nextTransition(6, tl));
		Assert.assertEquals(12, TransitionRefinery.nextTransition(7, tl));
		Assert.assertEquals(12, TransitionRefinery.nextTransition(8, tl));
		Assert.assertEquals(12, TransitionRefinery.nextTransition(9, tl));

		Assert.assertEquals(12, TransitionRefinery.nextTransition(10, tl));
		Assert.assertEquals(12, TransitionRefinery.nextTransition(11, tl));
		Assert.assertEquals(12, TransitionRefinery.nextTransition(12, tl));
		Assert.assertEquals(14, TransitionRefinery.nextTransition(13, tl));
		Assert.assertEquals(14, TransitionRefinery.nextTransition(14, tl));

		Assert.assertEquals(18, TransitionRefinery.nextTransition(15, tl));
		Assert.assertEquals(18, TransitionRefinery.nextTransition(16, tl));
		Assert.assertEquals(18, TransitionRefinery.nextTransition(17, tl));
		Assert.assertEquals(18, TransitionRefinery.nextTransition(18, tl));
		Assert.assertEquals(-1, TransitionRefinery.nextTransition(19, tl));

		Assert.assertEquals(-1, TransitionRefinery.nextTransition(20, tl));
		Assert.assertEquals(-1, TransitionRefinery.nextTransition(21, tl));
		Assert.assertEquals(-1, TransitionRefinery.nextTransition(22, tl));
		Assert.assertEquals(-1, TransitionRefinery.nextTransition(23, tl));
		Assert.assertEquals(-1, TransitionRefinery.nextTransition(24, tl));
	}

}
