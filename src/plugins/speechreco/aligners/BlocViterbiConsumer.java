/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant e aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est regi par la licence CeCILL-C soumise au droit franeais et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffusee par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilite au code source et des droits de copie,
de modification et de redistribution accordes par cette licence, il n'est
offert aux utilisateurs qu'une garantie limitee.  Pour les memes raisons,
seule une responsabilite restreinte pese sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les concedants successifs.

A cet egard  l'attention de l'utilisateur est attiree sur les risques
associes au chargement,  e l'utilisation,  e la modification et/ou au
developpement et e la reproduction du logiciel par l'utilisateur etant 
donne sa specificite de logiciel libre, qui peut le rendre complexe e 
manipuler et qui le reserve donc e des developpeurs et des professionnels
avertis possedant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invites e charger  et  tester  l'adequation  du
logiciel e leurs besoins dans des conditions permettant d'assurer la
securite de leurs systemes et ou de leurs donnees et, plus generalement, 
e l'utiliser et l'exploiter dans les memes conditions de securite. 

Le fait que vous puissiez acceder e cet en-tete signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accepte les
termes.
*/

package plugins.speechreco.aligners;

import plugins.text.elements.Element_Mot;

public interface BlocViterbiConsumer {
	public void newMot(OldAlignment mot, Element_Mot motDepart);
}
