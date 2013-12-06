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

package jtrans.gui.signalViewers.temporalSigPanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/** Panel contenant les differents boutons utiles pour le TemporalSig
 * choix du curseur, etc */
public class ToolBarTemporalSig extends JToolBar implements Observer {

	//------ Private Fields --------------
	private TemporalSigPanel temporalSigPanel;


	//------ pour le selecteur de curseur ---------
	private final ImageIcon iconeHand = new ImageIcon(getClass().getResource("/ressources/icones/hand.gif"));
	private final ImageIcon iconeArrow = new ImageIcon(getClass().getResource("/ressources/icones/arrow.gif"));

	private JToggleButton cursorHand;
	private JToggleButton cursorNormal;
	
	//-------- pour la legende -----
	private final ImageIcon iconeLegende = new ImageIcon(getClass().getResource("/ressources/icones/legende.png"));
	
	//------ pour le goto -------
	private final ImageIcon iconeError = new ImageIcon(getClass().getResource("/ressources/icones/error.png"));
	private JTextField gotoTextField;
	private final ImageIcon iconeNext = new ImageIcon(getClass().getResource("/ressources/icones/next.png"));
	
	//--------- Constructor -----------
	public ToolBarTemporalSig(TemporalSigPanel temporalSig) {
		super();
		temporalSigPanel = temporalSig;
		temporalSigPanel.addObserver(this);
		
		this.setFloatable(false);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		cursorNormal = new JToggleButton(iconeArrow);
		cursorNormal.setToolTipText("Curseur de selection");
		cursorNormal.setSelected(true);
		cursorNormal.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				temporalSigPanel.setCursorNormal();
			}
		});
		buttonGroup.add(cursorNormal);
		add(cursorNormal);
		
		cursorHand = new JToggleButton(iconeHand);
		cursorHand.setToolTipText("Curseur de deplacement");
		cursorHand.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				temporalSigPanel.setCursorHand();
			}
		});
		buttonGroup.add(cursorHand);
		add(cursorHand);
		
		add(Box.createHorizontalStrut(20));
		
		JButton legende = new JButton(iconeLegende);
		legende.setToolTipText("Legende");
		legende.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				showLegend(arg0);
			}
		});
		add(legende);
		
		//--------------- Partie GoTo --------------------
		//add(Box.createRigidArea(new Dimension(25,20)));
		add(Box.createGlue());
		
		JLabel jl =new JLabel("Aller e : "); 
		add(jl);
		gotoTextField = new JTextField(15);
		gotoTextField.setEditable(true);
		gotoTextField.setToolTipText("Entrer une duree de la forme ...h...[min|m]...[sec|s]...ms");
		gotoTextField.setPreferredSize(new Dimension(120,jl.getPreferredSize().height));
		gotoTextField.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER){
					gotoPosi();
				}
			}
			public void keyReleased(KeyEvent arg0) {}

			public void keyTyped(KeyEvent arg0) {}
		});
		add(gotoTextField);
		
		JButton go = new JButton(iconeNext);
		go.setToolTipText("Aller e la duree indiquee.");
		go.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				gotoPosi();
			}
		});
		add(go);

		add(Box.createGlue());
		JButton zoomin = new JButton("Z+");
		add(zoomin);
		JButton zoomout = new JButton("Z-");
		add(zoomout);
		add(Box.createGlue());
		zoomin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				int newz = temporalSigPanel.getHZoom()-10;
				if (newz<=1) newz=1;
				temporalSigPanel.setHZoom(newz);
			}
		});
		zoomout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				int newz = temporalSigPanel.getHZoom()+10;
				temporalSigPanel.setHZoom(newz);
			}
		});

		setMinimumSize(new Dimension(10000,20));
		setMaximumSize(new Dimension(10000,100));
	}//constructor

	private void showLegend(ActionEvent e){
		PopupMenuLegende popupMenu = new PopupMenuLegende(temporalSigPanel);
		JButton source = (JButton)e.getSource();
		popupMenu.show(source,0,source.getHeight());
	}//showLegend
	
	public void setPos(String time) {
		gotoTextField.setText(time);
	}
	
	private void gotoPosi(){
		String text = gotoTextField.getText();
		
		double timeEnSec = 0.0;
		
		int indiceParcour = 0;
		//Variables de travail
		String temp; int indice;

		try {
			//on recherche d'abord une occurence de "h"
			if((indice = text.indexOf('h')) > 0){
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp)*3600.0;
				indiceParcour = indice+1;
			}
			//de même pour "min" 
			if ( ((indice = text.indexOf("min")) > 0)){
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp)*60.0;
				indiceParcour = indice+3;
			}
			//si on n'a pas trouve "min", on cherche 'm'
			else if ((indice = text.indexOf('m')) > 0) {
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp)*60.0;
				indiceParcour = indice+1;
			}
			//de même pour 's' ou "sec"
			if ( (indice = text.indexOf("sec")) > 0) {
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp);
				indiceParcour = indice+3;
			}
			else if ((indice = text.indexOf('s')) > 0) {
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp);
				indiceParcour = indice+1;
			}
			
			//de meme pour ms
			if ((indice = text.indexOf("ms")) > 0) {
				temp = text.substring(indiceParcour,indice);
				timeEnSec += Double.parseDouble(temp)/1000.0;
				indiceParcour = indice+2;
			}
			
			temporalSigPanel.setOffsetStart((int) (timeEnSec*temporalSigPanel.getFrameRate()));
			temporalSigPanel.repaint();
		} catch (NumberFormatException nfe){
			JOptionPane.showMessageDialog(this,
					"Format non reconnu.\n " +
					"La duree entree doit etre de la forme :\n " +
					"...h...[min|m]...[sec|s]...ms",
					"Erreur", 
					JOptionPane.ERROR_MESSAGE,
					iconeError);
		}
	}//gotoPosi()
	
	public void update(Observable arg0, Object arg1) {
		cursorHand.setSelected(temporalSigPanel.isCursorHand());
		cursorNormal.setSelected(temporalSigPanel.isCursorNormal());
	}//update
	
	
	
	
	
	

}//class PanelBoutonsZoom
