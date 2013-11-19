/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text;

import java.awt.Color;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import plugins.text.elements.Element_Mot;

/**
 * Contient un ordre pour colorier une partie de texte
 * Gere egalement les priorites de coloriage
 * 
 * @author xtof
 *
 */
public class ColoriageEvent implements Comparable<ColoriageEvent> {
	
	final static public ColoriageEvent endofthread = new ColoriageEvent(null, null, null, true, 0);
	
	// plus c'est grand, plus c'est prioritaire
	int priority = 1;
	Element_Mot seldeb, selfin;
	int posdeb=-1,posfin;
	Color c;
	boolean background;
	private StyleContext styler = StyleContext.getDefaultStyleContext();
	
	public ArrayBlockingQueue<Boolean> coloriageDone = new ArrayBlockingQueue<Boolean>(1);
	
	public void waitForColoriageDone() throws InterruptedException {
		coloriageDone.take();
	}

	public ColoriageEvent(int deb, int fin, Color col, boolean surligne, int priority) {
		posdeb=deb; posfin=fin;
		c=col;
		background=surligne;
		this.priority=priority;
	}
	public ColoriageEvent(Element_Mot mot1, Element_Mot mot2, Color col, boolean surligne, int priority) {
		seldeb=mot1; selfin=mot2;
		c=col;
		background=surligne;
		this.priority=priority;
	}
	
	public void colorie(JTextPane textpane) {
		synchronized(textpane) {
			try {
				if (posdeb>=0) {
					textpane.setCaretPosition(posdeb);
					textpane.moveCaretPosition(posfin);
				} else {
					textpane.setCaretPosition(seldeb.start);
					textpane.moveCaretPosition(selfin.end);
				}
			} catch (Exception e) {
				// un problème dans l'affichage !
				return;
			}
			AttributeSet a = textpane.getCharacterAttributes();
			// Color: R G B
			AttributeSet b;
			if (!background)
				b = styler.addAttribute(a, StyleConstants.ColorConstants.Foreground, c);
			else
				b = styler.addAttribute(a, StyleConstants.ColorConstants.Background, c);
			textpane.setCharacterAttributes(b, false);
			textpane.repaint();
		}
	}
	
	@Override
	public int compareTo(ColoriageEvent o) {
		if (priority>o.priority) return 1;
		else if (priority<o.priority) return -1;
		else return 0;
	}
	
}
