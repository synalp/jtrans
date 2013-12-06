package jtrans.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * on peut transformer une suite de mots en une autre (par exemple, normalisation pour calculer l'accuracy)
 * cette classe conserve l'association entre les 2 suites de mots
 * 
 * puis, on peut associer un tag a l'une des suites de mot (par exemple, un boolean JUSTE ou FAUX)
 * et recuperer ainsi le/les tag(s) de la suite de mot d'origine
 * 
 * @author xtof
 *
 */
public class SuiteDeMots {
	float[] tags=null;
	String[] mots;
	/*
	 * contient les liens de chaque mot vers les mots de targetLink. Codage:
	 * - Nb de mots dans targetLink correspondant au mot courant
	 * - Liste des indices des mots dans targetLink
	 */
	ArrayList<Integer> linkedTo;
	SuiteDeMots targetLink=null;

	public SuiteDeMots(String[] ss) {
		mots = ss;
	}

	public String[] getMots() {
		return mots;
	}
	
	public void append(SuiteDeMots s2) {
		int ntot = mots.length+s2.mots.length;
		String[] newmots = new String[ntot];
		System.arraycopy(mots, 0, newmots, 0, mots.length);
		System.arraycopy(s2.mots, 0, newmots, mots.length, s2.mots.length);
		if (tags!=null||s2.tags!=null) {
			float[] newtags = new float[ntot];
			if (tags!=null)
				System.arraycopy(tags, 0, newtags, 0, tags.length);
			if (s2.tags!=null)
				System.arraycopy(s2.tags, 0, newtags, mots.length, s2.tags.length);
			tags=newtags;
		}
		mots=newmots;
	}

	/**
	 * retourne les tags DE LA SUITE DE MOTS LIEE correspondant au mot "mot" DE LA SUITE THIS !
	 * @param mot
	 * @return
	 */
	public float[] getLinkedTags(int mot) {
		int posInList = 0;
		int curmot = 0;
		while (curmot<mot) {
			posInList += linkedTo.get(posInList)+1;
			curmot++;
		}
		int nMotsLinked = linkedTo.get(posInList);
		float[] rtags = new float[nMotsLinked];
		for (int k=0, j=posInList+1;k<nMotsLinked;j++,k++) {
			rtags[k] = targetLink.tags[linkedTo.get(j)];
		}
		return rtags;
	}
	public float getThisTag(int mot) {
		if (tags==null) return -Float.MAX_VALUE;
		return tags[mot];
	}
	public SuiteDeMots getLinkedSuite() {return targetLink;}

	public void setTag(int i, float t) {
		if (tags==null) tags = new float[mots.length];
		tags[i]=t;
	}

	public SuiteDeMots(String s) {
		mots = s.split(" ");
	}

	public int getNmots() {
		return mots.length;
	}
	public String getMot(int i) {
		return mots[i];
	}

	final static String digits[] = {"zéro","un","deux","trois","quatre","cinq","six","sept","huit","neuf"};
	public static String normalizeNumbers(String s) {
		/*
		 * si le mot n'est composé que de chiffres, on le traite, sinon, on le laisse
		 */
		s=s.trim();
		for (int i=0;i<s.length();i++)
			if (!Character.isDigit(s.charAt(i))) {
				return s;
			}
		if (s.length()>7) {
			System.err.println("warning: chiffre trop long: non traite ! "+s);
			return s;
		}
		String r="";
		int pos=0;
		if (s.length()>=7) {
			int i = Integer.parseInt(""+s.charAt(pos));
			if (i>1)
				r=digits[i]+" millions ";
			else
				r=digits[i]+" million ";
			pos++;
		}
		if (s.length()>=6) {
			int i = Integer.parseInt(""+s.charAt(pos));
			if (i>1) r+=digits[i]+" cent ";
			else if (i==1) r+="cent ";
			pos++;
		}
		if (s.length()>=5) {
			String rdiz = dizaines(s.substring(pos,pos+2));
			if (rdiz.length()>0)
				r+=rdiz+"mille ";
			pos+=2;
		} else if (s.length()==4) {
			int i = Integer.parseInt(""+s.charAt(pos));
			switch (i) {
			case 0: break;
			case 1: r+="mille "; break;
			default: r+=digits[i]+" mille ";
			}
			pos++;
		}
		if (s.length()>=3) {
			int i = Integer.parseInt(""+s.charAt(pos));
			switch (i) {
			case 0: break;
			case 1: r+="cent "; break;
			default: r+=digits[i]+" cent ";
			}
			pos++;
		}
		if (s.length()>=2) {
			String rdiz = dizaines(s.substring(pos,pos+2));
			if (rdiz.length()>0)
				r+=rdiz;
			pos+=2;
		} else if (s.length()==1) {
			int i = Integer.parseInt(""+s.charAt(pos));
			switch (i) {
			case 0: break;
			default: r+=digits[i]+" ";
			}
			pos++;
		}
		return r;
	}
	private static String dizaines(String s) {
		int d = Integer.parseInt(""+s.charAt(0));
		switch(d) {
		case 0:
			if (s.charAt(1)=='0') return "";
			return digits[Integer.parseInt(""+s.charAt(1))];
		case 1:
			int u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "dix ";
			case 1: return "onze ";
			case 2: return "douze ";
			case 3: return "treize ";
			case 4: return "quatorze ";
			case 5: return "quinze ";
			case 6: return "seize ";
			case 7: return "dix sept ";
			case 8: return "dix huit ";
			case 9: return "dix neuf ";
			}
		case 2:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "vingt ";
			case 1: return "vingt et un ";
			default: return "vingt "+digits[u];
			}			
		case 3:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "trente ";
			case 1: return "trente et un ";
			default: return "trente "+digits[u];
			}			
		case 4:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "quarante ";
			case 1: return "quarante et un ";
			default: return "quarante "+digits[u];
			}			
		case 5:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "cinquante ";
			case 1: return "cinquante et un ";
			default: return "cinquante "+digits[u];
			}			
		case 6:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "soixante ";
			case 1: return "soixante et un ";
			default: return "soixante "+digits[u];
			}			
		case 7:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "soixante dix ";
			case 1: return "soixante et onze ";
			case 2: return "soixante douze ";
			case 3: return "soixante treize ";
			case 4: return "soixante quatorze ";
			case 5: return "soixante quinze ";
			case 6: return "soixante seize ";
			case 7: return "soixante dix sept ";
			case 8: return "soixante dix huit ";
			case 9: return "soixante dix neuf ";
			}			
		case 8:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "quatre vingt ";
			default: return "quatre vingt "+digits[u];
			}			
		case 9:
			u = Integer.parseInt(""+s.charAt(1));
			switch(u) {
			case 0: return "quatre vingt dix ";
			case 1: return "quatre vingt onze ";
			case 2: return "quatre vingt douze ";
			case 3: return "quatre vingt treize ";
			case 4: return "quatre vingt quatorze ";
			case 5: return "quatre vingt quinze ";
			case 6: return "quatre vingt seize ";
			case 7: return "quatre vingt dix sept ";
			case 8: return "quatre vingt dix huit ";
			case 9: return "quatre vingt dix neuf ";
			}			
		}
		return null;
	}
	
	public static String normalizeWord(String w) {
		if (w.trim().length()>0 && w.trim().charAt(0)=='<') return "";
		// on met tout en minuscule
		String s = w.toLowerCase();
		s=" "+s+" ";
		s=s.replace('_',' ');
		s=s.replace('-',' ');
		s=s.replace("'", "' ");
		s=s.replace("aujourd' hui","aujourd'hui");
		s=s.replace('=', ' ');
		s=s.replaceAll(" %hesitation "," ");
		s=s.replaceAll(" euh "," ");
		s=s.replaceAll(" hh "," ");
		s=s.replaceAll(" xx "," ");
		s=s.replaceAll(" bb "," ");
		s=s.replaceAll("<s> ","");
		s=s.replaceAll("<sil> ","");
		s=s.replaceAll(" </s>","");
		s=s.replaceAll("  +"," ");
		// on transforme tous les nombres en ecriture avec des lettres
		s=normalizeNumbers(s);
		s=s.trim();
		return s;
	}
	
	/**
	 * normalise la suite;
	 * ne modifie pas la suite d'origine, mais place la suite normalisee dans linkedTo
	 * ne recopie pas les tags d'une suite a l'autre
	 * @return
	 */
	public SuiteDeMots normaliseForAccuracy() {
		// nouvelle suite de mots:
		ArrayList<String> newsuite = new ArrayList<String>();
		// liens des anciens vers les nouveaux mots:
		linkedTo = new ArrayList<Integer>();
		// liens des nouveaux mots vers les anciens:
		ArrayList<Integer> linkedFrom = new ArrayList<Integer>();

		int curposInTarget=0;
		for (int i=0;i<mots.length;i++) {
			String s = normalizeWord(mots[i]);
			if (s.length()>0) {
				String[] ss = s.split(" ");
				linkedTo.add(ss.length);
				for (int j=0;j<ss.length;j++) {
					newsuite.add(ss[j]);
					linkedTo.add(curposInTarget++);
					linkedFrom.add(1);
					linkedFrom.add(i);
				}
			} else linkedTo.add(0);
		}
		String[] newsuites = new String[newsuite.size()];
		newsuite.toArray(newsuites);
		SuiteDeMots target = new SuiteDeMots(newsuites);
		target.linkedTo=linkedFrom;
		target.targetLink=this;
		targetLink=target;
		return target;
	}
	public SuiteDeMots normaliseForPrint() {
		// nouvelle suite de mots:
		ArrayList<String> newsuite = new ArrayList<String>();
		// liens des anciens vers les nouveaux mots:
		linkedTo = new ArrayList<Integer>();
		// liens des nouveaux mots vers les anciens:
		ArrayList<Integer> linkedFrom = new ArrayList<Integer>();

		int curposInTarget=0;
		for (int i=0;i<mots.length;i++) {
			// on met tout en minuscule
			String s = mots[i].toLowerCase();
			s=" "+s+" ";
			s=s.replace('_',' ');
			s=s.replace('=', ' ');
			s=s.replaceAll(" %hesitation "," ");
			s=s.replaceAll(" euh "," ");
			s=s.replaceAll(" hh "," ");
			s=s.replaceAll(" xx "," ");
			s=s.replaceAll(" bb "," ");
			s=s.replaceAll("<s> ","");
			s=s.replaceAll(" </s>","");
			s=s.replaceAll("  +"," ");
			// on transforme tous les nombres en ecriture avec des lettres
			//			s=normalizeNumbers(s);
			s=s.trim();
			if (s.length()>0) {
				String[] ss = s.split(" ");
				linkedTo.add(ss.length);
				for (int j=0;j<ss.length;j++) {
					newsuite.add(ss[j]);
					linkedTo.add(curposInTarget++);
					linkedFrom.add(1);
					linkedFrom.add(i);
				}
			} else linkedTo.add(0);
		}
		String[] newsuites = new String[newsuite.size()];
		newsuite.toArray(newsuites);
		SuiteDeMots target = new SuiteDeMots(newsuites);
		target.linkedTo=linkedFrom;
		target.targetLink=this;
		targetLink=target;
		return target;
	}

	/**
	 * aligne la suite de mots courante avec la 2eme suite de mots,
	 * en creant des liens entre les 2 suites de mots
	 */
	public void align(SuiteDeMots second) {
		targetLink = second;
		JDiff diff = new JDiff(mots,second.mots);
		JDiff.change c = diff.diff_2(false);
		ArrayList<Integer> align = JDiff.getAlignement(c);
		linkedTo = new ArrayList<Integer>();
		int pos1=0, pos2=0;
		for (int i=0;i<align.size();i++) {
			switch (align.get(i)) {
			case 0: linkedTo.add(1); linkedTo.add(pos2++); pos1++; break;
			case 1: linkedTo.add(0); pos1++; break;
			case 2: pos2++; break;
			case 3: linkedTo.add(1); linkedTo.add(pos2++); pos1++; break;
			}
		}
		for (;pos1<mots.length;pos1++) {
			linkedTo.add(1);
			linkedTo.add(pos2++);
		}
	}
	public boolean isMatch(int mot) {
		int[] w = getLinkedWords(mot);
		if (w.length==0) return false;
		for (int i=0;i<w.length;i++) {
			if (getMot(mot).equals(getLinkedSuite().getMot(w[i])))
				return true;
		}
		return false;
	}
	public int[] getLinkedWords(int mot) {
		int posInList = 0;
		int curmot = 0;
		while (curmot<mot) {
			posInList += linkedTo.get(posInList)+1;
			curmot++;
		}
		int nMotsLinked = linkedTo.get(posInList);
		int[] w = new int[nMotsLinked];
		for (int k=0, j=posInList+1;k<nMotsLinked;j++,k++) {
			w[k] = linkedTo.get(j);
		}
		return w;

	}
	public void printLinks() {
		int poslink=0;
		for (int i=0;i<mots.length;i++) {
			String s = "";
			int n = linkedTo.get(poslink++);
			for (int j=0;j<n;j++) {
				int m = linkedTo.get(poslink++);
				s+=targetLink.mots[m]+" ";
			}
			System.out.println(i+" "+mots[i]+" === "+s);
		}
		System.out.println();
	}

	/**
	 * projette les tags de la suite courante vers les tags de la suite liee
	 * (ecrase les tags de la suite liee)
	 */
	public void projectTagsToLinkedSuite() {
		int pos=0;
		for (int i=0;i<getNmots();i++) {
			float t = getThisTag(i);
			int nmots = linkedTo.get(pos++);
			for (int j=0;j<nmots;j++) {
				int idx = linkedTo.get(pos++);
				targetLink.setTag(idx, t);
			}
		}
	}

	public String toString() {
		return toString(false);
	}
	public String toString(boolean withTags) {
		if (mots==null||mots.length==0) return "";
		if (tags==null) {
			tags = new float[mots.length];
		}
		String r = "";
		if (withTags)
			for (int i=0;i<mots.length;i++)
				r+=mots[i]+"("+tags[i]+") ";
		else {
			r=mots[0];
			for (int i=1;i<mots.length;i++)
				if (mots[i-1].endsWith("'"))
					r+=mots[i];
				else
					r+=" "+mots[i];
		}
		return r;
	}
	public static SuiteDeMots parse(String s) {
		ArrayList<String> m = new ArrayList<String>();
		ArrayList<Float> tag = new ArrayList<Float>();
		int debmot = 0,finmot;
		while (debmot<s.length()) {
			finmot = s.indexOf('(',debmot+1);
			String mm = s.substring(debmot,finmot);
			m.add(mm);
			debmot = s.indexOf(')',finmot+1);
			Float t = Float.parseFloat(s.substring(finmot+1,debmot));
			tag.add(t);
			debmot+=2;
		}
		String[] mm = new String[m.size()];
		m.toArray(mm);
		SuiteDeMots mots = new SuiteDeMots(mm);
		for (int i=0;i<mm.length;i++)
			mots.setTag(i, tag.get(i));
		return mots;
	}
	public static void load2txt(String nom) {
		try {
			BufferedReader f = new BufferedReader(new FileReader(nom));
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				SuiteDeMots mots = parse(s);
				mots = mots.normaliseForPrint();
				System.out.println(mots.toString(false));
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws IOException {
		if (args[0].equals("-2txt")) {
			SuiteDeMots.load2txt(args[1]);
		} else {
			System.out.println("entrez une phrase, affiche la phrase normalisee pour l'accuracy");
			BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
			for (;;) {
				String s = f.readLine();
				if (s==null||s.length()==0) break;
				String[] ss = s.split(" ");
				SuiteDeMots m = new SuiteDeMots(ss);
				SuiteDeMots mm = m.normaliseForAccuracy();
				System.out.println(mm.toString());
			}
		}
	}
}
