/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.speechreco.decoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import plugins.speechreco.acousticModels.HMM.SingleHMM;

// TODO: gerer les noeuds non emetteurs
public class TokenPassing {
	
	private static int NTOKMAX = 600; // pruning !
	/**
	 * indique si le backtrack a trouve le dernier noeud dans la pile des hypotheses conservees
	 */
	public boolean isComplete;
	
	Network net;
	/**
	 * permet de retrouver un token sachant un noeud du reseau
	 * trop lent : j'alloue plutot les tokens statiquement !
	 * j'en alloue 2: anciens et nouveaux
	 */
//	HashMap<Integer,Token> tokens;
	Token[][] tokens;
	int newtokens=1;
	LinkedList<Integer> activeList;
	
	/**
	 * mapping entre le dernier etat d'un HMM et un node
	 */
	int[] mapHMM2node=null;
	int nFrames;
	
	/**
	 * Puisque l'on stocke un token par noeud du reseau, il ne faut pas stocker le path
	 * complet (de la 1er a la derniere trame du signal) dans chaque token, sinon, cela prend
	 * trop de memoire.
	 * 
	 * Je stocke donc dans un tableau separe un path complet pour chacun des 200 elements de la active list
	 * chaque token contient un index vers un chemin de ce tableau.
	 * lorsque un token transite, on stocke egalement dans chaque token de destination
	 * l'index du _nouveau noeud_ (et seulement celui-ci).
	 * Ensuite, lorsqu'on a fini d'inserer tous les nouveaux tokens dans l'active list,
	 * donc apres le pruning,
	 * on parcourt l'active list et on retient tous les path du tableau qui ne sont plus utilises 
	 * puis on reparcourt l'active list
	 * et pour chaque token restant, on ajoute l'index du noveau noeud
	 * au path dans le tableau, et si on y a deja ajoute un nouveau noeud, alors on recopie
	 * ce path dans une case non utilisee du tableau
	 */
	int[][] chemins;
	int[] nbnoeuds; // taille de chaque chemin
	int[] freqpath; // variable temporaire "locale": a ne pas utiliser globalement !
	boolean firstFrame = true;
	
	ArrayList<Integer> lastNodes = new ArrayList<Integer>();
	HashSet<Integer> newactivelist = new HashSet<Integer>();
	
	public void setPruning(int ntokmax) {
		NTOKMAX = ntokmax;
		createTokens();
	}
	
	public TokenPassing(Network network, int nFrames) {
		net = network;
		// cree un noeud par _etat_, et plus seulement un noeud par HMM
		net.expandNetwork();
		this.nFrames=nFrames;
		createTokens();
	}
	public void createTokens() {
		activeList = new LinkedList<Integer>();
		tokens = new Token[2][net.nodes.size()];
		chemins = new int[NTOKMAX][nFrames];
		nbnoeuds = new int[NTOKMAX];
		freqpath= new int[NTOKMAX];
		for (int i=0;i<nbnoeuds.length;i++) {
			nbnoeuds[i]=0;
		}
		int chemin = 0;
		firstFrame=true;
		for (int i=0;i<tokens[0].length;i++) {
			tokens[1-newtokens][i] = new Token(i,nFrames);
			tokens[newtokens][i] = new Token(i,nFrames);
			tokens[1-newtokens][i].clear();
			if (net.nodes.get(i).isFirstNode) {
				tokens[1-newtokens][i].activate();
				addInActiveList(i);
				if (chemin>=NTOKMAX)
					System.err.println("ERROR trop de noeuds initiaux ! "+chemin+" "+NTOKMAX);
				chemins[chemin][0]=i;
				nbnoeuds[chemin]=1;
				tokens[1-newtokens][i].path = chemin++;
			}
			if (net.nodes.get(i).isLastNode) {
				lastNodes.add(i);
			}
		}
	}
	public static String getAlignHMM(String oneFrameAlign) {
		String[] els = oneFrameAlign.split("\\[");
		String hmm = els[0];
		return hmm;
	}
	public Vector<String> backtrackold() {
		String r = backtrack();
		StringTokenizer st = new StringTokenizer(r);
		Vector<String> res = new Vector<String>();
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			res.add(s);
		}
		return res;
	}
	
	/**
	 * retourne le loglike du meilleur chemin (meme si ce n'est pas un noeud terminal)
	 * 
	 * @return
	 */
	public float getLogLikeBest() {
		return tokens[1-newtokens][activeList.get(0)].loglike;
	}
	
	/**
	 * retourne le loglike a la derniere trame calculee du token dans le dernier noeud
	 * @return
	 */
	public float getLogLike() {
		// on cherche le meilleur parmi les noeuds terminaux possibles
		Iterator<Integer> it = activeList.iterator();
		int n = -1;
		while (it.hasNext()) {
			int i = it.next();
			if (net.nodes.get(i).isLastNode && (n<0 || tokens[1-newtokens][i].loglike>tokens[1-newtokens][n].loglike)) {
				n=i;
			}
		}
		if (n<0) {
			return -Float.MAX_VALUE;
		} else {
			return tokens[1-newtokens][n].loglike;
		}
	}
	public String backtrack() {
		// on cherche le meilleur parmi les noeuds terminaux possibles
		Iterator<Integer> it = activeList.iterator();
		int n = -1;
		while (it.hasNext()) {
			int i = it.next();
			if (net.nodes.get(i).isLastNode && (n<0 || tokens[1-newtokens][i].loglike>tokens[1-newtokens][n].loglike)) {
				n=i;
			}
		}
		if (n<0) {
			isComplete=false;
			System.out.println("WARNING: last node has been pruned !");
			// dans ce cas, on backtrack depuis le meilleur dernier noeud
			n = activeList.get(0);
		} else
			isComplete=true;
		String res = pathToString(tokens[1-newtokens][n]);
		return res;
	}
	public String backtrackFromBest() {
		int n = activeList.get(0);
		String res = pathToString(tokens[1-newtokens][n]);
		return res;
	}
	/**
	 * affiche le meilleur chemin jusqu'a delayback secondes en arriere !
	 */
	String pathToStringBeginning(Token tok, int delayback) {
		String res = "";
		for (int i=nbnoeuds[tok.path]-1;i>=0;i--) {
			res+=net.getNodeName(chemins[tok.path][i])+" ";
		}
		return res;
	}
	
	String pathToString(Token tok) {
		ArrayList<String> resvec = new ArrayList<String>();
		String prevmot = null;
		int prevmotidx = -1;
		for (int i=0;i<nbnoeuds[tok.path];i++) {
			int netnoeud = chemins[tok.path][i];
			String netnom =net.getNodeName(netnoeud);
			
			String[] mots = net.getNodeMots(netnoeud);
			if (mots!=null&&mots.length>0) {
				// TODO: cas avec plusieurs mots ?
				if (prevmot==mots[0]) {
					// c'est le meme mot: on l'efface dans le precedent
					String oldres = resvec.get(prevmotidx);
					resvec.set(prevmotidx,oldres.substring(0,oldres.indexOf('(')));
				}
				prevmot=mots[0];
				prevmotidx=resvec.size();
			}
			resvec.add(netnom);
		}
		String res = "";
		for (int i=0;i<resvec.size();i++) {
			res+=resvec.get(i)+" ";
		}
		return res;
	}
	
	public String backtrackFrom(int hmmIdx) {
		int nodeIdx = mapHMM2node[hmmIdx];
		return null;
//		return tokens.get(nodeIdx).path;
	}
	/**
	 * calcule le score a la fin de chaque HMM
	 * @return
	 */
	public float[] getModelsScores() {
		if (mapHMM2node==null) {
			ArrayList<Integer> nodestmp = new ArrayList<Integer>();
			Iterator<Integer> it = activeList.iterator();
			while (it.hasNext()) {
				int tokidx = it.next();
				// on cherche les etats finaux
				SingleHMM hmm = net.getHMM(tokidx);
				int stateidx = net.getStateNum(tokidx);
				if (stateidx>=hmm.getNstates()) {
					// dernier etat
					nodestmp.add(tokidx);
				}
			}
			mapHMM2node = new int[nodestmp.size()];
			for (int i=0;i<mapHMM2node.length;i++) {
				mapHMM2node[i]=nodestmp.get(i);
			}
		}
		float[] sc = new float[mapHMM2node.length];
		for (int i=0;i<mapHMM2node.length;i++) {
			sc[i]=tokens[1-newtokens][mapHMM2node[i]].loglike;
		}
		return sc;
	}
	public SingleHMM getModel(int i) {
		int nodeIdx = mapHMM2node[i];
		return net.getHMM(nodeIdx);
	}
	/**
	 * insere une nouvelle observation dans le reseau: les tokens transitent
	 * retourne le dernier loglike
	 */
	public float nextObs(float[] x) {
		if (firstFrame) {
			// on passe la 1ere trame aux etats initiaux, et on ne transite pas !
			float lmax = -Float.MAX_VALUE;
			Iterator<Integer> it = activeList.iterator();
			while (it.hasNext()) {
				int tokidx = it.next();
				Token tok = tokens[1-newtokens][tokidx];	
				float b = net.calcEmissionLogLike(tok.netnode,x);
				tok.loglike += b;
				if (tok.loglike>lmax) lmax = tok.loglike;
			}
			firstFrame=false;
			// on retourne le loglike
			return lmax;
		}

		// on fait transiter les tokens actifs
		newactivelist.clear();
		Iterator<Integer> it = activeList.iterator();
		while (it.hasNext()) {
			int tokidx = it.next();
			Token tok = tokens[1-newtokens][tokidx];	
			for (int j=0;j<net.nbNextNodes(tok.netnode);j++) {
				int dest = net.nextNode(tok.netnode, j);

				// on ajoute les trans probas
				float tr = net.calcTransProba(tok.netnode,dest);
//System.out.println("debug "+tok.netnode+" "+net.getNodeName(tok.netnode)+" to "+dest+" "+net.getNodeName(dest)+" trprob "+tr);
				float newloglike;
				if (tr!=-Float.MAX_VALUE) {
					newloglike = tok.loglike+tr;
					// on ne garde que le meilleur
					Token ntok = tokens[newtokens][dest];
					if (newloglike>ntok.loglike) {
						ntok.loglike = newloglike;
						// TODO: ceci coute trop cher en temps: 
						// ntok.path=""+tok.path+" "+net.getNodeName(dest);
						
						ntok.path = tok.path;
						ntok.tmpnoeud = dest;
						
						newactivelist.add(dest);
					}
				}
			}
			tok.clear();
		}
		
		activeList.clear();
		
		Iterator<Integer> itt = newactivelist.iterator();
		while (itt.hasNext()) {
			int tokidx = itt.next();
			float b = net.calcEmissionLogLike(tokens[newtokens][tokidx].netnode,x);
			tokens[newtokens][tokidx].loglike += b;
			addInActiveList(tokidx);
		}

		// on mets a jour les chemins
		// on commence par chercher les chemins qui sont utilises (et donc aussi ceux qui sont vides !)
		for (int i=0;i<freqpath.length;i++) {
			freqpath[i]=0;
		}
		it = activeList.iterator();
		while (it.hasNext()) {
			int tokidx = it.next();
			freqpath[tokens[newtokens][tokidx].path]++;
		}
		// puis on ajoute le nouveau noeud au chemin
		it = activeList.iterator();
		while (it.hasNext()) {
			int tokidx = it.next();
			int chemin = tokens[newtokens][tokidx].path;
			if (freqpath[chemin]>0) {
				// on peut modifier le chemin du tableau
				chemins[chemin][nbnoeuds[chemin]++]=tokens[newtokens][tokidx].tmpnoeud;
				freqpath[chemin]=-freqpath[chemin];
			} else {
				// ce chemin a deja ete modifie: il faut le dupliquer !
				// on cherche un chemin libre
				int newchemin = 0;
				for (;newchemin<NTOKMAX;newchemin++) {
					if (freqpath[newchemin]==0) break;
				}
				// on recopie le chemin dans ce nouveau:
				int len = nbnoeuds[chemin]-1;
				System.arraycopy(chemins[chemin], 0, chemins[newchemin], 0, len);
				chemins[newchemin][len]=tokens[newtokens][tokidx].tmpnoeud;
				nbnoeuds[newchemin]=len+1;
				freqpath[newchemin]=-1;
				tokens[newtokens][tokidx].path=newchemin;
			}
		}
		
		/*
		{
			int co = 0;
			it = activeList.iterator();
			while (it.hasNext()) {
				int tokidx = it.next();
				System.out.print("debug3 "+co+" ");
				System.out.println(tokens[newtokens][tokidx].loglike+" "+tokens[newtokens][tokidx].path);
				co++;
				break;
			}
		}
*/
		// on remplace les anciens tokens par les nouveaux
		newtokens = 1-newtokens;
		// on retourne le loglike
		float lmax = -Float.MAX_VALUE;
		for (int i=0;i<lastNodes.size();i++)
			if (tokens[1-newtokens][i].loglike>lmax)
				lmax = tokens[1-newtokens][i].loglike;
		return lmax;
	}

	void addInActiveList(int tokidx) {
		Iterator<Integer> it = activeList.iterator();
		int pos=0;
		while (it.hasNext()) {
			int tok = it.next();
			if (tokens[newtokens][tokidx].loglike>=tokens[newtokens][tok].loglike) {
				// on l'insere a cet endroit
				activeList.add(pos, tokidx);
				break;
			}
			pos++;
		}
		if (NTOKMAX>0) {
			if (pos>=activeList.size()&&pos<NTOKMAX) {
				activeList.add(tokidx);
			}
			// on vire la queue de la liste
			while (activeList.size()>NTOKMAX)
				activeList.removeLast();
		} else {
			if (pos>=activeList.size()) {
				activeList.add(tokidx);
			}
		}
	}
	
	class Token {
		public Token(int netnode, int maxnodes) {
			this.netnode=netnode;
			path = 0;
		}
		int netnode;
		float loglike=-Float.MAX_VALUE;
		int path; // index du chemin
		int tmpnoeud; // variable temporaire utilisee pendant le token passing
		public void activate() {
			loglike = 0;
		}
		public void clear() {
			loglike = -Float.MAX_VALUE;
		}
	}
}
