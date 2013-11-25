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

package plugins.text.regexp.graphique;


import java.awt.BorderLayout;
import java.util.regex.Pattern;

import javax.swing.*;

import facade.Project;
import plugins.text.regexp.TypeElement;
import plugins.text.regexp.controler.ActionAddRegexp;
import plugins.text.regexp.controler.ActionDeleteRegexp;

/** Fenetre d'edition des regexp */
public class RegExpFrame extends JFrame {

	private static final String EXPLANATION =
			"Cette fenêtre vous permet d'éditer les expressions regulières "
			+ "qui permettent de reconnaître les différentes syntaxes présentes dans "
			+ "les fichiers textes que vous allez ouvrir.\n"
			+ "Ces expressions régulières utilisent la syntaxe d'expression du langage Java.\n"
			+ "Une description de la syntaxe est disponible sur la page :\n"
			+ "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html\n\n"
			+ "Attention, ne modifiez cela que si vous savez ce que vous faites !";


	public RegExpFrame(Project project) {
		setTitle("Regexp Editor");

		JTabbedPane tabbedPane = new JTabbedPane();

		JTextPane explanation = new JTextPane();
		explanation.setText(EXPLANATION);
		tabbedPane.add(new JScrollPane(explanation), "Help");

		JPanel pan;
		JList<Pattern> liste;
		//Un panel pour chaque element defini.
		for (TypeElement typeElement : project.types) {
			pan = new JPanel();
			pan.setLayout(new BorderLayout());

			liste = new JList<Pattern>(typeElement.getPatterns());
			pan.add(liste,BorderLayout.CENTER);

			//---- Creation du panel des boutons ----------
			JPanel panBoutons = new JPanel();
			panBoutons.setLayout(new BoxLayout(panBoutons,BoxLayout.X_AXIS));

			JButton boutonAdd = new JButton("Add");
			boutonAdd.addActionListener(new ActionAddRegexp(typeElement, liste));
			panBoutons.add(boutonAdd);

			JButton boutonDel = new JButton("Remove");
			boutonDel.addActionListener(new ActionDeleteRegexp(typeElement, liste));
			panBoutons.add(boutonDel);

			panBoutons.add(Box.createHorizontalGlue());
			panBoutons.add(new JButtonCouleurRegexp(typeElement));

			pan.add(panBoutons,BorderLayout.SOUTH);

			tabbedPane.add(pan,typeElement.getName());
		}

		getContentPane().add(tabbedPane);
		this.setSize(500,300);
		this.setVisible(true);
	}
}
