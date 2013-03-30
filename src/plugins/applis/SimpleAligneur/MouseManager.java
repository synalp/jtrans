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
		
		/*
		principale.edit.getCaret().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!principale.caretSensible) return;
				principale.caretSensible=false;
				int posCaret = ((Caret)e.getSource()).getDot();
				Element_Mot mot = principale.edit.getListeElement().getMotAtTextPosi(posCaret);
				int motidx = principale.edit.getListeElement().indiceMot;

				principale.edit.getCaret().setSelectionVisible(false);

				if (mot!=null) {
					if (principale.player!=null&&principale.player.isPlaying()) {
						principale.insertManualAnchor(motidx);
						// mon pas encore align� !
						// principale.edit.griseMotred(mot);
						principale.edit.souligne(mot);
					} else {
						if (principale.kmgr.isShiftOn) {
							principale.textSelection(motidx);
						} else {
							int frdeb = principale.alignement.getFrameDeb(motidx);
							principale.wordSelectedIdx=motidx;
							if (frdeb>=0) {
								if (principale.sigPanel!=null)
									principale.sigPanel.moveAtFrame(frdeb);
								principale.edit.griseMot(mot);
							} else {
								// mon pas encore align� !
								principale.edit.griseMotred(mot);
							}
						}
					}
				}
				principale.caretSensible=true;
			}
		});
*/
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
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				JPopupMenu clicmenu = new JPopupMenu("actions on word");
				JMenuItem setTime = new JMenuItem("set Time Deb");
				setTime.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						removeClicMenu();
						String s = JOptionPane.showInputDialog("Absolute Time in seconds:");
						try {
							Float tdeb = Float.parseFloat(s);
							a.doForceAnchor(tdeb, mot);
						} catch (Exception ee) {
						}
					}
				});
				clicmenu.add(setTime);
				JMenuItem cancel = new JMenuItem("cancel");
				cancel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						removeClicMenu();
					}
				});
				clicmenu.add(cancel);
				clicmenu.setSize(100, 300);
				clicmenu.setLocation(lastMouseClicPos);
				clicmenu.setVisible(true);
				clicMenu=clicmenu;
			}
		});
		t.start();
	}
}
