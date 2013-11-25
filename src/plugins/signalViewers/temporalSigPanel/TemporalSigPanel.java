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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import plugins.applis.SimpleAligneur.Aligneur;
import plugins.applis.SimpleAligneur.PlayerListener;
import plugins.buffer.RoundBuffer;
import plugins.speechreco.aligners.OldAlignment;
import plugins.text.ListeElement;
import plugins.text.TexteEditor;
import plugins.text.elements.Element;
import plugins.text.elements.Element_Locuteur;
import plugins.text.elements.Element_Mot;
import plugins.utils.TraducteurTime;

public class TemporalSigPanel extends JComponent {

	private static final long serialVersionUID = -4606895284527729713L;


	//---------------------------------------------------
	//--------------- Privates Fields -------------------
	//---------------------------------------------------
	public Aligneur aligneur;

	public static boolean showPhones = false;

	/** Position du debut de la fenetre, 
	 * en nombre de sample par rapport au debut du fichier */
	private long offsetStart;
	// offsetMax sert a empecher le defilement de la fenetre au-dela du signal
	private long offsetMax=-1;

	/** Position de la barre de progression, 
	 *  en nombre de sample par rapport au debut du fichier */
	private long progressBar;	

	/** Zoom Horizontal, en nombre de sample par pixel. */
	private int hZoom;


	/** Zoom Vertical, en nombre de sample par pixel.*/
	private int vZoom;

	/**
	 * Cette variable est utilise pour maintenir la reference
	 * e un point du precedent affichage, afin de maintenir le meme 
	 * decallage par rapport e l'origine... 
	 * Ceci permet d'eviter l'effet de changement de forme de la courbe
	 * lors des deplacements, changement de e des moments d'echantillonage differents.
	 */
	private int point;





	private int selectedWordIndice = -1;

	/** Champs positionne pour savoir quelle ligne
	 * a ete selectionnee par un mousePressed */
	private int selectedLineIndice = -1;

	/** Champ utilise pour indiquer le debut de la selection. */
	private long selection1 = 0;
	/** Champ utilise pour indiquer la fin de la selection. */
	private long selection2 = -1;


	/** Champ utilise pour retenir le mot e mettre en highLight */
	private String searchedWord;

	//---- pour "extends" Observable ----
	private ArrayList<Observer> observers;


	//---------------- Les curseur utilises ----------------------
	private Cursor cursorResize = new Cursor(Cursor.TEXT_CURSOR);
	private Cursor cursorNormal = new Cursor(Cursor.DEFAULT_CURSOR);
	private Cursor cursorHand = new Cursor(Cursor.MOVE_CURSOR);

	private Cursor actualCursor = cursorNormal;

	//------------------- Les couleurs utilises ---------------------
	private final Color[] defaultSettings = 
	{Color.WHITE,
			Color.DARK_GRAY,
			Color.BLACK,
			Color.ORANGE.brighter().brighter(),
			Color.MAGENTA,
			Color.RED,
			Color.RED,
			Color.LIGHT_GRAY,
			Color.LIGHT_GRAY,
			Color.RED,
			Color.RED.darker(),
			Color.DARK_GRAY,
			Color.GREEN};
	private Color colorBackGround;
	private Color colorAbscisse;
	private Color colorCourbe;
	private Color colorSelection;
	private Color colorExtremiteSelection;
	private Color colorProgressBar;

	//private Color colorMot;
	private Color colorMotInconnu;
	private Color colorLigneMot;
	private Color colorLigneReglage;

	private Color colorSearchedWord;
	private Color colorReglette;
	
	private Color colorZoneNonAlignable;
	private Color colorAncre;
	
	/** Entier indiquant la "resolution" du graphique : 
	 * Si resolution = 10, on affiche un sample sur 10.*/
	private int resolution;




	/* donnees de position dans le graphe */
	private int ecartBasEtligneBaseMot;
	private int ecartLigneBaseMotEtMot;
	private int hauteurLigne;
	/* donnees pour la reglette */
	private int ecartHautEtReglette;
	private int tailleGrandTick;
	private int taillePetitTick;

	private double coeffPositionCourbe;

	private int ecartMilieuLigneLocuteurEtNomLocuteur;
	
	private int lineBase;
	private int lineMax;

	private boolean motSurDeuxLigne = false;
	private int ecartVerticalEntreDeuxMots;


	private ArrayList<Color> colorLocuteur;
	

	//----------------------------------------------------------
	//----------------- Constructor ----------------------------
	//----------------------------------------------------------
	public TemporalSigPanel(Aligneur fen){
		super();
		
		aligneur = fen;

		searchedWord = null;
		resolution = 60;
		resolution = 1;

		offsetStart = 0;
		offsetMax = -1;
		progressBar = 0;

		hZoom = 150;
		vZoom = 400;

		point = 0;

		observers = new ArrayList<Observer>();

		restoreColorDefaultSettings();
		restoreReglageTailleDefaultSettings();
		ecartVerticalEntreDeuxMots = 8;

		colorLocuteur = new ArrayList<Color>();
		colorLocuteur.add(Color.BLUE);
		colorLocuteur.add(Color.RED);
		

		MouseAdapter mouseAdapter = new CustomMouseInputAdapter();
		addMouseMotionListener(mouseAdapter);
		addMouseListener(mouseAdapter);
		addMouseWheelListener(mouseAdapter);

		// Listener sur la windows pour remettre e jour l'offsetMax
		//En cas de resize
		addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent arg0) {}
			public void componentMoved(ComponentEvent arg0) {}

			public void componentResized(ComponentEvent arg0) {
				calculerOffsetMax();
			}
			public void componentShown(ComponentEvent arg0) {}
		});
		
		setMinimumSize(new Dimension(50,100));
//		setMaximumSize(new Dimension(10000,200));
		
	}//TempSigPanel
	
	public Dimension getPreferredSize() {
		return new Dimension(500,120);
	}
	
	public void paintComponent(Graphics g){
		
		panelWidth = getWidth();
		panelHeight = getHeight();

		g.setColor(colorBackGround);
		g.fillRect(0, 0, panelWidth, panelHeight);

		RoundBuffer audioModele = aligneur.audiobuf;

		double frameRate = 100;

		double frameRateDivhZoom = frameRate/hZoom;

		//------------------------------------------------------------
		//----------- Partie Affichage de la Selection ----------------
		//------------------------------------------------------------
		if (selection2 > -1) {
			int selection1Coord = (int) (((selection1-offsetStart)/hZoom));
			int selection2Coord = (int) (((selection2-offsetStart)/hZoom));
			//les deux lignes extremes
			g.setColor(colorExtremiteSelection);
			g.drawLine(selection1Coord, 0, selection1Coord, panelHeight);
			g.drawLine(selection2Coord, 0, selection2Coord, panelHeight);

			g.setColor(colorSelection);
			if(selection1 < selection2){
				int debutRect = selection1Coord+1;
				g.fillRect(debutRect,0,selection2Coord-debutRect,panelHeight);
			}
			else {
				int debutRect = selection2Coord+1;
				g.fillRect(debutRect,0,selection1Coord-debutRect,panelHeight);
			}
		}//partie selection

		//-----------------------------------------------------------
		//--------------------- Reglette Temps ----------------------
		//-----------------------------------------------------------
		g.setColor(colorReglette);

		//-------- Affichage de la ligne ----
		g.drawLine(0,ecartHautEtReglette,panelWidth, ecartHautEtReglette);
		int hauteurMaxGrandTick = ecartHautEtReglette + tailleGrandTick;
		int hauteMaxPetitTick = ecartHautEtReglette + taillePetitTick;
		int hauteurText = hauteurMaxGrandTick+10;


		String timeString;

		//-------- affichage des ticks et des autres temps -----
		double timeStart = (offsetStart/frameRate)*10.0;
		byte compteur = (byte) ((1 + timeStart)%10);

		//ecart, en 10eme de seconde, avec la prochaine 0.1s "entiere"
		double ecartS = ((1.0 - (timeStart - (int)timeStart)))/10.0;
		//on affiche un trait tous les dixieme de seconde.
		double increment = frameRateDivhZoom /10;

		int xInt;
		double time;

		int motWidth;
		fontMetric = getFontMetrics(getFont());

/*
		for(double x = (ecartS*frameRateDivhZoom); x < panelWidth; x += increment){
			xInt = (int) Math.round(x);
			if(compteur == 10){
				compteur = 1;
				g.drawLine(xInt,ecartHautEtReglette,xInt,hauteurMaxGrandTick);

				//affichage du string correspondant
				time = Math.round((xInt*hZoom+offsetStart)/frameRate);
				
				timeString = TraducteurTime.getTimeMinMSFromSecondes(time);
				motWidth = SwingUtilities.computeStringWidth(fontMetric, timeString);
				posiMot = xInt - (motWidth >> 1);
				g.drawString(timeString, posiMot, hauteurText);

			}
			else {
				++compteur;
				g.drawLine(xInt,ecartHautEtReglette,xInt,hauteMaxPetitTick);
			}
		}//while

*/

		//-----------------------------------------------------------
		//---------------- Partie Affichage Courbe ------------------
		//-----------------------------------------------------------
		//afficher la ligne horizontale des abscises
		g.setColor(colorAbscisse);
		int hMilieuGraph = (int) (panelHeight * coeffPositionCourbe);
		g.drawLine(0, hMilieuGraph, panelWidth, hMilieuGraph);


		g.setColor(colorCourbe);

		/* On recalcule le point de depart de facon e empecher l'effect d'ondulation de la courbe */
		point += (((offsetStart-point)/hZoom)*hZoom/resolution)*resolution;

		RoundBuffer buffer = aligneur.audiobuf;

		int lastY = (buffer.getSample(point)/vZoom)+hMilieuGraph;
		int lastX = 0;

//		buffer.print=true;
		
		int nextY, nextX, i;
		for(i = point; 
		(nextX = (int) ((i-offsetStart)/hZoom)) < panelWidth; 
		i += resolution ){
			nextY = (buffer.getSample(i)/vZoom) + hMilieuGraph;
			g.drawLine(lastX, lastY, nextX, nextY);
			lastY = nextY;
			lastX = nextX;
		}

		//-----------------------------------------------------------
		//---------------- Partie Affichage Mots --------------------
		//-----------------------------------------------------------
		ListeElement listeElement = aligneur.edit.getListeElement();
		int listeElementSize = listeElement.size();
		int posiLine = 0;

		completerColorLocuteur(aligneur.project.speakers.size());
		
		lineBase = panelHeight - ecartBasEtligneBaseMot;
		lineMax = lineBase - hauteurLigne;
		hauteurMot = lineBase - ecartLigneBaseMotEtMot;
		g.drawLine(0, lineBase, panelWidth, lineBase);
		int lastLine = 0;
		
		//pour la zone de couleur
		int hauteurZoneCouleur = panelHeight-lineBase;
		int hauteurNomLocuteur = (hauteurZoneCouleur>>1)+lineBase+ecartMilieuLigneLocuteurEtNomLocuteur;
		
		byte indiceLocuteur = 0;
		int posiFinLastLocuteur = 0;
		String mot;
		i = 0;
		boolean breakNextBoucle = false;
		int motidx=-1;
		
		boucleMot:while (i < listeElementSize){

			Element element = listeElement.get(i);

			//si l'element est un locuteur, on le memorise, pour savoir oe afficher les mots suivants
			if(element instanceof Element_Locuteur){
				
				g.setColor(colorLocuteur.get(indiceLocuteur));
				g.fillRect(posiFinLastLocuteur, lineBase, lastLine-posiFinLastLocuteur, hauteurZoneCouleur);
				
				g.setColor(Color.WHITE);
				mot = aligneur.project.speakers.get(indiceLocuteur).getName();
				motWidth = SwingUtilities.computeStringWidth(fontMetric, mot);
				
				
				/* position du mot = 
				 * positionLigne - (position de la ligne - position de la ligne precedente) / 2   => milieu du segment 
				 * - moitie taille du mot.*/
				posiMot = lastLine - ((lastLine - posiFinLastLocuteur) >> 1) - (motWidth >> 1);
				g.drawString(mot, posiMot, hauteurNomLocuteur);
				
				posiFinLastLocuteur = lastLine;
				indiceLocuteur = ((Element_Locuteur)element).getLocuteurID();
			}
			else if (element instanceof Element_Mot){
				Element_Mot elm = (Element_Mot)element;
				motidx++;
				if (showPhones) {
				} else {
					long lastSample = OldAlignment.frame2sample(aligneur.project.words.getSegmentEndFrame(elm.posInAlign));
					Element_Mot elementAvecDuree = (Element_Mot)element;
					posiLine = (int) ((lastSample-offsetStart)/hZoom);
					
					if(posiLine > 0){
						//pour sortir de la boucle si on sort de l'ecran
						if (posiLine > panelWidth) {
							breakNextBoucle = true;
							posiLine = panelWidth;
						}
						else {
							
							//Si la ligne est selectionnee
							if(selectedLineIndice == i){
								g.setColor(colorLigneReglage);
			
								//on affiche une ligne de positionnement
								g.drawLine(posiLine, 0, posiLine, panelHeight);
			
								//on affiche le temps au dessus
								/*
								time = 
									elementAvecDuree.endSample/frameRate;
								timeString = TraducteurTime.getTimeMinSMSFromSeconds(time);
								motWidth = SwingUtilities.computeStringWidth(fontMetric, timeString);
								posiMot = posiLine - (motWidth >> 1);
			
								g.drawString(timeString, posiMot, lineMax - 5);
								*/
			
							}
							else {
								//affichage de la ligne de fin du mot.
								g.setColor(colorLigneMot);
								g.drawLine(posiLine, lineBase, posiLine, lineMax);
							}
						}
						if(elementAvecDuree instanceof Element_Mot){
							Element_Mot elementMot = (Element_Mot)element;
							
							//---------------- affichage du mot. ------------
							mot = aligneur.edit.getMot(elementMot);
							motWidth = SwingUtilities.computeStringWidth(fontMetric, mot);
			
							/* position du mot = 
							 * positionLigne - (position de la ligne - position de la ligne precedente) / 2   => milieu du segment 
							 * - moitie taille du mot.*/
							posiMot = posiLine - ((posiLine - lastLine) >> 1) - (motWidth >> 1);
			
							//si c'est le mot recherche, on prend une couleur speciale.
							if(searchedWord != null && mot.equals(searchedWord)) {
								g.setColor(colorSearchedWord);
							}
							else g.setColor(colorLocuteur.get(indiceLocuteur));
							
							g.drawString(mot, posiMot, hauteurMot);
						}
		
							
						lastLine = posiLine;
						if (breakNextBoucle) break boucleMot;
					}
				}//if (element instanceof Element_Mot )
			}
			++i;
		}//while sur les mots

		// affichage des phones
		if (showPhones) {
			g.setColor(Color.black);
			int frdeb = OldAlignment.sample2frame(offsetStart);
			int segidx = aligneur.project.phons.getSegmentAtFrame(frdeb);
System.out.println("debugsegtoprint "+segidx);
			if (segidx>=0) {
				int endFrOfSeg = aligneur.project.phons.getSegmentEndFrame(segidx);
				long endSampleOfSeg = OldAlignment.frame2sample(endFrOfSeg);
				posiLine = (int) ((endSampleOfSeg-offsetStart)/hZoom);
				if(posiLine > 0){
					g.drawLine(posiLine, lineBase, posiLine, lineMax);
					lastLine=posiLine;
					// afficher la fin du premier segment visible ? bof...
					for (int k=segidx+1;k<aligneur.project.phons.getNbSegments();k++) {
						endFrOfSeg = aligneur.project.phons.getSegmentEndFrame(k);
						endSampleOfSeg = OldAlignment.frame2sample(endFrOfSeg);
						posiLine = (int) ((endSampleOfSeg-offsetStart)/hZoom);
						if(posiLine > 0){
							//pour sortir de la boucle si on sort de l'écran
							if (posiLine > panelWidth) break;
							// ligne de fin du phone:
							g.drawLine(posiLine, lineBase, posiLine, lineMax);
							// pos du phone:
							String phone = aligneur.project.phons.getSegmentLabel(k);
							int phoneWidth = SwingUtilities.computeStringWidth(fontMetric, phone);
							posiMot = posiLine - ((posiLine - lastLine) >> 1) - (phoneWidth >> 1);
							g.drawString(phone, posiMot, hauteurMot);
							lastLine=posiLine;
						}
					}
					// afficher le nom du dernier seg ??
				}
			}
		}
		
		//-------------------- remplissage de la fin ---------------------
		//On remplit tout de meme le dernier locuteur :
		g.setColor(colorLocuteur.get(indiceLocuteur));
		g.fillRect(posiFinLastLocuteur, lineBase, lastLine-posiFinLastLocuteur, hauteurZoneCouleur);
		
		g.setColor(Color.WHITE);
		mot = aligneur.project.speakers.get(indiceLocuteur).getName();
		if(mot != null){
			motWidth = SwingUtilities.computeStringWidth(fontMetric, mot);
			
			/* position du mot = 
			 * positionLigne - (position de la ligne - position de la ligne precedente) / 2   => milieu du segment 
			 * - moitie taille du mot.*/
			posiMot = lastLine - ((lastLine - posiFinLastLocuteur) >> 1) - (motWidth >> 1);
			g.drawString(mot, posiMot, hauteurNomLocuteur);
		}
		
		
		
		
		//---------------------------------------------------------------
		//----------- Partie Affichage Informations Alignement ----------
		//---------------------------------------------------------------

		/* TODO
		ListeAlignement listeAlignement = audioModele.getListeAlignement();
		for(Element_Alignement elementAlign : listeAlignement){
			if(elementAlign instanceof Element_ZoneNonAlignable){
			
				Element_ZoneNonAlignable zone = 
					(Element_ZoneNonAlignable)elementAlign;
				
				g.setColor(colorZoneNonAlignable);
				long startSample = zone.getStartSample() - offsetStart;
				long endSample = zone.getEndSample() - offsetStart;
				
				if(startSample > panelWidth*hZoom) break;
				
				int startSampleX = (int) (startSample/hZoom);
				int endSampleX = (int) (endSample/hZoom);
				int lengthX = endSampleX-startSampleX;
				g.fillRect(startSampleX, 0, lengthX, ecartHautEtReglette);
				
				
				//Affichage du texte "Zone non alignable" dans la zone des locuteurs 
				g.setColor(Color.WHITE);
				mot = "Zone non alignable";
				motWidth = SwingUtilities.computeStringWidth(fontMetric, mot);
				
				
				// position du mot = 
				// positionLigne - (position de la ligne - position de la ligne precedente) / 2   => milieu du segment 
				// - moitie taille du mot.
				posiMot = endSampleX - (lengthX >> 1) - (motWidth >> 1);
				g.drawString(mot, posiMot, ecartHautEtReglette);
			}
			else if (elementAlign instanceof Element_Ancre){
				// TODO: afficer le mot en vert dans la zone des mots !!
				g.setColor(colorAncre);
				Element_Ancre ancre = (Element_Ancre)elementAlign;
				
				
				int sampleAncre = (int) ((ancre.getSample() - offsetStart)/hZoom);
				if(sampleAncre > panelWidth*hZoom) break;
				
				// affichage du mot en vert
				motWidth = SwingUtilities.computeStringWidth(fontMetric, mot);
				posiMot = sampleAncre - motWidth;
				if (posiMot>10) {
					g.drawString(ancre.getMot().getMot(), posiMot, hauteurMot+18);
				}
				
				int largeurAncreFromCenter = 5;
				int hauteurCroisementAncre = 5;
				//----- Dessin de la forme de l'ancre ---
				//	 _
				//	| |
				//	\ /
				
				int[] xPoints = new int[5];
				int[] yPoints = new int[5];
				
				//Point haut gauche
				xPoints[0] = sampleAncre - largeurAncreFromCenter;
				yPoints[0] = 0;
				
				//Point haut droit
				xPoints[1] = sampleAncre + largeurAncreFromCenter;
				yPoints[1] = 0;
				
				
				//Point milieu droit
				xPoints[2] = sampleAncre + largeurAncreFromCenter;
				yPoints[2] = hauteurCroisementAncre;
				
				//Point bas milieu
				xPoints[3] = sampleAncre;
				yPoints[3] = ecartHautEtReglette;

				//Point milieu droit
				xPoints[4] = sampleAncre - largeurAncreFromCenter;
				yPoints[4] = hauteurCroisementAncre;
				
				g.drawPolygon(xPoints, yPoints, 5);
				
				//Ligne e tracer en dessous
				g.drawLine(sampleAncre,ecartHautEtReglette,sampleAncre,panelHeight);
			}
			
		}//for listeAlignement
*/
		//-----------------------------------------------------------
		//------------- Partie Affichage Progressbar ----------------
		//-----------------------------------------------------------
		g.setXORMode(colorProgressBar);
		int posi = (int) ((progressBar-offsetStart)/hZoom);
		g.drawLine(posi, 0, posi, panelHeight);
		
/*
		if (restartPlay) {
			System.err.println("restart play");
			principale.getToolBarPlayer().getPlayer().play(principale.getToolBarPlayer());
			restartPlay=false;
		}
		*/
	}//paintComponent

	int panelWidth,posiMot,hauteurMot,panelHeight;
	FontMetrics fontMetric;
	
/*
	public void paintAncre(Element_Ancre ancre) {
		Graphics gg = getGraphics();
		gg.setColor(colorAncre);
		int sampleAncre = (int) ((ancre.getSample() - offsetStart)/hZoom);
		if(sampleAncre > panelWidth*hZoom) return;
		
		// affichage du mot en vert
		int motWidth = SwingUtilities.computeStringWidth(fontMetric, ancre.getMot().getMot());
		posiMot = sampleAncre - motWidth;
		if (posiMot>10) {
			gg.drawString(ancre.getMot().getMot(), posiMot, hauteurMot+18);
		}
		
		int largeurAncreFromCenter = 5;
		int hauteurCroisementAncre = 5;
		//----- Dessin de la forme de l'ancre ---
		//	 _
		//	| |
		//	\ /
		
		int[] xPoints = new int[5];
		int[] yPoints = new int[5];
		
		//Point haut gauche
		xPoints[0] = sampleAncre - largeurAncreFromCenter;
		yPoints[0] = 0;
		
		//Point haut droit
		xPoints[1] = sampleAncre + largeurAncreFromCenter;
		yPoints[1] = 0;
		
		
		//Point milieu droit
		xPoints[2] = sampleAncre + largeurAncreFromCenter;
		yPoints[2] = hauteurCroisementAncre;
		
		//Point bas milieu
		xPoints[3] = sampleAncre;
		yPoints[3] = ecartHautEtReglette;

		//Point milieu droit
		xPoints[4] = sampleAncre - largeurAncreFromCenter;
		yPoints[4] = hauteurCroisementAncre;
		
		gg.drawPolygon(xPoints, yPoints, 5);
		
		//Ligne e tracer en dessous
		gg.drawLine(sampleAncre,ecartHautEtReglette,sampleAncre,panelHeight);
	}
*/	
	public int getHZoom() {
		return hZoom;
	}


	public void setHZoom(int zoom) {
		hZoom = zoom;
		calculerOffsetMax();
		point = 0;
		checkOffsetStart();
		repaint();
		notifyObserver();
	}


	public int getOffsetStart() {
		return (int)offsetStart;
	}


	public void setOffsetStart(long offsetStart) {
		point = 0;
		progressBar = offsetStart;
		this.offsetStart = offsetStart;
		checkOffsetStart();
		repaint();
		notifyObserver();
	}


	public int getVZoom() {
		return vZoom;
	}


	public void setVZoom(int zoom) {
		vZoom = zoom;
		repaint();
		notifyObserver();
	}



	/*
	 * attention ! il semble y avoir un decalage entre l'appel a setProgressBar
	 * et l'affichage effectif, peut-etre a cause du repaint() qui est long ?
	 * 
	 * Pour reduire le temps de calcul du repaint, j'utilise un XOR pour afficher
	 * la progress barre.
	 * 
	 * return true lorsqu'on passe un ecran
	 */
	public boolean setProgressBar(long readedShort){
		int widthFoisHZoom = getWidth()*hZoom;
		if (readedShort<offsetStart) return setProgressBar(readedShort,0.5f);
		aligneur.toolbar.setPos(TraducteurTime.getTimeHMinSFromSeconds(OldAlignment.sample2second(readedShort)));
		if(Math.abs(readedShort - offsetStart) > widthFoisHZoom){
			offsetStart = (readedShort/widthFoisHZoom)*widthFoisHZoom;
			checkOffsetStart();
			progressBar = readedShort;
			repaint();
			return true;
		}
		
		// on efface l'ancienne progressbar:
		Graphics g = getGraphics();
		g.setXORMode(colorProgressBar);
		int panelHeight = getHeight();
		int posi = (int) ((progressBar-offsetStart)/hZoom);
		g.drawLine(posi, 0, posi, panelHeight);
		
		// on affiche la nouvelle
		progressBar = readedShort;
		posi = (int) ((progressBar-offsetStart)/hZoom);
		g.drawLine(posi, 0, posi, panelHeight);
		notifyObserver();
		
		return false;
	}
	public boolean setProgressBar(long sample, float posPB){
		aligneur.toolbar.setPos(TraducteurTime.getTimeHMinSFromSeconds(OldAlignment.sample2second(sample)));
		int widthFoisHZoom = getWidth()*hZoom;
		// on force le repaint dans ce cas !
		offsetStart = sample - (long)(posPB * (float)widthFoisHZoom);
		checkOffsetStart();
		progressBar = sample;
		repaint();
		return true;
	}//setProgressBar

	public long getProgressBar() {
		return progressBar;
	}



	/** Methode utilisee pour eviter au composant de depasser la taille du signal */
	private void checkOffsetStart(){
		if (offsetMax>=0)
			offsetStart = Math.min(offsetStart,offsetMax);
		offsetStart = Math.max(offsetStart,0);
	}//checkOffsetStart
	
	private void calculerOffsetMax(){
/* TODO
		if (principale.getModele().isOpen()){
			float l = principale.getModele().getFrameLength();
			if (l>=0)
				offsetMax = (long) (l-getWidth()*hZoom);
			else offsetMax=-1;
		} else 
*/
		offsetMax = -1;
	}
	
	public void centrerSurSample(long sample){
		offsetStart = sample - (getWidth()*hZoom>>1);
		checkOffsetStart();
	}
	


	//----- Methode d'Observable -------
	public void addObserver(Observer o){
		observers.add(o);
	}

	public void notifyObserver(){
		for(Observer o:observers)
			o.update(null, null);
	}

	public void notifyObserver(Object obj){
		for(Observer o:observers)
			o.update(null, obj);
	}





	//----------------------------------------------------------------
	//--------- Getters & Setters pour les Cursor ------------------
	//----------------------------------------------------------------
	public void setCursorHand(){
		actualCursor = cursorHand;
		setCursor(cursorHand);
		notifyObserver();
	}

	public void setCursorNormal(){
		actualCursor = cursorNormal;
		setCursor(cursorNormal);
		notifyObserver();
	}

	public boolean isCursorHand(){
		return actualCursor == cursorHand;
	}

	public boolean isCursorNormal(){
		return actualCursor == cursorNormal;
	}



	//----------------------------------------------------------------
	//------------- Getters & Setters pour les Couleurs --------------
	//----------------------------------------------------------------
	public Color[] getColors(){
		Color[] res = new Color[13];
		int i = 0;
		res[i++] = colorBackGround;
		res[i++] = colorAbscisse;
		res[i++] = colorCourbe;
		res[i++] = colorSelection;
		res[i++] = colorExtremiteSelection;
		res[i++] = colorProgressBar;
		res[i++] = colorMotInconnu;
		res[i++] = colorLigneMot;
		res[i++] = colorLigneReglage;
		res[i++] = colorSearchedWord;
		res[i++] = colorReglette;
		res[i++] = colorZoneNonAlignable;
		res[i++] = colorAncre;
		return res;
	}//getColors();


	public String[] getColorsName(){
		String[] res = new String[13];
		int i = 0;
		res[i++] = "Arriere-plan : ";
		res[i++] = "Ligne des abscisses : ";
		res[i++] = "Courbe : ";
		res[i++] = "Zone de selection : ";
		res[i++] = "Extremites de la zone de selection : ";
		res[i++] = "Barre de progression : ";
		res[i++] = "Mots inconnus : ";
		res[i++] = "Ligne entourant les mots : ";
		res[i++] = "Ligne de reglage des frontieres de mot : ";
		res[i++] = "Couleur des mots recherches : ";
		res[i++] = "Couleur de la reglette des temps : ";
		res[i++] = "Couleur d'une zone non alignable";
		res[i++] = "Couleur des ancres";
		return res;
	}//getColorsName


	public void setColor(Color color, int indice){
		switch (indice) {
		case 0 : colorBackGround = color; 			break;
		case 1 : colorAbscisse = color; 			break;
		case 2 : colorCourbe = color;				break;
		case 3 : colorSelection = color; 			break;
		case 4 : colorExtremiteSelection = color; 	break;
		case 5 : colorProgressBar = color;			break;
		case 6 : colorMotInconnu = color;			break;
		case 7 : colorLigneMot = color;				break;
		case 8 : colorLigneReglage = color;			break;
		case 9 : colorSearchedWord = color; 		break;
		case 10 : colorReglette = color;			break;
		case 11 : colorZoneNonAlignable = color;	break;
		case 12 : colorAncre = color;				break;
		}//switch
	}//setColor(Color,int)

	public void restoreColorDefaultSettings(){
		int i = 0;
		colorBackGround = defaultSettings[i++];
		colorAbscisse = defaultSettings[i++];
		colorCourbe = defaultSettings[i++];
		colorSelection = defaultSettings[i++];
		colorExtremiteSelection = defaultSettings[i++];
		colorProgressBar = defaultSettings[i++];
		colorMotInconnu = defaultSettings[i++];
		colorLigneMot = defaultSettings[i++];
		colorLigneReglage = defaultSettings[i++];
		colorSearchedWord = defaultSettings[i++];
		colorReglette = defaultSettings[i++];
		colorZoneNonAlignable = defaultSettings[i++];
		colorAncre = defaultSettings[i++];
		repaint();
	}

	private void completerColorLocuteur(int nb){
		while(colorLocuteur.size() < nb)
			colorLocuteur.add(Color.BLACK);
	}
	
	
	public ArrayList<Color> getColorsLocuteurs(){
		return colorLocuteur;
	}
	
	
	//----------------------------------------------------------------
	//------ Getters & Setters pour les Reglage de taille -------------
	//----------------------------------------------------------------
	public int[] getReglagesTaille(){
		int[] res = new int[8];
		int i = 0;
		res[i++] = ecartBasEtligneBaseMot;
		res[i++] = ecartLigneBaseMotEtMot;
		res[i++] = hauteurLigne;
		res[i++] = ecartHautEtReglette;
		res[i++] = tailleGrandTick;
		res[i++] = taillePetitTick;
		res[i++] = ecartMilieuLigneLocuteurEtNomLocuteur;
		res[i++] = (int)(coeffPositionCourbe*100);
		return res;
	}//getReglagesTaille()

	public String[] getReglagesTailleName(){
		String[] res = new String[8];
		int i = 0;
		res[i++] = "Ecart entre le bord inferieur et la ligne des mots : ";
		res[i++] = "Ecart entre la ligne des mots et les mots : ";
		res[i++] = "Hauteur des lignes entourant les mots : ";
		res[i++] = "Ecart entre le haut et la reglette des temps : ";
		res[i++] = "Taille d'un grand marqueur de la reglette : ";
		res[i++] = "Taille d'un petit marqueur de la reglette : "; 
		res[i++] = "Ecart entre le milieu de la ligne des locuteurs et le nom du locuteur";
		res[i++] = "Position de la courbe dans le graphique (en %) : ";
		return res;
	}//getReglagesTailleName

	public void restoreReglageTailleDefaultSettings(){
		ecartBasEtligneBaseMot = 16;
		ecartLigneBaseMotEtMot = 10;
		hauteurLigne = 20;
		ecartHautEtReglette = 10;
		tailleGrandTick = 10;
		taillePetitTick = 5;
		ecartLigneBaseMotEtMot = 5;
		ecartMilieuLigneLocuteurEtNomLocuteur = 5;
		coeffPositionCourbe = 0.45;

		repaint();
	}//restoreReglageTailleDefaultSettings

	public void setReglageTaille(int val, int indice){
		switch (indice) {
		case 0 : ecartBasEtligneBaseMot = val; 					break;
		case 1 : ecartLigneBaseMotEtMot= val;					break;
		case 2 : hauteurLigne= val;								break;
		case 3 : ecartHautEtReglette= val; 						break;
		case 4 : tailleGrandTick = val;							break;
		case 5 : taillePetitTick = val;							break;
		case 6 : ecartMilieuLigneLocuteurEtNomLocuteur = val; 	break;
		case 7 : coeffPositionCourbe = val/100.0;				break;
		}//switch
		repaint();
	}//setColor(Color,int)




	//---------- Fonction utilisees pour la recherche de mot ------
	public void setSearchedWord(String searchedWord) {
		this.searchedWord = searchedWord;
		repaint();
	}



	//---------------------------------------------------------------------
	//-------------- Fonctions de reglage de la resolution ----------------
	//---------------------------------------------------------------------
	public int getResolution() {
		return resolution;
	}


	public void setResolution(int resolution) {
		this.resolution = resolution;
		repaint();
	}


	/** Fonction permettant de calculer le frame rate possible avec cette resolution	 */
	public double computeFrameRate(int nbIter) {
		Graphics g = createImage(getWidth(), getHeight()).getGraphics();

		long dt = 0;
		long startTime;
		for (int i = 0; i < nbIter; i++) {
			startTime = System.currentTimeMillis();
			paintComponent(g);
			dt += System.currentTimeMillis() - startTime;
		}
		return (dt / (double)nbIter);
	}


	/**
	 * Fonction permettant de recuperer le debut de la selection, en sample.
	 * @return debut de la selection (indice du sample par rapport au debut du fichier)
	 */
	public long getSelectionStart(){
		if( selection2 == -1) return -1;
		else return Math.min(selection1,selection2);
	}

	/**
	 * Fonction permettant de recuperer la fin de la selection, en sample.
	 * @return fin de la selection (indice du sample par rapport au debut du fichier)
	 */
	public long getSelectionEnd(){
		if(selection2 == -1) return -1;
		return Math.max(selection1,selection2);
	}

	//------- pour les reglage d'intercalage de deux mots -----
	public int getEcartVerticalEntreDeuxMots() {
		return ecartVerticalEntreDeuxMots;
	}

	public void setEcartVerticalEntreDeuxMots(int ecartVerticalEntreDeuxMots) {
		this.ecartVerticalEntreDeuxMots = ecartVerticalEntreDeuxMots;
		repaint();
	}

	public boolean getMotSurDeuxLigne() {
		return motSurDeuxLigne;
	}

	public void setMotSurDeuxLigne(boolean motSurDeuxLigne) {
		this.motSurDeuxLigne = motSurDeuxLigne;
		repaint();
	}
	
	public void translateSignal(int deltax) {
		offsetStart -= deltax;
		checkOffsetStart(); 
		notifyObserver();
		if(offsetStart < 0) offsetStart = 0;
	}

	//----------------------------------------------------------------
	//---------------------- MouseAdapter ---------------------------
	//----------------------------------------------------------------
	private class CustomMouseInputAdapter extends MouseAdapter {
		/** Pour memoriser le debut d'un deplacement d'ecran manuel. */
		private int indiceDebutDeplacement = -1;
		

		@Override
		public void mousePressed(MouseEvent e) {	
			/* - si le bouton 1 est presse :
			 * 		+ si une ligne est pointee par le curseur, on la selectionne
			 * - si le bouton 3 est presse : 
			 */

			requestFocusInWindow();
			
			switchBouton:switch(e.getButton()){
			case MouseEvent.BUTTON1: 
				switch (e.getClickCount()){
					case 1 : 
						if(e.getY() > ecartHautEtReglette){
							if(getCursor().equals(cursorHand)){
								indiceDebutDeplacement = e.getX();
								selectedWordIndice = getIndiceMotPointe(e);
							}
							else {
								//Si le pointeur est sur une ligne, on selectionne cette ligne
								selectedLineIndice = getIndiceBarrePointee(e);
								//sinon, c'est une selection
								if(selectedLineIndice == -1) {
//									if (!aligneur.player.isPlaying()){
//										selection1 = e.getX()*hZoom+offsetStart;
//										setProgressBar(selection1);
//									} 
								}
							}
							repaint();
						}
						break switchBouton;
					case 2 : 
						if(e.getY() < ecartHautEtReglette){
							TexteEditor texteEditor = aligneur.edit;
							ListeElement listeElement = texteEditor.getListeElement();
							int indiceMotSelectionDansTextEditor = 
								listeElement.getIndiceMotAtTextPosi(texteEditor.getCaretPosition());
/*
							if(indiceMotSelectionDansTextEditor != -1){
								principale.getModele().getListeAlignement()
								.addAncre((int)(e.getX()*hZoom+offsetStart), 
										(Element_Mot)listeElement.get(indiceMotSelectionDansTextEditor));
							}
							*/
						}
						repaint();
						break switchBouton;
				}//switchClicCoutn
				

			case MouseEvent.BUTTON3:
				
				//Un clic : on positionne la fin de la selection
				//Deux clic : on annule la selection
				switchClicCount:switch (e.getClickCount()){
					case 1 : 
/*
						int zoneNonAlignableSelected = getIndiceElementAlignementPointe(e);
						if(zoneNonAlignableSelected > -1){
							new PopupSuppressionMarqueurAlignement(
									principale.getModele().getListeAlignement(),
									zoneNonAlignableSelected,
									TemporalSigPanel.this)
							.show(TemporalSigPanel.this,e.getX(),e.getY());
						}
						if(e.getY() > ecartHautEtReglette){
							selection2 = e.getX()*hZoom+offsetStart; 
						}
						*/
						break switchClicCount;
					case 2 : selection2 = -1; selection1 = 0; break switchClicCount;
				}
				break;
			}//switch
		}//mousePressed

		/** Bouton relache : on deselectionne la ligne selectionnee.
		 * on place l'indice fin de selection : 
		 * si il n'est pas assez different de l'indice de depart, on annule la selection */
		@Override
		public void mouseReleased(MouseEvent e) {
			selectedLineIndice = -1;
			if(e.getY() > ecartHautEtReglette){
				if(e.getButton() == MouseEvent.BUTTON1){
					if(getCursor().equals(cursorNormal)){
						int ecartMinimum = 5;
						selection2 = e.getX()*hZoom+offsetStart;
						if(Math.abs(selection1 - selection2) < ecartMinimum){
							selection2 = -1;
						}
	
					}
					else if(getCursor().equals(cursorHand)){
						indiceDebutDeplacement = -1;
						selectedWordIndice = -1;
					} 
				}
				repaint();
			}
		}//mouseReleased

		@Override
		public void mouseDragged(MouseEvent e) {
			if(selectedLineIndice >= 0){
				ListeElement listeElement = aligneur.edit.getListeElement();
				if(listeElement.size() > 0){
					int longueurMotMinimale = hZoom*5;

					long positionLineInSample = (long) (e.getX()*hZoom + offsetStart);
					long positionMin = 0;
					
					if(selectedLineIndice > 0){
						//pour empecher d'aller e gauche de la barre de debut de mot
						Element_Mot motPrecedent = listeElement.getElementAvecDureePrecedent(selectedLineIndice);
						if(motPrecedent != null) {
							positionMin = aligneur.project.words.getSegmentEndFrame(motPrecedent.posInAlign);
						}
						else positionMin = 0;
					}
					positionLineInSample = Math.max((long) (e.getX()*hZoom + offsetStart),positionMin+(long)longueurMotMinimale);


					if(selectedLineIndice < listeElement.size()){
						//pour empecher de depasser la barre de fin du mot suivant
						Element_Mot prochainMot = listeElement.getElementAvecDureeSuivant(selectedLineIndice);

						if(prochainMot != null){
							long positionMax = aligneur.project.words.getSegmentEndFrame(prochainMot.posInAlign) - longueurMotMinimale;
							positionLineInSample = Math.min(positionLineInSample,positionMax);
						}	
					}

					// TODO
//					aligneur.project.words.setEndSample(((Element_Mot)(listeElement.get(selectedLineIndice))).getIndexInAlignement(), positionLineInSample);
					repaint();
				}//if
			} else if (selectedWordIndice != -1) {
				/*
					int valeurDeplacement = (int) ((e.getX()-indiceDebutDeplacement)*hZoom);
					int deltafr = OldAlignment.sample2frame(valeurDeplacement);
					int oldfr = aligneur.project.words.getWordEndFrame(selectedWordIndice);
					int newfr = oldfr+deltafr;
					int minfr = aligneur.project.words.getWordFirstFrame(selectedWordIndice)+1;
					if (newfr<minfr) newfr=minfr;
					int maxfr = aligneur.project.words.getWordFirstFrame(selectedWordIndice+1)-1;
					if (maxfr>=0 && newfr>maxfr) newfr=maxfr;
// TODO
//					aligneur.project.words.setAlignForWord(selectedWordIndice, minfr-1, newfr);
					indiceDebutDeplacement = e.getX();
					*/
					TemporalSigPanel.this.repaint();
			} else {
				if(getCursor().equals(cursorNormal)){
					selection2 = e.getX()*hZoom+offsetStart;
				}
				//Quand c'est un deplacement avec la main : on deplace le signal
				else if(getCursor().equals(cursorHand)){
					int coeffMultipli = 1;
					if(e.isControlDown()){
						coeffMultipli *= 2;
					}
					if(e.isShiftDown()){
						coeffMultipli *= 5;
					}
					int deltax = e.getX() - indiceDebutDeplacement;
					deltax *= coeffMultipli * hZoom;
					translateSignal(deltax);
					indiceDebutDeplacement = e.getX();
				}
				repaint();
			}//else

		}//mouseDragged

		@Override
		public void mouseMoved(MouseEvent e) {
			/* si il y une ligne selectionnee : 
			 * on change le curseur.
			 * sinon, curseur normal.*/
			if (getIndiceBarrePointee(e) >= 0) {
				setCursor(cursorResize);
			}
			else {
				int motidx = getIndiceMotPointe(e);
				if (motidx >= 0){
					setCursor(cursorHand);
				} else {
					setCursor(actualCursor);
				}
			}
		}//mouseMoved

		/**
		 * Pour recuperer l'indice du mot pointee par la souris.
		 */
		private int getIndiceBarrePointee(MouseEvent e){
			int y = e.getY();
			if(   (y < lineBase) && ( y > lineMax) ) {
				int ecartAutorise = 5;
				int panelWidth = getWidth();
				int x = e.getX();
			
				ListeElement listeElement = aligneur.edit.getListeElement();
				
				
				List<Element_Mot> listeMot = listeElement.getMots();
				int listeMotSize = listeMot.size();
				int posiLine = 0;

				int i = 0;
				while ((i < listeMotSize) 
						&& ( (posiLine = (int) ((aligneur.project.words.getSegmentEndFrame(listeMot.get(i).posInAlign)-offsetStart)/hZoom)) < panelWidth) ){
					if( Math.abs(x - posiLine) < ecartAutorise ){
						return listeElement.indexOf(listeMot.get(i));
					}//if 
					i++;
				}//while
			}//if locuteur selected
			return -1;
		}//getSelectedIndice

		
		/**
		 * Pour recuperer l'indice du mot pointee par la souris, 
		 * si il y en a un.
		 */		
		private int getIndiceMotPointe(MouseEvent e){
			int y = e.getY();
			if( (y < lineBase ) && ( y > lineMax) ) {
			
				// sample sous le curseur:
				long sample = e.getX()*hZoom+offsetStart;
				if (aligneur==null||aligneur.project.words==null) return -1;
				int segidx = aligneur.project.words.getSegmentAtFrame(PlayerListener.sample2frame(sample));
				if (segidx>=0) {
					int motidx=0;
					for (Element_Mot m : aligneur.edit.getListeElement().getMots()) {
						if (m.posInAlign==segidx) {
							return motidx;
						}
					}
				} else return -1;
			}//if y
			return -1;
		}//getIndicePointeur


		/** Fonction de gestion de la roulette de la souris.
		 * Deplacement de la roulette vers l'avant = deplacement dans le fichier vers l'avant.
		 * Si ctrl presse : seconde par seconde
		 * Si maj presse : 10s par 10s
		 * si ctrl+maj presse : minute par minute
		 */
		@Override
		public void mouseWheelMoved(MouseWheelEvent e){
			int wheelRotation = e.getWheelRotation();

			//Si ctrl est presse : on deplace seconde par seconde
			if(e.isControlDown()){
					int deplacement=100;
					//si ctrl + maj => minute par minute
					if(e.isShiftDown()){
						deplacement *= 60;
					}
					deplacement *= wheelRotation;
					offsetStart -= deplacement;
			}
			//Si shift est maintenu, on se deplace 10s par 10s
			else if (e.isShiftDown()){
				int deplacement=1000;
				deplacement *= wheelRotation;
				offsetStart -= deplacement;
			}
			else {
				offsetStart -= hZoom*e.getUnitsToScroll()*2;
			}
			checkOffsetStart();
			TemporalSigPanel.this.repaint();
			TemporalSigPanel.this.notifyObserver();
		}
		
		
	}//class CustomMouseMotionListener	
	
	public int getFrameRate() {
		return (int) aligneur.audiobuf.getFrameRate();
	}
	public long getEndSelection() {
		return selection2;
	}
	public long getStartSelection() {
		return selection1;
	}

	private class BarProgresserThread extends Thread {
		public boolean goon=true;
		public void run() {
			/*
			try {
				while (goon) {
					Thread.sleep(10);
					long cursample = aligneur.player.getLastSamplePlayed();
					if (cursample<0) continue;
//					cursample-=5000; // decalage visible... ?!
					setProgressBar(cursample);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
		}
	}
	BarProgresserThread barProgresser = null;
	public void moveAtFrame(int fr) {
		long sa = OldAlignment.frame2sample(fr);
		setProgressBar(sa);
	}
	public void replayFrom(long startSample) {
		if (barProgresser!=null) barProgresser.goon=false;
		setProgressBar(startSample);
		barProgresser = new BarProgresserThread();
		barProgresser.start();
	}
	public void stopPlaying() {
		if (barProgresser!=null) barProgresser.goon=false;
	}
	public void removeAndDestroy() {
		aligneur.sigPanel=null;
	}
}//class TemporalSigPanel
