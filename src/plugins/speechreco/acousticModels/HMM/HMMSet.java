/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.acousticModels.HMM;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import plugins.speechreco.acousticModels.HMM.stateModels.GMMDiag;

/**
 * - can load an HMM in HTK format. - supports ~s ~t ~h - can load a tied list
 * of HMMs => supports 3phones
 * 
 * @author cerisara
 */
public class HMMSet implements Serializable {
	private int ngauss;

	/**
	 * contains HMMState instances
	 * contains GMMDiag instances important: on a la garantie que l'ordre des
	 * GMMs est celui dans lequel ils apparaissent dans le fichier MMF !!
	 */
	public ArrayList<HMMState> etats = null;

	/**
	 * contains HMM instances
	 */
	public ArrayList<SingleHMM> hmms = null;

	/*
	 * indice du HMM dans le vector hmms
	 */
	private HashMap<String,Integer> tiedlist = new HashMap<String,Integer>();

	/*
	 * transitions inutile d'utiliser un HashMap, car la recherche n'est
	 * realisee que pendant la lecture du modele !
	 */
	ArrayList<float[][]> transPool = null;

	HashMap<String, Integer> transNomTmp = null;
	boolean isMono=false, isTri=false;

	void checkContextDependency() {
		Iterator<String> it = tiedlist.keySet().iterator();
		isTri=isMono=false;
		while (it.hasNext()) {
			String h = it.next();
			if (h.indexOf('-')>=0) {
				isTri=true; return;
			}
		}
		isMono=true;
	}
	public boolean isMonophone() {
		if (!isMono&&!isTri) checkContextDependency();
		return isMono;
	}
	public boolean isTriphone() {
		if (!isMono&&!isTri) checkContextDependency();
		return isTri;
	}
	
	public int getNstates() {
		return etats.size();
	}

	public int getNstates(int state) {
		return etats.size();
	}

	public void addNewTiedHMM(String nomVirtuel, String nomExistant) {
		int id = tiedlist.get(nomExistant);
		tiedlist.put(nomVirtuel,id);
	}
	public void addNewHMM(SingleHMM hmm) {
		tiedlist.put(hmm.getNom(),hmms.size());
		hmms.add(hmm);
		for (int i = 0; i < hmm.getNstates(); i++) {
			if (hmm.isEmitting(i)) {
				HMMState s = hmm.getState(i);
				etats.add(s);
			}
		}
		transPool.add(hmm.trans);
	}

	public String[] getHMMnames() {
		String[] rep = new String[hmms.size()];
		for (int i = 0; i < rep.length; i++) {
			SingleHMM h = hmms.get(i);
			rep[i] = h.getNom();
		}
		return rep;
	}

	public int getNhmms() {
		return hmms.size();
	}

	public int getHMMIndex(SingleHMM h) {
		return hmms.indexOf(h);
	}

	public SingleHMM getHMM(int idx) {
		return hmms.get(idx);
	}

	public SingleHMM getHMM(String nom) {
		// il ne faut pas faire ce traitement ici, mais dans l'appelant !
		//if (nom.startsWith("QCQ")) nom=nom.substring(3);
		
		Integer idx = tiedlist.get(nom);
		if (idx==null) {
			System.out.println("ERROR getHMM not found "+nom);
			return null;
		} else {
			return (SingleHMM)hmms.get(idx);
		}
	}

	public HMMSet() {
		etats = new ArrayList<HMMState>();
		hmms = new ArrayList<SingleHMM>();
		transPool = new ArrayList<float[][]>();
	}

	public static HMMSet load(String nomFich) {
		HMMSet h = null;
		try {
			ObjectInputStream out = new ObjectInputStream(new FileInputStream(
					nomFich));
			h = (HMMSet) out.readObject();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return h;
	}

	public void loadHTK(String nomFich) {
		try {
			loadHTK(new FileInputStream(nomFich));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void loadHTK(InputStream fich) {
		transNomTmp = new HashMap<String, Integer>();
		try {
			BufferedReader f = new BufferedReader(new InputStreamReader(fich));
			String s;
			for (;;) {
				s = f.readLine();
				if (s == null)
					break;
				if (s.startsWith("~s")) {
					String nomEtat = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					loadEtat(f, nomEtat, null);
				} else if (s.startsWith("~t")) {
					String nomTrans = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					float[][] tr = loadTrans(f);
					transNomTmp.put(nomTrans, transPool.size());
					transPool.add(tr);
				} else if (s.startsWith("~h")) {
					String nomHMM = s.substring(s.indexOf('"') + 1, s.lastIndexOf('"'));
					tiedlist.put(nomHMM,hmms.size());
					hmms.add(loadHMMfromHTK(f, nomHMM, etats));
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		transNomTmp = null;
	}

	public float[][] loadTrans(BufferedReader f) {
		try {
			String s = f.readLine();
			if (!s.startsWith("<TRANSP>")) {
				throw new Error("ERROR in trans load " + s);
			}
			String[] ss = s.split(" ");
			int nstates = Integer.parseInt(ss[1]);
			float[][] trans = new float[nstates][nstates];
			for (int i = 0; i < nstates; i++) {
				s = f.readLine().trim();
				ss = s.split(" ");
				for (int j = 0; j < nstates; j++) {
					trans[i][j] = Float.parseFloat(ss[j]);
				}
			}
			return trans;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void loadTiedList(String nomFich) {
		try {
			BufferedReader f = new BufferedReader(new FileReader(nomFich));
			String s;
			String[] ss;
			f = new BufferedReader(new FileReader(nomFich));
			for (int i = 0;;) {
				s = f.readLine();
				if (s == null)
					break;
				ss = s.split(" ");
				if (ss.length >= 2) {
					// on a un tiedHMM !
					Integer targetidx = tiedlist.get(ss[1]);
					if (targetidx == null) {
						System.out.println("ERROR tiedlist "+s);
					} else {
						tiedlist.put(ss[0],targetidx);
					}
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO: a completer, car la structure HMM n'est pas lue pour l'instant
	 * 
	 * @param f
	 * @param n
	 * @param autresEtats
	 * @throws IOException
	 */
	private SingleHMM loadHMMfromHTK(BufferedReader f, String n, ArrayList<HMMState> autresEtats)
			throws IOException {
		int curstate;
		String nom = n;
		String s = "";
		while (!s.startsWith("<NUMSTATES>")) {
			s = f.readLine();
		}
		int nstates = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
		SingleHMM theHMM = new SingleHMM(nstates);
		theHMM.setNom(n);
		while (!s.startsWith("<STATE>"))
			s = f.readLine();
		while (s.startsWith("<STATE>")) {
			curstate = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
			s = f.readLine();
			HMMState e=null;
			if (s.startsWith("~s")) {
				String nomEtat = s.substring(s.indexOf('"') + 1, s
						.lastIndexOf('"'));
				int i;
				for (i = 0; i < autresEtats.size(); i++) {
					e = autresEtats.get(i);
					if (e.nom.equals(nomEtat))
						break;
				}
				if (i == autresEtats.size()) {
					System.out.println("erreur new hmm : etat " + nom
							+ " non trouve");
					System.exit(1);
				}
			} else {
				e=loadEtat(f, "", s);
			}
			e.lab=new Lab(nom, curstate);
			etats.add(e);
			theHMM.setState(curstate - 1, e); // -1 car dans HTK, les HMMs
												// comptent a partir de 1
			s = f.readLine();
			// on elimine le gconst car il est recalcule ensuite !
			if (s.startsWith("<GCONST>"))
				s = f.readLine();
		}
		if (s.startsWith("~t")) {
			// simple appel de macro
			String nomTrans = s.substring(s.indexOf('"') + 1, s
					.lastIndexOf('"'));
			int tridx = transNomTmp.get(nomTrans);
			theHMM.setTrans(transPool.get(tridx));
		} else {
			// les trans sont explicites
			if (!s.startsWith("<TRANSP>")) {
				System.out.println("erreur new hmm : pas de trans ? " + s);
				System.exit(1);
			}
			String[] ss;
			float[][] trans = new float[nstates][nstates];
			for (int i = 0; i < nstates; i++) {
				s = f.readLine().trim();
				ss = s.split(" ");
				for (int j = 0; j < nstates; j++) {
					trans[i][j] = Float.parseFloat(ss[j]);
				}
			}
			theHMM.setTrans(trans);
			transPool.add(trans);
		}
		s = f.readLine();
		if (!s.startsWith("<ENDHMM>")) {
			System.out.println("erreur new hmm : pas de fin ? " + s);
			System.exit(1);
		}
		return theHMM;
	}

	GMMDiag g;
	private HMMState loadEtat(BufferedReader f, String nomEtat, String prem)
			throws IOException {
		ngauss = 1;
		String s;
		if (prem != null)
			s = prem;
		else
			s = f.readLine().trim();
		if (s.startsWith("<NUMMIXES>")) {
			ngauss = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
			s = f.readLine().trim();
		}
		g = null;
		if (!s.startsWith("<MIXTURE>")) {
			// cas particulier a 1 mixture
			if (ngauss != 1) {
				System.out.println("erreur gmm loadHTK: n mixes " + ngauss
						+ " mais pas de mixture ! " + s);
				System.exit(1);
			}
			loadHTKGauss(f, 0, s);
			g.setWeight(0, 1f);
		} else {
			String[] ss;
			for (int i = 0; i < ngauss; i++) {
				if (i > 0)
					s = f.readLine().trim();
				if (s.startsWith("<GCONST>"))
					s = f.readLine().trim();
				ss = s.split(" ");
				if (Integer.parseInt(ss[1]) != i + 1) {
					System.out.println("erreur gmm loadHTK: mixture conflict "
							+ i + " " + s);
					System.exit(1);
				}
				loadHTKGauss(f, i, null);
				g.setWeight(i, Float.parseFloat(ss[2]));
			}
		}
		g.precomputeDistance();
		g.setNom(nomEtat);
		return g;
	}

	/***************************************************************************
	 * lit jusqu'a la derniere ligne d'une gauss, mais laisse la ligne suivante
	 * en place ! Il peut donc rester un <GCONST> en place
	 */
	private void loadHTKGauss(BufferedReader f, int n, String prem)
			throws IOException {
		String s;
		String[] ss;
		if (prem != null) {
			// premiere ligne a prendre en compte
			s = prem;
		} else
			s = f.readLine().trim();
		if (s.startsWith("<GCONST>"))
			s = f.readLine().trim();
		if (s.startsWith("<RCLASS>"))
			s = f.readLine().trim();
		if (!s.startsWith("<MEAN>")) {
			System.out.println("erreur gmm loadHTK: pas de <MEAN> ! " + s);
			System.exit(1);
		}
		int ncoefs = Integer.parseInt(s.substring(s.indexOf(' ') + 1));
		if (g == null)
			g = new GMMDiag(ngauss, ncoefs);
		s = f.readLine().trim();
		ss = s.split(" ");
		if (ss.length != ncoefs) {
			System.out.println("erreur gmm loadHTK: pas le bon nb de coefs "
					+ ncoefs + " " + s + " " + ss[0] + " " + ss[39]);
			System.exit(1);
		}
		for (int i = 0; i < ncoefs; i++) {
			g.setMean(n, i, Float.parseFloat(ss[i]));
		}
		s = f.readLine().trim();
		if (!s.startsWith("<VARIANCE>")) {
			System.out.println("erreur gmm loadHTK: pas de <VARIANCE> ! " + s);
			System.exit(1);
		}
		s = f.readLine().trim();
		ss = s.split(" ");
		if (ss.length != ncoefs) {
			System.out.println("erreur gmm loadHTK: pas le bon nb de coefs "
					+ ncoefs + " " + s);
			System.exit(1);
		}
		for (int i = 0; i < ncoefs; i++) {
			g.setVar(n, i, Float.parseFloat(ss[i]));
		}
	}
}
