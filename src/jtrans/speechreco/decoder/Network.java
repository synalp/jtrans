/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package jtrans.speechreco.decoder;

import java.util.ArrayList;

//import plugins.speechreco.acousticModels.HMM.GMMDiag;
import jtrans.speechreco.acousticModels.HMM.HMMSet;
import jtrans.speechreco.acousticModels.HMM.HMMState;
import jtrans.speechreco.acousticModels.HMM.SingleHMM;

/**
 * pour le moment, tous les noeuds du reseau sont emetteurs
 *
 */
public class Network {
	public ArrayList<Node> nodes = new ArrayList<Node>();
	public ArrayList<ArrayList<Integer>> nextNodes = new ArrayList<ArrayList<Integer>>();
	
	// cette penalite a ete mise completement arbitrairement a log(0.5) (a peu pres)
	public static float penalty = -0.5f;
	
	public int nbNodes() {
		return nodes.size();
	}
	public int nbNextNodes(int node) {
		if (nextNodes.size()<=node) return 0;
		ArrayList<Integer> list = nextNodes.get(node);
		return list.size();
	}
	public int nextNode(int node, int num) {
		ArrayList<Integer> list = nextNodes.get(node);
		return list.get(num);
	}
	public String[] getNodeMots(int i) {
		return nodes.get(i).mots;
	}
	public String getNodeName(int i) {
		return nodes.get(i).toString();
	}
	/**
	 * 
	 * @param hmm
	 * @return node ID
	 */
	public int createNode(SingleHMM hmm) {
		Node n = new Node(hmm);
		int id = nodes.size();
		nodes.add(n);
		return id;
	}
	public void setNom(int nodeidx, String nom) {
		Node n = nodes.get(nodeidx);
		n.nom=nom;
	}
	public void addPrefix(int nodeidx,String prefix) {
		Node n = nodes.get(nodeidx);
		n.nom=prefix+n.nom;
	}
	public String getSpeaker(int node) {
		Node no= nodes.get(node);
		return no.loc;
	}
	public void setSpeaker(int node, String loc) {
		Node no= nodes.get(node);
		no.loc=loc;
	}
	
	/**
	 * tag un noeud avec un "mot": ce doit etre le dernier noeud d'emission de ce mot
	 */
	public void setMot(int node, String mot) {
		Node no= nodes.get(node);
		String[] m = no.mots;
		if (m==null) {
			m = new String[1];
			// je fais expres de ne pas cloner le mot, car le network peut avoir plusieurs fois le meme mot dans des noeuds successifs,
			// mais par la grammaire ! J'utiliserai cette propriete pour ne pas afficher plusieurs fois le meme mot ensuite...
			m[0] = mot;
			no.mots=m;
		} else {
			String[] mm = new String[m.length+1];
			System.arraycopy(m, 0, mm, 0, m.length);
			mm[m.length]=mot;
			no.mots=m;
		}
	}
	public void setFirstNode(int id) {
		nodes.get(id).isFirstNode=true;
	}
	public boolean isLastNode(int id) {
		return nodes.get(id).isLastNode;
	}
	public void setLastNode(int id) {
		nodes.get(id).isLastNode=true;
	}
	public void addTransition(int deb, int fin) {
		if (nextNodes.size()<=deb) {
			for (int i=nextNodes.size();i<nodes.size();i++) {
				nextNodes.add(new ArrayList<Integer>());
			}
		}
		ArrayList<Integer> priorTrans = nextNodes.get(deb);
		priorTrans.add(fin);
	}
	public void addNullTransition(int deb) {
		if (nextNodes.size()<=deb) {
			for (int i=nextNodes.size();i<nodes.size();i++) {
				nextNodes.add(new ArrayList<Integer>());
			}
		}
	}
	public static Network createLoopNetwork(HMMSet hmms) {
		Network loop = new Network();
		int sil=-1;
		for (int i=0;i<hmms.getNhmms();i++) {
			SingleHMM hmm = hmms.getHMM(i);
			if (hmm.getNom().equals("sil")) sil=i;
			loop.nodes.add(loop.new Node(hmm));
		}
		SingleHMM sildeb = loop.nodes.get(sil).hmm.clone();
		sildeb.setNom("sil");
		loop.nodes.add(loop.new Node(sildeb));
		loop.setFirstNode(loop.nodes.size()-1);
		SingleHMM silfin = loop.nodes.get(sil).hmm.clone();
		silfin.setNom("sil");
		loop.nodes.add(loop.new Node(silfin));
		int lastnodeidx = loop.nodes.size()-1;
		loop.setLastNode(lastnodeidx);
		// transitions internes
		for (int i=0;i<loop.nodes.size()-2;i++) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int j=0;j<loop.nodes.size()-2;j++)
				list.add(j);
			list.add(lastnodeidx);
			loop.nextNodes.add(list);
		}
		// transitions depuis etat d'entree
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int j=0;j<loop.nodes.size()-2;j++)
			list.add(j);
		loop.nextNodes.add(list);
		// transitions depuis etat de sortie
		loop.nextNodes.add(new ArrayList<Integer>());
		return loop;
	}
	/**
	 * remplace un noeud par HMM par un noeud par etat:
	 * l'ancien noeud HMM devient le noeud du 1er etat emetteur,
	 * et on cree de nouveaux noeuds a la fin de la liste des noeuds existants
	 * qui representent les autres etats
	 */
	public void expandNetwork() {
		int nbHMMnodes = nodes.size();
		for (int i=0;i<nbHMMnodes;i++) {
			ArrayList<Integer> successors = nextNodes.get(i);
			Node noeud = nodes.get(i);
			Node newnoeud = noeud;
			SingleHMM hmm = noeud.hmm;
			boolean firstState = true;
			for (int etat=0;etat<hmm.getNstates();etat++) {
				if (hmm.isEmitting(etat)) {
					if (firstState) {
						newnoeud.idxEtat=etat;
						newnoeud.idxNoeud=i;
						firstState = false;
						ArrayList<Integer> suivants = new ArrayList<Integer>();
						suivants.add(i); // boucle
						suivants.add(nodes.size()); // etat suivant
						nextNodes.set(i,suivants);
					} else {
						newnoeud = new Node(hmm);
						newnoeud.nom=noeud.nom;
						newnoeud.idxEtat=etat;
						newnoeud.idxNoeud=nodes.size();
						nodes.add(newnoeud);
						ArrayList<Integer> suivants = new ArrayList<Integer>();
						suivants.add(nodes.size()-1); // boucle
						suivants.add(nodes.size()); // etat suivant
						nextNodes.add(suivants);
					}
				}
			}
			// newnoeud contient le dernier etat:
			ArrayList<Integer> suivants = nextNodes.get(newnoeud.idxNoeud);
			// supprime la transition vers "etat suivant"
			suivants.remove(suivants.size()-1);
			// rajoute les trans vers les HMMs
			for (int j=0;j<successors.size();j++)
				suivants.add(successors.get(j));
			if (nodes.get(i).isLastNode) {
				nodes.get(i).isLastNode=false;
				nodes.get(newnoeud.idxNoeud).isLastNode=true;
			}
		}
	}
	public float calcEmissionLogLike(int nodeidx, float[] x) {
		Node node = nodes.get(nodeidx);
		SingleHMM hmm = node.hmm;
		HMMState etat = hmm.getState(node.idxEtat);
		etat.computeLogLikes(x);
		float b = etat.getLogLike();
		return b;
	}
	public float calcTransProba(int orig, int dest) {
		Node noeud = nodes.get(orig);
		ArrayList<Integer> suivants = nextNodes.get(noeud.idxNoeud);
		float tr = -Float.MAX_VALUE;
		for (int i=0;i<suivants.size();i++) {
			if (dest==suivants.get(i)) {
				Node ndest = nodes.get(dest);
				// non: le test suivant ne marche pas, car on peut transiter de sil vers un autre sil par exemple !
				// puisque j'avais suppose que les HMMs sont toujours gauche-droite, alors il suffit de tester si l'etat revient en arriere !
				if (ndest.idxEtat<noeud.idxEtat) {
					// nouveau HMM: on suppose trans equiprobables
					// TODO: ajouter ML
					tr = penalty;
				} else {
					// trans interne:
					tr = noeud.hmm.getTrans(noeud.idxEtat, ndest.idxEtat);
					if (tr<=0) {
						// il est possible qu'un noeud ait une trans nulle par exemple pour boucler sur lui-meme...
						tr = -Float.MAX_VALUE;
					} else
						tr = (float)Math.log(tr);
				}
				break;
			}
		}
		return tr;
	}
	public SingleHMM getHMM(int nodeidx) {
		return nodes.get(nodeidx).hmm;
	}
	public int getStateNum(int nodeidx) {
		return nodes.get(nodeidx).idxEtat;
	}
	class Node {
		SingleHMM hmm;
		String nom,loc;
		int idxEtat=-1;
		int idxNoeud=-1;
		boolean isFirstNode = false, isLastNode = false;
		String[] mots=null;
		public Node(SingleHMM h) {hmm=h;nom=hmm.getNom();}
		public String toString() {
			if (mots==null) {
				if (idxEtat==-1) return nom;
				else return nom+"["+idxEtat+"]";
			} else {
				String s = "(";
				int i;
				for (i=0;i<mots.length-1;i++)
					s+=mots[i]+",";
				s+=mots[i]+")";
				if (idxEtat==-1)
					return nom+s;
				else
					return nom+"["+idxEtat+"]"+s;
			}
		}
	}
}
