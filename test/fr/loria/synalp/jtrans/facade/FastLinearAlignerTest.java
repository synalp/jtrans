package fr.loria.synalp.jtrans.facade;

import org.junit.Assert;
import org.junit.Test;

public class FastLinearAlignerTest {

	@Test
	public void testFillInterpolate1() {
		int[] buf = new int[9];
		FastLinearAligner.fillInterpolate(5, buf);
		Assert.assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4}, buf);
	}


	@Test
	public void testFillInterpolate2() {
		int[] buf = new int[9];
		FastLinearAligner.fillInterpolate(9, buf);
		Assert.assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, buf);
	}


	@Test
	public void testFillInterpolate3() {
		int[] buf = new int[12];
		FastLinearAligner.fillInterpolate(4, buf);
		Assert.assertArrayEquals(new int[]{0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3}, buf);
	}

}
