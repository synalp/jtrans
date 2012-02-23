/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import utils.SuiteDeMots;

import main.RecoWord;

import edu.cmu.sphinx.util.NISTAlign;

public class SpeechRecoAccuracy {

	NISTAlign aligner = new NISTAlign(true, true);

	public void printAccuracy(String rec, String ref) {
		aligner.align(ref, rec);
		aligner.printNISTSentenceSummary();
	}
	public void calcAccuracy(String rec, String ref) {
		aligner.align(ref, rec);
	}

	public void printAccuracy() {
		aligner.printNISTTotalSummary();
		aligner.printTotalSummary();
	}

	public static List<String> normalize(List<String> mots) {
		ArrayList<String> res = new ArrayList<String>();
		for (String mot : mots) {
			res.add(normalizeMot(mot));
		}
		return res;
	}
	/**
	 * 
	 * @param mots
	 * @param decoup if true, remove empty words and split multi-words
	 */
	public static List<RecoWord> normalizeMots(List<RecoWord> mots, boolean decoup) {
		for (RecoWord mot : mots) {
			String s = normalizeMot(mot.word);
			mot.word=s;
		}

		if (decoup) {
			// supprime les mots "vides" et decoupe les groupes de mots
			ArrayList<RecoWord> rec2 = new ArrayList<RecoWord>();
			for (RecoWord w : mots) {
				if (w.word.length()==0) continue;
				String[] ss = w.word.split(" ");
				if (ss.length>1) {
					for (String ww : ss) {
						RecoWord rw = new RecoWord(ww, w.conf);
						rec2.add(rw);
					}
				} else rec2.add(w);
			}
			return rec2;
		} else return mots;
	}

	public static String normalizeMot(String mot) {
		final String accents = "çéèêëàâäïîöôûü";
		if (mot.charAt(0)=='<') return "";
		if (mot.equals("sp")) return "";
		String s=mot;
		s=s.toLowerCase();
		s=s.replace("'", "' ");
		s=s.replaceAll("[^0-9a-z"+accents+"']"," ");
		s=s.replaceAll("  +"," ");
		return s.trim();
	}

	public static String normalize(String phrase) {
		StringBuilder sb = new StringBuilder();
		String[] ss = phrase.split(" ");
		for (String s : ss) {
			sb.append(normalizeMot(s)+" ");
		}
		return sb.toString().trim();
	}

	public static String loadFromLab(String labfile) {
		try {
			BufferedReader f = FileUtils.openFileUTF(labfile);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				sb.append(ss[ss.length-1]+" ");
			}
			return normalize(sb.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void confidenceDET(DET det, String ctmfile, String labfile) {
		try {
			BufferedReader f=FileUtils.openFileISO(ctmfile);
			List<RecoWord> rec = new ArrayList<RecoWord>();
			for (;;) {
				String s= f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				RecoWord w = new RecoWord(ss[ss.length-2], Double.parseDouble(ss[ss.length-1]));
				rec.add(w);
			}
			f.close();
			rec = normalizeMots(rec, true);

			String ref = loadFromLab(labfile);
			String[] ss = new String[rec.size()];
			for (int i=0;i<ss.length;i++) ss[i]=rec.get(i).word;
			SuiteDeMots srec = new SuiteDeMots(ss);
			SuiteDeMots sref = new SuiteDeMots(ref);
			srec.align(sref);

			System.out.println("HYP: "+srec.toString(false));
			System.out.println("REF: "+sref.toString(false));
			for (int i=0;i<ss.length;i++) {
				int[] corr = srec.getLinkedWords(i);
				boolean good = false;
				for (int ww : corr) {
					if (ss[i].equals(sref.getMot(ww))) {good=true; break;}
				}
				System.out.println("\t MOT "+srec.getMot(i)+" "+good+" "+rec.get(i).conf);
				det.updateExample(good, (float)rec.get(i).conf);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String loadJuliusReco(String ctmfile) {
		try {
			BufferedReader f=FileUtils.openFileISO(ctmfile);
			StringBuilder sb = new StringBuilder();
			for (;;) {
				String s= f.readLine();
				if (s==null) break;
				String[] ss = s.split(" ");
				sb.append(ss[ss.length-2]+" ");
			}
			f.close();
			return sb.toString().trim();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static float combineWERs(String[] fichs) {
		try {
			ArrayList<Integer> corrects = new ArrayList<Integer>();
			ArrayList<Integer> subs = new ArrayList<Integer>();
			ArrayList<Integer> dels = new ArrayList<Integer>();
			ArrayList<Integer> inss = new ArrayList<Integer>();
			ArrayList<Integer> errs = new ArrayList<Integer>();
			for (String s: fichs) {
				BufferedReader f = FileUtils.openFileUTF(s);
				for (;;) {
					String ss = f.readLine();
					if (s==null) break;
					if (ss.startsWith("WORD RECOGNITION PERFORMANCE:")) {
						{
							ss = f.readLine();
							StringTokenizer st = new StringTokenizer(ss);
							String sss = "";
							while (st.hasMoreTokens()) sss=st.nextToken();
							sss=sss.replace(')', ' ').trim();
							int n = Integer.parseInt(sss);
							corrects.add(n);
						}
						{
							ss = f.readLine();
							StringTokenizer st = new StringTokenizer(ss);
							String sss = "";
							while (st.hasMoreTokens()) sss=st.nextToken();
							sss=sss.replace(')', ' ').trim();
							int n = Integer.parseInt(sss);
							subs.add(n);
						}
						{
							ss = f.readLine();
							StringTokenizer st = new StringTokenizer(ss);
							String sss = "";
							while (st.hasMoreTokens()) sss=st.nextToken();
							sss=sss.replace(')', ' ').trim();
							int n = Integer.parseInt(sss);
							dels.add(n);
						}
						{
							ss = f.readLine();
							StringTokenizer st = new StringTokenizer(ss);
							String sss = "";
							while (st.hasMoreTokens()) sss=st.nextToken();
							sss=sss.replace(')', ' ').trim();
							int n = Integer.parseInt(sss);
							inss.add(n);
						}
						{
							ss = f.readLine();
							StringTokenizer st = new StringTokenizer(ss);
							String sss = "";
							while (st.hasMoreTokens()) sss=st.nextToken();
							sss=sss.replace(')', ' ').trim();
							int n = Integer.parseInt(sss);
							errs.add(n);
						}
						break;
					}
				}
				f.close();
			}

			System.out.println("found "+errs.size()+" files");
			int nreftot=0, nsubstot=0, ndelstot=0, ninstot=0;
			for (int i=0;i<errs.size();i++) {
				int nref = corrects.get(i)+subs.get(i)+dels.get(i);
				nreftot += nref;
				nsubstot += subs.get(i);
				ndelstot += dels.get(i);
				ninstot += inss.get(i);
			}
			float wer = (float)(nsubstot+ndelstot+ninstot)/(float)nreftot;
			System.out.println("N="+nreftot+" S="+nsubstot+" D="+ndelstot+" I="+ninstot+" WER="+wer);
			return wer;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Float.NaN;
	}

	public static void calcConfidenceEER(String ctmlist) {
		try {
			SpeechRecoAccuracy m = new SpeechRecoAccuracy();
			DET det = new DET();

			BufferedReader f;
			f = FileUtils.openFileUTF(ctmlist);
			for (;;) {
				String s = f.readLine();
				if (s==null) break;
				String labfile = FileUtils.noExt(s)+".lab";
				String ref = loadFromLab(labfile);
				String rec = loadJuliusReco(s);
				rec = normalize(rec);
				m.printAccuracy(rec, ref);
				confidenceDET(det, s, labfile);
			}
			m.printAccuracy();
			System.out.println("EER="+det.computeEERold());
			det.showDET2();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("combine wers");
		String resfiles = args[0];
		BufferedReader f = FileUtils.openFileUTF(resfiles);
		ArrayList<String> resf = new ArrayList<String>();
		for (;;) {
			String s = f.readLine();
			if (s==null) break;
			resf.add(s);
		}
		f.close();

		combineWERs(resf.toArray(new String[resf.size()]));
	}
}
