package fr.loria.synalp.jtrans.viterbi;

import fr.loria.synalp.jtrans.elements.Word;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static fr.loria.synalp.jtrans.facade.FastLinearAligner.fillInterpolate;

public class AlignmentScorerTest {

	float[][] data;
	StateGraph sg;
	StatePool sp;
	AlignmentScorer spkAh;
	AlignmentScorer spkOh;

	/**
	 * Timeline of an 'ah' utterance preceded by a silence
	 * (assuming phone 'a' occupies state slots 3-5)
	 * (in any StatePool, SIL occupies 0-2)
	 */
	final static int[] AH_SGNODES    = new int[] {3, 3, 4, 4, 5, 5};
	final static int[] AH_AS1USTATES = new int[] {3, 3, 4, 4, 5, 5};
	final static int[] AH_AS2USTATES = new int[] {6, 6, 7, 7, 8, 8};

	/**
	 * Timeline of an 'oh' utterance
	 * (assuming phone 'o' occupies state slots 3-5)
	 */
	final static int[] OH_SGNODES    = new int[] {9, 9, 9, 9,10,11};
	final static int[] OH_AS1USTATES = new int[] {3, 3, 3, 3, 4, 5};
	final static int[] OH_AS2USTATES = new int[] {6, 6, 6, 6, 7, 8};

	/**
	 * Phone length. In our scenarios, for simplicity's sake, all phones are
	 * uttered for the same duration.
	 */
	final static int PLEN = 6;

	/**
	 * In our scenarios, three phones are uttered in total
	 */
	final static int FRAMES = 3*PLEN;


	/**
	 * Standard scenario:
	 * spkAh utters 'ah' at frame 0;
	 * spkOh utters 'oh' at frame PLEN;
	 * pad timeline with silence.
	 */
	@Before
	public void setUp() {
		data = new float[FRAMES][AlignmentScorer.FRAME_DATA_LENGTH];

		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < AlignmentScorer.FRAME_DATA_LENGTH; j++) {
				data[i][j] = i * 100 + j;
			}
		}

		sp = new StatePool();

		Word ah = new Word("ah");
		ah.setSpeaker(0);
		Word oh = new Word("oh");
		oh.setSpeaker(1);

		sg = new StateGraph(
				sp,
				new String[][]{{"a"}, {"o"}},
				Arrays.asList(ah, oh),
				true);

		spkAh = new AlignmentScorer(data);
		spkOh = new AlignmentScorer(data);

		// standard scenario:
		// 'ah' is uttered right before 'oh', the rest is silence
		spkAh.learn(sg, AH_SGNODES, 0);
		spkOh.learn(sg, OH_SGNODES, PLEN);
	}


	private static double merge(AlignmentScorer... scorers) {
		AlignmentScorer merger = AlignmentScorer.merge(scorers);

		System.out.println("***MERGER***");
		for (Integer i: merger.getLongTimeline()) {
			System.out.print(String.format("%2d ", i));
		}
		System.out.println();

		merger.score();
		return AlignmentScorer.sum(merger.getLikelihoods());
	}


	private static void copy(int[] src, int[] dest, int pos) {
		System.arraycopy(src, 0, dest, pos, src.length);
	}


	@Test
	public void testMergeAhOh2Speakers() {
		AlignmentScorer m = AlignmentScorer.merge(spkAh, spkOh);

		int[] mTL = new int[FRAMES];
		copy(AH_AS1USTATES, mTL, 0);
		copy(OH_AS2USTATES, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeOhAh2Speakers() {
		AlignmentScorer m = AlignmentScorer.merge(spkOh, spkAh);
		int[] mTL = new int[FRAMES];

		copy(AH_AS2USTATES, mTL, 0);
		copy(OH_AS1USTATES, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeAhOh1Speaker() {
		spkAh.learn(sg, OH_SGNODES, PLEN);

		AlignmentScorer m = AlignmentScorer.merge(spkAh);

		int[] mTL = new int[FRAMES];
		copy(AH_AS1USTATES, mTL, 0);
		copy(OH_AS2USTATES, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeAh() {
		AlignmentScorer m = AlignmentScorer.merge(spkAh);

		int[] mTL = new int[FRAMES];
		copy(AH_AS1USTATES, mTL, 0);
		fillInterpolate(3, mTL, PLEN, 2*PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeOh() {
		AlignmentScorer m = AlignmentScorer.merge(spkOh);

		int[] mTL = new int[FRAMES];
		fillInterpolate(3, mTL, 0, PLEN);
		copy(OH_AS1USTATES, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testConsistentScoreOrdering() {
		double L1x2 = merge(spkAh, spkOh);
		double L2x1 = merge(spkOh, spkAh);
		double L1 = merge(spkAh);
		double L2 = merge(spkOh);

		System.out.println(L1x2);
		System.out.println(L2x1);
		System.out.println(L1);
		System.out.println(L2);

		Assert.assertTrue(L1x2 == L2x1);
		Assert.assertTrue(L1 != L2);
		Assert.assertTrue(L1x2 > L1);
		Assert.assertTrue(L1x2 > L2);
	}


	@Test
	public void testSamePhoneDifferentSpeakers() {
		// Proposal 1: different speakers
		// Starting point: existing speaker says 'ah' at frame 0 (spkAh)
		// *New* speaker learns 'ah' at frame PLEN
		AlignmentScorer ah2AS = new AlignmentScorer(data);
		ah2AS.learn(sg, AH_SGNODES, PLEN);
		double differentSpeakers = merge(spkAh, ah2AS);

		// Proposal 2: single speaker
		// Starting point: existing speaker says 'ah' at frame 0 (spkAh)
		// *Existing* speaker learns 'ah' again at frame PLEN
		spkAh.learn(sg, AH_SGNODES, PLEN);
		double singleSpeaker = merge(spkAh);

		// The likelihoods of the proposals should differ
		Assert.assertTrue(differentSpeakers != singleSpeaker);
	}


	/*
	TODO: This test is broken for now because AlignmentScorer ignores silences
	and drops unused states, so we can't use spkAh.fillVoids(0,1,2) (since
	spkAh only uses 3 states)
	 */
	@Test
	public void testMergingOneScorerOnlyHasNoEffectOnLikelihood() {
		double merged = merge(spkAh);

		spkAh.fillVoids(0, 1, 2); // TODO: broken fill with silences! 0,1,2 is 'a' in the AlignmentScorer's QStatePool!
		spkAh.finishLearning();
		spkAh.score();
		double standalone = AlignmentScorer.sum(spkAh.getLikelihoods());

		// the computation must yield the exact same result
		Assert.assertEquals(merged, standalone, 0);
	}

}
