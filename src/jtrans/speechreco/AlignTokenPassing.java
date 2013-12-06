package jtrans.speechreco;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import jtrans.utils.SuiteDeMots;

import edu.cmu.sphinx.result.ConfusionSet;
import edu.cmu.sphinx.result.Sausage;
import edu.cmu.sphinx.result.WordResult;

/**
 * pour calculer l'accuracy des saucisses
 * Plus souple que l'alignement de NIST, mais plus lent
 * 
 * @author xtof
 *
 */
public class AlignTokenPassing {
	public static enum transition {insertion, deletion, association};
	public static final float INSCOST = -1;
	public static final float DELCOST = -1;
	public static final float SUBCOST = -1;

	class TokenPos {
		int posRef=-1, posHyp=-1;
		@Override
		public boolean equals(Object o) {
			TokenPos t = (TokenPos)o;
			return (posRef==t.posRef && posHyp==t.posHyp);
		}
		@Override
		public int hashCode() {
			return posRef+posHyp;
		}
		public String toString() {return posRef+","+posHyp;}
		public TokenPos(int r, int h) {posRef=r; posHyp=h;}
		public TokenPos(){}
	}
	class Token {
		transition[] path = {};
		float score=0;
		int nins=0, ndel=0, nsub=0;

		public Token cloneExtend() {
			Token t = new Token();
			t.score=score; t.nins=nins; t.ndel=ndel; t.nsub=nsub;
			t.path=Arrays.copyOf(path, path.length+1);
			return t;
		}
		public String toString() {return score+":"+nins+":"+ndel+":"+nsub+":"+Arrays.toString(path);}
	}

	HashMap<TokenPos, Token> toks = new HashMap<TokenPos, Token>();

	int ni=0, nd=0, ns=0, nn=0;
	
	private Token prop(Token to, transition tr, String ref, ConfusionSet hyp) {
		if (to==null) return null;
		Token t = to.cloneExtend();
		t.path[t.path.length-1]=tr;
		switch(tr) {
		case insertion:
			if (hyp.containsWord("<noop>")|| hyp.containsWord("<sil>")||
					hyp.containsWord("<s>")|| hyp.containsWord("</s>")) {
				// pas de cout d'insertion dans ce cas !
			} else {
				t.score += INSCOST;
				t.nins++;
			}
			break;
		case deletion: t.score += DELCOST; t.ndel++; break;
		case association: 
			ArrayList<String> hypwords = new ArrayList<String>();
			for (Set<WordResult> ws : hyp.values())
				for (WordResult w : ws)
					hypwords.add(SuiteDeMots.normalizeWord(w.toString()));
			String ref2 = SuiteDeMots.normalizeWord(ref);
			if (!hypwords.contains(ref2)) {
				t.score += SUBCOST;
				t.nsub++;
			}
			break;
		}
		return t;
	}
	private void propage(Token[] v0, Token[] v1, String ref, Sausage so6) {
		int i=0;
		v1[i] = prop(v0[0],transition.deletion,ref,null);
		for (i=1;i<v1.length;i++) {
			v1[i] = prop(v1[i-1],transition.insertion,ref,so6.getConfusionSet(i-1));
			Token t = prop(v0[i-1],transition.association,ref,so6.getConfusionSet(i-1));
			if (t!=null&&(v1[i]==null||t.score>v1[i].score)) v1[i]=t;
			t = prop(v0[i],transition.deletion,ref,null);
			if (t!=null&&(v1[i]==null||t.score>v1[i].score)) v1[i]=t;
		}
	}
	
	/**
	 * utilise 2 vecteurs de tokens: axe des X = ref, axe des Y = hyp
	 * 
	 * ATTENTION : le WER calcule par cette fonction est FAUX !!
	 * a debugger !
	 * 
	 */
	public float alignSausage(String[] ref, Sausage so6) {
		// ces vecteurs commencent a 1, l'indice 0 est reserve pour l'etat initial, avant inclusion d'un mot
		Token[] vprev = new Token[so6.size()+1];
		Token[] v = new Token[so6.size()+1];
		
		// init
		vprev[0]=new Token();
		
		// iter
		for (int refi=0;refi<ref.length;refi++) {
			propage(vprev,v,ref[refi],so6);
			Token[] tmp = vprev;
			vprev=v; v=tmp;
		}
		
		// backtrack
		Token tok = vprev[vprev.length-1];
		printAlign(ref,so6,tok);
		
		float locwer = (float)(tok.nins+tok.ndel+tok.nsub)/(float)ref.length;
		System.out.println("sent: I="+tok.nins+" D="+tok.ndel+" S="+tok.nsub+" N="+ref.length+" WER="+locwer);
		ni+=tok.nins; nd+=tok.ndel; ns+=tok.nsub; nn+=ref.length;
		float wer = (float)(ni+nd+ns)/(float)nn;
		System.out.println("glob: I="+ni+" D="+nd+" S="+ns+" N="+nn+" WER="+wer);
		return wer;
	}
	
	void printAlign(String[] ref, Sausage so6, Token tok) {
		String aref="", ahyp="";
		int iref=-1, ihyp=-1;
		for (transition tr : tok.path) {
			switch(tr) {
			case deletion:
				aref+=ref[++iref]+" ";
				ahyp+=ref[iref].replaceAll(".", "*")+" ";
				break;
			case insertion:
				String w = so6.getConfusionSet(++ihyp).getBestHypothesis().toString();
				ahyp+=w+" ";
				aref+=w.replaceAll(".", "*")+" ";
				break;
			case association:
				if (so6.getConfusionSet(++ihyp).containsWord(ref[++iref])) {
					ahyp+=ref[iref].toLowerCase()+" ";
					aref+=ref[iref].toLowerCase()+" ";
				} else {
					w = so6.getConfusionSet(ihyp).getBestHypothesis().toString().toUpperCase();
					if (w.length()>ref[iref].length()) {
						ahyp+=w+" ";
						aref+=ref[iref].toUpperCase()+" ";
						for (int i=ref[iref].length();i<w.length();i++) aref+=" ";
					} else {
						aref+=ref[iref].toUpperCase()+" ";
						ahyp+=w+" ";
						for (int i=w.length();i<ref[iref].length();i++) ahyp+=" ";
					}
				}
			}
		}
		System.out.println("REF= "+aref);
		System.out.println("HYP= "+ahyp);
	}
	
	/**
	 * trop lent: il faut exploiter les 2 vecteurs qui translatent...
	 * @param ref
	 * @param so6
	 * @return
	 */
	public float alignSausageOld(String[] ref, Sausage so6) {
		// init
		toks.clear();
		toks.put(new TokenPos(), new Token());

		for (;;) {
			// cherche un token qui peut etre etendu
			Iterator<TokenPos> tokit = toks.keySet().iterator();
			TokenPos t = null;
			boolean isExtendable = false;
			while (tokit.hasNext()) {
				t = tokit.next();
				if (t.posRef<ref.length-1||t.posHyp<so6.size()-1) {
					isExtendable = true;
					break;
				}
			}
			if (!isExtendable) break;

			// etend ce token
			Token tok = toks.remove(t);
			System.out.println("extend "+t+"--"+tok);
			
			if (t.posHyp<so6.size()-1 && t.posRef<ref.length-1) {
				extend(new TokenPos(t.posRef,t.posHyp),tok.cloneExtend(),transition.insertion, ref, so6);
				extend(new TokenPos(t.posRef,t.posHyp),tok.cloneExtend(),transition.deletion, ref, so6);
				extend(new TokenPos(t.posRef,t.posHyp),tok.cloneExtend(),transition.association, ref, so6);
			} else if (t.posHyp<so6.size()-1 && t.posRef>=ref.length-1) {
				extend(new TokenPos(t.posRef,t.posHyp),tok.cloneExtend(),transition.insertion, ref, so6);
			} else if (t.posHyp>=so6.size()-1 && t.posRef<ref.length-1) {
				extend(new TokenPos(t.posRef,t.posHyp),tok.cloneExtend(),transition.deletion, ref, so6);
			}
		}
		
		// backtrack
		if (toks.size()!=1) {
			System.out.println("ERROR BACKTRACK "+toks.size()+" "+ref.length+" "+so6.size());
			for (TokenPos p : toks.keySet()) {
				System.out.println(p+": "+toks.get(p));
			}
			return Float.NaN;
		}
		Token tok = toks.values().iterator().next();
		float locwer = (float)(tok.nins+tok.ndel+tok.nsub)/(float)ref.length;
		System.out.println("sent: I="+tok.nins+" D="+tok.ndel+" S="+tok.nsub+" N="+ref.length+" WER="+locwer);
		ni+=tok.nins; nd+=tok.ndel; ns+=tok.nsub; nn+=ref.length;
		float wer = (float)(ni+nd+ns)/(float)nn;
		System.out.println("glob: I="+ni+" D="+nd+" S="+ns+" N="+nn+" WER="+wer);
		return wer;
	}

	private void extend(TokenPos tokpos, Token newtok, transition tr, String[] ref, Sausage so6) {
		// update le token et calcule le score
		if (newtok.path.length<1)
			System.out.println("ERROR EXTEND "+newtok.path);
		newtok.path[newtok.path.length-1] = tr;
		switch(tr) {
		case insertion:
			tokpos.posHyp++;
			if (so6.getConfusionSet(tokpos.posHyp).containsWord("<noop>")||
					so6.getConfusionSet(tokpos.posHyp).containsWord("<sil>")||
					so6.getConfusionSet(tokpos.posHyp).containsWord("<s>")||
					so6.getConfusionSet(tokpos.posHyp).containsWord("</s>")) {
				// pas de cout d'insertion dans ce cas !
			} else {
				newtok.score += INSCOST;
				newtok.nins++;
			}
			break;
		case deletion: newtok.score += DELCOST; tokpos.posRef++; newtok.ndel++; break;
		case association: 
			tokpos.posRef++; tokpos.posHyp++;
			if (!so6.getConfusionSet(tokpos.posHyp).containsWord(ref[tokpos.posRef])) {
				newtok.score += SUBCOST;
				newtok.nsub++;
			}
			break;
		}
		Token oldtok = toks.get(tokpos);
		if (oldtok==null)
			toks.put(tokpos, newtok);
		else {
			if (newtok.score>oldtok.score)
				toks.put(tokpos, newtok);
		}
	}
}
