package fr.loria.synalp.jtrans.speechreco;

import junit.framework.Assert;

import org.junit.Test;

import edu.cmu.sphinx.result.ConfusionSet;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.util.LogMath;

public class AlignTokenPassingTest {

	@Test
	public void testAlignSausage2() {
		String[][] so6 = {{"a","b"},{"c"},{"<noop>"},{"d"}};
		String[] ref = {"b","d"};
		
		Sausage s = new Sausage(so6.length);
		LogMath logMath = new LogMath();
		for (int i=0;i<so6.length;i++) {
			for (int j=0;j<so6[i].length;j++)
				s.addWordHypothesis(i, so6[i][j], 0, logMath);
		}
		
		AlignTokenPassing aligner = new AlignTokenPassing();
		float wer = aligner.alignSausage(ref, s);
		System.out.println("wer "+wer);
		Assert.assertTrue(wer==0.5);
	}
	@Test
	public void testAlignSausage() {
		String[][] so6 = {{"a","b"},{"c"},{"<noop>"},{"d"}};
		String[] ref = {"b","c","d"};
		
		Sausage s = new Sausage(so6.length);
		LogMath logMath = new LogMath();
		for (int i=0;i<so6.length;i++) {
			for (int j=0;j<so6[i].length;j++)
				s.addWordHypothesis(i, so6[i][j], 0, logMath);
		}
		
		AlignTokenPassing aligner = new AlignTokenPassing();
		float wer = aligner.alignSausage(ref, s);
		System.out.println("wer "+wer);
		Assert.assertTrue(wer==0);
	}
	@Test
	public void testNormalizeSaucisse() {
		Sausage s = new Sausage(3);
		LogMath logMath = new LogMath();
		s.addWordHypothesis(0, "a-b", logMath.linearToLog(1), logMath);
		s.addWordHypothesis(1, "<sil>", logMath.linearToLog(0.5), logMath);
		s.addWordHypothesis(2, "d_e", logMath.linearToLog(1), logMath);
		Object[] args = {s};
		Object res;
		try {
			res = PrivateAccess.callPrivateStaticMethod(Class.forName("main.SpeechReco"), "normaliseForAccuracy", args);
			s=(Sausage)res;
			String[] attendu = {"0.0:a","-20.0:b 0.0:<noop>","-0.0:<sil>","0.0:d","-20.0:e 0.0:<noop>"};
			int a=0;
			for (ConfusionSet cs : s) {
				System.out.println(cs);
				Assert.assertEquals(attendu[a++], cs.toString().trim());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
