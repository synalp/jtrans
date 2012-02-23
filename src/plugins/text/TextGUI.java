/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.text;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import plugins.text.TexteEditor;

public class TextGUI extends JFrame {
	public TexteEditor edit;
	
	public TextGUI() {
		super("text editor");
		edit = new TexteEditor();
		JScrollPane scrollPane = new JScrollPane(edit,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane);
		menus();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(600,500);
		setVisible(true);
	}

	private void menus() {
		JMenuBar menubar = new JMenuBar();
		JMenu actionsm = new JMenu("actions");
		menubar.add(actionsm);
		JMenuItem parse = new JMenuItem("parse");
		actionsm.add(parse);
		JMenuItem editb = new JMenuItem("edit text");
		actionsm.add(editb);
		setJMenuBar(menubar);
		
		parse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edit.setEditable(false);
				edit.reparse();
			}
		});
		editb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edit.setEditable(true);
			}
		});
	}
	
	public static void main(String args[]) {
		TextGUI m = new TextGUI();
	}
}
