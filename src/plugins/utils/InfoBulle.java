/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils;

import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class InfoBulle extends JFrame {
	public static boolean isShown = false;
	public static Window preparent = null;
	public InfoBulle(String msg, Window parent) {
		super("info: "+msg);
		if (parent==null) {
			if (preparent!=null && !preparent.hasFocus()) setFocusableWindowState(false);
		} else if (!parent.hasFocus()) setFocusableWindowState(false);
		System.err.println("info msg "+msg);
		JLabel l = new JLabel("info: "+msg);
		add(l);
		int w = l.getPreferredSize().width;
		int h = l.getPreferredSize().height;
		setSize(w+20,h+50);
		isShown=true;
		setVisible(true);		
	}
	public void dextroy() {
		isShown=false;
		dispose();
	}
}
