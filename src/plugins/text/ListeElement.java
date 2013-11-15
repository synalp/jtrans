/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant � aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est r�gi par la licence CeCILL-C soumise au droit fran�ais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffus�e par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilit� au code source et des droits de copie,
de modification et de redistribution accord�s par cette licence, il n'est
offert aux utilisateurs qu'une garantie limit�e.  Pour les m�mes raisons,
seule une responsabilit� restreinte p�se sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les conc�dants successifs.

A cet �gard  l'attention de l'utilisateur est attir�e sur les risques
associ�s au chargement,  � l'utilisation,  � la modification et/ou au
d�veloppement et � la reproduction du logiciel par l'utilisateur �tant 
donn� sa sp�cificit� de logiciel libre, qui peut le rendre complexe � 
manipuler et qui le r�serve donc � des d�veloppeurs et des professionnels
avertis poss�dant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invit�s � charger  et  tester  l'ad�quation  du
logiciel � leurs besoins dans des conditions permettant d'assurer la
s�curit� de leurs syst�mes et ou de leurs donn�es et, plus g�n�ralement, 
� l'utiliser et l'exploiter dans les m�mes conditions de s�curit�. 

Le fait que vous puissiez acc�der � cet en-t�te signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accept� les
termes.
*/

package plugins.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTextPane;

import facade.JTransAPI;

import plugins.text.elements.Element;
import plugins.text.elements.Element_Mot;
import plugins.text.elements.Element_Commentaire;
import plugins.text.elements.Element_DebutChevauchement;
import plugins.text.elements.Element_FinChevauchement;
import plugins.text.elements.Element_Locuteur;
import plugins.text.elements.Element_Ponctuation;
import plugins.text.elements.Locuteur_Info;

/**
 * Le texte est stock� sous forme d'une liste d'Element.
 * Element est une interface d�finie pour englober tous les types
 * n�cessaires pour stocker les informations encod�s dans les fichier texte
 * ou dans les fichiers TRS.
 * 
 * Les Elements forment un arbre de type pattern Composite, 
 * m�me si il n'y a pas vraiment de composite pour l'instant. 
 * Il est bien entendu possible de rajouter 
 * ou supprimer facilement des �l�ments en fonction des besoins. 
 * (ajout de nouvelles conventions, etc)
 * Il faudra juste rajouter/supprimer ces �l�ments des parsers et des Visitors.
 * 
 * Le remplissage de la liste est assur� soit par des appels de m�thodes,
 * pour l'interaction interface graphique/liste de mot directe,
 * soit par des Parser, pour remplir la liste � partir d'un fichier.
 * 
 * Un Visitor, la classe ListeElementVisitor, 
 * impl�mante plusieurs fa�ons de parcourir cette liste.
 * Bien entendu, il est facile de rajouter d'autres Visitor, 
 * afin de g�rer de nouveaux formats.
 *
 * La fonction paint du temporalSigPanel implemente aussi un mini-Visitor
 * afin de parcourir et afficher les �l�ments de la liste qui doivent l'�tre.
 */
public class ListeElement extends ArrayList<Element> implements Serializable {

	public static final long serialVersionUID = 1;
	
	//----------- Champs priv�s --------------
	private ArrayList<Locuteur_Info> locuteursInfo;
	
	public int importAlign(int[] mots2segidx, int fromMot) {
		List<Element_Mot> mots = getMots();
		assert mots2segidx.length==mots.size();
		int firstWordNotAligned=fromMot;
		for (int i=mots.size()-1;i>=fromMot;i--) {
			if (firstWordNotAligned==fromMot&&mots2segidx[i]>=0) firstWordNotAligned=i+1;
			mots.get(i).posInAlign=mots2segidx[i];
		}
		return firstWordNotAligned;
	}
	
	private Element_Mot[] seg2mot = null;
	// TODO: il faut mettre a jour l'index a la moindre modification !

	/**
	 * Refresh reverse index (segment indices to word indices).
	 */
	public void refreshIndex() {
		List<Element_Mot> mots = getMots();
		if (mots.size()==0) return;
		int l = mots.size()-1;
		int lastseg=-1;
		while (l>=0 && lastseg<0) {
			lastseg = mots.get(l--).posInAlign;
		}
		System.out.println("indexmots lastseg "+lastseg);
		seg2mot = new Element_Mot[lastseg+1];
		for (Element_Mot m : mots) {
			if (m.posInAlign>=0) seg2mot[m.posInAlign]=m;
		}
		System.out.println("index mots ok "+seg2mot.length);
	}
	public Element_Mot getMotAtSegment(int segidx) {
		if (seg2mot==null) {
			refreshIndex();
			if (seg2mot==null) return null;
		}
		if (segidx>=seg2mot.length) return null;
		return seg2mot[segidx];
	}
	
	public Element_Mot getWordElement(int widx) {
		int j=-1;
		for (int i=0;i<size();i++) {
			Element e = get(i);
			if (e instanceof Element_Mot) {
				if (++j==widx) return (Element_Mot)e;
			}
		}
		return null;
	}
	
	public void load(BufferedReader f, JTextPane textarea) {
		JTransAPI.setElts(this);
		try {
			String s = f.readLine();
			assert s.startsWith("listeelements ");
			int nelts = Integer.parseInt(s.substring(14));
			if (nelts<=0) return;
			for (int i=0;i<nelts;i++) {
				s = f.readLine();
				if (s.startsWith("mot")) {
					String[] ss = s.split(" ");
					int pdeb = Integer.parseInt(ss[1]);
					int pfin = Integer.parseInt(ss[2]);
					boolean bruit = false;
					if (ss.length>3) bruit=Boolean.parseBoolean(ss[3]);
					add(Element_Mot.fromSubstring(textarea.getText(), pdeb, pfin, bruit));
				} else if (s.startsWith("loc")) {
					int k=s.indexOf(' ')+1;
					int j=s.indexOf(' ',k);
					int id = Integer.parseInt(s.substring(k,j)); k=j+1; j=s.indexOf(' ',k);
					int num = Integer.parseInt(s.substring(k));
					Element_Locuteur loc = new Element_Locuteur((byte)id,num);
					add(loc);
				} else if (s.startsWith("dchev")) {
					Element_DebutChevauchement ee = new Element_DebutChevauchement();
					add(ee);
				} else if (s.startsWith("fchev")) {
					Element_FinChevauchement ee = new Element_FinChevauchement();
					add(ee);
				} else if (s.startsWith("cmt")) {
					String[] ss = s.split(" ");
					int pdeb = Integer.parseInt(ss[1]);
					int pfin = Integer.parseInt(ss[2]);
					Element_Commentaire mot = Element_Commentaire.fromSubstring(textarea.getText(), pdeb, pfin);
					add(mot);
				} else if (s.startsWith("pun")) {
					int k=s.indexOf(' ')+1;
					char p = s.charAt(k);
					Element_Ponctuation ee = new Element_Ponctuation(p);
					add(ee);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void save(PrintWriter f) {
		f.println("listeelements "+size());
		for (int i=0;i<size();i++) {
			Element e = get(i);
			if (e instanceof Element_Mot) {
				Element_Mot mot = (Element_Mot) e;
				f.println("mot "+mot.posDebInTextPanel+" "+mot.posFinInTextPanel+" "+mot.isBruit);
			} else if (e instanceof Element_Locuteur) {
				Element_Locuteur loc = (Element_Locuteur) e;
				f.println("loc "+loc.getLocuteurID()+" "+loc.getNumeroParole());
			} else if (e instanceof Element_DebutChevauchement) {
				f.println("dchev");
			} else if (e instanceof Element_FinChevauchement) {
				f.println("fchev");
			} else if (e instanceof Element_Commentaire) {
				Element_Commentaire mot = (Element_Commentaire) e;
				f.println("cmt "+mot.posDebInTextPanel+" "+mot.posFinInTextPanel);
			} else if (e instanceof Element_Ponctuation) {
				Element_Ponctuation p = (Element_Ponctuation)e;
				f.println("pun "+p.getPonctuation());
			}
		}
	}

	//----------- Constructeur ---------
	public ListeElement(){
		locuteursInfo = new ArrayList<Locuteur_Info>();
	}
	
	//-----------------------------------------------------------
	//-------------------Partie gestion des locuteurs -----------
	//-----------------------------------------------------------
	public void addLocuteurElement(String locuteurName, int numeroParole){
		//on v�rifie qu'il n'appartient pas d�j� � la liste des locuteurs
		boolean trouve = false;
		byte i = 0;
		byte id = -1;
		while(!trouve && (i < locuteursInfo.size())){
			if(locuteursInfo.get(i).getName().equals(locuteurName)) {
				id = i;
				trouve = true;
			}
			i++;
		}
		
		//Si il n'a pas �t� trouv�, on l'ajoute � la liste des locuteurs
		if(!trouve){
			id = (byte)locuteursInfo.size();
			locuteursInfo.add(new Locuteur_Info(id,locuteurName));
		}
		
		//On rajoute l'element locuteur � la liste
		Element_Locuteur elementLocuteur = new Element_Locuteur(id,numeroParole);
		add(elementLocuteur);
	}//addLocuteurElement
	
	

	public ArrayList<String> getLocuteurs(){
		ArrayList<String> res = new ArrayList<String>();
		
		for(Locuteur_Info info : locuteursInfo){
			res.add(info.getName());
		}
		return res;
	}//getLocuteurs
	
	public int getNbLocuteur(){
		return locuteursInfo.size();
	}
	
	public String getLocuteurName(int indice){
		for(Locuteur_Info info : locuteursInfo){
			if(info.getId() == indice) {
				return info.getName();
			}
		}
		return null;
	}//getLocuteurName


	/**
	 * Fills a mapping of segments (array indices) to speaker IDs (array items).
	 * @param seg2spk an array whose length is the total segment count
	 * @return the mapping
	 */
	public byte[] getSegmentToSpeakerIndex(byte[] seg2spk) {
		int startSeg = 0;
		int currSeg = 0;
		byte currSpk = -1;
		for (Element el: this) {
			if (el instanceof Element_Locuteur) {
				Arrays.fill(seg2spk, startSeg, currSeg + 1, currSpk);
				startSeg = currSeg + 1;
				currSpk = ((Element_Locuteur) el).getLocuteurID();
			} else if (el instanceof Element_Mot) {
				int seg = ((Element_Mot) el).posInAlign;
				// sometimes words get stuck at -1 because they couldn't be aligned
				if (seg > currSeg)
					currSeg = seg;
				assert currSeg < seg2spk.length;
			}
		}
		return seg2spk;
	}


	/**
	 * Fonction permettant de r�cup�rer une ArrayList
	 * contenant uniquement les �l�ments Element_Mot de la liste globale.
	 * 
	 * @return ArrayList<Element_Mot>
	 */
	public List<Element_Mot> getMots(){
		ArrayList<Element_Mot> res = new ArrayList<Element_Mot>();
		for(Element element:this){
			if(element instanceof Element_Mot ) res.add((Element_Mot)element);
		}
		return res;
	}//getMots
	public String[] getMotsInTab() {
		List<Element_Mot> l = getMots();
		String[] r = new String[l.size()];
		for (int i=0;i<r.length;i++) r[i]=l.get(i).getWordString();
		return r;
	}
	
	
	public Element_Mot getElementAvecDureePrecedent(int posi){
		while(--posi > 0){
			if(get(posi) instanceof Element_Mot )
				return (Element_Mot) get(posi);
		}
		return null;
	}//getMotPrecedent
	
	public int getIndiceElementAvecDureePrecedent(int posi){
		while(--posi > 0){
			if(get(posi) instanceof Element_Mot )
				return posi;
		}
		return -1;
	}
	
	public Element_Mot getElementAvecDureeSuivant(int posi){
		while(posi++ > 0){
			if(get(posi) instanceof Element_Mot )
				return (Element_Mot) get(posi);
		}
		return null;
	}//getMotPrecedent
	
	
	
//	//TODO : remplacer par une recherche dichotomique ? => normalement faisable, 
//	//puisque les mots sont cens�s �tre ordonn�s par endSample
//	public Element_Mot getElementAtSample(int endSample){
//		for (int i=0;i<size();i++) {
//			Element e = get(i);
//			if(e instanceof Element_Mot){
//				Element_Mot ee=(Element_Mot)e;
//				if(ee.endSample >= endSample) return ee; 
//			}
//		}
//		return null;
//	}//getElementAtSample
	
//	public int getIndiceAtSample(long endSample){
//		int size = size();
//		Element e;
//		for(int i = 0; i < size; ++i){
//			e = get(i);
//			if(e instanceof Element_Mot){
//				if( ((Element_Mot)e).endSample >= endSample) return i; 
//			}
//		}
//		return -1;
//	}//getIndiceAtSample
//	
	
//	public void deplacerTousLesElementsDe(int posiDebut,int nbSampleDeplacement){
//		int size = size();
//		Element e;
//		Element_Mot mot;
//		for(int i = posiDebut; i < size; i++){
//			e = get(i);
//			if(e instanceof Element_Mot){
//				mot = (Element_Mot)e;
//				mot.endSample=mot.endSample+nbSampleDeplacement;
//			}
//		}//for
//	}//deplacerTousLesElementsDe
	
//	public void cleanAlignFromElement(int eltidx) {
//		Element element;
//		int size = size();
//		for(int i = eltidx; i < size; i++){
//			element = get(i);
//			if(element instanceof Element_Mot){
//				Element_Mot seg = (Element_Mot)element;
//				seg.endSample=-1;
//			}
//		}
//	}
	
//	public void cleanAlignFromSample(long sample) {
//		Element element;
//		int size = size();
//		boolean removeNext=false;
//		for(int i = 0; i < size; i++){
//			element = get(i);
//			if(element instanceof Element_Mot){
//				Element_Mot seg = (Element_Mot)element;
//				long sampfin = seg.endSample;
//				if (removeNext || sampfin>sample) {
//					seg.endSample=-1;
//					for (i++;i<size;i++) {
//						element = get(i);
//						if(element instanceof Element_Mot){
//							seg = (Element_Mot)element;
//							seg.endSample=-1;
//						}
//					}
//					return;
//				} else if (sampfin==sample) removeNext=true;
//			}
//		}
//	}
	
//	public int getIndiceMotAtSample(long sample) {
//		Element element;
//		int size = size();
//		for(int i = 0; i < size; i++){
//			element = get(i);
//			if(element instanceof Element_Mot){
//				Element_Mot seg = (Element_Mot)element;
//				long sampfin = seg.endSample;
//				if (sampfin>sample) {
//					// si ce n'est pas un mot, on retourne le mot precedent
//					if (element instanceof Element_Mot) return i;
//					else {
//						for (i--;i>=0;i--) {
//							element = get(i);
//							if (element instanceof Element_Mot) return i;
//						}
//						return -1;
//					}
//				}
//			}
//		}
//		return -1;
//	}
	
	public Element_Mot getFirstMot() {
		int size = size();
		for(int i = 0; i < size; i++){
			Element e = get(i);
			if (e instanceof Element_Mot) {
				Element_Mot ee = (Element_Mot)e;
				return ee;
			}
		}		
		return null;
	}
	public Element_Mot getMot(int motidx) {
		int size = size();
		for(int i = 0, imot=-1; i < size; i++){
			Element e = get(i);
			if (e instanceof Element_Mot) {
				imot++;
				if (imot==motidx) {
					Element_Mot ee = (Element_Mot)e;
					return ee;
				}
			}
		}		
		return null;
	}
	
//	public Element_Mot getWordFromFirstWord(int motidx, Element_Mot firstWord) {
//		Element_Mot e = firstWord;
//		for (int i=0;i<motidx;i++) {
//			e = e.nextEltInGram;
//		}
//		return e;
//	}
//	

	/**
	 * retourne l'indice de l'element dans la liste qui a ete selectionne (c'est un mot)
	 * 
	 * @param posiDansLeTexte
	 * @return
	 */
	public int getIndiceElementAtTextPosi(int posiDansLeTexte){
		Element_Mot mot;
		Element element;
		int size = size();
		for(int i = 0; i < size; i++){
			element = get(i);
			if(element instanceof Element_Mot){
				mot = (Element_Mot)element;
				if(mot.posDebInTextPanel <= posiDansLeTexte){
					if(mot.posFinInTextPanel >= posiDansLeTexte) return i;
				}
			}
		}
		return -1;
	}//getMotAtTextPosi
	public int getIndiceMotAtTextPosi(int posiDansLeTexte){
		List<Element_Mot> mots = getMots();
		for(int i = 0;i<mots.size(); i++) {
			Element_Mot mot = mots.get(i);
			if(mot.posDebInTextPanel <= posiDansLeTexte){
				if(mot.posFinInTextPanel >= posiDansLeTexte) return i;
			}
		}
		return -1;
	}//getMotAtTextPosi
	
	public int indiceMot=-1;
	public Element_Mot getMotAtTextPosi(int posiDansLeTexte){
		List<Element_Mot> mots = getMots();
		for(int i = 0;i<mots.size(); i++) {
			Element_Mot mot = mots.get(i);
			if(mot.posDebInTextPanel <= posiDansLeTexte){
				if(mot.posFinInTextPanel >= posiDansLeTexte) {
					indiceMot=i;
					return mot;
				}
			}
		}
		return null;
	}//getMotAtTextPosi
	
	
	public void decalerTextPosi(int valeur, int from){
		//On parcourt la liste pour d�caler tous les indices des mots
		int listeElementSize = size();
		Element element; 
		Element_Mot elementMot;
		for(int i = from; i < listeElementSize; ++i ){
			element = get(i);
			if(element instanceof Element_Mot){
				elementMot = (Element_Mot)element;
				elementMot.posDebInTextPanel += valeur;
				elementMot.posFinInTextPanel += valeur;
			}
		}//for
	}
	
	
	
	
	
	
	
	
	
	
	
	//------------- Anciennes Fonctions stock�es ici, au cas o� elles serviraient un jour
	/**
	 * ajoute un element avant le Nieme mot (attention: N ne correspond pas a une
	 * position dans la liste, mais a la position dans la liste des mots seulement
	 * (telle que obtenue par getMots())
	 * N commence a 0 : N=0 correspond donc au 1er mot de la liste
	 */
	/*
	public void addElementAvantNiemeMot(int niemeMot, Element element) {
		int idxMots=-1;
		for (int i=0;i<size();i++) {
			Element el = get(i);
			if (el instanceof Element_Mot) {
				idxMots++;
				if (idxMots==niemeMot) {
					add(i,element);
					return;
				}
			}
		}
		System.err.println("WARNING: impossible d'ajouter le Nieme mot "+niemeMot+" "+size());
	}//addMotAvantNiemeMot
	*/
	/*
	public Element_Silence addSilenceAvantNiemeMot(int niemeMot, int endSample) {
		int idxMots=-1;
		Element_Silence res = new Element_Silence(endSample);
		for (int i=0;i<size();i++) {
			Element el = get(i);
			if (el instanceof Element_Mot) {
				idxMots++;
				if (idxMots==niemeMot) {
					add(i,res);
					return res;
				}
			}
		}
		System.err.println("WARNING: impossible d'ajouter le Nieme silence "+niemeMot+" "+size());
		return null;
	}//addMotAvantNiemeMot
	*/
	/*
	public void addMotAtPosi(int indiceMot, String mot, int endSample){
		Element_Mot elementMot = new Element_Mot(mot,endSample);
		
		Element element = get(indiceMot);
		if(element instanceof Element_Commentaire && 
				((Element_Commentaire)element).getCommentaire().startsWith("pron")) {
			indiceMot++;
		}
		add(indiceMot,elementMot);
	}//addMotAtPosi
	*/
	
}//class ListeElement
