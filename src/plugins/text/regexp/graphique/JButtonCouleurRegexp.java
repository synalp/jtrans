/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

/*
Copyright Christophe Cerisara, Josselin Pierre (1er septembre 2008)

cerisara@loria.fr

Ce logiciel est un programme informatique servant à aligner un
corpus de parole avec sa transcription textuelle.

Ce logiciel est régi par la licence CeCILL-C soumise au droit français et
respectant les principes de diffusion des logiciels libres. Vous pouvez
utiliser, modifier et/ou redistribuer ce programme sous les conditions
de la licence CeCILL-C telle que diffusée par le CEA, le CNRS et l'INRIA 
sur le site "http://www.cecill.info".

En contrepartie de l'accessibilité au code source et des droits de copie,
de modification et de redistribution accordés par cette licence, il n'est
offert aux utilisateurs qu'une garantie limitée.  Pour les mêmes raisons,
seule une responsabilité restreinte pèse sur l'auteur du programme,  le
titulaire des droits patrimoniaux et les concédants successifs.

A cet égard  l'attention de l'utilisateur est attirée sur les risques
associés au chargement,  à l'utilisation,  à la modification et/ou au
développement et à la reproduction du logiciel par l'utilisateur étant 
donné sa spécificité de logiciel libre, qui peut le rendre complexe à 
manipuler et qui le réserve donc à des développeurs et des professionnels
avertis possédant  des  connaissances  informatiques approfondies.  Les
utilisateurs sont donc invités à charger  et  tester  l'adéquation  du
logiciel à leurs besoins dans des conditions permettant d'assurer la
sécurité de leurs systèmes et ou de leurs données et, plus généralement, 
à l'utiliser et l'exploiter dans les mêmes conditions de sécurité. 

Le fait que vous puissiez accéder à cet en-tête signifie que vous avez 
pris connaissance de la licence CeCILL-C, et que vous en avez accepté les
termes.
*/

package plugins.text.regexp.graphique;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import plugins.text.regexp.TypeElement;

public class JButtonCouleurRegexp extends JButton implements ActionListener {

	//------ private Fields -------
	private TypeElement typeElement;
	
	
	//-------- Constructor -------
	public JButtonCouleurRegexp(TypeElement typeElement){
		this.typeElement = typeElement;
		setBackground(typeElement.getColor());
		setText("Couleur");
		addActionListener(this);
	}//constructor
	
	public void actionPerformed(ActionEvent e){
		SelecteurCouleur colorPanel = new SelecteurCouleur();
		colorPanel.setColor(getBackground());
		
		Object[] options = {"Editer","Annuler"};

		int selectedOption = JOptionPane.showOptionDialog(null,
				colorPanel,
				"Réglage des couleurs",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,     //do not use a custom Icon
				options,  //the titles of buttons
				options[0]); //default button title
		
		if(selectedOption == JOptionPane.YES_OPTION){
			typeElement.setColor(colorPanel.getColor());
			setBackground(colorPanel.getColor());
		}//if
	}//actionPerformed
	
}//JButton
