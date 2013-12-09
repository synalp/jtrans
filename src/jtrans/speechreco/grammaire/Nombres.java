package jtrans.speechreco.grammaire;

import java.io.BufferedReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jtrans.utils.FileUtils;

/**
 * classe qui gère la phonétisation des nombres
 * 
 * @author xtof
 *
 */
public class Nombres implements Serializable {
	ArrayList<String> var = new ArrayList<String>();
	ArrayList<String> var2rule = new ArrayList<String>();
	ArrayList<String> chars = new ArrayList<String>();
	ArrayList<String> chars2rule = new ArrayList<String>();
	
	private String segmentNormed = null;
	
	void load() {
		String sx = "res/Nbrules.txt";
		try {
			BufferedReader f = FileUtils.openFileUTF(sx);
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				s=s.trim();
				if (s.length()==0) continue;
				if (s.charAt(0)=='_') loadVar(s);
				else if (s.startsWith("%%")) continue; // commentaire
				else loadRule(s);
			}
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("nombres rules loaded "+chars2rule.size());
	}
	
	/**
	 * @param s
	 * @return une succession de segments; les segments qui ont été transformés et qu
	 * correspondent à des nombres sont précédés par un segment null
	 */
	public List<String> getRule(String s) {
		ArrayList<String> ressegs = new ArrayList<String>();
		
		int prevSegEnd=0;
		int[] endpos = {-1};
		for (int i=0;i<s.length();i++) {
			String x=match(s,i,endpos);
			if (x!=null) {
				// matche ! on cree un nouveau segment
				// on ajoute d'abord le segment precedent qui ne matchait pas (s'il y en a un)
				if (prevSegEnd<i) {
					ressegs.add(s.substring(prevSegEnd,i));
				}
				// puis on ajoute null pour dire que le segment qui suit est une rule
				ressegs.add(null);
				// puis enfin la rule
				ressegs.add(x);
				// on se positionne sur la suite à parser
				i=endpos[0]-1;
				prevSegEnd=i+1;
			} else {
				// ne matche pas: on continue
			}
		}
		// on ajoute le dernier segment eventuel qui ne matche pas
		if (prevSegEnd<s.length()) {
			ressegs.add(s.substring(prevSegEnd));
		}
		return ressegs;
	}
	
	/**
	 * retourne la position suivante, après la fin du match, ou -1
	 * De plus, modifie segmentNormed en y mettant le segment réécrit / normalisé
	 */
	private int match(String s, int pos, int ruleidx) {
		// stocke les correspondances des variables x au fur et a mesure qu'on les matche
		char[] correspondances = {'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'};
		
		String chars2match = chars.get(ruleidx);
		int spos=pos;
		int xidx=0;
		for (int i=0;i<chars2match.length();i++) {
			if (spos>=s.length()) return -1; // obs trop courtes pour matcher
			if (chars2match.charAt(i)=='x') {
				if (Character.isDigit(s.charAt(spos))) {
					correspondances[xidx++]=s.charAt(spos);
					spos++;
				} else return -1;
			} else {
				if (chars2match.charAt(i)==s.charAt(spos)) {
					spos++;
				} else return -1;
			}
		}
		// la règle matche !
		String partieDroite = chars2rule.get(ruleidx);
		String[] ss = partieDroite.split(" ");
		StringBuilder res = new StringBuilder();
		xidx=0;
		for (int i=0;i<ss.length;i++) {
			if (ss[i].equals("x")) {
				// on recupere la regle correspondant au chiffre x
				List<String> rulechiffre = getRule(""+correspondances[xidx++]);
				assert rulechiffre.size()==2;
				assert rulechiffre.get(0)==null;
				res.append(rulechiffre.get(1)+" ");
			} else {
				// on ne peut pas avoir de x entre []
				if (ss[i].equals("|")) xidx=0;
				res.append(ss[i]+" ");
			}
		}
		segmentNormed=res.toString();
		return spos;
	}
	
	/**
	 * retourne la regle-resultat ou null si aucun match
	 * 
	 */
	private String match(String s, int pos, int[] endpos) {
		int matchendmax=-1;
		int bestruleidx=-1;
		String bestres = null;
		for (int ruleidx=0;ruleidx<chars.size();ruleidx++) {
			int matchend = match(s,pos,ruleidx);
//System.out.println("debm "+ruleidx+" "+chars.get(ruleidx)+" "+matchend);
			if (matchend>matchendmax) {
				matchendmax=matchend;
				bestruleidx=ruleidx;
				bestres = segmentNormed;
			}
		}
		if (bestruleidx<0) return null;
		endpos[0] = matchendmax;
		return bestres;
	}
	
	void loadRule(String s) {
		int i=s.indexOf('=');
		assert i>0;
		String chs = s.substring(0,i).trim();
		String r = s.substring(i+1).trim();
		chars.add(chs);
		chars2rule.add(r);
	}
	
	void loadVar(String s) {
		int i=s.indexOf('=');
		assert i>0;
		String v = s.substring(0,i).trim();
		var.add(v);
		String r = s.substring(i+1).trim();
		var2rule.add(r);
	}
	
	public Nombres() {
		load();
	}
	
	public static void main(String args[]) {
		unittest();
	}
	
	private static void unittest() {
		Nombres n = new Nombres();
		String[] totest = {"1234","-56,4","128 218","08 57 43 32 22","4,562","-4.5%","1987"};
		for (String s : totest) {
			System.out.print("testing "+s+" : ");
			System.out.println(n.getRule(s));
		}
	}
}
