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

package fr.loria.synalp.jtrans.gui;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.*;

import fr.loria.synalp.jtrans.facade.Project;
import fr.loria.synalp.jtrans.elements.ElementType;


public class RegExpFrame extends JFrame {

	private static final String EXPLANATION =
			"Cette fenêtre vous permet d'éditer les expressions regulières "
			+ "qui permettent de reconnaître les différentes syntaxes présentes dans "
			+ "les fichiers textes que vous allez ouvrir.\n"
			+ "Ces expressions régulières utilisent la syntaxe d'expression du langage Java.\n"
			+ "Une description de la syntaxe est disponible sur la page :\n"
			+ "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html\n\n"
			+ "Attention, ne modifiez cela que si vous savez ce que vous faites !";


	private static class ColorIcon implements Icon {
		private int size;
		private Color color;

		public ColorIcon(int size, Color color) {
			this.size = size;
			this.color = color;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setColor(Color.black);
			g2d.fillOval(x, y, size, size);
			g2d.setColor(color);
			g2d.fillOval(x+1, y+1, size-2, size-2);
		}

		@Override
		public int getIconWidth() {
			return size;
		}

		@Override
		public int getIconHeight() {
			return size;
		}
	}


	public RegExpFrame(Project project) {
		setTitle("Regexp Editor");

		JTabbedPane tabbedPane = new JTabbedPane();

		JTextPane explanation = new JTextPane();
		explanation.setText(EXPLANATION);
		tabbedPane.add(new JScrollPane(explanation), "Help");

		JPanel pan;
		//Un panel pour chaque element defini.
		for (final ElementType elementType: project.types) {
			pan = new JPanel();
			pan.setLayout(new BorderLayout());

			final JList<Pattern> list = new JList<Pattern>(elementType.getPatterns());
			pan.add(list,BorderLayout.CENTER);

			JPanel panBoutons = new JPanel();
			JButton add = new JButton("Add");
			JButton remove = new JButton("Remove");
			JButton color = new JButton("Color");

			panBoutons.setLayout(new BoxLayout(panBoutons,BoxLayout.X_AXIS));
			panBoutons.add(add);
			panBoutons.add(remove);
			panBoutons.add(Box.createHorizontalGlue());
			panBoutons.add(color);

			// The color must be shown as an icon instead of a background color.
			// Button background colors have no effect in OS X.
			color.setIcon(new ColorIcon(20, elementType.getColor()));

			pan.add(panBoutons,BorderLayout.SOUTH);

			tabbedPane.add(pan, elementType.getName());

			add.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String str = JOptionPane.showInputDialog(null,
							"Enter regexp for this type:");
					if (str != null){
						try {
							elementType.addRegexp(str);
							list.setListData(elementType.getPatterns());
						} catch (PatternSyntaxException ex) {
							JOptionPane.showMessageDialog(list, "Illegal regexp");
						}
					}
				}});

			remove.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int index = list.getSelectedIndex();
					if (index != -1) {
						elementType.removePattern(index);
						list.setListData(elementType.getPatterns());
					}
				}
			});

			color.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JColorChooser colorPanel = new JColorChooser(elementType.getColor());

					int selectedOption = JOptionPane.showConfirmDialog(null,
							colorPanel,
							"Color for \"" + elementType.getName() + "\"",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);

					if (selectedOption == JOptionPane.OK_OPTION){
						elementType.setColor(colorPanel.getColor());
						setBackground(colorPanel.getColor());
					}
				}
			});
		}

		getContentPane().add(tabbedPane);
		this.setSize(500,300);
		this.setVisible(true);
	}
}
