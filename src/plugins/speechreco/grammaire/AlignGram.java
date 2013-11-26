/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.grammaire;

import java.util.*;
import java.util.Map.Entry;

import plugins.speechreco.acousticModels.HMM.HMMSet;
import plugins.speechreco.acousticModels.HMM.SingleHMM;
import plugins.speechreco.decoder.Network;

/**
 * grammaire construite pour realiser un alignement force avec silences optionnels
 * entre les mots et differentes prononciations possibles
 * 
 * @author cerisara
 *
 */
public class AlignGram {
	Noeud root;
	Noeud curnode;
	Vector<Noeud> suiv = new Vector<Noeud>();
	
	private String[] rule;	
	private int rulepos;
	private Vector<Integer> dernierNoeuds;
	private Network net=null;
	private String[] CIphones = {"sil","bb","hh","xx"};
	private HMMSet hmms;
	HashSet<String> motsCourant = new HashSet<String>();
	
	/**
	 * La regle doit encoder toutes les prononciations possibles de la phrase courante
	 * 
	 * @param rule
	 */
	public AlignGram(String rule) {
		this.rule = rule.split(" ");
		rulepos=0;
		root = new Noeud();
		Noeud derniernoeud = parseRule("",root);
		if (derniernoeud.unit!=null) {
			Noeud fin = new Noeud();
			derniernoeud.suivants.add(fin);
		}
		curnode=root;
		this.rule=null;
	}
	
	/**
	 * possibilite de sauter chaque mot avec un modele de "bruit"
	 *
	 */
	public static String addNoiseLinks(String gram) {
		System.err.println("adding noise links...");
		String s="";
		int deb=0;
		int cur=0;
		for (;;) {
			cur = gram.indexOf(' ',cur+1);
			if (cur<0) break;
			if (gram.charAt(cur+1)=='€') {
				s+=" ( "+gram.substring(deb,cur);
				deb = gram.indexOf(' ',cur+1);
				// il vaut mieux ne pas mettre le mot dans le chemin du noise
				s+=gram.substring(cur,deb)+" | noise €??€ ) ";
				cur=deb;
			}
		}
		s+=gram.substring(deb);
		System.err.println("done !");
		return s;
	}
/* a supprimer
	public void addNoiseLinksParse(Noeud debMot, Noeud n) {
		if (n==null||(n.unit!=null&&n.unit.equals("noise"))) return;
		if (n.mot!=null) {
			// fin d'un mot
			Noeud noise = new Noeud();
			noise.mot=n.mot;
			noise.unit="noise";
			debMot.suivants.add(noise);
			if (n.unit==null) {
				// noeud non-emetteur: on peut lier le noise dessus
				noise.suivants.add(n);
				for (int i=0;i<n.suivants.size();i++) {
					Noeud newn = n.suivants.get(i);
					addNoiseLinksParse(newn,newn);
				}
			} else {
				// noeud emetteur: il faut lier le noise au noeud suivant
				for (int i=0;i<n.suivants.size();i++) {
					Noeud newn = n.suivants.get(i);
					noise.suivants.add(newn);
					addNoiseLinksParse(newn,newn);
				}
			}
		} else {
			for (int i=0;i<n.suivants.size();i++) {
				Noeud newn = n.suivants.get(i);
				addNoiseLinksParse(debMot,newn);
			}
		}
	}
*/	
	/**
	 * retourne le HMM en tenant compte des QCQ de la grammaire
	 * 
	 * @param hmms
	 * @param nom
	 * @return
	 */
	public static SingleHMM getHMM(HMMSet hmms, String nom) {
		SingleHMM hmm = null;
		hmm = hmms.getHMM(nom); 
		if (hmm==null) {
			try {
				throw new Exception("HMM not found "+nom);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return hmm;
	}
	
	/**
	 * transforme cette grammaire en reseau de decodage pour le token passing
	 * en etandant le reseau pour tenir compte des 3ph contextuels
	 * le phoneme "sil" n'est pas contextuel.
	 * 
	 * supprime aussi les noeuds non-emetteurs, car ils ne sont pas supportes pas Token Passing
	 * 
	 * NE PLUS UTILISER CETTE FONCTION CAR ELLE ETEND LE RESEAU EN UN ARBRE EXPONENTIEL !
	 * UTILISER A LA PLACE expand2triphones !!
	 * 
	 * @return
	 */
	public Network getTriphoneNetwork(HMMSet hmms) {
		Network net = new Network();
		// attention: tous les noeuds du reseau doivent etre emetteurs !
		String[] roots = nextUnits(-1);
		SingleHMM hmm;
		Vector<Integer> path = new Vector<Integer>();
		path.add(-1);
		String ntmp;
		if (roots.length>1) {
			// on ajoute un silence devant
			hmm = getHMM(hmms,"sil");
			ntmp="sil";
		} else {
			// on considere que le 1er noeud est contexte-independent
			hmm = getHMM(hmms,roots[0]);
			path.add(0);
			roots = nextUnits(0);
			ntmp=roots[0];
		}
		int root = net.createNode(hmm);
		if (ntmp.startsWith("QCQ")) net.addPrefix(root,"QCQ");
		net.setFirstNode(root);
		path.add(0);
		dernierNoeuds = new Vector<Integer>();
		for (int i=0;i<roots.length;i++) {
			path.set(path.size()-1,i);
			buildNetRecurs(net,path,root,roots[i],hmms);
		}
		// TODO: j'ajoute simplement un silence a la fin, mais il faudrait
		// rassembler tous les noeuds finaux en un seul !
		int fin = net.createNode(getHMM(hmms,"sil"));
		net.setLastNode(fin);
		for (int i=0;i<dernierNoeuds.size();i++)
			net.addTransition(dernierNoeuds.get(i), fin);
		return net;
	}

	/**
	 * TODO: pour le moment, je cree un reseau sous forme d'arbre
	 * il faudrait factoriser des branches pour retrouver une forme de graphe avec
	 * beaucoup moins de noeuds !
	 * ==> Ceci est indispensable a cause de l'explostion combinatoire !
	 * 
	 * @param net
	 * @param path
	 * @param precnode
	 * @param middlephone
	 * @param hmms
	 * @param finalnode
	 */
	void buildNetRecurs(Network net, Vector<Integer> path, int precnode, String middlephone, HMMSet hmms) {
		// reparcourt tout le chemin
		for (int i=0;i<path.size()-1;i++) {
			String[] tmpw = nextUnits(path.get(i));
		}
		// nouveaux embranchements
		String[] words = nextUnits(path.get(path.size()-1));

		SingleHMM hh = net.getHMM(precnode);
		String leftphone = hh.getNom();
		leftphone = removecontext(leftphone);
		// on teste tous les right phones
		path.add(0);
		for (int i=0;i<words.length;i++) {
			String triphone;
			if (middlephone.equals("sil")) {
				// context-independent
				triphone = middlephone;
				// TODO: on cree des noeuds inutiles... a ameliorer !
			} else {
				// en contexte
				if (words[i]==null) {
					// words[i]==null = fin du reseau
					triphone = leftphone+"-"+middlephone;
				} else
					triphone = leftphone+"-"+middlephone+"+"+words[i];
			}
			SingleHMM hmm = getHMM(hmms,triphone);
			int id = net.createNode(hmm);
			if (triphone.startsWith("QCQ")) net.addPrefix(id,"QCQ");
			net.addTransition(precnode, id);
			path.set(path.size()-1, i);
			if (words[i]==null) {
				dernierNoeuds.add(id);
			} else
				buildNetRecurs(net, path, id, words[i], hmms);
		}
		path.remove(path.size()-1);
	}
	public static String removecontext(String ph) {
		String s = ph;
		int i=s.indexOf('-');
		if (i>=0) s=s.substring(i+1);
		i=s.indexOf('+');
		if (i>=0) s=s.substring(0,i);
		return s;
	}

	/**
	 * etend le reseau monophones en le parcourant et annotant chacun de ses noeuds avec la liste des nouveaux noeuds 3ph associes
	 * lorsque, en entrant dans un noeud 1ph, on veut y creer un 3ph, on verifie si ce 3ph existe, auquel cas on on s'y rattache
	 * sans creer de nouveaux noeuds
	 * 
	 * @param hmms
	 * @return
	 */
	public Network expand2triphones(HMMSet hmms) {
		this.hmms=hmms;
		net = new Network();
		// il faut donner un noeud initial
		int firsttriphnode = net.createNode(hmms.getHMM("sil"));
		net.setFirstNode(firsttriphnode);
		int lasttriphnode = net.createNode(hmms.getHMM("sil"));
		net.setLastNode(lasttriphnode);

		// "root" est un noeud non-emetteur !
		// on suppose qu'il n'a qu'un seul successeur ?!
		Noeud n = root.suivants.get(0);
		deepparse3ph(net,n,firsttriphnode,null,lasttriphnode);
		return net;
	}
	public Network expand2monophones(HMMSet hmms) {
		this.hmms=hmms;
		net = new Network();
		// "root" est un noeud non-emetteur
		Noeud[] premiersNoeuds = nextEmittingNodes(-1);
		for (int i=0;i<premiersNoeuds.length;i++) {
			deepparse1ph(net,premiersNoeuds[i],-1,null,"");
		}

		// attention: dernier noeud sans nextNodes ?
		for (int i=0;i<net.nodes.size();i++) {
			if (net.isLastNode(i) && net.nextNodes.size()>=i)
				net.addNullTransition(i);
		}
		
		return net;
	}
	void deepparse3ph(Network net, Noeud n1ph, int n3phprec, String leftctxt, int n3phlast) {
		// 2 etapes:
		
		// 1ere etape, on repere tous les "nouveaux noeuds", car il faudra ajouter a ces nouveaux noeuds tous les chemins sortant
		// alors qu'il ne faut pas les ajouter aux anciens noeuds
		
		// cherche tous les contextes droits possibles
		this.suiv.removeAllElements();
		this.motsCourant.clear();
		findNextEmitting(n1ph);
		Vector<Noeud> suivlocal = new Vector<Noeud>();
		for (int i=0;i<suiv.size();i++) {
			suivlocal.add(suiv.get(i));
		}
		HashMap<String,Vector<Integer>> newnoeuds = new HashMap<String,Vector<Integer>>();
		boolean linked = false;
		for (int i=0;i<suivlocal.size();i++) {
			// pour chaque contexte droit:
			String triphone;
			int j=0;
			if (!n1ph.unit.startsWith("QCQ"))
				for (;j<CIphones.length;j++)
					if (n1ph.unit.equals(CIphones[j])) break;
			if (j<CIphones.length) {
				// CI
				triphone = n1ph.unit;
			} else {
				// CD
				// QCQ est aussi un phoneme context-free et CI !!
				if (leftctxt==null||leftctxt.startsWith("QCQ")) {
					if (suivlocal.get(i)==null||suivlocal.get(i).unit==null||suivlocal.get(i).unit.startsWith("QCQ"))
						triphone = n1ph.unit;
					else
						triphone = n1ph.unit+"+"+suivlocal.get(i).unit;
				} else {
					if (suivlocal.get(i)==null||suivlocal.get(i).unit==null||suivlocal.get(i).unit.startsWith("QCQ"))
						triphone = leftctxt+"-"+n1ph.unit;
					else
						triphone = leftctxt+"-"+n1ph.unit+"+"+suivlocal.get(i).unit;
				}
			}
			Integer n3phcur = n1ph.get3phNode(triphone);
			if (n3phcur==null) {
				// c'est un nouveau noeud !
				Vector<Integer> nextnodes = new Vector<Integer>();
				nextnodes.add(i);
				newnoeuds.put(triphone,nextnodes);
				// si c'est un CI, alors il peut etre QCQ
				SingleHMM tied3ph = AlignGram.getHMM(hmms,triphone);
//				SingleHMM tied3ph = hmms.getHMM(triphone);
				n3phcur = net.createNode(tied3ph);
				if (triphone.startsWith("QCQ")) {
					// parfois le 1ph est tied a un 3ph: ici, on force un nom QCQ+1ph
					net.setNom(n3phcur,triphone);
					if (net.getNodeName(n3phcur).indexOf('-')>=0) {
						System.err.println("LLLLLLLLLLLLLLLLLL "+net.getNodeName(n3phcur)+" "+triphone);
						System.exit(1);
					}
				}
				n1ph.add3phNode(triphone, n3phcur);
				linked=false;
			} else {
				Vector<Integer> nextnodes = newnoeuds.get(triphone);
				if (nextnodes!=null)
					nextnodes.add(i);
			}
			if (!linked) {
				// que le noeud soit nouveau ou existe deja, on doit ajouter la liaison depuis le noeud prec
				net.addTransition(n3phprec,n3phcur);
				linked = true;
			}
		}
		
		// 2eme etape, on continue a parser seulement pour les nouveaux noeuds
		// on s'interesse maintenant aux trans sortantes, alors que en passe 1 on regardait les trans entrantes
		for (Map.Entry<String, Vector<Integer>> entry: newnoeuds.entrySet()) {
			String triphone = entry.getKey();
			Vector<Integer> nextnodes = entry.getValue();
			Integer n3phcur = n1ph.get3phNode(triphone);
			for (int j=0;j<nextnodes.size();j++) {
				int i = nextnodes.get(j);
				if (suivlocal.get(i)==null||suivlocal.get(i).unit==null) {
					net.addTransition(n3phcur,n3phlast);
				}
				if (!(suivlocal.get(i)==null||suivlocal.get(i).unit==null)) {
					deepparse3ph(net,suivlocal.get(i),n3phcur,n1ph.unit,n3phlast);
				}
			}
		}
	}
	void deepparse1ph(Network net, Noeud n1ph, int n3phprec, String leftctxt, String locID) {
		// 2 etapes:
		if (n1ph.unit.startsWith("§%")) {
			locID = n1ph.unit.substring(2);
			/* on peut avoir plusieurs noeuds suivants le noeud locuteur, par exemple lorsque
			 * le 1er mot suivant est un mot inconnu...
			 
			if (n1ph.suivants.size()!=1)
				System.err.println("ERROR DEEPPARSE1PH: LOCINFO SUIVANTS = "+n1ph.suivants.size());
			n1ph = n1ph.suivants.get(0);
			*/
			for (Noeud n : n1ph.suivants) {
				deepparse1ph(net, n, n3phprec, leftctxt, locID);
			}
			return;
		}
		
		// 1ere etape, on repere tous les "nouveaux noeuds", car il faudra ajouter a ces nouveaux noeuds tous les chemins sortant
		// alors qu'il ne faut pas les ajouter aux anciens noeuds
		
		// cherche tous les contextes droits possibles
		this.suiv.removeAllElements();
		this.motsCourant.clear();
		findNextEmitting(n1ph);
		
		// TODO: pourquoi a-t-on besoin de recopier le suiv global dans un suiv local ?? 
		Vector<Noeud> suivlocal = new Vector<Noeud>();
		for (int i=0;i<suiv.size();i++) {
			suivlocal.add(suiv.get(i));
		}
		HashMap<String,Vector<Integer>> newnoeuds = new HashMap<String,Vector<Integer>>();
		boolean linked = false;
		for (int i=0;i<suivlocal.size();i++) {
			// pour chaque contexte droit:
			String phone;
			phone = n1ph.unit;

			Integer netNoeudCur = n1ph.get3phNode(phone);
			if (netNoeudCur==null) {
				// c'est un nouveau noeud !
				Vector<Integer> nextnodes = new Vector<Integer>();
				nextnodes.add(i);
				newnoeuds.put(phone,nextnodes);
				SingleHMM tied3ph = AlignGram.getHMM(hmms,phone);
				if (tied3ph==null) {
					System.exit(1);
				}
				netNoeudCur = net.createNode(tied3ph);
				net.setSpeaker(netNoeudCur, locID);
				if (phone.startsWith("QCQ")) net.addPrefix(netNoeudCur,"QCQ");
				n1ph.add3phNode(phone, netNoeudCur);
				linked=false;
			} else {
				Vector<Integer> nextnodes = newnoeuds.get(phone);
				if (nextnodes!=null)
					nextnodes.add(i);
			}
			if (!linked) {
				if (n3phprec>=0) {
					// que le noeud soit nouveau ou existe deja, on doit ajouter la liaison depuis le noeud prec
					net.addTransition(n3phprec,netNoeudCur);
					linked = true;
				} else {
					net.setFirstNode(netNoeudCur);
				}
			}
		}
		
		// 2eme etape, on continue a parser seulement pour les nouveaux noeuds (qui viennent d'etre crees)
		// on s'interesse maintenant aux trans sortantes, alors que en passe 1 on regardait les trans entrantes
		for (Map.Entry<String, Vector<Integer>> entry: newnoeuds.entrySet()) {
			String triphone = entry.getKey();
			Vector<Integer> nextnodes = entry.getValue();
			Integer n3phcur = n1ph.get3phNode(triphone);
			{
				// on ajoute les mots eventuels
				if (n1ph.mot!=null)
					net.setMot(n3phcur, n1ph.mot);
				Iterator<String> itmots = motsCourant.iterator();
				while (itmots.hasNext()) {
					String mot = itmots.next();
					net.setMot(n3phcur, mot);
				}
			}
			
			for (int j=0;j<nextnodes.size();j++) {
				int i = nextnodes.get(j);
				if (suivlocal.get(i)==null || suivlocal.get(i).unit==null) {
					net.setLastNode(n3phcur);
				} else {
					deepparse1ph(net,suivlocal.get(i),n3phcur,n1ph.unit,net.getSpeaker(n3phcur));
				}
			}
		}
	}
	/**
	 * place tous les noeuds suivants EMETTEURS dans la liste suiv
	 * @param n
	 */
	void findNextEmitting(Noeud n) {
		if (n.suivants==null||n.suivants.size()==0) {
			// dernier noeud
			suiv.add(null);
		} else {
			for (int i=0;i<n.suivants.size();i++) {
				Noeud nn = n.suivants.get(i);
				// on teste si le noeud est non-emetteur ou si c'est une info locuteur
				if (nn.unit==null||(nn.unit.startsWith("§%"))) {
					if (nn.mot!=null) motsCourant.add(nn.mot);
					findNextEmitting(nn);
				} else if (!suiv.contains(nn))
					suiv.add(nn);
			}
		}
	}
	
	/**
	 * recherche les 1er noeuds emetteurs a partir de la position courante en largeur d'abord
	 * 
	 * @param n
	 */
	void largeur(Noeud n) {
		if (n.suivants.size()==0) {
			// dernier noeud
			suiv.add(null);
		} else if (n.unit!=null) {
			suiv.add(n);
		} else
			for (int i=0;i<n.suivants.size();i++) {
				Noeud nn = n.suivants.get(i);
				largeur(nn);
			}
	}
	
	/**
	 * pareil que nextUnits, mais retourne des noeuds
	 * @param prev
	 * @return
	 */
	public Noeud[] nextEmittingNodes(int prev) {
		if (prev<0) {
			curnode=root;
			suiv.removeAllElements();
			if (root.unit!=null) {
				suiv.add(root);
			} else
				largeur(curnode);
		} else {
			curnode = suiv.get(prev);
			suiv.removeAllElements();
			for (int i=0;i<curnode.suivants.size();i++) {
				largeur(curnode.suivants.get(i));
			}
		}
		Noeud[] r = new Noeud[suiv.size()];
		suiv.toArray(r);
		return r;
	}
	
	public String[] nextUnits(int prev) {
		if (prev<0) {
			curnode=root;
			suiv.removeAllElements();
			if (root.unit!=null) {
				suiv.add(root);
			} else
				largeur(curnode);
		} else {
			curnode = suiv.get(prev);
			suiv.removeAllElements();
			for (int i=0;i<curnode.suivants.size();i++) {
				largeur(curnode.suivants.get(i));
			}
		}
		String[] r = new String[suiv.size()];
		for (int i=0;i<suiv.size();i++) {
			Noeud tt = suiv.get(i);
			if (tt==null) r[i]=null;
			else r[i]=tt.unit;
		}
		return r;
	}

	/**
	 * parse en iteratif une sequence continue de mots
	 * appelle en recursif les sous-sequences 
	 * 
	 * parse directement dans un Network ??
	 * Non, car Network utilise des HMMs, or ici, on veut decrire une grammaire a un niveau plus abstrait
	 * 
	 * @param rule
	 * @param precn = noeud precedent auquel raccrocher la sequence
	 * @return le dernier noeud de la sequence (qui est deja rempli)
	 */
	Noeud parseRule(String stop, Noeud precn) {
		Noeud n = precn;
		for (;rulepos<rule.length;rulepos++) {
			if (rule[rulepos].length()==0) continue;
			// fin de la sequence ?
			int i=0;
			for (i=0;i<stop.length();i++) {
				if (rule[rulepos].charAt(0)==stop.charAt(i)) return n;
			}
			
			Noeud noeudfin = new Noeud();
			if (rule[rulepos].charAt(0)=='(') {
				Noeud noeuddeb = n;
				for (rulepos++;;rulepos++) {
					Noeud ntmp = parseRule("|)",noeuddeb);
					ntmp.suivants.add(noeudfin);
					if (rule[rulepos].charAt(0)==')') break;
				}
			} else if (rule[rulepos].charAt(0)=='#') {
				// indicateur de mot
				// le dernier noeud etait le noeud final d'un mot
				n.mot=rule[rulepos].substring(1,rule[rulepos].length()-1);
				continue;
			} else if (rule[rulepos].charAt(0)=='<') {
				// TODO
				System.out.println("WARNING <> not yet supported !");
			} else if (rule[rulepos].charAt(0)=='[') {
				rulepos++;
				n.suivants.add(noeudfin);
				Noeud ntmp = parseRule("]",n);
				ntmp.suivants.add(noeudfin);
			} else {
				// mot (ou locuteur)
				noeudfin.unit=rule[rulepos];
				n.suivants.add(noeudfin);
			}
			n=noeudfin;
		}
		return n;
	}
}

class Noeud {
	public String toString() {
		if (mot==null)
			return unit;
		else
			return unit+" -"+mot;
		}
	public String unit=null;
	public Vector<Noeud> suivants=new Vector<Noeud>();
	public String mot = null;
	
	private HashMap<String,Integer> triphones=new HashMap<String,Integer>();
	public Integer get3phNode(String triphone) {
		return triphones.get(triphone);
	}
	public void add3phNode(String tr, int n) {
		triphones.put(tr,n);
	}
}