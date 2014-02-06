package fr.loria.synalp.jtrans.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import fr.loria.synalp.jtrans.speechreco.SpeechReco;
import org.junit.Test;
import fr.loria.synalp.jtrans.speechreco.RecoUtterance;
import fr.loria.synalp.jtrans.speechreco.RecoUtteranceImmutable;
import fr.loria.synalp.jtrans.speechreco.RecoWord;

public class SpeechRecoTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testTokenPassing() {
		ArrayList<Set<RecoUtterance>> pos2candidats = new ArrayList<Set<RecoUtterance>>();
		{
			// pos0
			// a=0.8 OU b=0.7 
			HashSet<RecoUtterance> candidats = new HashSet<RecoUtterance>();
			pos2candidats.add(candidats);
			RecoWord m1 = new RecoWord(); m1.word="a"; m1.frameDeb=0; m1.frameEnd=1; m1.score=0.8f;
			RecoUtterance mot1 = new RecoUtterance(); mot1.add(m1);
			candidats.add(mot1);
			RecoWord m2 = new RecoWord(); m2.word="b"; m2.frameDeb=0; m2.frameEnd=1; m2.score=0.7f;
			RecoUtterance mot2 = new RecoUtterance(); mot2.add(m2);
			candidats.add(mot2);
		}
		{
			// pos1
			// c=0.2 OU d=0.5
			HashSet<RecoUtterance> candidats = new HashSet<RecoUtterance>();
			pos2candidats.add(candidats);
			RecoWord m1 = new RecoWord(); m1.word="c"; m1.frameDeb=2; m1.frameEnd=3; m1.score=0.2f;
			RecoUtterance mot1 = new RecoUtterance(); mot1.add(m1);
			candidats.add(mot1);
			RecoWord m2 = new RecoWord(); m2.word="d"; m2.frameDeb=2; m2.frameEnd=3; m2.score=0.5f;
			RecoUtterance mot2 = new RecoUtterance(); mot2.add(m2);
			candidats.add(mot2);
		}
		
		Object[] args = {pos2candidats,3};
		Object res = PrivateAccess.callPrivateStaticMethod(SpeechReco.class, "tokenPassing0", args);
		List<RecoUtteranceImmutable> r = (List<RecoUtteranceImmutable>)res;
		for (RecoUtteranceImmutable utt : r)
			System.out.println(utt);
		
		// a c = 0.5
		// a d = 0.65
		// b c = 0.45
		// b d = 0.6
		// donc la bonne solution pour nbest=3 est ad + bd + ac
		Assert.assertEquals(r.get(0).toString(), "a d ");
		Assert.assertEquals(r.get(1).toString(), "b d ");
		Assert.assertEquals(r.get(2).toString(), "a c ");
	}

}
