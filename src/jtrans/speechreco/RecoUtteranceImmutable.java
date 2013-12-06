package jtrans.speechreco;

/**
 * utilise un objet immutable pour ne jamais recalculer le score,
 * ce qui est trop couteux dans les algos de tris, etc.
 * 
 * @author xtof
 *
 */
public final class RecoUtteranceImmutable implements Comparable<RecoUtteranceImmutable> {
	private final float score;
	public final String[] words;
	// attention, on ne peut pas garantir que la liste suivante ne sera pas modifiee:
	// on ne peut que garantir que le score ne sera pas modifie !
	public final RecoUtterance originalUtt;
	
	@Override
	public String toString() {
		String s="";
		for (String w : words) s+=w+" ";
		return s;
	}
	
	private RecoUtteranceImmutable(RecoUtterance utt, float sc) {
		originalUtt=utt;
		score=sc;
		words=new String[utt.size()];
		for (int i=0;i<words.length;i++)
			words[i]=utt.get(i).word;
	}
	
    public float getScore() {return score;}
    
	@Override
	public int compareTo(RecoUtteranceImmutable o) {
		float myscore = score;
		float hisscore = o.score;
		if (myscore>hisscore)
			return -1;
		else if (myscore<hisscore)
			return 1;
		else return 0;
	}

    private static float calcScore(RecoUtterance utt) {
    	float sc=0f;
    	for (RecoWord w : utt)
    		sc+=w.score;
    	sc/=(float)utt.size();
    	return sc;
    }
    
	public static RecoUtteranceImmutable getUtterance(RecoUtterance u) {
		float s = calcScore(u);
		RecoUtteranceImmutable ru = new RecoUtteranceImmutable(u,s);
		return ru;
	}
	
	public static RecoUtteranceImmutable getUtterance(RecoUtterance u, RecoUtterance u2) {
		RecoUtterance ubis = u.clone();
		ubis.addAll(u2);
		float s = calcScore(ubis);
		RecoUtteranceImmutable ru = new RecoUtteranceImmutable(ubis,s);
		return ru;
	}
}
