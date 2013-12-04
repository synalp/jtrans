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

public class ListeElement extends ArrayList<Element> implements Serializable {
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
		seg2mot = new Element_Mot[lastseg+1];
		for (Element_Mot m : mots) {
			if (m.posInAlign>=0) seg2mot[m.posInAlign]=m;
		}
	}
	public Element_Mot getMotAtSegment(int segidx) {
		if (seg2mot==null) {
			refreshIndex();
			if (seg2mot==null) return null;
		}
		if (segidx>=seg2mot.length) return null;
		return seg2mot[segidx];
	}


	/**
	 * Neighbors of an Element in the list
	 * @param <T> subclass of Element
	 */
	public class Neighborhood<T extends Element> {
		public final T prev;
		public final T next;
		public final int prevIdx;
		public final int nextIdx;

		private Neighborhood(int p, int n) {
			prevIdx = p;
			nextIdx = n;

			prev = prevIdx>=0? (T)get(prevIdx): null;
			next = nextIdx>=0? (T)get(nextIdx): null;
		}
	}

	/**
	 * Returns the surrounding neighbors (of a specific class) of an element.
	 * @param central central element
	 * @param surroundClass class of the surrounding neighbors
	 * @param <T> should be the same type as surroundClass
	 */
	public <T extends Element> Neighborhood<T> getNeighbors(
			Element central, Class<T> surroundClass)
	{
		int prev = -1;
		int curr = -1;
		int next = -1;

		for (int i = 0; i < size(); i++) {
			Element el = get(i);

			if (el == central) {
				curr = i;
			} else if (!surroundClass.isInstance(el)) {
				;
			} else if (curr < 0) {
				prev = i;
			} else {
				next = i;
				break;
			}
		}

		return new Neighborhood<T>(prev, next);
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
	
	public Element_Mot getElementAvecDureePrecedent(int posi){
		while(--posi > 0){
			if(get(posi) instanceof Element_Mot )
				return (Element_Mot) get(posi);
		}
		return null;
	}//getMotPrecedent
	
	public Element_Mot getElementAvecDureeSuivant(int posi){
		while(posi++ > 0){
			if(get(posi) instanceof Element_Mot )
				return (Element_Mot) get(posi);
		}
		return null;
	}//getMotPrecedent

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

	/**
	 * retourne l'indice de l'element dans la liste qui a ete selectionne (c'est un mot)
	 * 
	 * @param posiDansLeTexte
	 * @return
	 */
	public int getIndiceElementAtTextPosi(int posiDansLeTexte) {
		for (int i = 0; i < size(); i++) {
			Element el = get(i);
			if (el.start <= posiDansLeTexte && el.end >= posiDansLeTexte)
				return i;
		}
		return -1;
	}
}
