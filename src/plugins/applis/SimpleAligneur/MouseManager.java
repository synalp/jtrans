/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis.SimpleAligneur;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

public class MouseManager {
	static Point lastMouseClicPos;
	static JPopupMenu clicMenu = null;
	
	public MouseManager(final Aligneur principale) {
		// listener clic mouse
		// attention: capture aussi les "setCaret" generes par les operations de coloriage !
		// il faudrait plutot utiliser un vrai listener de mouse, qui recupere ensuite la Caret position !
		// pour reagir a un clic de souris, il faut ensuite utiliser une operation de coloriage prioritaire
		// puis attendre qu'elle soit terminee, mais il ne faut surtout pas reagir en modifiant le caret !

		final MouseListener[] ml = principale.edit.getMouseListeners();
		for (MouseListener l : ml) principale.edit.removeMouseListener(l);

		principale.edit.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
//				removeClicMenu();
				if (!principale.caretSensible) {
					for (MouseListener l : ml) l.mouseReleased(e);
					return;
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (!principale.caretSensible) {
					for (MouseListener l : ml) l.mousePressed(e);
					return;
				}
				int clicCaret = principale.edit.viewToModel(e.getPoint());
				lastMouseClicPos = e.getLocationOnScreen();
				principale.clicAtCaretPosition(clicCaret,e.getButton());
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				if (!principale.caretSensible) {
					for (MouseListener l : ml) l.mouseExited(e);
					return;
				}
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				if (!principale.caretSensible) {
					for (MouseListener l : ml) l.mouseEntered(e);
					return;
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!principale.caretSensible) {
					for (MouseListener l : ml) l.mouseClicked(e);
					return;
				}
			}
		});
	}
	
	private static void removeClicMenu() {
		if (clicMenu!=null) {
			clicMenu.setEnabled(false);
			clicMenu.setVisible(false);
			clicMenu=null;
		}
	}
	
	public static void clicMotMenu(final Aligneur a, final int mot) {
		removeClicMenu();
	}
}
