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
	AlignmentScorer spk0;
	AlignmentScorer spk1;

	/**
	 * Unique states in each speaker's grammar
	 */
	final static int STATE_COUNT = 9; // 'a': 3, 'o': 3, extra sil: 3

	/**
	 * Timeline of an 'ah' utterance
	 * (assuming phone 'a' occupies state slots 0-2)
	 */
	final static int[] AH_TL = new int[] {0, 0, 1, 2, 2, 2};

	/**
	 * Timeline of an 'oh' utterance
	 * (assuming phone 'o' occupies state slots 3-5)
	 */
	final static int[] OH_TL = new int[] {3, 4, 4, 4, 4, 5};

	final static int[] AH_TL_AS_FIRST  = increment(3, AH_TL);
	final static int[] AH_TL_AS_SECOND = increment(3+STATE_COUNT, AH_TL);

	final static int[] OH_TL_AS_FIRST  = increment(3, OH_TL);
	final static int[] OH_TL_AS_SECOND = increment(3+STATE_COUNT, OH_TL);

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
	 * spk0 utters 'ah' at frame 0;
	 * spk1 utters 'oh' at frame PLEN;
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

		spk0 = new AlignmentScorer(data);
		spk1 = new AlignmentScorer(data);

		// standard scenario:
		// 'ah' is uttered right before 'oh', the rest is silence
		spk0.learn(sg, AH_TL, 0);
		spk1.learn(sg, OH_TL, PLEN);
	}


	private static double merge(AlignmentScorer... scorers) {
		AlignmentScorer merger = AlignmentScorer.merge(scorers);

		System.out.println("***MERGER***");
		for (Integer i: merger.getLongTimeline()) {
			System.out.print(String.format("%2d ", i));
		}
		System.out.println();

		merger.finishLearning();
		merger.score();
		return AlignmentScorer.sum(merger.getLikelihoods());
	}


	private static int[] increment(int step, int[] array) {
		int[] incremented = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			incremented[i] = array[i] + step;
		}
		return incremented;
	}

	private static void copy(int[] src, int[] dest, int pos) {
		System.arraycopy(src, 0, dest, pos, src.length);
	}


	@Test
	public void testMergeAhOh2Speakers() {
		AlignmentScorer m = AlignmentScorer.merge(spk0, spk1);

		int[] mTL = new int[FRAMES];
		copy(AH_TL_AS_FIRST,  mTL, 0);
		copy(OH_TL_AS_SECOND, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeOhAh2Speakers() {
		AlignmentScorer m = AlignmentScorer.merge(spk1, spk0);
		int[] mTL = new int[FRAMES];

		copy(AH_TL_AS_SECOND, mTL, 0);
		copy(OH_TL_AS_FIRST,  mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeAhOh1Speaker() {
		spk0.learn(sg, OH_TL, PLEN);

		AlignmentScorer m = AlignmentScorer.merge(spk0);

		int[] mTL = new int[FRAMES];
		copy(AH_TL_AS_FIRST, mTL, 0);
		copy(OH_TL_AS_FIRST, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeAh() {
		AlignmentScorer m = AlignmentScorer.merge(spk0);

		int[] mTL = new int[FRAMES];
		copy(AH_TL_AS_FIRST, mTL, 0);
		fillInterpolate(3, mTL, PLEN, 2*PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testMergeOh() {
		AlignmentScorer m = AlignmentScorer.merge(spk1);

		int[] mTL = new int[FRAMES];
		fillInterpolate(3, mTL, 0, PLEN);
		copy(OH_TL_AS_FIRST, mTL, PLEN);
		fillInterpolate(3, mTL, 2*PLEN, PLEN);

		Assert.assertArrayEquals(mTL, m.getLongTimeline());
	}


	@Test
	public void testConsistentScoreOrdering() {
		double L1x2 = merge(spk0, spk1);
		double L2x1 = merge(spk1, spk0);
		double L1 = merge(spk0);
		double L2 = merge(spk1);

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
		// Starting point: existing speaker says 'ah' at frame 0 (spk0)
		// *New* speaker learns 'ah' at frame PLEN
		AlignmentScorer ah2AS = new AlignmentScorer(data);
		ah2AS.learn(sg, AH_TL, PLEN);
		double differentSpeakers = merge(spk0, ah2AS);

		// Proposal 2: single speaker
		// Starting point: existing speaker says 'ah' at frame 0 (spk0)
		// *Existing* speaker learns 'ah' again at frame PLEN
		spk0.learn(sg, AH_TL, PLEN);
		double singleSpeaker = merge(spk0);

		// The likelihoods of the proposals should differ
		Assert.assertTrue(differentSpeakers != singleSpeaker);
	}


	@Test
	public void testMergingOneScorerOnlyHasNoEffectOnLikelihood() {
		double merged = merge(spk0);

		spk0.fillVoids(6, 7, 8);
		spk0.finishLearning();
		spk0.score();
		double standalone = AlignmentScorer.sum(spk0.getLikelihoods());

		// the computation must yield the exact same result
		Assert.assertEquals(merged, standalone, 0);
	}

}
