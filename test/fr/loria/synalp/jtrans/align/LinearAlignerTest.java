package fr.loria.synalp.jtrans.align;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;

public class LinearAlignerTest {

	@Test
	public void testFillInterpolate1() {
		int[] buf = new int[9];
		LinearAligner.fillInterpolate(5, buf, 0, buf.length);
		assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4}, buf);
	}


	@Test
	public void testFillInterpolate2() {
		int[] buf = new int[9];
		LinearAligner.fillInterpolate(9, buf, 0, buf.length);
		assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, buf);
	}


	@Test
	public void testFillInterpolate3() {
		int[] buf = new int[12];
		LinearAligner.fillInterpolate(4, buf, 0, buf.length);
		assertArrayEquals(new int[]{0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3}, buf);
	}


	@Test
	public void testOffsetAndLength() {
		int[] buf = new int[12];
		Arrays.fill(buf, -1);
		LinearAligner.fillInterpolate(4, buf, 4, 4);
		assertArrayEquals(new int[]{
				-1, -1, -1, -1,
				0, 1, 2, 3,
				-1, -1, -1, -1}, buf);

	}

}
