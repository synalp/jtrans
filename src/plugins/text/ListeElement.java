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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import speechreco.aligners.sphiinx4.Alignment;
import plugins.text.elements.*;

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
	private Element_Mot[] seg2mot = null;
	// TODO: il faut mettre a jour l'index a la moindre modification !
	public int indiceMot=-1;
	
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


	/**
	 * Creates an alignment whose segments spans speaker turns.
	 * This method does not account for overlaps, hence the produced
	 * alignment is 'linear'.
	 */
	public Alignment getLinearSpeakerTimes(Alignment words) {
		Alignment speakerAlignment = new Alignment();

		int startSeg = 0;
		int currSeg = 0;
		byte currSpk = -1;

		for (int i = 0; i < size(); i++) {
			Element el = get(i);

			// Switching to next speaker or reached the last element:
			// add new speaker segment
			if ((i == size()-1 || el instanceof Element_Locuteur) &&
					currSpk != -1 && startSeg < currSeg)
			{
				speakerAlignment.addRecognizedSegment("" + currSpk,
						words.getSegmentDebFrame(startSeg),
						words.getSegmentEndFrame(currSeg),
						null,
						null);
			}

			// Adjust current segment
			if (el instanceof Element_Locuteur) {
				startSeg = currSeg + 1;
				currSpk = ((Element_Locuteur) el).getLocuteurID();
			} else if (el instanceof Element_Mot) {
				int seg = ((Element_Mot) el).posInAlign;
				// sometimes words get stuck at -1 because they couldn't be aligned
				if (seg > currSeg)
					currSeg = seg;
			}
		}

		return speakerAlignment;
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
				if (mot.start <= posiDansLeTexte && mot.end >= posiDansLeTexte)
					return i;
			}
		}
		return -1;
	}


	public int getIndiceMotAtTextPosi(int posiDansLeTexte){
		List<Element_Mot> mots = getMots();
		for(int i = 0;i<mots.size(); i++) {
			Element_Mot mot = mots.get(i);
			if (mot.start <= posiDansLeTexte && mot.end >= posiDansLeTexte)
				return i;
		}
		return -1;
	}


	public Element_Mot getMotAtTextPosi(int posiDansLeTexte){
		List<Element_Mot> mots = getMots();
		for(int i = 0;i<mots.size(); i++) {
			Element_Mot mot = mots.get(i);
			if (mot.start <= posiDansLeTexte && mot.end >= posiDansLeTexte) {
				indiceMot=i;
				return mot;
			}
		}
		return null;
	}
	
	
	public void decalerTextPosi(int valeur, int from){
		//On parcourt la liste pour d�caler tous les indices des mots
		int listeElementSize = size();
		Element element; 
		Element_Mot elementMot;
		for(int i = from; i < listeElementSize; ++i ){
			element = get(i);
			if(element instanceof Element_Mot){
				elementMot = (Element_Mot)element;
				elementMot.start += valeur;
				elementMot.end += valeur;
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
