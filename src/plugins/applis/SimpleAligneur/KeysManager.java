/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.applis.SimpleAligneur;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import plugins.speechreco.aligners.sphiinx4.S4ForceAlignBlocViterbi;

public class KeysManager {
	Aligneur aligneur;
	public boolean isShiftOn = false, isControlOn = false;

	public KeysManager(Aligneur main) {
		aligneur=main;
		// TODO: tester si on perd la frame/applet perd le focus pour mettre a jour les control
		if (aligneur.jf!=null) {
			aligneur.jf.addWindowFocusListener(new WindowFocusListener() {
				@Override
				public void windowLostFocus(WindowEvent e) {
					isControlOn=isShiftOn=false;
				}
				@Override
				public void windowGainedFocus(WindowEvent e) {
					isControlOn=isShiftOn=false;
				}
			});
		}

		final KeyboardFocusManager focusm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusm.addKeyEventDispatcher(new KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent e) {
				try {
					// TODO: je n'ai pas trouve d'autres moyens pour savoir si
					// c'est un keypressed ou keyrelease !!
					String parms = e.paramString();

					if (parms.startsWith("KEY_RELEASED,")) {
						// cas particulier du SHIFT
						switch (e.getKeyCode()) {
						case 16:
							isShiftOn=false;
							break;
						case 17:
							System.out.println("KMGR set control off");
							isControlOn=false;
							break;
						}
						return true;
					}


					if (!parms.startsWith("KEY_PRESSED,"))
						return false;
					switch (e.getKeyCode()) {
					case 80: // lettre "p"
						aligneur.restartPlaying();
						break;
					case 113: // F2
						aligneur.alignManual();
						break;
					case 116: // F5
//						aligneur.s4fastAutoAlign();
						break;
					case 117: // F6
						break;
					case 27: // ESC
						aligneur.stopAll();
//						if (aligneur.userShouldConfirm) aligneur.confirmAlign(false);
						break;
					case 10: // Enter
//						if (aligneur.userShouldConfirm) aligneur.confirmAlign(true);
//						else if (aligneur.userShouldAlign) aligneur.manualAlign();
						break;
					case 16: // Shift
						isShiftOn=true;
						break;
					case 17:
						if (!isControlOn)
							System.out.println("KMGR set control on");
						isControlOn=true;
						break;
					case 35: // END
						aligneur.goToLastAlignedWord();
						return true;
					case 36: // HOME
						aligneur.goHome();
						break;
					case 37: // fleche gauche
						break;
					case 39: // fleche droite
						break;
					case 77: // lettre M
						S4ForceAlignBlocViterbi.silprob *= 0.1f;
						System.out.println("SIL INS PROB "+S4ForceAlignBlocViterbi.silprob);
						break;
					case 78: // lettre N
						S4ForceAlignBlocViterbi.silprob /= 0.1f;
						System.out.println("SIL INS PROB "+S4ForceAlignBlocViterbi.silprob);
						break;
					case 88: // lettre X
						aligneur.debug();
					default:
						System.err.println("key " + e.getKeyCode());
					}
					return false;
				} catch (Exception ee) {
					return false;
				}
			}
		});

	}
}
