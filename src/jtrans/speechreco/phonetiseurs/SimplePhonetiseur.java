/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.phonetiseurs;

import java.io.Serializable;

/**
 * utilise une simple base de regles
 * 
 * @author cerisara
 *
 */
public class SimplePhonetiseur implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Les phonemes utilises en sortie sont les suivants:
	 */
	final public static String[] phones = {"a","an","b","bb","d","e",
		"E","eh","eu","euf","f","g","H","hh","i","in","j","J","k","l",
		"m","n","o","O","oh","on","p","R","s","S","sil","swa","t","u",
		"v","w","xx","y","z","Z"};

	// base de règles par ordre de priorité
	final static String[] rules = {
		"an=an", "en=an", "on=on", "in=in", "ain=in", "ein=in",
		"qu=k", "ch=( S | k )", "ou=u", "tt=t", "ll=( l | j )",
		"eu=eu", "eau=o", "au=o", "ee=i",
		
		// lettre seules
		"a=a", "e=eu", "i=i", "o=o", "u=y", "j=Z", "y=i",
		"t=t", "p=p", "r=R", "s=s", "q=k", "b=b", "c=( s | k )", "d=d",
		"f=f", "g=g", "k=k", "l=l", "m=m", "n=n", "v=v", "w=v", "x=( g | k ) s",
		"z=z", "ç=s",
		"é=e","è=E","ê=E","ë=E",
		"â=a","à=a","ä=a",
		"ô=o","ö=o","î=i","ï=i","û=y","ü=y"
	};
	
	String[][] rules2;
	
	public SimplePhonetiseur() {
		rules2 = new String[rules.length][2];
		for (int i=0;i<rules.length;i++) {
			String[] ss = rules[i].split("=");
			rules2[i][0]=ss[0];
			rules2[i][1]=ss[1];
		}
	}
	
	private String applyRules(String mot) {
		if (mot.length()==0) return "";
		for (int i=0;i<rules2.length;i++) {
			if (mot.startsWith(rules2[i][0])) {
				return rules2[i][1]+" "+applyRules(mot.substring(rules2[i][0].length()));
			}
		}
		//pas de regles: on saute cette "lettre"
		return applyRules(mot.substring(1));
	}
	
	private String filter(String rule) {
		return rule;
	}
	public String getRule(String mot) {
		return filter(applyRules(mot.toLowerCase()));
	}
}
