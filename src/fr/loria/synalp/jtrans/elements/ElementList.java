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

package fr.loria.synalp.jtrans.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ElementList extends ArrayList<Element> implements Serializable {
	private Word[] seg2mot = null;
	// TODO: il faut mettre a jour l'index a la moindre modification !

	/**
	 * Refresh reverse index (segment indices to word indices).
	 */
	public void refreshIndex() {
		List<Word> mots = getMots();
		if (mots.size()==0) return;
		int l = mots.size()-1;
		int lastseg=-1;
		while (l>=0) {
			int s = mots.get(l--).posInAlign;
			if (s > lastseg)
				lastseg = s;
		}
		seg2mot = new Word[lastseg+1];
		for (Word m : mots) {
			if (m.posInAlign>=0) seg2mot[m.posInAlign]=m;
		}
	}
	public Word getMotAtSegment(int segidx) {
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
	 * Fonction permettant de r�cup�rer une ArrayList
	 * contenant uniquement les �l�ments Element_Mot de la liste globale.
	 * 
	 * @return ArrayList<Word>
	 */
	public List<Word> getMots(){
		ArrayList<Word> res = new ArrayList<Word>();
		for(Element element:this){
			if(element instanceof Word) res.add((Word)element);
		}
		return res;
	}//getMots

	public Word getMot(int motidx) {
		int size = size();
		for(int i = 0, imot=-1; i < size; i++){
			Element e = get(i);
			if (e instanceof Word) {
				imot++;
				if (imot==motidx) {
					Word ee = (Word)e;
					return ee;
				}
			}
		}		
		return null;
	}
}
