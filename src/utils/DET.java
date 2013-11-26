/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class DET {
	
	public static boolean withX = true;
	
	ArrayList<Float> scores = new ArrayList<Float>();
	ArrayList<Boolean> isgood = new ArrayList<Boolean>();
	public void updateExample(boolean isGood, float score) {
		scores.add(score);
		isgood.add(isGood);
	}
	public int getNex() {return scores.size();}
	/**
	 * nouvelle version beaucoup plus rapide
	 * 
	 * @return
	 */
	public float computeEER() {
		float smin=scores.get(0), smax=scores.get(0);
		for (int i=1;i<scores.size();i++) {
			if (scores.get(i)>smax) smax=scores.get(i);
			else if (scores.get(i)<smin) smin=scores.get(i);
		}
		// divise en 10 parties
		float seuilgauche=smin, seuildroit=smax;
		float frt=0,fat=0;
		while (smin<smax) {
			float sdelta = (smax-smin)/10f;
			for (float seuil=smin+sdelta;seuil<smax;seuil+=sdelta) {
				int FA=0, FR=0, TA=0, TR=0;
				for (int i=0;i<scores.size();i++) {
					if (scores.get(i)<seuil) {
						if (isgood.get(i)) FR++;
						else TR++;
					} else {
						if (isgood.get(i)) TA++;
						else FA++;
					}
				}
				frt = (float)FR/(float)(TA+FR);
				fat = (float)FA/(float)(TR+FA);
				if (frt<fat) seuilgauche = seuil;
				else {
					seuildroit = seuil;
					// inutile d'aller plus loin
					break;
				}
			}
			System.out.println("smin "+smin+" smax "+smax+" "+frt+" "+fat);
			if (Math.abs(fat-frt)<0.001) break;
			smin=seuilgauche; smax=seuildroit;
		}
		return (fat+frt)/2;
	}
	public float computeEERold() {
		float frgauche=-Float.MAX_VALUE, frdroit=Float.MAX_VALUE, fagauche=-Float.MAX_VALUE, fadroit=Float.MAX_VALUE;
		
		class Item implements Comparable<Item> {
			float sc;
			boolean isGood;
			public int compareTo(Item it) {
				if (sc>it.sc) return 1;
				else if (sc<it.sc) return -1;
				return 0;
			}
		}
		ArrayList<Item> items = new ArrayList<Item>();
		for (int i=0;i<scores.size();i++) {
			Item it = new Item();
			it.sc=scores.get(i);
			it.isGood=isgood.get(i);
			items.add(it);
		}
		Collections.sort(items);

		System.out.println("sorted collection "+items.size());
		System.out.println("collection min "+items.get(0).sc);
		System.out.println("collection max "+items.get(items.size()-1).sc);

		float sc=Float.NaN;
		for (int i=0;i<items.size();i++) {
			// on rejette jusqu'a l'exemple i inclu et tous ceux qui ont le meme score !
			Item it=items.get(i);
			if (it.sc==sc) continue;
			sc=it.sc;
			it=null;
			int FA=0, FR=0, TA=0, TR=0;
			for (int j=0;j<=i;j++) {
				it = items.get(j);
				if (it.isGood) FR++;
				else TR++;
			}
			for (int j=i+1;j<items.size();j++) {
				it = items.get(j);
				if (it.isGood) TA++;
				else FA++;
			}
			float frt = (float)FR/(float)(TA+FR);
			float fat = (float)FA/(float)(TR+FA);
			if (frt<fat) {
				// on est a gauche du EER
				if (frt>frgauche) {
					frgauche=frt;
					fagauche=fat;
				}
			} else {
				// on est a droite du EER
				if (frt<frdroit) {
					frdroit=frt;
					fadroit=fat;
					break; // inutile d'aller plus loin
				}
			}
		}
		
		// interpolation lineaire entre les points gauche et droit
		/*
		 * a frg + b = fag
		 * a frd + b = fad
		 * a (frd-frg) = (fad-fag)
		 * b = fag - a * frg
		 */

		System.out.println("interpol: point gauche: "+frgauche+" "+fagauche);
		System.out.println("interpol: point droit: "+frdroit+" "+fadroit);
		float diffx = (frdroit-frgauche);
		if (diffx==0) {
			// verticale ! x=frdroit, donc l'intersection est y=frdroit
			return frdroit;
		} else {
			float a = (fadroit-fagauche)/diffx;
			float b = fagauche - a * frgauche;
			System.out.println("interpol a b "+a+" "+b);
			/*
			 * on cherche le point de cette droite tel que ax+b=x
			 * (1-a)x=b
			 */
			float EER = b/(1f-a);
			return EER;
		}
	}
	public void showDET() {
		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/tt"));
			float scmin = scores.get(0);
			float scmax = scmin;
			for (int i=0;i<scores.size();i++)
				if (scmin>scores.get(i)) scmin=scores.get(i);
				else if (scmax<scores.get(i)) scmax=scores.get(i);
			float delta = (scmax-scmin)/50f;
			System.err.println("seuils min/max "+scmin+" "+scmax+" "+delta+" nex "+scores.size());
			scmin -= delta;
			scmax += delta;
			float seuil = scmin;
			while (seuil<=scmax) {
				int FA=0, FR=0, TA=0, TR=0;
				for (int i=0;i<scores.size();i++) {
					float r = scores.get(i);
					if (isgood.get(i)) {
						if (r>seuil) TA++;
						else {
							FR++;
						}
					} else {
						if (r>seuil) FA++;
						else TR++;
					}
				}
				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				f.println(frt+" "+fat);
				seuil+=delta;
			}
			f.println();
			f.println("0 1");
			f.println("1 0");
			f.close();
			f = new PrintWriter(new FileWriter("/tmp/ttt"));
			f.println("plot \"/tmp/tt\" notitle with lines");
			f.close();
			Runtime.getRuntime().exec("gnuplot /tmp/ttt -");
			BufferedReader ff = new BufferedReader(new InputStreamReader(System.in));
			ff.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * je trace tous les seuils entre tous les points pour avoir la meilleure courbe possible !
	 */
	public void showDET2() {
		if (!withX) return;
		class Item implements Comparable<Item> {
			float sc;
			boolean isGood;
			public int compareTo(Item it) {
				if (sc>it.sc) return 1;
				else if (sc<it.sc) return -1;
				return 0;
			}
		}
		ArrayList<Item> items = new ArrayList<Item>();
		for (int i=0;i<scores.size();i++) {
			Item it = new Item();
			it.sc=scores.get(i);
			it.isGood=isgood.get(i);
			items.add(it);
		}
		Collections.sort(items);

		try {
			PrintWriter f = new PrintWriter(new FileWriter("/tmp/tt"));
			float sc=Float.NaN;
			for (int i=0;i<items.size();i++) {
				// on rejette jusqu'a l'exemple i inclu et tous ceux qui ont le meme score !
				Item it=items.get(i);
				if (it.sc==sc) continue;
				sc=it.sc;
				it=null;
				int FA=0, FR=0, TA=0, TR=0;
				for (int j=0;j<=i;j++) {
					it = items.get(j);
					if (it.isGood) FR++;
					else TR++;
				}
				for (int j=i+1;j<items.size();j++) {
					it = items.get(j);
					if (it.isGood) TA++;
					else FA++;
				}
				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				f.println(frt+" "+fat);
				System.out.println("FR="+FR+" TR="+TR+" FA="+FA+" TA="+TA+" frt="+frt+" fat="+fat+ " thr="+sc);
			}

			f.println();
			f.println("0 1");
			f.println("1 0");
			f.close();
			f = new PrintWriter(new FileWriter("/tmp/ttt"));
			f.println("plot \"/tmp/tt\" notitle with lines");
			f.close();
			Runtime.getRuntime().exec("gnuplot /tmp/ttt -");
			BufferedReader ff = new BufferedReader(new InputStreamReader(System.in));
			ff.readLine();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws IOException {
		// load un ARFF file et affiche la DET
		for (int z=0;z<args.length;z++) {
			PrintWriter fout = new PrintWriter(new FileWriter("/tmp/tt"+z));
			BufferedReader f = new BufferedReader(new FileReader(args[z]));
			int FRpos=-1, FApos=-1, Tpos=-1, TRpos=-1, TApos=-1;
			int attpos=0;
			for (;;) {
				String s = f.readLine();
				if (s.indexOf("@data")>=0) break;
				if (s.indexOf("@attribute")>=0) {
					if (s.indexOf("False Negatives")>=0) FRpos=attpos;
					if (s.indexOf("False Positives")>=0) FApos=attpos;
					if (s.indexOf("True Positives")>=0) TApos=attpos;
					if (s.indexOf("True Negatives")>=0) TRpos=attpos;
					if (s.indexOf("Threshold")>=0) Tpos=attpos;
					attpos++;
				}
			}
			if (FRpos==-1||FApos==-1||Tpos==-1||TRpos==-1||TApos==-1) {
				System.err.println("ERROR: FR/FA/thres non trouves !");
				return;
			}
			float frgauche=-Float.MAX_VALUE, frdroit=Float.MAX_VALUE, fagauche=-Float.MAX_VALUE, fadroit=Float.MAX_VALUE;
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] ss = s.split(",");
				float FR = Float.parseFloat(ss[FRpos]);
				float FA = Float.parseFloat(ss[FApos]);
				float TR = Float.parseFloat(ss[TRpos]);
				float TA = Float.parseFloat(ss[TApos]);

				float frt = (float)FR/(float)(TA+FR);
				float fat = (float)FA/(float)(TR+FA);
				fout.println(frt+" "+fat);

				if (frt<fat) {
					// on est a gauche du EER
					if (frt>frgauche) {
						frgauche=frt;
						fagauche=fat;
					}
				} else if (frdroit==Float.MAX_VALUE) {
					// on est a droite du EER
					if (frt<frdroit) {
						frdroit=frt;
						fadroit=fat;
					}
				}

			}
			System.out.println("interpol: point gauche: "+frgauche+" "+fagauche);
			System.out.println("interpol: point droit: "+frdroit+" "+fadroit);
			float diffx = (frdroit-frgauche);
			if (diffx==0) {
				// verticale ! x=frdroit, donc l'intersection est y=frdroit
				System.out.println("EER= "+frdroit);
			} else {
				float a = (fadroit-fagauche)/diffx;
				float b = fagauche - a * frgauche;
				System.out.println("interpol a b "+a+" "+b);
				/*
				 * on cherche le point de cette droite tel que ax+b=x
				 * (1-a)x=b
				 */
				float EER = b/(1f-a);
				System.out.println("EER= "+EER);
			}

			f.close();
			fout.println("0 1");
			fout.println("1 0");
			fout.close();
		}
		PrintWriter ff = new PrintWriter(new FileWriter("/tmp/ttt"));
		ff.print("plot ");
		int z;
		for (z=0;z<args.length-1;z++) {
			ff.print("\"/tmp/tt"+z+"\" title \""+args[z]+"\" with lines,");
		}
		ff.print("\"/tmp/tt"+z+"\" title \""+args[z]+"\" with lines");
		ff.println();
		ff.close();
		Runtime.getRuntime().exec("gnuplot /tmp/ttt -");
		BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
		f.readLine();
	}
	
}
