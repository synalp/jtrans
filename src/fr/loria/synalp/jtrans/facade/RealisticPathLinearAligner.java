package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import fr.loria.synalp.jtrans.viterbi.Alignment;
import static fr.loria.synalp.jtrans.facade.FastLinearAligner.interpolatedLengths;

import java.io.File;
import java.io.IOException;


/**
 * "Smart" linear aligner that walks the same path in the state graph as the
 * Viterbi aligner would. Every HMM state will last roughly the same amount of
 * time.
 *
 * Slow and inaccurate, but more realistic than {@link FastLinearAligner}.
 * For testing purposes only, e.g. to compare with likelihoods yielded by
 * ViterbiAligner.
 *
 * @see FastLinearAligner
 */
public class RealisticPathLinearAligner extends CheatingAligner {

	public RealisticPathLinearAligner(File audio, ProgressDisplay progress)
			throws IOException
	{
		super(audio, progress);
	}


	@Override
	protected Alignment tweak(Alignment base) {
		Alignment al = new Alignment(base.getFrameOffset());

		int[] lengths = interpolatedLengths(
				base.getSegmentCount(), base.getLength());
		assert lengths.length == base.getSegmentCount();

		for (int i = 0; i < lengths.length; i++) {
			Alignment.Segment seg = base.getSegment(i);
			al.newSegment(seg.state, seg.word, lengths[i]);
		}

		return al;
	}

}
