/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 * utiliser plutot JOptionPane a la place ?!
 * @author cerisara
 *
 */
public class TextInputWindow extends JFrame {
	public TextInputWindow(String msg, final UserInputProcessor proc) {
		super(msg);
		getContentPane().setLayout(new FlowLayout());
		final JTextField tt = new JTextField(15);
		getContentPane().add(tt);
		JButton ok = new JButton("OK");
		getContentPane().add(ok);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				proc.processInput(tt.getText());
				dispose();
			}
		});
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(200,100);
		setVisible(true);
	}
}
