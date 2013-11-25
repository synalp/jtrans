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

package plugins.signalViewers.temporalSigPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;

import plugins.text.elements.Locuteur_Info;

/** Popup affichant la legende couleur/nom du locuteur. */
public class PopupMenuLegende extends JPopupMenu {

	//-------- Private Fields ---------
	private TemporalSigPanel temporalSigPanel;

	private BoutonCouleur[] boutonsCouleurs;
	private JLabel[] labelsCouleurs;


	//------- Constructor ----------
	public PopupMenuLegende(TemporalSigPanel temporalSigPan) {
		super();
		this.temporalSigPanel = temporalSigPan;


		//--- la taille des tableaux est definie par le temporalSigPanel;
		ArrayList<Color> tabColors = temporalSigPanel.getColorsLocuteurs();
		ArrayList<String> tabColorsName = new ArrayList<String>();
		for (Locuteur_Info info: temporalSigPanel.aligneur.project.speakers)
			tabColorsName.add(info.getName());

		
		boutonsCouleurs = new BoutonCouleur[tabColorsName.size()];
		labelsCouleurs = new JLabel[tabColorsName.size()];


		//--- Configuration du layout -------
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		GroupLayout.ParallelGroup labelGroup = layout.createParallelGroup();
		GroupLayout.ParallelGroup boutonGroup = layout.createParallelGroup();

		GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
		GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

		GroupLayout.ParallelGroup ligneGroup;


		Dimension dim = new Dimension(50,20);
		for(int i = 0; i < tabColorsName.size(); ++i){
			ligneGroup = layout.createParallelGroup();

			//Creation des labels
			labelsCouleurs[i] = new JLabel(tabColorsName.get(i));
			labelGroup.addComponent(labelsCouleurs[i]);
			ligneGroup.addComponent(labelsCouleurs[i]);

			//Creation des boutons
			boutonsCouleurs[i] = new BoutonCouleur(i,tabColors.get(i),dim);


			boutonGroup.addComponent(boutonsCouleurs[i]);
			ligneGroup.addComponent(boutonsCouleurs[i]);

			ligneGroup.addGap(15);
			vGroup.addGroup(ligneGroup);
		}//for



		hGroup.addGroup(labelGroup);
		hGroup.addGroup(boutonGroup);
		layout.setHorizontalGroup(hGroup);
		layout.setVerticalGroup(vGroup);


	}//constructor 



	/** Classe des boutons selectionnant les couleurs */
	private class BoutonCouleur extends JButton implements ActionListener {

		private int indice;

		public BoutonCouleur(int indice, Color color, Dimension dim){
			this.indice = indice;
			// TODO setting a button's background color won't work in OS X
			setBackground(color);
			setPreferredSize(dim);
			setMinimumSize(dim);
			addActionListener(this);
		}//constructor

		public void actionPerformed(ActionEvent e){
			JColorChooser colorPanel = new JColorChooser(getBackground());

			Object[] options = {"Editer","Annuler"};

			int selectedOption = JOptionPane.showOptionDialog(null,
					colorPanel,
					"Reglage des couleurs",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,     //do not use a custom Icon
					options,  //the titles of buttons
					options[0]); //default button title

			if(selectedOption == JOptionPane.YES_OPTION){
				temporalSigPanel.getColorsLocuteurs().set(indice,colorPanel.getColor());
				temporalSigPanel.repaint();
				setBackground(colorPanel.getColor());
			}//if
		}//action
	}//class BoutonCouleur

}//class PopupMenuLegende
